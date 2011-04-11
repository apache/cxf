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
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.policytest.doubleit.DoubleIt;
import org.apache.cxf.policytest.doubleit.DoubleItFault_Exception;
import org.apache.cxf.policytest.doubleit.DoubleItPortType;
import org.apache.cxf.policytest.doubleit.DoubleItPortTypeHeader;
import org.apache.cxf.policytest.doubleit.DoubleItResponse;
import org.apache.cxf.policytest.doubleit.DoubleItService;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
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
    
    private DoubleItService service = new DoubleItService();

    
    @BeforeClass 
    public static void init() throws Exception {
        
        createStaticBus(SecurityPolicyTest.class.getResource("https_config.xml").toString())
            .getExtension(PolicyEngine.class).setEnabled(true);
        getStaticBus().getOutInterceptors().add(new LoggingOutInterceptor());
        EndpointImpl ep = (EndpointImpl)Endpoint.publish(POLICY_HTTPS_ADDRESS,
                                       new DoubleItImplHttps());
        ep.getServer().getEndpoint().getEndpointInfo().setProperty(SecurityConstants.CALLBACK_HANDLER,
                                                                   new ServerPasswordCallback());
        Endpoint.publish(POLICY_ADDRESS,
                         new DoubleItImpl());
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_ENCSIGN_ADDRESS,
                                            new DoubleItImplEncryptThenSign());
        
        EndpointInfo ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGNENC_ADDRESS,
                                            new DoubleItImplSignThenEncrypt());
        
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGN_ADDRESS,
                                            new DoubleItImplSign());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_XPATH_ADDRESS,
                                            new DoubleItImplXPath());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGNENC_PROVIDER_ADDRESS,
                                            new DoubleItProvider());
        
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_SIGNONLY_ADDRESS,
                                            new DoubleItImplSignOnly());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_CXF3041_ADDRESS,
                                            new DoubleItImplCXF3041());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("bob.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        
        ep = (EndpointImpl)Endpoint.publish(POLICY_CXF3042_ADDRESS,
                                            new DoubleItImplCXF3042());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

        ep = (EndpointImpl)Endpoint.publish(POLICY_CXF3452_ADDRESS,
                                            new DoubleItImplCXF3452());
        ei = ep.getServer().getEndpoint().getEndpointInfo(); 
        ei.setProperty(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ei.setProperty(SecurityConstants.SIGNATURE_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());
        ei.setProperty(SecurityConstants.ENCRYPT_PROPERTIES, 
                       SecurityPolicyTest.class.getResource("alice.properties").toString());

    }
    
    @Test
    public void testPolicy() throws Exception {
        DoubleItPortType pt;

        pt = service.getDoubleItPortXPath();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        assertEquals(BigInteger.valueOf(10), pt.doubleIt(BigInteger.valueOf(5)));
        
        
        pt = service.getDoubleItPortEncryptThenSign();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));

        
        pt = service.getDoubleItPortSign();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));


        pt = service.getDoubleItPortSignThenEncrypt();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        pt.doubleIt(BigInteger.valueOf(5));
        
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_SIGNENC_PROVIDER_ADDRESS);
        int x = pt.doubleIt(BigInteger.valueOf(5)).intValue();
        assertEquals(10, x);
        
        pt = service.getDoubleItPortHttps();
        updateAddressPort(pt, SSL_PORT);
        try {
            pt.doubleIt(BigInteger.valueOf(25));
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (!msg.contains("sername")) {
                throw ex;
            }
        }
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_USERNAME, "bob");
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.PASSWORD, "pwd");
        pt.doubleIt(BigInteger.valueOf(25));
        
        try {
            pt = service.getDoubleItPortHttp();
            updateAddressPort(pt, PORT);
            pt.doubleIt(BigInteger.valueOf(25));
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
        DoubleItPortType pt;

        pt = service.getDoubleItPortSignedOnly();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        //This should work as it should be properly signed.
        assertEquals(BigInteger.valueOf(10), pt.doubleIt(BigInteger.valueOf(5)));
        
        StringWriter swriter = new StringWriter();
        PrintWriter writer = new PrintWriter(swriter);
        try {
            ClientProxy.getClient(pt).getInInterceptors()
                .add(new LoggingInInterceptor("CheckFaultLogging", writer));
            pt.doubleIt(BigInteger.valueOf(-100));
            fail("Should have resulted in a DoubleItFault_Exception");
        } catch (DoubleItFault_Exception ex) {
            //expected
            writer.flush();
            String s = swriter.toString();
            s = s.substring(s.indexOf("Payload: ") + 9);
            s = s.substring(0, s.lastIndexOf("Envelope>") + 9);
            assertTrue("Content wasn't encrypted!", !s.contains("I don't like that."));
            //System.out.println(s);
            Document d = XMLUtils.parse(new InputSource(new StringReader(s)));
            Node nd = d.getDocumentElement().getFirstChild();
            while (nd != null && !"Body".equals(nd.getLocalName())) {
                nd = nd.getNextSibling();
            }
            if (nd == null) {
                throw ex;
            }
            //System.out.println(s);
            Attr val = ((org.w3c.dom.Element)nd)
                .getAttributeNodeNS(PolicyConstants.WSU_NAMESPACE_URI, "Id");
            assertNotNull("No wsu:Id, thus, not signed", val);
        }

        
        //Try sending a message with the "TimestampOnly" policy into affect to the 
        //service running the "signed only" policy.  This SHOULD fail as the
        //body is then not signed.
        pt = service.getDoubleItPortTimestampOnly();
        ((BindingProvider)pt).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                      POLICY_SIGNONLY_ADDRESS);
        try {
            pt.doubleIt(BigInteger.valueOf(5));
            fail("should have had a security/policy exception as the body wasn't signed");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("policy alternatives"));
        }
        
    }
    
    @Test
    public void testDispatchClient() throws Exception {
        Dispatch<Source> disp = service.createDispatch(DoubleItService.DoubleItPortEncryptThenSign, 
                                                       Source.class,
                                                       Mode.PAYLOAD);
        
        disp.getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                     new KeystorePasswordCallback());
        disp.getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                     getClass().getResource("alice.properties"));
        disp.getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                     getClass().getResource("bob.properties"));
        updateAddressPort(disp, PORT);

        String req = "<ns2:DoubleIt xmlns:ns2=\"http://cxf.apache.org/policytest/DoubleIt\">"
            + "<numberToDouble>25</numberToDouble></ns2:DoubleIt>";
        Source source = new StreamSource(new StringReader(req));
        source = disp.invoke(source);
        
        Node nd = XMLUtils.fromSource(source);
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("ns2", "http://cxf.apache.org/policytest/DoubleIt");
        XPathUtils xp = new XPathUtils(ns);
        Object o = xp.getValue("//ns2:DoubleItResponse/doubledNumber", nd, XPathConstants.STRING);
        assertEquals(XMLUtils.toString(nd), "50", o);
    }
    
    public abstract static class AbstractDoubleItImpl implements DoubleItPortType {
        /** {@inheritDoc}*/
        public BigInteger doubleIt(BigInteger numberToDouble) throws DoubleItFault_Exception {
            if (numberToDouble.equals(BigInteger.valueOf(-100))) {
                org.apache.cxf.policytest.doubleit.DoubleItFault f 
                    = new org.apache.cxf.policytest.doubleit.DoubleItFault();
                f.setReason("Number is -100.  I don't like that.");
                throw new DoubleItFault_Exception("DoubleItException.", f);
            }
            return numberToDouble.multiply(new BigInteger("2"));
        }
    }
    
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortHttp",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImpl extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortHttps",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplHttps extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortEncryptThenSign",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplEncryptThenSign extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortSignThenEncrypt",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplSignThenEncrypt extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortSign",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplSign extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortXPath",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplXPath extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortSignedOnly",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplSignOnly extends AbstractDoubleItImpl {
    }
    
    @WebServiceProvider(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                        portName = "DoubleItPortSignThenEncrypt",
                        serviceName = "DoubleItService", 
                        wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl") 
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
            ns.put("ns2", "http://cxf.apache.org/policytest/DoubleIt");
            XPathUtils xp = new XPathUtils(ns);
            String o = (String)xp.getValue("//ns2:DoubleIt/numberToDouble", el, XPathConstants.STRING);
            int i = Integer.parseInt(o);
            
            String req = "<ns2:DoubleItResponse xmlns:ns2=\"http://cxf.apache.org/policytest/DoubleIt\">"
                + "<doubledNumber>" + Integer.toString(i * 2) + "</doubledNumber></ns2:DoubleItResponse>";
            return new StreamSource(new StringReader(req));
        }
        
    }
    
    
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortCXF3041",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplCXF3041 extends AbstractDoubleItImpl {
    }
    
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortCXF3042",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortType",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplCXF3042 extends AbstractDoubleItImpl {
    }
    @WebService(targetNamespace = "http://cxf.apache.org/policytest/DoubleIt", 
                portName = "DoubleItPortCXF3452",
                serviceName = "DoubleItService", 
                endpointInterface = "org.apache.cxf.policytest.doubleit.DoubleItPortTypeHeader",
                wsdlLocation = "classpath:/wsdl_systest_wssec/DoubleIt.wsdl")
    public static class DoubleItImplCXF3452 implements DoubleItPortTypeHeader {
        public DoubleItResponse doubleIt(DoubleIt parameters, int header) throws DoubleItFault_Exception {
            DoubleItResponse r = new DoubleItResponse();
            r.setDoubledNumber(parameters.getNumberToDouble().shiftLeft(header));
            return r;
        }
    }
    @Test
    public void testCXF3041() throws Exception {
        DoubleItPortType pt;

        pt = service.getDoubleItPortCXF3041();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("bob.properties"));
        assertEquals(BigInteger.valueOf(10), pt.doubleIt(BigInteger.valueOf(5)));
    }

    @Test
    public void testCXF3042() throws Exception {
        DoubleItPortType pt;
        pt = service.getDoubleItPortCXF3042();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("alice.properties"));
        assertEquals(BigInteger.valueOf(10), pt.doubleIt(BigInteger.valueOf(5)));
    }
    @Test
    public void testCXF3452() throws Exception {
        DoubleItPortTypeHeader pt;
        pt = service.getDoubleItPortCXF3452();
        updateAddressPort(pt, PORT);
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.CALLBACK_HANDLER, 
                                                      new KeystorePasswordCallback());
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.SIGNATURE_PROPERTIES,
                                                      getClass().getResource("alice.properties"));
        ((BindingProvider)pt).getRequestContext().put(SecurityConstants.ENCRYPT_PROPERTIES, 
                                                      getClass().getResource("alice.properties"));
        
        DoubleIt di = new DoubleIt();
        di.setNumberToDouble(BigInteger.valueOf(5));
        assertEquals(BigInteger.valueOf(10), pt.doubleIt(di, 1).getDoubledNumber());
    }
}
