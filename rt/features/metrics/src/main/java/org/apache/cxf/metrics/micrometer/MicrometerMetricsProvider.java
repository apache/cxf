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

package org.apache.cxf.metrics.micrometer;

import java.util.List;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;
import org.apache.cxf.service.model.BindingOperationInfo;

import io.micrometer.core.instrument.MeterRegistry;

/**
 *
 */
@NoJSR250Annotations
public class MicrometerMetricsProvider implements MetricsProvider {

    private final MeterRegistry registry;
    private final TagsProvider tagsProvider;
    private final List<TagsCustomizer> tagsCustomizers;
    private final TimedAnnotationProvider timedAnnotationProvider;
    private final MicrometerMetricsProperties micrometerMetricsProperties;

    public MicrometerMetricsProvider(MeterRegistry registry,
                                     TagsProvider tagsProvider,
                                     List<TagsCustomizer> tagsCustomizers,
                                     TimedAnnotationProvider timedAnnotationProvider,
                                     MicrometerMetricsProperties micrometerMetricsProperties) {
        this.registry = registry;
        this.tagsProvider = tagsProvider;
        this.tagsCustomizers = tagsCustomizers;
        this.timedAnnotationProvider = timedAnnotationProvider;
        this.micrometerMetricsProperties = micrometerMetricsProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetricsContext createEndpointContext(Endpoint endpoint, boolean asClient, String clientId) {
        if (asClient) {
            return null;
        } else {
            return new MicrometerServerMetricsContext(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers,
                micrometerMetricsProperties.getServerRequestsMetricName(), 
                micrometerMetricsProperties.isAutoTimeRequests());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetricsContext createOperationContext(Endpoint endpoint, BindingOperationInfo boi, boolean asClient,
                                                 String clientId) {
        if (asClient) {
            return new MicrometerClientMetricsContext(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers,
                micrometerMetricsProperties.getClientRequestsMetricName());
        } else {
            return new MicrometerServerMetricsContext(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers,
                micrometerMetricsProperties.getServerRequestsMetricName(), 
                micrometerMetricsProperties.isAutoTimeRequests());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MetricsContext createResourceContext(Endpoint endpoint, String resourceName, boolean asClient,
                                                String clientId) {
        if (asClient) {
            return new MicrometerClientMetricsContext(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers,
                micrometerMetricsProperties.getClientRequestsMetricName());
        } else {
            return new MicrometerServerMetricsContext(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers,
                micrometerMetricsProperties.getServerRequestsMetricName(), 
                micrometerMetricsProperties.isAutoTimeRequests());
        }
    }
}
