/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.titus.api.appscale.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricDimension {

    private final String name;
    private final String value;

    public MetricDimension(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public String getValue() {
        return value;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String value;

        private Builder() {
        }

        public static Builder aMetricDimension() {
            return new Builder();
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withValue(String value) {
            this.value = value;
            return this;
        }

        public MetricDimension build() {
            return new MetricDimension(name, value);
        }
    }
}
