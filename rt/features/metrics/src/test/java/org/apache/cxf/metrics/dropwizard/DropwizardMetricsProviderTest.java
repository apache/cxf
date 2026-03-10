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

package org.apache.cxf.metrics.dropwizard;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import io.dropwizard.metrics5.MetricRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DropwizardMetricsProviderTest {

    private DropwizardMetricsProvider provider;
    private Bus bus;
    private Endpoint endpoint;
    private MetricRegistry registry;
    private BindingOperationInfo boi;

    @Before
    public void setUp() throws EndpointException {
        registry = new MetricRegistry();
        
        bus = new ExtensionManagerBus();
        bus.setExtension(registry, MetricRegistry.class);

        provider = new DropwizardMetricsProvider(bus);

        final ServiceInfo si = new ServiceInfo();
        si.setName(new QName("http://www.apache.org", "ServiceName"));
        final Service s = new ServiceImpl(si);
        final EndpointInfo endpointInfo = new EndpointInfo();
        endpointInfo.setName(si.getName());

        final OperationInfo opinfo = new OperationInfo();
        opinfo.setName(si.getName());

        boi = new BindingOperationInfo(new BindingInfo(si, "test"), opinfo);
        endpoint = new EndpointImpl(bus, s, endpointInfo);
    }

    @After
    public void tearDown() {
        bus.shutdown(true);
    }

    @Test
    public void testCreateEndpointContext() throws IOException {
        final MetricsContext actual = provider.createEndpointContext(endpoint, true, "clientId");
        assertThat(actual, instanceOf(DropwizardMetricsContext.class));

        try (DropwizardMetricsContext context = (DropwizardMetricsContext) actual) {
            assertThat(context.getRegistry(), is(registry));
            assertThat(context.getBaseName(), containsString("Metrics.Client"));
        }
    }

    @Test
    public void testCreateServerOperationContext() throws IOException {
        final MetricsContext actual = provider.createOperationContext(endpoint, boi, false, "clientId");
        assertThat(actual, instanceOf(DropwizardMetricsContext.class));

        try (DropwizardMetricsContext context = (DropwizardMetricsContext) actual) {
            assertThat(context.getRegistry(), is(registry));
            assertThat(context.getBaseName(), containsString("Metrics.Server"));
        }
    }
    
    @Test
    public void testCreateClientOperationContext() throws IOException {
        final MetricsContext actual = provider.createOperationContext(endpoint, boi, true, "clientId");
        assertThat(actual, instanceOf(DropwizardMetricsContext.class));

        try (DropwizardMetricsContext context = (DropwizardMetricsContext) actual) {
            assertThat(context.getRegistry(), is(registry));
            assertThat(context.getBaseName(), containsString("Metrics.Client"));
        }
    }
    
    @Test
    public void testCreateServerResourceContext() throws IOException {
        final MetricsContext actual = provider.createResourceContext(endpoint, "resourceName", false, "clientId");
        assertThat(actual, instanceOf(DropwizardMetricsContext.class));

        try (DropwizardMetricsContext context = (DropwizardMetricsContext) actual) {
            assertThat(context.getRegistry(), is(registry));
            assertThat(context.getBaseName(), containsString("Metrics.Server"));
        }
    }

    @Test
    public void testCreateClientResourceContext() throws IOException {
        final MetricsContext actual = provider.createResourceContext(endpoint, "resourceName", true, "clientId");
        assertThat(actual, instanceOf(DropwizardMetricsContext.class));

        // then
        // then
        try (DropwizardMetricsContext context = (DropwizardMetricsContext) actual) {
            assertThat(context.getRegistry(), is(registry));
            assertThat(context.getBaseName(), containsString("Metrics.Client"));
        }
    }
}
