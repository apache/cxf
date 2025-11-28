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
package org.apache.cxf.systest.jaxws;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.schemavalidation.CkRequestType;
import org.apache.cxf.jaxws.schemavalidation.RequestHeader;
import org.apache.cxf.jaxws.schemavalidation.RequestIdType;
import org.apache.cxf.jaxws.schemavalidation.Service;
import org.apache.cxf.jaxws.schemavalidation.ServicePortType;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProperties;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultExceptionClassProvider;
import org.apache.cxf.metrics.micrometer.provider.DefaultTimedAnnotationProvider;
import org.apache.cxf.metrics.micrometer.provider.StandardTags;
import org.apache.cxf.metrics.micrometer.provider.StandardTagsProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeProvider;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsFaultCodeTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsOperationTagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.jaxws.JaxwsTags;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CustomizedMicrometerProviderClientServerTest extends AbstractBusClientServerTestBase {
    
    
    public static final MeterRegistry METER_REGISTER = new SimpleMeterRegistry();
    private static final String PORT = allocatePort(Server.class);
    
    private final QName portName = new QName("http://cxf.apache.org/jaxws/schemavalidation", "servicePort");

    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
            var jaxwsTags = new JaxwsTags();
            var operationsCustomizer = new JaxwsOperationTagsCustomizer(jaxwsTags);
            var faultsCustomizer = new JaxwsFaultCodeTagsCustomizer(jaxwsTags, new JaxwsFaultCodeProvider());
            var standardTags = new StandardTags();
            var tagsProvider = new StandardTagsProvider(new DefaultExceptionClassProvider(), standardTags);
            var properties = new MicrometerMetricsProperties();

            var provider = new NullEndpointMicrometerProvider(new MicrometerMetricsProvider(
                    METER_REGISTER,
                    tagsProvider,
                    List.of(operationsCustomizer, faultsCustomizer),
                    new DefaultTimedAnnotationProvider(),
                    properties
            ));

            String address;
            Object implementor = new ServicePortTypeImpl();
            address = "http://localhost:" + PORT + "/schemavalidation";
            Endpoint ep = Endpoint.create(implementor);
            
            ((EndpointImpl)ep).setWsdlLocation("wsdl_systest_jaxws/schemaValidation.wsdl");
            ((EndpointImpl)ep).setServiceName(new QName(
                    "http://cxf.apache.org/jaxws/schemavalidation", "service"));
            ((EndpointImpl)ep).getInInterceptors().add(new LoggingInInterceptor());
            ((EndpointImpl)ep).getOutInterceptors().add(new LoggingOutInterceptor());
            ((EndpointImpl)ep).setFeatures(Arrays.asList(new MetricsFeature(provider)));
            ep.publish(address);
            Endpoint.publish("http://localhost:" + PORT + "/metrics", 
                             new MicrometerMetricsHttpProvider(METER_REGISTER));

        }
    }

    @BeforeClass
    public static void startServers() {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testCustomizedMetricsProvider() throws Exception {
        sendRequest();
        testCxfServerRequestsCount();
    }
    
    
    
    
    private void sendRequest() throws Exception {
        Service service = new Service();
        assertNotNull(service);

        try (ServicePortType greeter = service.getPort(portName, ServicePortType.class)) {
            greeter.getInInterceptors().add(new LoggingInInterceptor());
            greeter.getOutInterceptors().add(new LoggingOutInterceptor());
            updateAddressPort(greeter, PORT);

            RequestIdType requestId = new RequestIdType();
            requestId.setId("550e8400-e29b-41d4-a716-446655440000");
            CkRequestType request = new CkRequestType();
            request.setRequest(requestId);
            RequestHeader header = new RequestHeader();
            header.setHeaderValue("AABBCC");

            try {
                greeter.ckR(request, header);
            } finally {
                assertEquals(1, METER_REGISTER.getMeters().size());
            }
        }
    }
    
    
    private void testCxfServerRequestsCount() throws Exception {
        String endpoint = "http://localhost:" + PORT + "/metrics";
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/xml");

        assertEquals("HTTP response code", 200, conn.getResponseCode());

        try (InputStream in = conn.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);

            NodeList meters = doc.getElementsByTagName("meter");
            boolean found = false;
            for (int i = 0; i < meters.getLength(); i++) {
                Element el = (Element) meters.item(i);
                String name = el.getAttribute("name");
                if ("cxf.server.requests".equals(name)) {
                    String count = el.getAttribute("count");
                    assertEquals("cxf.server.requests count", "1", count);
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
    
    //A customized MetricProvider which mimics the way that
    //always return null MetricContext from Endpoint
    static class NullEndpointMicrometerProvider implements MetricsProvider {
        private final MetricsProvider delegate;

        NullEndpointMicrometerProvider(MetricsProvider delegate) {
            this.delegate = delegate;
        }

        

        @Override
        public MetricsContext createEndpointContext(org.apache.cxf.endpoint.Endpoint endpoint, 
                                                    boolean asClient,
                                                    String clientId) {
            return null;
        }

        @Override
        public MetricsContext createOperationContext(org.apache.cxf.endpoint.Endpoint endpoint,
                                                     BindingOperationInfo boi, 
                                                     boolean asClient, 
                                                     String clientId) {
            return delegate.createOperationContext(endpoint, boi, asClient, clientId);
        }

        @Override
        public MetricsContext createResourceContext(org.apache.cxf.endpoint.Endpoint endpoint, 
                                                    String resourceName,
                                                    boolean asClient, 
                                                    String clientId) {
            return delegate.createResourceContext(endpoint, resourceName, asClient, clientId);
        }
    }
}