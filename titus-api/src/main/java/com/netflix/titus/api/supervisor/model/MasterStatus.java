/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.titus.api.supervisor.model;

import java.util.Objects;

public class MasterStatus {

    private final MasterState state;
    private final String message;
    private final long timestamp;

    public MasterStatus(MasterState state, String message, long timestamp) {
        this.state = state;
        this.message = message;
        this.timestamp = timestamp;
    }

    public MasterState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MasterStatus that = (MasterStatus) o;
        return timestamp == that.timestamp &&
                state == that.state &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, message, timestamp);
    }

    @Override
    public String toString() {
        return """
                MasterStatus{\
                state=\
                """ + state +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public Builder toBuilder() {
        return newBuilder().withState(state).withMessage(message).withTimestamp(timestamp);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private MasterState state;
        private String message;
        private long timestamp;

        private Builder() {
        }

        public Builder withState(MasterState state) {
            this.state = state;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder but() {
            return newBuilder().withState(state).withMessage(message).withTimestamp(timestamp);
        }

        public MasterStatus build() {
            return new MasterStatus(state, message, timestamp);
        }
    }
}
