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

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.Test;

public class InMemoryConnectionsDescriptorsProviderTest {

    @Test
    public void testGetConnections() throws Exception {

        InMemoryConnectionsDescriptorsProvider connectionDescriptorsProvider = new InMemoryConnectionsDescriptorsProvider(null);
        ConnectionDescriptor got = connectionDescriptorsProvider.get(null, null, null, null);
        Assert.assertNull(got);

        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertNull(got);

        ConnectionDescriptorsRequest requestConnections = new ConnectionDescriptorsRequest("tenantId", "instanceId", "connectToServiceNamed", "portName");
        ConnectionDescriptorsResponse response = connectionDescriptorsProvider.requestConnections(requestConnections);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getConnections());
        Assert.assertTrue(response.getConnections().isEmpty());
        Assert.assertEquals(response.getReleaseGroup(), "default");

        ConnectionDescriptor a = new ConnectionDescriptor("a", 1, new HashMap<String, String>());
        connectionDescriptorsProvider.set("tenantId", "instanceId", "connectToServiceNamed", "portName", a);
        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertEquals(got, a);

        response = connectionDescriptorsProvider.requestConnections(requestConnections);
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getConnections());
        Assert.assertTrue(response.getConnections().size() == 1);
        Assert.assertEquals(response.getConnections().get(0), a);
        Assert.assertEquals(response.getReleaseGroup(), "overridden");

        connectionDescriptorsProvider.clear("tenantId", "instanceId", "connectToServiceNamed", "portName");

        got = connectionDescriptorsProvider.get("tenantId", "instanceId", "connectToServiceNamed", "portName");
        Assert.assertNull(got);

    }

}