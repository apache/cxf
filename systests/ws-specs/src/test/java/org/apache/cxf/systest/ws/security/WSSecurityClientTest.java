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

package org.apache.cxf.systest.ws.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class WSSecurityClientTest extends AbstractBusClientServerTestBase {

    private static final java.net.URL WSDL_LOC;
    static {
        java.net.URL tmp = null;
        try {
            tmp = WSSecurityClientTest.class.getClassLoader().getResource(
                "org/apache/cxf/systest/ws/security/hello_world.wsdl"
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        WSDL_LOC = tmp;
    }

    private static final QName GREETER_SERVICE_QNAME =
        new QName(
            "http://apache.org/hello_world_soap_http",
            "GreeterService"
        );

    private static final QName TIMESTAMP_SIGN_ENCRYPT_PORT_QNAME =
        new QName(
            "http://apache.org/hello_world_soap_http",
            "TimestampSignEncryptPort"
        );

    private static final QName USERNAME_TOKEN_PORT_QNAME =
        new QName(
            "http://apache.org/hello_world_soap_http",
            "UsernameTokenPort"
        );

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }
    
    @Test
    public void testUsernameToken() {
        final javax.xml.ws.Service svc 
            = javax.xml.ws.Service.create(WSDL_LOC, GREETER_SERVICE_QNAME);
        final Greeter greeter = svc.getPort(USERNAME_TOKEN_PORT_QNAME, Greeter.class);
        
        Client client = ClientProxy.getClient(greeter);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("action", "UsernameToken");
        props.put("user", "alice");
        WSS4JOutInterceptor wss4jOut = new WSS4JOutInterceptor(props);
        
        client.getOutInterceptors().add(wss4jOut);

        ((BindingProvider)greeter).getRequestContext().put("password", "password");
        String s = greeter.greetMe("CXF");
        assertEquals("Hello CXF", s);
        
        try {
            ((BindingProvider)greeter).getRequestContext().put("password", "foo");
            greeter.greetMe("CXF");
            fail("should fail");
        } catch (Exception ex) {
            //expected
        }
        try {
            props.put("passwordType", "PasswordText");
            ((BindingProvider)greeter).getRequestContext().put("password", "password");
            greeter.greetMe("CXF");
            fail("should fail");
        } catch (Exception ex) {
            //expected
        }
    }

    @Test
    public void testTimestampSignEncrypt() {
        BusFactory.setDefaultBus(
            new SpringBusFactory().createBus(
                "org/apache/cxf/systest/ws/security/client.xml"
            )
        );
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(
            WSDL_LOC,
            GREETER_SERVICE_QNAME
        );
        final Greeter greeter = svc.getPort(
            TIMESTAMP_SIGN_ENCRYPT_PORT_QNAME,
            Greeter.class
        );

        // Add a No-Op JAX-WS SoapHandler to the dispatch chain to
        // verify that the SoapHandlerInterceptor can peacefully co-exist
        // with the explicitly configured SAAJOutInterceptor
        //
        List<Handler> handlerChain = new ArrayList<Handler>();
        Binding binding = ((BindingProvider)greeter).getBinding();
        TestOutHandler handler = new TestOutHandler();
        handlerChain.add(handler);
        binding.setHandlerChain(handlerChain);

        greeter.sayHi();

        assertTrue("expected Handler.handleMessage() to be called", 
                   handler.handleMessageCalledOutbound);
        assertFalse("expected Handler.handleFault() not to be called", 
                    handler.handleFaultCalledOutbound); 
    }

    @Test
    public void testMalformedSecurityHeaders() throws java.lang.Exception {
        Dispatch<Source> dispatcher = null;
        java.io.InputStream is = null;
        String result = null;
        //
        // Check to ensure that a well-formed request will pass
        //
        dispatcher = createUsernameTokenDispatcher();
        is = getClass().getResourceAsStream(
            "test-data/UsernameTokenRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Bonjour") != -1);
        //make sure the principal was set
        assertNotNull(GreeterImpl.getUser());
        assertEquals("alice", GreeterImpl.getUser().getName());
        
        //
        // Sending no security headers should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher();
        is = getClass().getResourceAsStream(
            "test-data/NoHeadersRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
        //
        // Sending and empty header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher();
        is = getClass().getResourceAsStream(
            "test-data/EmptyHeaderRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
        //
        // Sending and empty security header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher();
        is = getClass().getResourceAsStream(
            "test-data/EmptySecurityHeaderRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
    }

    private static Dispatch<Source> createUsernameTokenDispatcher() {
        final Service service = Service.create(
            GREETER_SERVICE_QNAME
        );
        service.addPort(
            USERNAME_TOKEN_PORT_QNAME,
            HTTPBinding.HTTP_BINDING,
            "http://localhost:9000/GreeterService/UsernameTokenPort"
        );
        final Dispatch<Source> dispatcher = service.createDispatch(
            USERNAME_TOKEN_PORT_QNAME,
            Source.class,
            Service.Mode.MESSAGE
        );
        final java.util.Map<String, Object> requestContext =
            dispatcher.getRequestContext();
        requestContext.put(
            MessageContext.HTTP_REQUEST_METHOD,
            "POST"
        );
        return dispatcher;
    }

    private static String source2String(Source source) throws Exception {
        final java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        final StreamResult sr = new StreamResult(bos);
        final Transformer trans =
            TransformerFactory.newInstance().newTransformer();
        final java.util.Properties oprops = new java.util.Properties();
        oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperties(oprops);
        trans.transform(source, sr);
        return bos.toString();
    }
}
