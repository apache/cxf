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

import java.util.List;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProperties;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.ExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.StandardTags;
import org.apache.cxf.metrics.micrometer.provider.StandardTagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxrs.JaxrsOperationTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxrs.JaxrsTags;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsOperationTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsTags;
import org.apache.cxf.spring.boot.autoconfigure.CxfProperties;
import org.apache.cxf.spring.boot.autoconfigure.CxfProperties.Metrics.Client;
import org.apache.cxf.spring.boot.autoconfigure.CxfProperties.Metrics.Server;
import org.apache.cxf.spring.boot.autoconfigure.micrometer.provider.SpringBasedTimedAnnotationProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.OnlyOnceLoggingDenyMeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;

@Configuration
@AutoConfigureAfter({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(MetricsProvider.class)
@ConditionalOnProperty(name = "cxf.observation.enabled", matchIfMissing = true)
@ConditionalOnBean(MeterRegistry.class)
public class MicrometerObservationAutoConfiguration {
    private final CxfProperties properties;

    public MicrometerObservationAutoConfiguration(CxfProperties properties) {
        this.properties = properties;
    }


    @Bean
    @ConditionalOnMissingBean(MetricsProvider.class)
    public MetricsProvider metricsProvider(TagsProvider tagsProvider,
                                           List<TagsCustomizer> tagsCustomizers,
                                           TimedAnnotationProvider timedAnnotationProvider,
                                           MeterRegistry registry) {
        MicrometerMetricsProperties micrometerMetricsProperties = new MicrometerMetricsProperties();

        final Server server = this.properties.getMetrics().getServer();
        micrometerMetricsProperties.setAutoTimeRequests(server.isAutoTimeRequests());
        micrometerMetricsProperties.setServerRequestsMetricName(server.getRequestsMetricName());
        
        final Client client = this.properties.getMetrics().getClient();
        micrometerMetricsProperties.setClientRequestsMetricName(client.getRequestsMetricName());

        return new MicrometerMetricsProvider(registry, tagsProvider, tagsCustomizers, timedAnnotationProvider,
                micrometerMetricsProperties);
    }
}
