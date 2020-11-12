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
@ConditionalOnProperty(name = "cxf.metrics.enabled", matchIfMissing = true)
@ConditionalOnBean(MeterRegistry.class)
public class MicrometerMetricsAutoConfiguration {
    private final CxfProperties properties;

    public MicrometerMetricsAutoConfiguration(CxfProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TimedAnnotationProvider timedAnnotationProvider() {
        return new SpringBasedTimedAnnotationProvider();
    }

    @Bean
    @ConditionalOnMissingBean(ExceptionClassProvider.class)
    public ExceptionClassProvider exceptionClassProvider() {
        return new DefaultExceptionClassProvider();
    }

    @Bean
    @ConditionalOnMissingBean(StandardTags.class)
    public StandardTags standardTags() {
        return new StandardTags();
    }

    @Bean
    @ConditionalOnMissingBean(TagsProvider.class)
    public TagsProvider tagsProvider(ExceptionClassProvider exceptionClassProvider, StandardTags standardTags) {
        return new StandardTagsProvider(exceptionClassProvider, standardTags);
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

    @Bean
    @Order(0)
    public MeterFilter cxfMetricsMaxAllowedServerUriTagsFilter() {
        String metricName = this.properties.getMetrics().getServer().getRequestsMetricName();
        MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
        () -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
        return MeterFilter.maximumAllowableTags(
                metricName, "uri", this.properties.getMetrics().getServer().getMaxUriTags(), filter);
    }
    
    @Bean
    @Order(0)
    public MeterFilter cxfMetricsMaxAllowedClientUriTagsFilter() {
        String metricName = this.properties.getMetrics().getClient().getRequestsMetricName();
        MeterFilter filter = new OnlyOnceLoggingDenyMeterFilter(
        () -> String.format("Reached the maximum number of URI tags for '%s'.", metricName));
        return MeterFilter.maximumAllowableTags(
                metricName, "uri", this.properties.getMetrics().getClient().getMaxUriTags(), filter);
    }
    
    @Configuration
    @ConditionalOnClass(JaxWsServerFactoryBean.class)
    @ConditionalOnProperty(name = "cxf.metrics.jaxws.enabled", matchIfMissing = true)
    protected static class JaxWsMetricsConfiguration {
        @Bean
        @ConditionalOnMissingBean(JaxwsFaultCodeProvider.class)
        public JaxwsFaultCodeProvider jaxwsFaultCodeProvider() {
            return new JaxwsFaultCodeProvider();
        }

        @Bean
        @ConditionalOnMissingBean(JaxwsFaultCodeTagsCustomizer.class)
        public JaxwsFaultCodeTagsCustomizer jaxwsFaultCodeTagsCustomizer(JaxwsTags jaxwsTags,
                JaxwsFaultCodeProvider jaxwsFaultCodeProvider) {
            return new JaxwsFaultCodeTagsCustomizer(jaxwsTags, jaxwsFaultCodeProvider);
        }

        @Bean
        @ConditionalOnMissingBean(JaxwsOperationTagsCustomizer.class)
        public JaxwsOperationTagsCustomizer jaxwsOperationTagsCustomizer(JaxwsTags jaxwsTags) {
            return new JaxwsOperationTagsCustomizer(jaxwsTags);
        }
        
        @Bean
        @ConditionalOnMissingBean(JaxwsTags.class)
        public JaxwsTags jaxwsTags() {
            return new JaxwsTags();
        }
    }
    
    @Configuration
    @ConditionalOnClass(JAXRSServerFactoryBean.class)
    @ConditionalOnProperty(name = "cxf.metrics.jaxrs.enabled", matchIfMissing = true)
    protected static class JaxRsMetricsConfiguration {
        @Bean
        @ConditionalOnMissingBean(JaxrsTags.class)
        public JaxrsTags jaxrsTags() {
            return new JaxrsTags();
        }
        
        @Bean
        @ConditionalOnMissingBean(JaxrsOperationTagsCustomizer.class)
        public JaxrsOperationTagsCustomizer jaxrsOperationTagsCustomizer(JaxrsTags jaxrsTags) {
            return new JaxrsOperationTagsCustomizer(jaxrsTags);
        }
    }
}
