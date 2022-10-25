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

package org.apache.cxf.systest.jaxrs.metrics;

import java.util.Arrays;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.TestUtil;

import io.specto.hoverfly.junit.core.SimulationSource;
import io.specto.hoverfly.junit.rule.HoverflyRule;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.HoverflyConfig.localConfigs;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.response;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class JAXRSClientMetricsTest {
    @ClassRule public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(localConfigs().asWebServer());
    @Rule public ExpectedException expectedException = ExpectedException.none();
    
    private MetricsProvider provider;
    private MetricsContext operationContext;
    private MetricsContext resourceContext;
    private MetricsContext endpointContext;
    
    @Before
    public void setUp() {
        endpointContext = Mockito.mock(MetricsContext.class);
        operationContext = Mockito.mock(MetricsContext.class);
        resourceContext = Mockito.mock(MetricsContext.class);

        provider = new MetricsProvider() {
            public MetricsContext createEndpointContext(Endpoint endpoint, boolean asClient, String cid) {
                return endpointContext;
            }

            public MetricsContext createOperationContext(Endpoint endpoint, BindingOperationInfo boi, 
                    boolean asClient, String cid) {
                return operationContext;
            }

            public MetricsContext createResourceContext(Endpoint endpoint, String resourceName, 
                    boolean asClient, String cid) {
                return resourceContext;
            }
        };
    }

    @Test
    public void usingClientProxyStopIsCalledWhenServerReturnsNotFound() throws Exception {
        final JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
        factory.setResourceClass(Library.class);
        factory.setAddress("http://localhost:" + hoverflyRule.getProxyPort() + "/");
        factory.setFeatures(Arrays.asList(new MetricsFeature(provider)));
        factory.setProvider(JacksonJsonProvider.class);

        hoverflyRule.simulate(SimulationSource.dsl(
            service("localhost")
                .get("/books/10")
                .willReturn(response().status(404))));

        try {
            final Library client = factory.create(Library.class);
            expectedException.expect(NotFoundException.class);
            client.getBook(10);
        } finally {
            Mockito.verify(resourceContext, times(1)).start(any(Exchange.class));
            Mockito.verify(resourceContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(operationContext);
        }
    }
   
    @Test
    public void usingClientStopIsCalledWhenServerReturnsNotFound() throws Exception {
        final Client client = ClientBuilder
                .newClient()
                .register(new MetricsFeature(provider))
                .register(JacksonJsonProvider.class);

        hoverflyRule.simulate(SimulationSource.dsl(
            service("localhost")
                .get("/books/10")
                .willReturn(response().status(404))));

        try {
            expectedException.expect(ProcessingException.class);
            client
                .target("http://localhost:" + hoverflyRule.getProxyPort() + "/books/10")
                .request(MediaType.APPLICATION_JSON).get()
                .readEntity(Book.class);
        } finally {
            Mockito.verify(resourceContext, times(1)).start(any(Exchange.class));
            Mockito.verify(resourceContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(operationContext);
        }
    }

    @Test
    public void usingClientStopIsCalledWhenConnectionIsRefused() throws Exception {
        final int port = Integer.parseInt(TestUtil.getNewPortNumber(getClass()));
        
        final Client client = ClientBuilder
            .newClient()
            .register(new MetricsFeature(provider))
            .register(JacksonJsonProvider.class);

        try {
            expectedException.expect(ProcessingException.class);
            client
                .target("http://localhost:" + port + "/books/10")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(Book.class);
        } finally {
            Mockito.verify(resourceContext, times(1)).start(any(Exchange.class));
            Mockito.verify(resourceContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(operationContext);
        }
    }

    @Test
    public void usingClientStopIsCalledWhenServerReturnSuccessfulResponse() throws Exception {
        final Client client = ClientBuilder
            .newClient()
            .register(new MetricsFeature(provider))
            .register(JacksonJsonProvider.class);

        hoverflyRule.simulate(SimulationSource.dsl(
            service("localhost")
                .get("/books/10")
                    .header("Accept", MediaType.APPLICATION_JSON)
                .willReturn(response()
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .body("{}")
                    .status(200))));

        try {
            client
                .target("http://localhost:" + hoverflyRule.getProxyPort() + "/books/10")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(Book.class);
        } finally {
            Mockito.verify(resourceContext, times(1)).start(any(Exchange.class));
            Mockito.verify(resourceContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(operationContext);
        }
    }
    
    @Test
    public void usingWebClientStopIsCalledWhenServerReturnsNotFound() throws Exception {
        final WebClient client = WebClient.create("http://localhost:" + hoverflyRule.getProxyPort() + "/books/10",
            Arrays.asList(JacksonJsonProvider.class), Arrays.asList(new MetricsFeature(provider)), null);

        hoverflyRule.simulate(SimulationSource.dsl(
            service("localhost")
                .get("/books/10")
                .willReturn(response().status(404))));

        try {
            expectedException.expect(ProcessingException.class);
            client.get().readEntity(Book.class);
        } finally {
            Mockito.verify(resourceContext, times(1)).start(any(Exchange.class));
            Mockito.verify(resourceContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(operationContext);
        }
    }
}
