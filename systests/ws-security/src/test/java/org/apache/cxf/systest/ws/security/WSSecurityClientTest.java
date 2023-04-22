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
import java.util.Arrays;
import java.util.Collection;
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

import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.http.HTTPBinding;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.wss4j.common.ext.WSSecurityException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSecurityClientTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String STAX_PORT = allocatePort(StaxServer.class);
    public static final String DEC_PORT = allocatePort(WSSecurityClientTest.class);

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

    final TestParam test;

    public WSSecurityClientTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        createStaticBus();
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(STAX_PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testUsernameToken() throws Exception {
        final jakarta.xml.ws.Service svc
            = jakarta.xml.ws.Service.create(WSDL_LOC, GREETER_SERVICE_QNAME);
        final Greeter greeter = svc.getPort(USERNAME_TOKEN_PORT_QNAME, Greeter.class);
        updateAddressPort(greeter, test.getPort());

        Client client = ClientProxy.getClient(greeter);
        Map<String, Object> props = new HashMap<>();
        props.put("action", "UsernameToken");
        props.put("user", "alice");
        props.put("passwordType", "PasswordText");
        WSS4JOutInterceptor wss4jOut = new WSS4JOutInterceptor(props);

        client.getOutInterceptors().add(wss4jOut);

        ((BindingProvider)greeter).getRequestContext().put("password", "password");

        try {
            greeter.greetMe("CXF");
            fail("should fail because of password text instead of digest");
        } catch (Exception ex) {
            //expected
        }

        props.put("passwordType", "PasswordDigest");
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

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testUsernameTokenStreaming() throws Exception {
        final jakarta.xml.ws.Service svc
            = jakarta.xml.ws.Service.create(WSDL_LOC, GREETER_SERVICE_QNAME);
        final Greeter greeter = svc.getPort(USERNAME_TOKEN_PORT_QNAME, Greeter.class);
        updateAddressPort(greeter, test.getPort());

        Client client = ClientProxy.getClient(greeter);
        Map<String, Object> props = new HashMap<>();
        props.put("action", "UsernameToken");
        props.put("user", "alice");
        props.put("passwordType", "PasswordText");
        WSS4JStaxOutInterceptor wss4jOut = new WSS4JStaxOutInterceptor(props);

        client.getOutInterceptors().add(wss4jOut);

        ((BindingProvider)greeter).getRequestContext().put("password", "password");

        try {
            greeter.greetMe("CXF");
            fail("should fail because of password text instead of digest");
        } catch (Exception ex) {
            //expected
        }
        client.getOutInterceptors().remove(wss4jOut);

        props.put("passwordType", "PasswordDigest");
        wss4jOut = new WSS4JStaxOutInterceptor(props);
        client.getOutInterceptors().add(wss4jOut);
        String s = greeter.greetMe("CXF");
        assertEquals("Hello CXF", s);
        client.getOutInterceptors().remove(wss4jOut);

        try {
            ((BindingProvider)greeter).getRequestContext().put("password", "foo");
            wss4jOut = new WSS4JStaxOutInterceptor(props);
            client.getOutInterceptors().add(wss4jOut);
            greeter.greetMe("CXF");
            fail("should fail");
        } catch (Exception ex) {
            //expected
        }
        client.getOutInterceptors().remove(wss4jOut);
        try {
            props.put("passwordType", "PasswordText");
            wss4jOut = new WSS4JStaxOutInterceptor(props);
            client.getOutInterceptors().add(wss4jOut);
            ((BindingProvider)greeter).getRequestContext().put("password", "password");
            greeter.greetMe("CXF");
            fail("should fail");
        } catch (Exception ex) {
            //expected
        }
        client.getOutInterceptors().remove(wss4jOut);

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testTimestampSignEncrypt() throws Exception {
        Bus b = new SpringBusFactory()
            .createBus("org/apache/cxf/systest/ws/security/client.xml");
        BusFactory.setDefaultBus(b);
        final jakarta.xml.ws.Service svc = jakarta.xml.ws.Service.create(
            WSDL_LOC,
            GREETER_SERVICE_QNAME
        );
        final Greeter greeter = svc.getPort(
            TIMESTAMP_SIGN_ENCRYPT_PORT_QNAME,
            Greeter.class
        );
        updateAddressPort(greeter, test.getPort());

        // Add a No-Op JAX-WS SoapHandler to the dispatch chain to
        // verify that the SoapHandlerInterceptor can peacefully co-exist
        // with the explicitly configured SAAJOutInterceptor
        //
        @SuppressWarnings("rawtypes")
        List<Handler> handlerChain = new ArrayList<>();
        Binding binding = ((BindingProvider)greeter).getBinding();
        TestOutHandler handler = new TestOutHandler();
        handlerChain.add(handler);
        binding.setHandlerChain(handlerChain);

        greeter.sayHi();

        assertTrue("expected Handler.handleMessage() to be called",
                   handler.handleMessageCalledOutbound);
        assertFalse("expected Handler.handleFault() not to be called",
                    handler.handleFaultCalledOutbound);
        ((java.io.Closeable)greeter).close();
        b.shutdown(true);
        BusFactory.setDefaultBus(getStaticBus());
    }

    @Test
    public void testMalformedSecurityHeaders() throws java.lang.Exception {
        Dispatch<Source> dispatcher = null;
        java.io.InputStream is = null;
        String result = null;
        //
        // Old Created Date should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(test.getPort());
        is = getClass().getResourceAsStream(
            "test-data/UsernameTokenRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);

        //
        // Sending no security headers should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(test.getPort());
        is = getClass().getResourceAsStream(
            "test-data/NoHeadersRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
        //
        // Sending and empty header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(test.getPort());
        is = getClass().getResourceAsStream(
            "test-data/EmptyHeaderRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
        //
        // Sending and empty security header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(test.getPort());
        is = getClass().getResourceAsStream(
            "test-data/EmptySecurityHeaderRequest.xml"
        );
        result = source2String(dispatcher.invoke(new StreamSource(is)));
        assertTrue(result.indexOf("Fault") != -1);
    }

    @Test
    public void testDecoupledFaultFromSecurity() throws Exception {
        Dispatch<Source> dispatcher = null;
        java.io.InputStream is = null;

        //
        // Sending no security headers should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(true, test.getPort());
        is = getClass().getResourceAsStream("test-data/NoHeadersRequest.xml");
        try {
            dispatcher.invoke(new StreamSource(is));
            fail("exception should have been generated");
        } catch (SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }

        //
        // Sending and empty header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(true, test.getPort());
        is = getClass().getResourceAsStream("test-data/EmptyHeaderRequest.xml");
        try {
            dispatcher.invoke(new StreamSource(is));
            fail("exception should have been generated");
        } catch (SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }
        //
        // Sending and empty security header should result in a Fault
        //
        dispatcher = createUsernameTokenDispatcher(true, test.getPort());
        is = getClass().getResourceAsStream("test-data/EmptySecurityHeaderRequest.xml");
        try {
            dispatcher.invoke(new StreamSource(is));
            fail("exception should have been generated");
        } catch (SOAPFaultException ex) {
            assertEquals(ex.getMessage(), WSSecurityException.UNIFIED_SECURITY_ERR);
        }

    }
    private static Dispatch<Source> createUsernameTokenDispatcher(String port) {
        return createUsernameTokenDispatcher(false, port);
    }
    private static Dispatch<Source> createUsernameTokenDispatcher(boolean decoupled, String port) {
        final Service service = Service.create(
            GREETER_SERVICE_QNAME
        );
        service.addPort(
            USERNAME_TOKEN_PORT_QNAME,
            decoupled ? SOAPBinding.SOAP11HTTP_BINDING : HTTPBinding.HTTP_BINDING,
            "http://localhost:" + port + "/GreeterService/UsernameTokenPort"
        );
        final Dispatch<Source> dispatcher = service.createDispatch(
            USERNAME_TOKEN_PORT_QNAME,
            Source.class,
            Service.Mode.MESSAGE,
            new AddressingFeature(decoupled, decoupled)
        );
        final java.util.Map<String, Object> requestContext =
            dispatcher.getRequestContext();
        requestContext.put(
            MessageContext.HTTP_REQUEST_METHOD,
            "POST"
        );
        if (decoupled) {
            HTTPConduit cond = (HTTPConduit)((DispatchImpl<?>)dispatcher).getClient().getConduit();
            cond.getClient().setDecoupledEndpoint("http://localhost:" + DEC_PORT + "/decoupled");
        }
        return dispatcher;
    }

    private static String source2String(Source source) throws Exception {
        final java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        final StreamResult sr = new StreamResult(bos);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        final java.util.Properties oprops = new java.util.Properties();
        oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperties(oprops);
        transformer.transform(source, sr);
        return bos.toString();
    }
}
