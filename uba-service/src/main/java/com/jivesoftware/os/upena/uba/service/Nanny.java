/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.uba.service;

import com.google.common.cache.Cache;
import com.jivesoftware.os.jive.utils.shell.utils.Curl;
import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import com.jivesoftware.os.routing.bird.shared.InstanceDescriptor;
import com.jivesoftware.os.uba.shared.NannyReport;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Nanny {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();

    private final RepositoryProvider repositoryProvider;
    private final InstancePath instancePath;
    private final DeployableValidator deployableValidator;
    private final DeployLog deployLog;
    private final HealthLog healthLog;
    private final DeployableScriptInvoker invokeScript;
    private final AtomicBoolean redeploy;
    private final AtomicBoolean destroyed;
    private final AtomicReference<InstanceDescriptor> instanceDescriptor;
    private final LinkedBlockingQueue<Runnable> linkedBlockingQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final AtomicLong lastRestart = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong restartAtTimestamp = new AtomicLong(-1);
    private final AtomicLong startupTimestamp = new AtomicLong(-1);
    private final UbaLog ubaLog;
    private final Cache<InstanceDescriptor, Boolean> haveRunConfigExtractionCache;
    private final AtomicReference<String> status = new AtomicReference<>("");
    final AtomicLong lastStartupId = new AtomicLong(-1);
    final AtomicLong startupId = new AtomicLong(0);
    final AtomicLong unexpectedRestartTimestamp = new AtomicLong(-1);

    public Nanny(RepositoryProvider repositoryProvider,
        InstanceDescriptor instanceDescriptor,
        InstancePath instancePath,
        DeployableValidator deployableValidator,
        DeployLog deployLog,
        HealthLog healthLog,
        DeployableScriptInvoker invokeScript,
        UbaLog ubaLog,
        Cache<InstanceDescriptor, Boolean> haveRunConfigExtractionCache) {

        this.repositoryProvider = repositoryProvider;
        this.instanceDescriptor = new AtomicReference<>(instanceDescriptor);
        this.instancePath = instancePath;
        this.deployableValidator = deployableValidator;
        this.deployLog = deployLog;
        this.healthLog = healthLog;
        this.invokeScript = invokeScript;
        this.ubaLog = ubaLog;
        linkedBlockingQueue = new LinkedBlockingQueue<>(10);
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, linkedBlockingQueue);
        boolean exists = instancePath.deployLog().exists();
        LOG.info("Stats script for {} exists == {}", instanceDescriptor, exists);
        redeploy = new AtomicBoolean(!exists);
        destroyed = new AtomicBoolean(false);
        this.haveRunConfigExtractionCache = haveRunConfigExtractionCache;
    }

    public String getStatus() {
        return status.get();
    }

    public long getUnexpectedRestartTimestamp() {
        return unexpectedRestartTimestamp.get();
    }

    public InstanceDescriptor getInstanceDescriptor() {
        return instanceDescriptor.get();
    }

    public long getStartTimeMillis() {
        return startupTimestamp.longValue();
    }

    synchronized public void setInstanceDescriptor(InstanceDescriptor id) {
        InstanceDescriptor got = instanceDescriptor.get();
        if (got != null && !got.equals(id)) {
            startupId.incrementAndGet();
            unexpectedRestartTimestamp.set(-1);
            redeploy.set(true);
            LOG.info("Instance changed from " + got + " to " + id);
        } else if (!instancePath.script("status").exists()) {
            startupId.incrementAndGet();
            unexpectedRestartTimestamp.set(-1);
            redeploy.set(true);
            LOG.info("Missing status script from " + got + " to " + id);
        }
        if (!redeploy.get()) {
            LOG.debug("Service:" + instancePath.toHumanReadableName() + " has NOT changed.");
        } else {
            instanceDescriptor.set(id);
        }
        if (id.restartTimestampGMTMillis > lastRestart.get()) {
            restartAtTimestamp.set(id.restartTimestampGMTMillis);
        }
    }

    public DeployLog getDeployLog() {
        return deployLog;
    }

    public HealthLog getHealthLog() {
        return healthLog;
    }

    public NannyReport report() {
        return new NannyReport(deployLog.getState(), instanceDescriptor.get(), deployLog.commitedLog());
    }

    synchronized public String nanny(String datacenter,
        String rack,
        String publicHostName,
        String host,
        String upenaHost,
        int upenaPort) throws InterruptedException, ExecutionException {

        long now = System.currentTimeMillis();
        if (restartAtTimestamp.get() > 0 && restartAtTimestamp.get() < now) {
            status.set("Restarting");
            deployLog.log("Nanny", "Restart triggered by timestamp. " + this, null);
            if (kill()) {
                lastRestart.set(now);
                restartAtTimestamp.set(-1);
            }
        }

        try {
            if (destroyed.get()) {
                status.set("Destroy");
                deployLog.log("Nanny", "tried to check a service that has been destroyed. " + this, null);
                return deployLog.getState();
            }
            if (linkedBlockingQueue.size() == 0) {
                try {
                    if (redeploy.get()) {
                        status.set("Redeploying");
                        NannyDestroyCallable destroyTask = new NannyDestroyCallable(instanceDescriptor.get(),
                            instancePath,
                            deployLog,
                            healthLog,
                            invokeScript,
                            ubaLog);

                        deployLog.log("Nanny", "destroying in preperation to redeploy. " + this, null);
                        Future<Boolean> destroyFuture = threadPoolExecutor.submit(destroyTask);
                        status.set("Waiting for destroy...");
                        if (destroyFuture.get()) {

                            NannyDeployCallable deployTask = new NannyDeployCallable(repositoryProvider,
                                datacenter,
                                rack,
                                publicHostName,
                                host,
                                upenaHost,
                                upenaPort,
                                instanceDescriptor.get(),
                                instancePath,
                                deployLog,
                                healthLog,
                                deployableValidator,
                                invokeScript,
                                ubaLog);
                            deployLog.log("Nanny", "redeploying. " + this, null);
                            Future<Boolean> deployedFuture = threadPoolExecutor.submit(deployTask);
                            try {
                                status.set("Waiting for deploy...");
                                if (deployedFuture.get()) {
                                    try {
                                        try (FileWriter writer = new FileWriter(instancePath.deployLog())) {
                                            for (String line : deployLog.peek()) {
                                                writer.write(line);
                                            }
                                        }
                                        redeploy.set(false);
                                        status.set("Successfully redeployed");
                                        deployLog.log("Nanny", " successfully redeployed. " + this, null);
                                    } catch (Exception x) {
                                        status.set("Failed redeployed");
                                        deployLog.log("Nanny", " failed to redeployed. " + this, x);
                                    }
                                }
                            } catch (ExecutionException ee) {
                                status.set("Unexpected state");
                                deployLog.log("Nanny", " Encountered an unexpected condition. " + this, ee);
                            }
                        }
                    }

                    NannyStatusCallable nannyTask = new NannyStatusCallable(this,
                        status,
                        startupTimestamp,
                        instanceDescriptor.get(),
                        instancePath,
                        deployLog,
                        healthLog,
                        invokeScript,
                        ubaLog,
                        haveRunConfigExtractionCache);
                    if (nannyTask.callable()) {
                        threadPoolExecutor.submit(nannyTask);
                    } else {
                        deployLog.log("Nanny", "skipped status check. " + this, null);
                    }

                } catch (InterruptedException | ExecutionException x) {
                    deployLog.log("Nanny", " is already running. " + this, x);
                }
                return deployLog.getState();
            } else {
                return deployLog.getState();
            }
        } finally {
            deployLog.commit();
        }
    }

    synchronized Boolean destroy() throws InterruptedException, ExecutionException {
        destroyed.set(true);
        startupId.incrementAndGet();
        unexpectedRestartTimestamp.set(-1);
        NannyDestroyCallable nannyTask = new NannyDestroyCallable(instanceDescriptor.get(),
            instancePath,
            deployLog,
            healthLog,
            invokeScript,
            ubaLog);
        Future<Boolean> waitForDestory = threadPoolExecutor.submit(nannyTask);
        Boolean result = waitForDestory.get();
        nannyTask.wipeoutFiles();
        return result;

    }

    synchronized Boolean kill() throws InterruptedException, ExecutionException {
        status.set("Killing");
        startupId.incrementAndGet();
        unexpectedRestartTimestamp.set(-1);
        NannyDestroyCallable nannyTask = new NannyDestroyCallable(instanceDescriptor.get(),
            instancePath,
            deployLog,
            healthLog,
            invokeScript,
            ubaLog);
        Future<Boolean> waitForDestory = threadPoolExecutor.submit(nannyTask);
        status.set("Waiting kill to complete...");
        Boolean result = waitForDestory.get();
        status.set("Killed");
        return result;

    }

    String invalidateRouting(String tenantId) {
        try {
            LOG.info("invalidating tenant routing for tenatId:" + tenantId + " on " + this);
            StringBuilder curl = new StringBuilder();
            curl.append("localhost:");
            curl.append(instanceDescriptor.get().ports.get("manage"));
            curl.append("/tenant/routing/invalidate?");
            curl.append("tenantId=").append(tenantId).append('&');
            curl.append("connectToServiceId=*").append('&');
            curl.append("portName=*");

            String response = Curl.create().curl(curl.toString());
            LOG.info(response);
            return response;
        } catch (IOException x) {
            LOG.warn("failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this);
            return "failed to invalidate tenant routing for tenantId:" + tenantId + " on " + this;
        }
    }

    public void stop() {
        threadPoolExecutor.shutdownNow();
    }

    @Override
    public String toString() {
        return "Nanny{"
            + "instancePath=" + instancePath
            + ", instanceDescriptor=" + instanceDescriptor
            + '}';
    }
}
