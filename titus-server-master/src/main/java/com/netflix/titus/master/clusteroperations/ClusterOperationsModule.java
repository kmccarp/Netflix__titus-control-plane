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

package com.netflix.titus.master.clusteroperations;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.ConfigProxyFactory;


public final class ClusterOperationsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ClusterRemovableInstanceGroupAgentRemover.class).asEagerSingleton();
        bind(ClusterRemovableAgentRemover.class).asEagerSingleton();
        bind(ClusterAgentAutoScaler.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public ClusterOperationsConfiguration getClusterOperationsConfiguration(ConfigProxyFactory factory) {
        return factory.newProxy(ClusterOperationsConfiguration.class);
    }
}