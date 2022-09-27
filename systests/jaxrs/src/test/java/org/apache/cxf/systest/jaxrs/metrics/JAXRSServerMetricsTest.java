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
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class JAXRSServerMetricsTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(JAXRSServerMetricsTest.class);

    private static MetricsProvider provider;
    private static MetricsContext operationContext;
    private static MetricsContext resourceContext;
    private static MetricsContext endpointContext;
    
    @Rule public ExpectedException expectedException = ExpectedException.none();
    
    public static class BookLibrary implements Library {
        @Override
        public Book getBook(int id) {
            if (id == 10) {
                throw new NotFoundException();
            } else {
                return new Book(id);
            }
        }
    }

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookLibrary.class);
            sf.setResourceProvider(BookLibrary.class, new SingletonResourceProvider(new BookLibrary()));
            sf.setFeatures(Arrays.asList(new MetricsFeature(provider)));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setProvider(new JacksonJsonProvider());
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
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

        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void setUp() {
        Mockito.reset(resourceContext);
        Mockito.reset(operationContext);
        Mockito.reset(endpointContext);
    }

    @Test
    public void usingClientProxyStopIsCalledWhenServerReturnsNotFound() throws Exception {
        final JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
        factory.setResourceClass(Library.class);
        factory.setAddress("http://localhost:" + PORT + "/");
        factory.setProvider(JacksonJsonProvider.class);
        
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
            .register(JacksonJsonProvider.class);

        try {
            expectedException.expect(ProcessingException.class);
            client
                .target("http://localhost:" + PORT + "/books/10")
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
    public void usingClientStopIsCalledWhenServerReturnSuccessfulResponse() throws Exception {
        final Client client = ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class);

        try {
            client
                .target("http://localhost:" + PORT + "/books/11")
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
        final WebClient client = WebClient.create("http://localhost:" + PORT + "/books/10",
            Arrays.asList(JacksonJsonProvider.class));
        
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
    
    @Test
    public void usingWebClientStopIsCalledWhenUrlIsNotServed() throws Exception {
        final WebClient client = WebClient.create("http://localhost:" + PORT + "/books",
            Arrays.asList(JacksonJsonProvider.class));
        
        try {
            expectedException.expect(ProcessingException.class);
            client.get().readEntity(Book.class);
        } finally {
            Mockito.verify(endpointContext, times(1)).start(any(Exchange.class));
            Mockito.verify(endpointContext, times(1)).stop(anyLong(), anyLong(), anyLong(), any(Exchange.class));
            Mockito.verifyNoInteractions(resourceContext);
            Mockito.verifyNoInteractions(operationContext);
        }
    }
}
