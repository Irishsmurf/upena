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
package com.jivesoftware.os.upena.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenant implements Stored<Tenant>, Serializable {

    public final String tenantId;
    public final String description;
    public final Map<ServiceKey, ReleaseGroupKey> overrideReleaseGroups;

    @JsonCreator
    public Tenant(@JsonProperty("tenantId") String tenantId,
        @JsonProperty("description") String description,
        @JsonProperty("overrideReleaseGroups") Map<ServiceKey, ReleaseGroupKey> overrideReleaseGroups) {
        this.tenantId = tenantId;
        this.description = description;
        this.overrideReleaseGroups = overrideReleaseGroups;
    }

    @Override
    public String toString() {
        return "Tenant{"
            + "tenantId=" + tenantId
            + ", description=" + description
            + ", overrideReleaseGroups=" + overrideReleaseGroups
            + '}';
    }

    @Override
    public int compareTo(Tenant o) {
        return tenantId.compareTo(o.tenantId);
    }
}
