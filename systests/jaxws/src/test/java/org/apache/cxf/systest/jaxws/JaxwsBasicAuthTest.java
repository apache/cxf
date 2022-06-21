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

import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class JaxwsBasicAuthTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(JaxwsBasicAuthTest.class);

    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            jakarta.xml.ws.Endpoint.publish(address, implementor);
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

        @WebService(serviceName = "ProtectedGreeterService",
                    portName = "GreeterPort",
                    endpointInterface = "org.apache.cxf.greeter_control.Greeter",
                    targetNamespace = "http://cxf.apache.org/greeter_control",
                    wsdlLocation = "testutils/greeter_control.wsdl")
        public class GreeterImpl extends AbstractGreeterImpl {
            @Resource private WebServiceContext context;
            
            @Override
            public String greetMe(String arg) {
                final MessageContext messageContext = context.getMessageContext();
                
                final Map<String, List<String>> headers =
                    CastUtils.cast((Map<?, ?>)messageContext.get(MessageContext.HTTP_REQUEST_HEADERS));

                if (headers == null) {
                    throw new WebServiceException("Not authorized");
                }

                final String authorization = headers.get("Authorization").get(0);
                final String expected = DefaultBasicAuthSupplier.getBasicAuthHeader("user", "test", true);
                
                if (!expected.equals(authorization)) {
                    throw new WebServiceException("Not authorized");
                }

                return "CXF is protected: " + arg;
            }
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        stopAllServers();
    }

    @Test
    public void testUseBasicAuthFromClient() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        // set a fake address to kick off the failover feature
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);

        Client clientProxy = ClientProxy.getClient(proxy);
        HTTPConduit conduit = (HTTPConduit) clientProxy.getConduit();
        conduit.getAuthorization().setAuthorizationType("Basic");
        conduit.getAuthorization().setUserName("user");
        conduit.getAuthorization().setPassword("test");
        
        final BindingProvider bindingProvider = (BindingProvider) proxy;
        bindingProvider.getRequestContext().put("encode.basicauth.with.iso8859", true);

        String response = proxy.greetMe("cxf");
        assertThat("CXF is protected: cxf", equalTo(response));
    }

}
