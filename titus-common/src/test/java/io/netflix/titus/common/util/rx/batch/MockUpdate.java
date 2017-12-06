/*
 * Copyright 2017 Netflix, Inc.
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

package io.netflix.titus.common.util.rx.batch;

import java.time.Instant;

class MockUpdate implements Update<String> {
    private final Priority priority;
    private final Instant timestamp;
    private final String resourceId;
    private final String subResourceId;
    private final String state;
    private final String identifier;

    MockUpdate(Priority priority, Instant timestamp, String resourceId, String subResourceId, String state) {
        this.priority = priority;
        this.timestamp = timestamp;
        this.resourceId = resourceId;
        this.subResourceId = subResourceId;
        this.state = state;
        this.identifier = resourceId + "-" + subResourceId;
    }

    /**
     * Updates are applied to a (resourceId, subResourceId) pair.
     *
     * @return the resourceId
     */
    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    public String getResourceId() {
        return resourceId;
    }

    @Override
    public boolean isEquivalent(Update<?> other) {
        if (!(other instanceof MockUpdate)) {
            return false;
        }
        MockUpdate otherMock = (MockUpdate) other;
        return priority.equals(otherMock.priority) && resourceId.equals(otherMock.resourceId)
                && subResourceId.equals(otherMock.subResourceId) && state.equals(otherMock.state);
    }
}
