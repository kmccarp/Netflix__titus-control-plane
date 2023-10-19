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

package com.netflix.titus.master.service.management.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Preconditions;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.patterns.PolledMeter;
import com.netflix.titus.api.model.ResourceDimension;
import com.netflix.titus.common.util.tuple.Pair;
import com.netflix.titus.master.service.management.CompositeResourceConsumption;
import com.netflix.titus.master.service.management.ResourceConsumption;
import com.netflix.titus.master.service.management.ResourceConsumption.ConsumptionLevel;

import static com.netflix.titus.common.util.CollectionsExt.copyAndRemove;

class ResourceConsumptionServiceMetrics {

    private final Id rootId;
    private final Registry registry;

    private final Map<Pair<String, String>, ApplicationMetrics> metricsByCapacityGroupAndApp = new HashMap<>();
    private final Map<String, ResourceMetrics> capacityGroupLimits = new HashMap<>();

    private volatile long updateTimestamp;

    ResourceConsumptionServiceMetrics(Id rootId, Registry registry) {
        this.rootId = rootId;
        this.registry = registry;
        this.updateTimestamp = registry.clock().wallTime();

        registry.gauge(
                registry.createId(rootId.name() + "updateDelay", rootId.tags()),
                this,
                self -> (registry.clock().wallTime() - updateTimestamp)
        );
    }

    public void update(DefaultResourceConsumptionService.ConsumptionEvaluationResult evaluationResult) {
        CompositeResourceConsumption systemConsumption = evaluationResult.getSystemConsumption();

        Set<Pair<String, String>> touchedApps = new HashSet<>();
        Set<String> touchedGroups = new HashSet<>();

        systemConsumption.getContributors().values().forEach(tierConsumption ->
                updateTier((CompositeResourceConsumption) tierConsumption, touchedApps, touchedGroups)
        );

        // Remove no longer referenced items
        copyAndRemove(metricsByCapacityGroupAndApp.keySet(), touchedApps).forEach(removed ->
                metricsByCapacityGroupAndApp.remove(removed).reset()
        );
        copyAndRemove(capacityGroupLimits.keySet(), touchedGroups).forEach(removed ->
                capacityGroupLimits.remove(removed).reset()
        );

        updateTimestamp = registry.clock().wallTime();
    }

    private void updateTier(CompositeResourceConsumption tierConsumption,
                            Set<Pair<String, String>> touchedApps,
                            Set<String> touchedGroups) {
        String tierName = tierConsumption.getConsumerName();
        Set<String> capacityGroupNames = tierConsumption.getContributors().keySet();
        Collection<ResourceConsumption> capacityGroupConsumption = tierConsumption.getContributors().values();

        // Process application level metrics
        capacityGroupConsumption
                .forEach(groupConsumption -> {
                            ((CompositeResourceConsumption) groupConsumption).getContributors().values()
                                    .forEach(appConsumption -> {
                                        touchedApps.add(updateApp(tierName, groupConsumption, appConsumption));
                                    });
                        }
                );

        // Process capacity group level metrics
        capacityGroupConsumption.forEach(groupConsumption -> updateCapacityGroupLimit(tierName, (CompositeResourceConsumption) groupConsumption));
        touchedGroups.addAll(capacityGroupNames);

    }

    private Pair<String, String> updateApp(String tierName, ResourceConsumption groupConsumption, ResourceConsumption appConsumption) {
        Pair<String, String> key = Pair.of(groupConsumption.getConsumerName(), appConsumption.getConsumerName());
        ApplicationMetrics metrics = metricsByCapacityGroupAndApp.get(key);
        if (metrics == null) {
            metrics = new ApplicationMetrics(tierName, groupConsumption.getConsumerName(), appConsumption.getConsumerName());
            metricsByCapacityGroupAndApp.put(key, metrics);
        }
        metrics.update(appConsumption);

        return key;
    }

    private void updateCapacityGroupLimit(String tierName, CompositeResourceConsumption groupConsumption) {
        String name = groupConsumption.getConsumerName();
        ResourceMetrics metrics = capacityGroupLimits.get(name);
        if (metrics == null) {
            metrics = new ResourceMetrics(registry.createId(rootId.name() + "limit", rootId.tags())
                    .withTag("tier", tierName)
                    .withTag("capacityGroup", name));
            capacityGroupLimits.put(name, metrics);
        }
        metrics.update(groupConsumption.getAllowedConsumption());
    }

    private enum ResourceType {Cpu, Memory, Disk, Network, Gpu, OpportunisticCpu}

    private final class ResourceMetrics {
        private final Map<ResourceType, AtomicLong> usage;

        private ResourceMetrics(Id id) {
            this.usage = initialize(id);
        }

        private Map<ResourceType, AtomicLong> initialize(Id id) {
            Map<ResourceType, AtomicLong> result = new EnumMap<>(ResourceType.class);
            for (ResourceType rt : ResourceType.values()) {
                result.put(rt, PolledMeter.using(registry)
                        .withId(id.withTag("resourceType", rt.name()))
                        .monitorValue(new AtomicLong()));
            }
            return result;
        }

        private void update(ResourceDimension consumption) {
            usage.get(ResourceType.Cpu).set((int) consumption.getCpu());
            usage.get(ResourceType.Memory).set(consumption.getMemoryMB());
            usage.get(ResourceType.Disk).set(consumption.getDiskMB());
            usage.get(ResourceType.Network).set(consumption.getNetworkMbs());
            usage.get(ResourceType.Gpu).set(consumption.getGpu());
            usage.get(ResourceType.OpportunisticCpu).set(consumption.getOpportunisticCpu());
        }

        private void reset() {
            usage.values().forEach(g -> g.set(0));
        }
    }

    private final class ApplicationMetrics {
        private final String tierName;
        private final String capacityGroup;
        private final String appName;
        private final ResourceMetrics maxUsage;
        private final Map<String, ResourceMetrics> actualUsageByInstanceType = new HashMap<>();

        private ApplicationMetrics(String tierName, String capacityGroup, String appName) {
            this.tierName = tierName;
            this.capacityGroup = capacityGroup;
            this.appName = appName;
            this.maxUsage = new ResourceMetrics(
                    registry.createId(rootId.name() + "maxUsage", rootId.tags())
                            .withTag("tier", tierName)
                            .withTag("capacityGroup", capacityGroup)
                            .withTag("applicationName", appName)
            );
        }

        private ResourceMetrics buildInstanceTypeMetrics(String instanceType) {
            return new ResourceMetrics(
                    registry.createId(rootId.name() + "actualUsage", rootId.tags())
                            .withTag("tier", tierName)
                            .withTag("capacityGroup", capacityGroup)
                            .withTag("applicationName", appName)
                            .withTag("instanceType", instanceType)
            );
        }

        private void update(ResourceConsumption appConsumption) {
            Preconditions.checkArgument(ConsumptionLevel.Application.equals(appConsumption.getConsumptionLevel()));
            maxUsage.update(appConsumption.getMaxConsumption());

            Set<String> instanceTypesInUse = new HashSet<>();
            Map<String, ResourceConsumption> consumptionByInstanceType = ((CompositeResourceConsumption) appConsumption).getContributors();
            consumptionByInstanceType.forEach((instanceType, consumption) -> {
                instanceTypesInUse.add(instanceType);
                actualUsageByInstanceType.computeIfAbsent(instanceType, this::buildInstanceTypeMetrics)
                        .update(consumption.getCurrentConsumption());
            });
            // clean up instance types not being used anymore, create a copy to avoid ConcurrentModificationException
            List<String> trackedInstanceTypes = new ArrayList<>(actualUsageByInstanceType.keySet());
            trackedInstanceTypes.stream()
                    .filter(i -> !instanceTypesInUse.contains(i))
                    .forEach(toRemove -> actualUsageByInstanceType.remove(toRemove).reset());
        }

        private void reset() {
            maxUsage.reset();
            actualUsageByInstanceType.values().forEach(ResourceMetrics::reset);
            actualUsageByInstanceType.clear();
        }
    }
}
