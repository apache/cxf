/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.spring.boot.autoconfigure.micrometer;

import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxrs.JaxrsTags;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsTags;
import org.apache.cxf.spring.boot.autoconfigure.CxfProperties;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class MicrometerMetricsAutoConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withBean(CxfProperties.class)
            .withConfiguration(AutoConfigurations.of(
                MetricsAutoConfiguration.class,
                SimpleMetricsExportAutoConfiguration.class,
                MicrometerMetricsAutoConfiguration.class
            ));
    
    @Test
    public void metricsProviderShouldBeAvailable() {
        runner
            .run(ctx -> {
                assertThat(ctx.getBean(MetricsProvider.class), instanceOf(MicrometerMetricsProvider.class));
                assertThat(ctx.getBean(JaxrsTags.class), not(nullValue()));
                assertThat(ctx.getBean(JaxwsTags.class), not(nullValue()));
            });
    }
    
    @Test
    public void metricsProviderShouldNotBeAvailable() {
        runner
            .withPropertyValues("cxf.metrics.enabled=false")
            .run(ctx -> {
                assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(MetricsProvider.class));
                assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(JaxrsTags.class));
                assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(JaxwsTags.class));
            });
    }
    
    @Test
    public void jaxrsMetricsShouldNotBeAvailable() {
        runner
            .withPropertyValues("cxf.metrics.jaxrs.enabled=false")
            .run(ctx -> {
                assertThat(ctx.getBean(MetricsProvider.class), instanceOf(MicrometerMetricsProvider.class));
                assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(JaxrsTags.class));
                assertThat(ctx.getBean(JaxwsTags.class), not(nullValue()));
            });
    }

    @Test
    public void jaxwsMetricsShouldNotBeAvailable() {
        runner
            .withPropertyValues("cxf.metrics.jaxws.enabled=false")
            .run(ctx -> {
                assertThat(ctx.getBean(MetricsProvider.class), instanceOf(MicrometerMetricsProvider.class));
                assertThat(ctx.getBean(JaxrsTags.class), not(nullValue()));
                assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(JaxwsTags.class));
            });
    }
}
