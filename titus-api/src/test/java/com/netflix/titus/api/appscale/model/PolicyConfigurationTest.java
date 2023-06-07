package com.netflix.titus.api.appscale.model;

import com.netflix.titus.api.json.ObjectMappers;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PolicyConfigurationTest {
    @Test
    public void deserializePolicyConfiguration() {
        String policyConfigStrNoMetricDimensions = """
                {
                  "name": null,
                  "policyType": "StepScaling",
                  "stepScalingPolicyConfiguration": {
                    "metricAggregationType": "Maximum",
                    "adjustmentType": "ChangeInCapacity",
                    "minAdjustmentMagnitude": null,
                    "steps": [
                      {
                        "scalingAdjustment": 1,
                        "metricIntervalLowerBound": 0.0,
                        "metricIntervalUpperBound": null
                      }
                    ],
                    "coolDownSec": 60
                  },
                  "alarmConfiguration": {
                    "name": null,
                    "region": null,
                    "actionsEnabled": true,
                    "comparisonOperator": "GreaterThanThreshold",
                    "evaluationPeriods": 1,
                    "threshold": 2.0,
                    "metricNamespace": "titus/integrationTest",
                    "metricName": "RequestCount",
                    "statistic": "Sum",
                    "periodSec": 60
                  },
                  "targetTrackingPolicy": null
                }\
                """;

        PolicyConfiguration policyConfigNoMetricDimension = ObjectMappers.readValue(ObjectMappers.appScalePolicyMapper(),
                policyConfigStrNoMetricDimensions, PolicyConfiguration.class);
        assertThat(policyConfigNoMetricDimension).isNotNull();
        assertThat(policyConfigNoMetricDimension.getAlarmConfiguration().getDimensions()).isNull();

        String policyConfigStrWithMetricDimensions = """
                {
                  "name": null,
                  "policyType": "StepScaling",
                  "stepScalingPolicyConfiguration": {
                    "metricAggregationType": "Maximum",
                    "adjustmentType": "ChangeInCapacity",
                    "minAdjustmentMagnitude": null,
                    "steps": [
                      {
                        "scalingAdjustment": 1,
                        "metricIntervalLowerBound": 0.0,
                        "metricIntervalUpperBound": null
                      }
                    ],
                    "coolDownSec": 60
                  },
                  "alarmConfiguration": {
                    "name": null,
                    "region": null,
                    "actionsEnabled": true,
                    "comparisonOperator": "GreaterThanThreshold",
                    "evaluationPeriods": 1,
                    "threshold": 2.0,
                    "metricNamespace": "titus/integrationTest",
                    "metricName": "RequestCount",
                    "statistic": "Sum",
                    "periodSec": 60,
                    "unknownField": 100,
                    "dimensions": [
                      {
                        "Name": "foo",
                        "Value": "bar"
                      },
                      {
                        "Name": "tier",
                        "Value": "1"
                      }
                    ]
                  },
                  "targetTrackingPolicy": null
                }\
                """;

        PolicyConfiguration policyConfigWithMetricDimensions = ObjectMappers.readValue(ObjectMappers.appScalePolicyMapper(),
                policyConfigStrWithMetricDimensions, PolicyConfiguration.class);
        assertThat(policyConfigWithMetricDimensions).isNotNull();
        assertThat(policyConfigWithMetricDimensions.getAlarmConfiguration().getDimensions()).isNotNull();
        assertThat(policyConfigWithMetricDimensions.getAlarmConfiguration().getDimensions().size()).isEqualTo(2);
    }
}
