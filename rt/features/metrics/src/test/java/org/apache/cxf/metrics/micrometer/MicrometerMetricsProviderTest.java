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

import java.lang.reflect.Field;
import java.util.Collections;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;
import org.apache.cxf.service.model.BindingOperationInfo;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

public class MicrometerMetricsProviderTest {

    private MicrometerMetricsProvider underTest;

    @Mock
    private MeterRegistry registry;
    @Mock
    private TagsProvider tagsProvider;
    @Mock
    private TimedAnnotationProvider timedAnnotationProvider;
    @Mock
    private Endpoint endpoint;
    @Mock
    private BindingOperationInfo boi;
    @Mock
    private TagsCustomizer tagsCustomizer;

    private MicrometerMetricsProperties micrometerMetricsProperties;

    @Before
    public void setUp() {
        openMocks(this);

        micrometerMetricsProperties = new MicrometerMetricsProperties();
        micrometerMetricsProperties.setServerRequestsMetricName("http.server.requests");
        micrometerMetricsProperties.setClientRequestsMetricName("http.client.requests");
        micrometerMetricsProperties.setAutoTimeRequests(true);

        underTest =
                new MicrometerMetricsProvider(registry,
                        tagsProvider,
                        Collections.singletonList(tagsCustomizer),
                        timedAnnotationProvider,
                        micrometerMetricsProperties);
    }

    @Test
    public void testCreateEndpointContextShouldReturnNull() {
        // when
        MetricsContext actual = underTest.createEndpointContext(endpoint, true, "clientId");

        // then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testCreateServerOperationContext() throws NoSuchFieldException, IllegalAccessException {
        // when
        MetricsContext actual = underTest.createOperationContext(endpoint, boi, false, "clientId");

        // then
        assertThat(actual, instanceOf(MicrometerServerMetricsContext.class));
        assertThat(getFieldValue(actual, "registry"), is(registry));
        assertThat(getFieldValue(actual, "tagsProvider"), is(tagsProvider));
        assertThat(getFieldValue(actual, "timedAnnotationProvider"), is(timedAnnotationProvider));
        assertThat(getFieldValue(actual, "metricName"), is("http.server.requests"));
        assertThat(getFieldValue(actual, "autoTimeRequests"), is(true));
        assertThat(getFieldValue(actual, "tagsCustomizers"), is(Collections.singletonList(tagsCustomizer)));
    }
    
    @Test
    public void testCreateClientOperationContext() throws NoSuchFieldException, IllegalAccessException {
        // when
        MetricsContext actual = underTest.createOperationContext(endpoint, boi, true, "clientId");

        // then
        assertThat(actual, instanceOf(MicrometerClientMetricsContext.class));
        assertThat(getFieldValue(actual, "registry"), is(registry));
        assertThat(getFieldValue(actual, "tagsProvider"), is(tagsProvider));
        assertThat(getFieldValue(actual, "timedAnnotationProvider"), is(timedAnnotationProvider));
        assertThat(getFieldValue(actual, "metricName"), is("http.client.requests"));
        assertThat(getFieldValue(actual, "autoTimeRequests"), is(true));
        assertThat(getFieldValue(actual, "tagsCustomizers"), is(Collections.singletonList(tagsCustomizer)));
    }
    
    @Test
    public void testCreateServerResourceContext() throws NoSuchFieldException, IllegalAccessException {
        // when
        MetricsContext actual = underTest.createResourceContext(endpoint, "resourceName", false, "clientId");

        // then
        assertThat(actual, instanceOf(MicrometerServerMetricsContext.class));
        assertThat(getFieldValue(actual, "registry"), is(registry));
        assertThat(getFieldValue(actual, "tagsProvider"), is(tagsProvider));
        assertThat(getFieldValue(actual, "timedAnnotationProvider"), is(timedAnnotationProvider));
        assertThat(getFieldValue(actual, "metricName"), is("http.server.requests"));
        assertThat(getFieldValue(actual, "autoTimeRequests"), is(true));
        assertThat(getFieldValue(actual, "tagsCustomizers"), is(Collections.singletonList(tagsCustomizer)));
    }

    @Test
    public void testCreateClientResourceContext() throws NoSuchFieldException, IllegalAccessException {
        // when
        MetricsContext actual = underTest.createResourceContext(endpoint, "resourceName", true, "clientId");

        // then
        // then
        assertThat(actual, instanceOf(MicrometerClientMetricsContext.class));
        assertThat(getFieldValue(actual, "registry"), is(registry));
        assertThat(getFieldValue(actual, "tagsProvider"), is(tagsProvider));
        assertThat(getFieldValue(actual, "timedAnnotationProvider"), is(timedAnnotationProvider));
        assertThat(getFieldValue(actual, "metricName"), is("http.client.requests"));
        assertThat(getFieldValue(actual, "autoTimeRequests"), is(true));
        assertThat(getFieldValue(actual, "tagsCustomizers"), is(Collections.singletonList(tagsCustomizer)));
    }

    private Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
