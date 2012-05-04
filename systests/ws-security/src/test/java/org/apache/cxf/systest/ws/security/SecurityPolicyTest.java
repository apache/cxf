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
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.systest.ws.common.DoubleItImpl;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;

import org.example.contract.doubleit.DoubleItPortType;
import org.example.contract.doubleit.DoubleItPortTypeHeader;
import org.example.schema.doubleit.DoubleIt;

import org.junit.BeforeClass;
import org.junit.Test;

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

            if (pc.getIdentifier().equals("bob")) {
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
        
        EndpointImpl ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortHttps"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_HTTPS_ADDRESS);
        ep.publish();
        ep.getServer().getEndpoint().getEndpointInfo().setProperty(SecurityConstants.CALLBACK_HANDLER,
                                                                   new ServerPasswordCallback());
        Endpoint.publish(POLICY_ADDRESS, new DoubleItImpl());
        
        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(
            new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortEncryptThenSign")
        );
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_ENCSIGN_ADDRESS);
        ep.publish();
        EndpointInfo ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(
            new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSignThenEncrypt")
        );
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGNENC_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSign"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGN_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "bob.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
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
        
        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortSignedOnly"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_SIGNONLY_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "bob.properties", "alice.properties");
        
        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3041"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3041_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "bob.properties", "alice.properties");
        
        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3042"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3042_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "alice.properties", "alice.properties");

        ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
        ep.setEndpointName(new QName("http://www.example.org/contract/DoubleIt", "DoubleItPortCXF3452"));
        ep.setWsdlLocation(wsdl.getPath());
        ep.setAddress(POLICY_CXF3452_ADDRESS);
        ep.publish();
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        setCryptoProperties(ei, "alice.properties", "alice.properties");
        ei.setProperty(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE); 
    }
    
    @org.junit.AfterClass
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }
    
    private static void setCryptoProperties(EndpointInfo ei, String sigProps, String encProps) {
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource(sigProps).toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource(encProps).toString());
    }
    
    @Test
    public void testPolicy() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        URL busFile = SecurityPolicyTest.class.getResource("https_config_client.xml");
        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortXPath");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        assertEquals(10, pt.doubleIt(5));
        
        portQName = new QName(NAMESPACE, "DoubleItPortEncryptThenSign");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(5);

        portQName = new QName(NAMESPACE, "DoubleItPortSign");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(5);

        portQName = new QName(NAMESPACE, "DoubleItPortSignThenEncrypt");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(5);
        
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_SIGNENC_PROVIDER_ADDRESS);
        int x = pt.doubleIt(5);
        assertEquals(10, x);
        
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
        pt.doubleIt(25);
        
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
    }
    
    @Test
    public void testSignedOnlyWithUnsignedMessage() throws Exception {
        //CXF-2244
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortSignedOnly");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        //This should work as it should be properly signed.
        assertEquals(10, pt.doubleIt(5));
        
        //Try sending a message with the "TimestampOnly" policy into affect to the 
        //service running the "signed only" policy.  This SHOULD fail as the
        //body is then not signed.
        portQName = new QName(NAMESPACE, "DoubleItPortTimestampOnly");
        pt = service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_SIGNONLY_ADDRESS);
        try {
            pt.doubleIt(5);
            fail("should have had a security/policy exception as the body wasn't signed");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("policy alternatives"));
        }
        
    }
    
    @Test
    public void testDispatchClient() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        QName portQName = new QName(NAMESPACE, "DoubleItPortEncryptThenSign");
        Dispatch<Source> disp = service.createDispatch(portQName, Source.class, Mode.PAYLOAD);
        
        disp.getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                     new KeystorePasswordCallback());
        disp.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                     getClass().getResource("alice.properties"));
        disp.getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                     getClass().getResource("bob.properties"));
        updateAddressPort(disp, PORT);

        String req = "<ns2:DoubleIt xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
            + "<numberToDouble>25</numberToDouble></ns2:DoubleIt>";
        Source source = new StreamSource(new StringReader(req));
        source = disp.invoke(source);
        
        Node nd = XMLUtils.fromSource(source);
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("ns2", "http://www.example.org/schema/DoubleIt");
        XPathUtils xp = new XPathUtils(ns);
        Object o = xp.getValue("//ns2:DoubleItResponse/doubledNumber", nd, XPathConstants.STRING);
        assertEquals(XMLUtils.toString(nd), "50", o);
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
                el = XMLUtils.fromSource(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (el instanceof Document) {
                el = ((Document)el).getDocumentElement();
            }
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("ns2", "http://www.example.org/schema/DoubleIt");
            XPathUtils xp = new XPathUtils(ns);
            String o = (String)xp.getValue("//ns2:DoubleIt/numberToDouble", el, XPathConstants.STRING);
            int i = Integer.parseInt(o);
            
            String req = "<ns2:DoubleItResponse xmlns:ns2=\"http://www.example.org/schema/DoubleIt\">"
                + "<doubledNumber>" + Integer.toString(i * 2) + "</doubledNumber></ns2:DoubleItResponse>";
            return new StreamSource(new StringReader(req));
        }
        
    }
    
    
    @Test
    public void testCXF3041() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3041");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        assertEquals(10, pt.doubleIt(5));
    }

    @Test
    public void testCXF3042() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortType pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3042");
        pt = service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("alice.properties"));
        assertEquals(10, pt.doubleIt(5));
    }
    
    @Test
    public void testCXF3452() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortTypeHeader pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF3452");
        pt = service.getPort(portQName, DoubleItPortTypeHeader.class);
        
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("alice.properties"));
        
        DoubleIt di = new DoubleIt();
        di.setNumberToDouble(5);
        assertEquals(10, pt.doubleIt(di, 1).getDoubledNumber());
    }
    
    @Test
    public void testCXF4119() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();

        Bus bus = bf.createBus();
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        
        DoubleItPortTypeHeader pt;

        QName portQName = new QName(NAMESPACE, "DoubleItPortCXF4119");
        pt = service.getPort(portQName, DoubleItPortTypeHeader.class);
        
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("revocation.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENABLE_REVOCATION, 
                                                      "true");
        
        DoubleIt di = new DoubleIt();
        di.setNumberToDouble(5);
        try {
            pt.doubleIt(di, 1);
            fail("Failure expected on a revoked certificate");
        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            // Different errors using different JDKs...
            assertTrue(errorMessage.contains("Certificate has been revoked")
                       || errorMessage.contains("Certificate revocation")
                       || errorMessage.contains("Error during certificate path validation"));
        }
    }
    
    @Test
    public void testCXF4122() throws Exception {
        URL wsdl = SecurityPolicyTest.class.getResource("DoubleIt.wsdl");
        EndpointImpl ep = (EndpointImpl)Endpoint.create(new DoubleItImpl());
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
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        Service service = Service.create(wsdl, SERVICE_QNAME);

        DoubleItPortType pt;

        QName
        portQName = new QName(NAMESPACE, "DoubleItPortCXF4122");
        pt = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER,
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("revocation.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES,
                                                      getClass().getResource("bob.properties"));
        try {
            pt.doubleIt(5);
            fail("should fail on server side when do signature validation due the revoked certificates");
        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            // Different errors using different JDKs...
            assertTrue(errorMessage.contains("Certificate has been revoked")
                       || errorMessage.contains("Certificate revocation")
                       || errorMessage.contains("Error during certificate path validation"));
        }

    }
}
