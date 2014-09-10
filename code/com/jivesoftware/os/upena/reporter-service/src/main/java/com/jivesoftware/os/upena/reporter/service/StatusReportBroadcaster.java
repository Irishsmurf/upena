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
package com.jivesoftware.os.upena.reporter.service;

import com.jivesoftware.os.jive.utils.base.service.ServiceHandle;
import com.jivesoftware.os.jive.utils.logger.LoggerSummary;
import com.jivesoftware.os.upena.reporter.shared.StatusReport;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class StatusReportBroadcaster implements ServiceHandle {

    static public interface StatusReportCallback {

        void annouce(StatusReport statusReport) throws Exception;
    }
    private ScheduledExecutorService newScheduledThreadPool;
    private final int startupTimestampInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    private final String uuid = UUID.randomUUID().toString();
    private final Map<String,String> instance;
    private final long annouceEveryNMills;
    private final StatusReportCallback statusReportCallback;

    public StatusReportBroadcaster(Map<String,String> instance,
        long annouceEveryNMills,
        StatusReportCallback statusReportCallback) {
        this.instance = instance;
        this.annouceEveryNMills = annouceEveryNMills;
        this.statusReportCallback = statusReportCallback;
    }

    @Override
    synchronized public void start() throws SocketException {
        ArrayList<String> ipAddrs = new ArrayList<>();
        ArrayList<String> hostnames = new ArrayList<>();
        scanNics(ipAddrs, hostnames);

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        StatusReport statusReport = new StatusReport(
                uuid,
                new File("." + File.separator).getAbsolutePath(),
                runtimeMXBean.getVmName(),
                runtimeMXBean.getVmVendor(),
                runtimeMXBean.getVmVersion(),
                hostnames,
                ipAddrs,
                instance,
                (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                startupTimestampInSeconds,
                0.0f,
                0.0f,
                LoggerSummary.INSTANCE.errors,
                LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.errors);

        newScheduledThreadPool = Executors.newScheduledThreadPool(1);
        if (annouceEveryNMills > 0) {
            newScheduledThreadPool.scheduleWithFixedDelay(
                    new StatusReportTask(statusReport, statusReportCallback),
                    0,
                    annouceEveryNMills,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    synchronized public void stop() {
        if (newScheduledThreadPool != null) {
            newScheduledThreadPool.shutdownNow();
        }
        newScheduledThreadPool = null;
    }

    synchronized public boolean isTerminated() {
        return newScheduledThreadPool == null || newScheduledThreadPool.isTerminated();
    }

    private static class StatusReportTask implements Runnable {

        private final List<GarbageCollectorMXBean> garbageCollectors;
        private final OperatingSystemMXBean osBean;
        long lastGCTotalTime;
        private final StatusReportCallback statusReportCallback;
        private final StatusReport statusReport;

        public StatusReportTask(StatusReport statusReport,
                StatusReportCallback statusReportCallback) {
            this.statusReportCallback = statusReportCallback;
            this.statusReport = statusReport;

            garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
            osBean = ManagementFactory.getOperatingSystemMXBean();
        }

        @Override
        public void run() {
            try {

                long totalTimeInGC = 0;
                for (GarbageCollectorMXBean gc : garbageCollectors) {
                    totalTimeInGC += gc.getCollectionTime();
                }

                statusReport.percentageOfCPUTimeInGC = ((float) (totalTimeInGC - lastGCTotalTime) / (float) lastGCTotalTime);
                lastGCTotalTime = totalTimeInGC;
                statusReport.timestampInSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                statusReport.load = (float) osBean.getSystemLoadAverage();
                statusReport.internalErrors = LoggerSummary.INSTANCE.errors;
                statusReport.interactionErrors = LoggerSummary.INSTANCE_EXTERNAL_INTERACTIONS.errors;

                statusReportCallback.annouce(statusReport);

            } catch (Exception x) {
                // We use system err to break recursion created by calling LOG.*
                System.err.println("Failed to broadcast to " + statusReportCallback + x);
            }

        }
    }

    private static void scanNics(List<String> ipAddrs, List<String> hostnames) throws SocketException {

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> ina = ni.getInetAddresses();
            while (ina != null && ina.hasMoreElements()) {
                InetAddress a = ina.nextElement();
                if (a.isAnyLocalAddress()
                        || a.isLoopbackAddress()
                        || a.isLinkLocalAddress()) {
                    continue;
                }
                ipAddrs.add(a.getHostAddress());
                if (a.getHostName() != null && a.getHostName().length() > 0) {
                    if (!a.getHostAddress().equals(a.getHostName())) {
                        hostnames.add(a.getHostName());
                    }
                }
            }
        }
    }
}
