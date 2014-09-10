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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class Service implements Stored<Service>, Serializable {

    public final String name;
    public final String repository;
    public final String description;

    @JsonCreator
    public Service(@JsonProperty ("name") String name,
        @JsonProperty ("description") String description,
        @JsonProperty ("repository") String repository) {
        this.name = name;
        this.repository = repository;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Service{"
            + "name=" + name
            + ", description=" + description
            + ", repository=" + repository
            + '}';
    }

    @Override
    public int compareTo(Service o) {
        return name.compareTo(o.name);
    }
}
