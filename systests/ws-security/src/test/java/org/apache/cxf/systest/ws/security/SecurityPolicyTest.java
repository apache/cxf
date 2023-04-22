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

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.ws.common.DoubleItPortTypeImpl;
import org.apache.cxf.systest.ws.common.KeystorePasswordCallback;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.example.contract.doubleit.DoubleItPortType;
import org.example.contract.doubleit.DoubleItPortTypeHeader;
import org.example.schema.doubleit.DoubleIt;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SecurityPolicyTest extends AbstractBusClientServerTestBase  {
    public static final String PORT = allocatePort(SecurityPolicyTest.class);
    public static final String SSL_PORT = allocatePort(SecurityPolicyTest.class, 1);

    public static final String POLICY_ADDRESS = "http://localhost:" + PORT + "/SecPolTest";
    public static final String POLICY_HTTPS_ADDRESS = "https://localhost:" + SSL_PORT + "/SecPolTest";
    public static final String POLICY_ENCSIGN_ADDRESS = "http://localhost:"
            + PORT + "/SecPolTestEncryptThenSign";
    public static final String POLICY_SIGNENC_ADDRESS = "http://localhost:"
            + PORT + "/SecPolTestSignThenEncrypt";
    public static final String POLICY_SIGNENC_PROVIDER_ADDRESS
        = "http://localhost:" + PORT + "/SecPolTestSignThenEncryptProvider";
    public static final String POLICY_FAULT_SIGNENC_PROVIDER_ADDRESS
    = "http://localhost:" + PORT + "/SecPolTestFaultSignThenEncryptProvider";
    public static final String POLICY_SIGN_ADDRESS = "http://localhost:" + PORT + "/SecPolTestSign";
    public static final String POLICY_XPATH_ADDRESS = "http://localhost:" + PORT + "/SecPolTestXPath";
    public static final String POLICY_SIGNONLY_ADDRESS = "http://localhost:" + PORT + "/SecPolTestSignedOnly";

    public static final String POLICY_CXF3041_ADDRESS = "http://localhost:" + PORT + "/SecPolTestCXF3041";
    public static final String POLICY_CXF3042_ADDRESS = "http://localhost:" + PORT + "/SecPolTestCXF3042";
    public static final String POLICY_CXF3452_ADDRESS = "http://localhost:" + PORT + "/SecPolTestCXF3452";
    public static final String POLICY_CXF4122_ADDRESS = "http://localhost:" + PORT + "/SecPolTestCXF4122";

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    public static class ServerPasswordCallback implements CallbackHandler {
        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            if ("bob".equals(pc.getIdentifier())) {
                // set the password on the callback. This will be compared to the
                // password which was sent from the client.
                pc.setPassword("pwd");
            }
        }
    }

    @BeforeClass
    public static void init() throws Exception {

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");

        createStaticBus(SecurityPolicyTest.class.getResource("https_config.xml").toString())
            .getExtension(PolicyEngine.class).setEnabled(true);
        getStaticBus().getOutInterceptors().add(new LoggingOutInterceptor());

        DoubleItPortTypeImpl implementor = new DoubleItPortTypeImpl();
        implementor.setEnforcePrincipal(false);
        EndpointImpl ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortHttps"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_HTTPS_ADDRESS);
        ep.publish();
        ep.getServer().getEndpoint().getEndpointInfo().setProperty(SecurityConstants.CALLBACK_HANDLER,
                                                                   new ServerPasswordCallback());
        Endpoint.publish(POLICY_ADDRESS, implementor);

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(
            new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortEncryptThenSign")
        );
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_ENCSIGN_ADDRESS);
        ep.publish();
        EndpointInfo ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(
            new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSignThenEncrypt")
        );
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGNENC_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSign"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGN_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortXPath"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_XPATH_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "alice.properties", "bob.properties");

        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGNENC_PROVIDER_ADDRESS,
                                            new DoubleItProvider());

        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.publish(POLICY_FAULT_SIGNENC_PROVIDER_ADDRESS,
                                            new DoubleItFaultProvider());

        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSignedOnly"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGNONLY_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3041"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3041_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3042"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3042_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "alice.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3452"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3452_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "alice.properties", "alice.properties");
        ei.setProperty(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    private static void setCryptoProperties(EndpointInfo ei, String sigProps, String encProps) {
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, sigProps);
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, encProps);
    }

    @Test
    public void testPolicy() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        URL busFile = SecurityPolicyTest.class.getResource("https_config_client.xml");
        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortXPath");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");
        assertEquals(10, pt.doubleIt(5));
        ((java.io.Closeable)pt).close();

        portQName = new QName(NAMESPACE, "DoubleItPortEncryptThenSign");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");

        // DOM
        assertEquals(10, pt.doubleIt(5));

        // TODO See WSS-464
        // SecurityTestUtil.enableStreaming(pt);
        // assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();

        portQName = new QName(NAMESPACE, "DoubleItPortSign");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");
        // DOM
        assertEquals(10, pt.doubleIt(5));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();

        portQName = new QName(NAMESPACE, "DoubleItPortSignThenEncrypt");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");

        // DOM
        assertEquals(10, pt.doubleIt(5));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();

        portQName = new QName(NAMESPACE, "DoubleItPortHttps");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, SSL_PORT);
        try {
            pt.doubleIt(25);
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!msg.contains("sername")) {
                throw ex;
            }
        }
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.PASSWORD, "pwd");

        // DOM
        assertEquals(50, pt.doubleIt(25));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(50, pt.doubleIt(25));

        ((java.io.Closeable)pt).close();

        try {
            portQName = new QName(NAMESPACE, "DoubleItPortHttp");
            pt = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(pt, PORT);
            pt.doubleIt(25);
            fail("https policy should have triggered");
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!msg.contains("HttpsToken")) {
                throw ex;
            }
        }

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testSignedOnlyWithUnsignedMessage() throws Exception {
        //CXF-2244
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortSignedOnly");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");
        //This should work as it should be properly signed.

        // DOM
        assertEquals(10, pt.doubleIt(5));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();

        //Try sending a message with the "TimestampOnly" policy into affect to the
        //service running the "signed only" policy.  This SHOULD fail as the
        //body is then not signed.
        portQName = new QName(NAMESPACE, "DoubleItPortTimestampOnly");
        pt = service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_SIGNONLY_ADDRESS);
        // DOM
        try {
            pt.doubleIt(5);
            fail("should have had a security/policy exception as the body wasn't signed");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("policy alternatives"));
        }

        // Streaming
        try {
            SecurityTestUtil.enableStreaming(pt);
            pt.doubleIt(5);
            fail("should have had a security/policy exception as the body wasn't signed");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testDispatchClient() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        QName portQName = new QName(NAMESPACE, "DoubleItPortEncryptThenSign");
        Dispatch<Source> disp = service.createDispatch(portQName, Source.class, Mode.PAYLOAD);

        disp.getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                     new KeystorePasswordCallback());
        disp.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                     "alice.properties");
        disp.getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                     "bob.properties");
        updateAddressPort(disp, PORT);

        String req = "<ns2:DoubleIt xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
            + "<numberToDouble>25</numberToDouble></ns2:DoubleIt>";
        Source source = new StreamSource(new StringReader(req));
        source = disp.invoke(source);

        Node nd = StaxUtils.read(source);
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        Map<String, String> ns = new HashMap<>();
        ns.put("ns2", "http://www.example.org/schema/DoubleIt");
        XPathUtils xp = new XPathUtils(ns);
        Object o = xp.getValue("//ns2:DoubleItResponse/doubledNumber", nd, XPathConstants.STRING);
        assertEquals(StaxUtils.toString(nd), "50", o);

        bus.shutdown(true);
    }

    @WebServiceProvider(targetNamespace = "http://www.example.org/contract/DoubleIt",
                        portName = "DoubleItPortSignThenEncrypt",
                        serviceName = "DoubleItService",
                        wsdlLocation = "classpath:/org/apache/cxf/systest/ws/security/DoubleIt.wsdl")
    @ServiceMode(value = Mode.PAYLOAD)
    public static class DoubleItProvider implements Provider<Source> {

        public Source invoke(Source obj) {
            //CHECK the incoming

            Node el;
            try {
                el = StaxUtils.read(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (el instanceof Document) {
                el = ((Document)el).getDocumentElement();
            }
            Map<String, String> ns = new HashMap<>();
            ns.put("ns2", "http://www.example.org/schema/DoubleIt");
            XPathUtils xp = new XPathUtils(ns);
            String o = (String)xp.getValue("//ns2:DoubleIt/numberToDouble", el, XPathConstants.STRING);
            int i = Integer.parseInt(o);

            String req = "<ns2:DoubleItResponse xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
                + "<doubledNumber>" + Integer.toString(i * 2) + "</doubledNumber></ns2:DoubleItResponse>";
            return new StreamSource(new StringReader(req));
        }

    }

    @WebServiceProvider(targetNamespace = "http://www.example.org/contract/DoubleIt",
                        portName = "DoubleItFaultPortSignThenEncrypt",
                        serviceName = "DoubleItService",
                        wsdlLocation = "classpath:/org/apache/cxf/systest/ws/security/DoubleIt.wsdl")
    @ServiceMode(value = Mode.MESSAGE)
    public static class DoubleItFaultProvider implements Provider<SOAPMessage> {

        public SOAPMessage invoke(SOAPMessage request) {
            try {
                MessageFactory messageFactory = MessageFactory.newInstance();
                SOAPMessage msg = messageFactory.createMessage();
                msg.getSOAPBody().addFault(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Server"),
                                           "Foo");
                return msg;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    @Test
    public void testCXF3041() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3041");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");

        // DOM
        assertEquals(10, pt.doubleIt(5));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testCXF3042() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3042");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "alice.properties");

        // DOM
        assertEquals(10, pt.doubleIt(5));

        // Streaming
        SecurityTestUtil.enableStreaming(pt);
        assertEquals(10, pt.doubleIt(5));

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testCXF3452() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortTypeHeader pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3452");
        pt = service.getPort(portQName, DoubleItPortTypeHeader.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "alice.properties");

        DoubleIt di = new DoubleIt();
        di.setNumberToDouble(5);
        assertEquals(10, pt.doubleIt(di, 1).getDoubledNumber());

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testCXF4119() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortTypeHeader pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF4119");
        pt = service.getPort(portQName, DoubleItPortTypeHeader.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "revocation.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENABLE_REVOCATION,
                                                      "true");

        DoubleIt di = new DoubleIt();
        di.setNumberToDouble(5);
        try {
            pt.doubleIt(di, 1);
            fail("Failure expected on a revoked certificate");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)pt).close();
        bus.shutdown(true);
    }

    @Test
    public void testCXF4122() throws Exception {
        Bus epBus = BusFactory.newInstance().createBus();
        BusFactory.setDefaultBus(epBus);
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        DoubleItPortTypeImpl implementor = new DoubleItPortTypeImpl();
        implementor.setEnforcePrincipal(false);
        EndpointImpl ep = (EndpointImpl)Endpoint.create(implementor);
        ep.setEndpointName(
            new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF4122")
        );
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF4122_ADDRESS);
        ep.publish();
        EndpointInfo ei = ep.getServer().getEndpoint().getEndpointInfo();
        setCryptoProperties(ei, "bob.properties", "revocation.properties");
        ei.setProperty(SecurityConstants.ENABLE_REVOCATION, Boolean.TRUE);

        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        Service service = Service.create(wsdl, SERVICE_QNAME);

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF4122");
        DoubleItPortType pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "revocation.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");
        // DOM
        try {
            pt.doubleIt(5);
            fail("should fail on server side when do signature validation due the revoked certificates");
        } catch (Exception ex) {
            // expected
        }

        // TODO See WSS-464
        /*
        SecurityTestUtil.enableStreaming(pt);
        try {
            pt.doubleIt(5);
            fail("should fail on server side when do signature validation due the revoked certificates");
        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            // Different errors using different JDKs...
            System.out.println("ERR1: " + errorMessage);
        }
        */
        ((java.io.Closeable)pt).close();
        ep.stop();
        epBus.shutdown(true);
        bus.shutdown(true);
    }

    @Test
    public void testFault() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        URL busFile = SecurityPolicyTest.class.getResource("https_config_client.xml");
        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        QName portQName = new QName(NAMESPACE, "DoubleItFaultPortSignThenEncrypt");
        DoubleItPortType pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      "alice.properties");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      "bob.properties");

        // DOM
        try {
            pt.doubleIt(5);
            fail("SOAPFaultException expected!");
        } catch (SOAPFaultException e) {
            assertEquals("Foo", e.getFault().getFaultString());
        } finally {
            ((java.io.Closeable)pt).close();
            bus.shutdown(true);
        }
    }

}
