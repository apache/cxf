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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.schemavalidation.CkRequestType;
import org.apache.cxf.jaxws.schemavalidation.CkResponseType;
import org.apache.cxf.jaxws.schemavalidation.RequestHeader;
import org.apache.cxf.jaxws.schemavalidation.RequestIdType;
import org.apache.cxf.jaxws.schemavalidation.Service;
import org.apache.cxf.jaxws.schemavalidation.ServicePortType;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class SchemaValidationClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    private static URL wsdlUrl;
    private final QName portName = new QName("http://cxf.apache.org/jaxws/schemavalidation", "servicePort");
    

    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
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
            ep.publish(address);
            EndpointImpl endpoint = (EndpointImpl)Endpoint.publish(
                                    "http://localhost:" + PORT + "/server", new TestEndpoint());
            endpoint.getProperties().put("not-use-msv-schema-validator", "true");
            wsdlUrl = this.getClass().getClassLoader().getResource("wsdl_systest_jaxws/cxf8979.wsdl");
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    
    public static class TestClient extends Service {

        protected TestClient(URL wsdlDocumentLocation, QName serviceName) {
            super(wsdlDocumentLocation, serviceName);
        }
    }

    @ServiceMode(value = Service.Mode.PAYLOAD)
    @WebServiceProvider(wsdlLocation = "wsdl_systest_jaxws/cxf8979.wsdl",
                serviceName = "TestService", portName = "TestService", targetNamespace = "http://test.namespace/")
    @SchemaValidation(type = SchemaValidation.SchemaValidationType.REQUEST)
    public static class TestEndpoint implements Provider<Source> {

                
        @Override
        public Source invoke(Source request) {
            return request;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
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
            requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
            CkRequestType request = new CkRequestType();
            request.setRequest(requestId);
            greeter.getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            RequestHeader header = new RequestHeader();
            header.setHeaderValue("AABBCC");
            CkResponseType response = greeter.ckR(request, header);
            assertEquals(response.getProduct().get(0).getAction().getStatus(), 4);

            try {
                requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeeez");
                request.setRequest(requestId);
                greeter.ckR(request, header);
                fail("should catch marshall exception as the invalid outgoing message per schema");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Marshalling Error"));
                boolean english = "en".equals(java.util.Locale.getDefault().getLanguage());
                if (english) {
                    assertTrue(e.getMessage().contains("is not facet-valid with respect to pattern"));
                }
            }

            try {
                requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
                request.setRequest(requestId);
                header.setHeaderValue("AABBCCDDEEFFGGHHIIJJ");

                //Check if incoming validation on server side works, turn off outgoing
                greeter.ckR(request, header);
                fail("should catch marshall exception as the invalid outgoing message per schema");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Marshalling Error"));
                boolean english = "en".equals(java.util.Locale.getDefault().getLanguage());
                if (english) {
                    assertTrue(e.getMessage().contains("is not facet-valid with respect to maxLength"));
                }
            }

            try {
                requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
                request.setRequest(requestId);
                header.setHeaderValue("AABBCCDDEEFFGGHHIIJJ");

                //Check if incoming validation on server side works, turn off outgoing
                greeter.getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE);
                greeter.ckR(request, header);
                fail("should catch marshall exception as the invalid outgoing message per schema");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Could not validate soapheader "));
                boolean english = "en".equals(java.util.Locale.getDefault().getLanguage());
                if (english) {
                    assertTrue(e.getMessage().contains("is not facet-valid with respect to maxLength"));
                }
            }
        }

    }
    
    @Test
    public void testWithValidation() {
        QName serviceName = new QName("http://test.namespace/", "TestService");
        QName port = new QName("http://test.namespace/", "TestService");
        TestClient service = new TestClient(wsdlUrl, serviceName);
        Source request = readResource("wsdl_systest_jaxws/cxf8979-request.xml");
        Dispatch<Source> dispatcher = service.createDispatch(port, Source.class, Service.Mode.MESSAGE);
        dispatcher.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                           "http://localhost:" + PORT + "/server");

        dispatcher.invoke(request);
    }

    private Source readResource(String resName) {
        return new StreamSource(this.getClass().getClassLoader().getResourceAsStream(resName));
    }


}
