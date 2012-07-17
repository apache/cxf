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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.schemavalidation.CkRequestType;
import org.apache.cxf.jaxws.schemavalidation.CkResponseType;
import org.apache.cxf.jaxws.schemavalidation.RequestIdType;
import org.apache.cxf.jaxws.schemavalidation.Service;
import org.apache.cxf.jaxws.schemavalidation.ServicePortType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.junit.BeforeClass;
import org.junit.Test;



public class SchemaValidationClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

    private final QName portName = new QName("http://cxf.apache.org/jaxws/schemavalidation", "servicePort");

    public static class Server extends AbstractBusTestServerBase {        

        protected void run()  {
            String address;
            Object implementor = new ServicePortTypeImpl();
            address = "http://localhost:" + PORT + "/schemavalidation";
            Endpoint ep = Endpoint.create(implementor);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("schema-validation-enabled", Boolean.TRUE);
            ep.setProperties(map);
            ((EndpointImpl)ep).setWsdlLocation("wsdl_systest_jaxws/schemaValidation.wsdl");
            ((EndpointImpl)ep).setServiceName(new QName(
                                   "http://cxf.apache.org/jaxws/schemavalidation", "service"));
            ((EndpointImpl)ep).getInInterceptors().add(new LoggingInInterceptor());
            ((EndpointImpl)ep).getOutInterceptors().add(new LoggingOutInterceptor());
            ep.publish(address);
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
   
    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
   
    @Test
    public void testSchemaValidationWithMultipleXsds() throws Exception {
        Service service = new Service();
        assertNotNull(service);

        ServicePortType greeter = service.getPort(portName, ServicePortType.class);
        ClientProxy.getClient(greeter).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor());
        updateAddressPort(greeter, PORT);

        RequestIdType requestId = new RequestIdType();
        requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CkRequestType request = new CkRequestType();
        request.setRequest(requestId);
        ((BindingProvider)greeter).getRequestContext().put("schema-validation-enabled", Boolean.TRUE);
        CkResponseType response = greeter.ckR(request); 
        assertEquals(response.getProduct().get(0).getAction().getStatus(), 4);
        
        try {
            requestId.setId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeeez");
            request.setRequest(requestId);
            greeter.ckR(request);
            fail("should catch marshall exception as the invalid outgoing message per schema");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Marshalling Error"));
            assertTrue(e.getMessage().contains("is not facet-valid with respect to pattern"));
        }
            
    }
    
}
