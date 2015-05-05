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
package com.jivesoftware.os.upena.routing.shared;

import com.jivesoftware.os.mlogger.core.MetricLogger;
import com.jivesoftware.os.mlogger.core.MetricLoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantsServiceConnectionDescriptorProvider<T> {

    private static final MetricLogger LOG = MetricLoggerFactory.getLogger();
    private final String instanceId;
    private final ConnectionDescriptorsProvider connectionsProvider;
    private final String connectToServiceNamed;
    private final String portName;
    private final Map<String, ConnectionDescriptors> userIdsConnectionDescriptors = new ConcurrentHashMap<>();
    private final Map<T, String> tenantToUserId = new ConcurrentHashMap<>();

    public TenantsServiceConnectionDescriptorProvider(String instanceId,
            ConnectionDescriptorsProvider connectionsProvider,
            String connectToServiceNamed,
            String portName) {
        this.instanceId = instanceId;
        this.connectionsProvider = connectionsProvider;
        this.connectToServiceNamed = connectToServiceNamed;
        this.portName = portName;
    }

    void invalidateAll() {
        tenantToUserId.clear();
        userIdsConnectionDescriptors.clear();
    }

    public void invalidateTenant(T tenantId) {
        tenantToUserId.remove(tenantId);
    }

    public TenantsRoutingServiceReport getRoutingReport() {
        TenantsRoutingServiceReport report = new TenantsRoutingServiceReport();
        report.tenantToUserId.putAll(tenantToUserId);
        report.userIdsConnectionDescriptors.putAll(userIdsConnectionDescriptors);
        return report;
    }

    public ConnectionDescriptors getConnections(T tenantId) {
        if (tenantId == null) {
            return new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
        }
        ConnectionDescriptors connections;
        String releaseGroup = tenantToUserId.get(tenantId);
        if (releaseGroup != null) {
            connections = userIdsConnectionDescriptors.get(releaseGroup);
            if (connections == null) {
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            }
        } else {
            ConnectionDescriptorsResponse connectionsResponse = connectionsProvider.requestConnections(new ConnectionDescriptorsRequest(
                    tenantId.toString(), instanceId, connectToServiceNamed, portName));
            if (connectionsResponse == null) {
                releaseGroup = "unknown";
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            } else if (connectionsResponse.getReturnCode() < 0) {
                releaseGroup = "unknown";
                LOG.warn(Arrays.deepToString(connectionsResponse.getMessages().toArray()));
                connections = new ConnectionDescriptors(System.currentTimeMillis(), Collections.<ConnectionDescriptor>emptyList());
            } else {
                releaseGroup = connectionsResponse.getReleaseGroup();
                connections = new ConnectionDescriptors(System.currentTimeMillis(), connectionsResponse.getConnections());
            }
            tenantToUserId.put(tenantId, releaseGroup);
            userIdsConnectionDescriptors.put(releaseGroup, connections);
        }
        return connections;
    }
}