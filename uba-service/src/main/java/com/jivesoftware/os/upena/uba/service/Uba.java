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

import com.jivesoftware.os.upena.routing.shared.InstanceDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class Uba {

    final String host;
    final String upenaHost;
    final int upenaPort;
    private final UbaTree ubaTree;
    private final DeployableScriptInvoker invokeScript;
    private final UbaLog ubaLog;

    public Uba(String host, String upenaHost, int upenaPort, UbaTree ubaTree, UbaLog ubaLog) {
        this.host = host;
        this.upenaHost = upenaHost;
        this.upenaPort = upenaPort;
        this.ubaTree = ubaTree;
        this.invokeScript = new DeployableScriptInvoker(Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicLong count = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                long id = count.incrementAndGet();
                return new Thread(r, "Script onvoker thread-" + id);
            }
        }));
        this.ubaLog = ubaLog;
    }

    public Map<InstanceDescriptor, InstancePath> getOnDiskInstances() {
        final Map<InstanceDescriptor, InstancePath> instances = new ConcurrentHashMap<>();
        ubaTree.build(new UbaTree.ConductorPathCallback() {
            @Override
            public void conductorPath(NameAndKey[] path) {
                InstancePath instancePath = new InstancePath(ubaTree.getRoot(), path);
                try {
                    if (instancePath.instanceProperties().exists()) {
                        InstanceDescriptor id = instancePath.readInstanceDescriptor();
                        instances.put(id, instancePath);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(); //hmmm
                }
            }
        });
        return instances;
    }

    public String instanceDescriptorKey(InstanceDescriptor instanceDescriptor) {
        StringBuilder key = new StringBuilder();
        key.append(instanceDescriptor.clusterKey).append('|');
        key.append(instanceDescriptor.serviceKey).append('|');
        key.append(instanceDescriptor.releaseGroupKey).append('|');
        key.append(instanceDescriptor.instanceKey);
        return key.toString();
    }

    Nanny newNanny(InstanceDescriptor instanceDescriptor) {
        InstancePath instancePath = new InstancePath(ubaTree.getRoot(), new NameAndKey[]{
            new NameAndKey(instanceDescriptor.clusterName, instanceDescriptor.clusterKey),
            new NameAndKey(instanceDescriptor.serviceName, instanceDescriptor.serviceKey),
            new NameAndKey(instanceDescriptor.releaseGroupName, instanceDescriptor.releaseGroupKey),
            new NameAndKey(Integer.toString(instanceDescriptor.instanceName), instanceDescriptor.instanceKey)
        });

        DeployLog deployLog = new DeployLog();
        HealthLog healthLog = new HealthLog(deployLog);
        return new Nanny(instanceDescriptor, instancePath, new DeployableValidator(), new DeployLog(), healthLog, invokeScript, ubaLog);
    }

}