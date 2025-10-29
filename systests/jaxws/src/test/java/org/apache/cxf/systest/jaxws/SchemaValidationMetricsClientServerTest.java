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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.schemavalidation.CkRequestType;
import org.apache.cxf.jaxws.schemavalidation.RequestHeader;
import org.apache.cxf.jaxws.schemavalidation.RequestIdType;
import org.apache.cxf.jaxws.schemavalidation.Service;
import org.apache.cxf.jaxws.schemavalidation.ServicePortType;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.MetricsFeature;
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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SchemaValidationMetricsClientServerTest extends AbstractBusClientServerTestBase {
    
    
    private static final MeterRegistry METER_REGISTER = new SimpleMeterRegistry();
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

            var provider = new MicrometerMetricsProvider(
                    METER_REGISTER,
                    tagsProvider,
                    List.of(operationsCustomizer, faultsCustomizer),
                    new DefaultTimedAnnotationProvider(),
                    properties
            );

            String address;
            Object implementor = new ServicePortTypeImpl();
            address = "http://localhost:" + PORT + "/schemavalidation";
            Endpoint ep = Endpoint.create(implementor);
            Map<String, Object> map = new HashMap<>();
            map.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            ep.setProperties(map);
            ((EndpointImpl)ep).setWsdlLocation("wsdl_systest_jaxws/schemaValidation.wsdl");
            ((EndpointImpl)ep).setServiceName(new QName(
                    "http://cxf.apache.org/jaxws/schemavalidation", "service"));
            ((EndpointImpl)ep).getInInterceptors().add(new LoggingInInterceptor());
            ((EndpointImpl)ep).getOutInterceptors().add(new LoggingOutInterceptor());
            ((EndpointImpl)ep).setFeatures(Arrays.asList(new MetricsFeature(provider)));
            ep.publish(address);
        }
    }

    @BeforeClass
    public static void startServers() {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testSchemaValidationWithMultipleXsds() throws Exception {
        Service service = new Service();
        assertNotNull(service);

        try (ServicePortType greeter = service.getPort(portName, ServicePortType.class)) {
            greeter.getInInterceptors().add(new LoggingInInterceptor());
            greeter.getOutInterceptors().add(new LoggingOutInterceptor());
            updateAddressPort(greeter, PORT);

            RequestIdType requestId = new RequestIdType();
            requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeeez");
            CkRequestType request = new CkRequestType();
            request.setRequest(requestId);
            RequestHeader header = new RequestHeader();
            header.setHeaderValue("AABBCC");

            try {
                greeter.ckR(request, header);
                fail("should catch marshall exception as the invalid outgoing message per schema");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Unmarshalling Error"));
                boolean english = "en".equals(java.util.Locale.getDefault().getLanguage());
                if (english) {
                    assertTrue(e.getMessage().contains("is not facet-valid with respect to pattern"));
                }
            } finally {
                assertEquals(1, METER_REGISTER.getMeters().size());
            }
        }
    }
}