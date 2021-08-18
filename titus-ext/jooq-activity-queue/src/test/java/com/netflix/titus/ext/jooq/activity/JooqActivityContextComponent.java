/*
 * Copyright 2021 Netflix, Inc.
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

package com.netflix.titus.ext.jooq.activity;

import com.netflix.titus.common.runtime.TitusRuntime;
import com.netflix.titus.common.runtime.TitusRuntimes;
import com.netflix.titus.common.util.archaius2.Archaius2Ext;
import com.netflix.titus.ext.jooq.JooqConfiguration;
import com.netflix.titus.ext.jooq.JooqContext;
import com.netflix.titus.ext.jooq.ProductionJooqContext;
import com.netflix.titus.ext.jooq.embedded.EmbeddedJooqContext;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty(name = "titus.ext.jooq.activity.enabled", havingValue = "true", matchIfMissing = true)
public class JooqActivityContextComponent {

    @Bean
    public TitusRuntime getTitusRuntime() {
        return TitusRuntimes.internal();
    }

    @Bean
    public JooqConfiguration getJooqPropertyConfiguration(Environment environment) {
        return Archaius2Ext.newConfiguration(JooqConfiguration.class, "titus.ext.jooq.activity", environment);
    }

    @Bean
    public JooqContext getJooqContext(JooqConfiguration jooqConfiguration,
                                      ConfigurableApplicationContext applicationContext,
                                      TitusRuntime titusRuntime) {
        if (jooqConfiguration.isInMemoryDb()) {
            return new EmbeddedJooqContext(applicationContext, "activity", titusRuntime);
        }
        return new ProductionJooqContext(jooqConfiguration, titusRuntime);
    }

    @Bean
    public DSLContext getDSLContext(JooqContext relocationContext) {
        return relocationContext.getDslContext();
    }
}
