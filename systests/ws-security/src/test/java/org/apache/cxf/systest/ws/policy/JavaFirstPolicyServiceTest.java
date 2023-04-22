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

package org.apache.cxf.systest.ws.policy;

import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.ws.common.UTPasswordCallback;
import org.apache.cxf.systest.ws.policy.javafirst.BindingSimpleService;
import org.apache.cxf.systest.ws.policy.javafirst.NoAlternativesOperationSimpleService;
import org.apache.cxf.systest.ws.policy.javafirst.OperationSimpleService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.neethi.Constants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaFirstPolicyServiceTest extends AbstractBusClientServerTestBase {
    static final String PORT = JavaFirstPolicyServer.PORT;
    static final String PORT2 = JavaFirstPolicyServer.PORT2;
    static final String PORT3 = JavaFirstPolicyServer.PORT3;

    private static final String WSDL_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch",
        // run the server in the same process
        // set this to false to fork
                   launchServer(JavaFirstPolicyServer.class, true));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testUsernameTokenInterceptorNoPasswordValidation() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer", PORT);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
            "org/apache/cxf/systest/ws/policy/javafirstclient.xml");

        JavaFirstAttachmentPolicyService svc = ctx.getBean("JavaFirstAttachmentPolicyServiceClient",
                                                           JavaFirstAttachmentPolicyService.class);

        WSS4JOutInterceptor wssOut = addToClient(svc);

        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doInputMessagePolicy();

        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            // expected
        }

        wssOut.setProperties(getNoPasswordProperties("alice"));

        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            // expected
        }

        ctx.close();
    }

    @org.junit.Test
    public void testUsernameTokenPolicyValidatorNoPasswordValidation() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.2", PORT2);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
            "org/apache/cxf/systest/ws/policy/javafirstclient.xml");

        SslUsernamePasswordAttachmentService svc = ctx.getBean("SslUsernamePasswordAttachmentServiceClient",
                                                               SslUsernamePasswordAttachmentService.class);

        WSS4JOutInterceptor wssOut = addToClient(svc);

        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doSslAndUsernamePasswordPolicy();

        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            // expected
        }

        wssOut.setProperties(getNoPasswordProperties("alice"));

        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            // expected
        }

        ctx.close();
    }

    @Test
    public void testBindingNoClientCertAlternativePolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer", PORT);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslnocertclient.xml"
        });

        BindingSimpleService simpleService = clientContext.getBean("BindingSimpleServiceClient",
                                                                         BindingSimpleService.class);

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        try {
            simpleService.doStuff();
            fail("Expected exception as no password and no client cert");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getPasswordProperties("alice", "password"));
        simpleService.doStuff();

        clientContext.close();
    }

    @Test
    public void testBindingClientCertAlternativePolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.3", PORT3);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslcertclient.xml"
        });

        BindingSimpleService simpleService = clientContext.getBean("BindingSimpleServiceClient",
                                                                         BindingSimpleService.class);

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        simpleService.doStuff();

        wssOut.setProperties(getPasswordProperties("alice", "password"));

        // this is successful because the alternative policy allows a password to be specified.
        simpleService.doStuff();

        clientContext.close();
    }

    @Test
    public void testNoAltOperationNoClientCertPolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.3", PORT3);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslnocertclient.xml"
        });

        NoAlternativesOperationSimpleService simpleService = clientContext
            .getBean("NoAlternativesOperationSimpleServiceClient",
                     NoAlternativesOperationSimpleService.class);

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        try {
            simpleService.doStuff();
            fail("Expected exception as no password and no client cert");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getPasswordProperties("alice", "password"));
        try {
            simpleService.doStuff();
            fail("Expected exception as no client cert and password not allowed");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getNoPasswordProperties("alice"));
        try {
            simpleService.ping();
            fail("Expected exception as no password");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getPasswordProperties("alice", "password"));
        simpleService.ping();

        clientContext.close();
    }

    @Test
    public void testNoAltOperationClientCertPolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.3", PORT3);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslcertclient.xml"
        });

        NoAlternativesOperationSimpleService simpleService = clientContext
            .getBean("NoAlternativesOperationSimpleServiceClient",
                     NoAlternativesOperationSimpleService.class);

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        simpleService.doStuff();

        wssOut.setProperties(getPasswordProperties("alice", "password"));
        try {
            simpleService.doStuff();
            fail("Expected exception as password not allowed");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getNoPasswordProperties("alice"));
        try {
            simpleService.ping();
            fail("Expected exception as no password");
        } catch (SOAPFaultException e) {
            // expected
        }

        wssOut.setProperties(getPasswordProperties("alice", "password"));
        simpleService.ping();

        clientContext.close();
    }

    @Test
    public void testOperationNoClientCertAlternativePolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.3", PORT3);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslnocertclient.xml"
        });

        OperationSimpleService simpleService = clientContext
            .getBean("OperationSimpleServiceClient", OperationSimpleService.class);

        // no security on ping!
        simpleService.ping();

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        try {
            simpleService.doStuff();
            fail("Expected exception as no password and no client cert");
        } catch (SOAPFaultException e) {
            // expected
        }

        // this is successful because the alternative policy allows a password to be specified.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        simpleService.doStuff();

        clientContext.close();
    }

    @Test
    public void testOperationClientCertAlternativePolicy() {
        System.setProperty("testutil.ports.JavaFirstPolicyServer.3", PORT3);

        ClassPathXmlApplicationContext clientContext = new ClassPathXmlApplicationContext(new String[] {
            "org/apache/cxf/systest/ws/policy/sslcertclient.xml"
        });

        OperationSimpleService simpleService = clientContext
            .getBean("OperationSimpleServiceClient", OperationSimpleService.class);

        // no security on ping!
        simpleService.ping();

        try {
            simpleService.doStuff();
            fail("Expected exception as no credentials");
        } catch (SOAPFaultException e) {
            // expected
        }

        WSS4JOutInterceptor wssOut = addToClient(simpleService);

        wssOut.setProperties(getNoPasswordProperties("alice"));
        simpleService.doStuff();

        // this is successful because the alternative policy allows a password to be specified.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        simpleService.doStuff();

        clientContext.close();
    }

    private WSS4JOutInterceptor addToClient(Object svc) {
        Client client = ClientProxy.getClient(svc);
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor();
        client.getEndpoint().getOutInterceptors().add(wssOut);
        client.getOutInterceptors().add(wssOut);
        return wssOut;
    }

    private Map<String, Object> getPasswordProperties(String username, String password) {
        UTPasswordCallback callback = new UTPasswordCallback();
        callback.setAliasPassword(username, password);

        Map<String, Object> outProps = new HashMap<>();
        outProps.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        outProps.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_TEXT);
        outProps.put(ConfigurationConstants.PW_CALLBACK_REF, callback);
        outProps.put(ConfigurationConstants.USER, username);
        return outProps;
    }

    private Map<String, Object> getNoPasswordProperties(String username) {
        Map<String, Object> outProps = new HashMap<>();
        outProps.put(ConfigurationConstants.ACTION, ConfigurationConstants.USERNAME_TOKEN);
        outProps.put(ConfigurationConstants.PASSWORD_TYPE, WSS4JConstants.PW_NONE);
        outProps.put(ConfigurationConstants.USER, username);
        return outProps;
    }

    @org.junit.Test
    public void testJavaFirstAttachmentWsdl() throws Exception {
        Document doc = loadWsdl("JavaFirstAttachmentPolicyService");
        testJavaFirstAttachmentWsdl(doc);

        // verify that the policy attachment not being defensively copied is working ok still!
        Document doc2 = loadWsdl("JavaFirstAttachmentPolicyService2");
        testJavaFirstAttachmentWsdl(doc2);
    }

    private void testJavaFirstAttachmentWsdl(Document doc) throws Exception {
        Element binding = DOMUtils.getFirstChildWithName(doc.getDocumentElement(), WSDL_NAMESPACE, "binding");
        assertNotNull(binding);

        List<Element> operationMessages = DOMUtils.getChildrenWithName(binding, WSDL_NAMESPACE, "operation");
        assertEquals(4, operationMessages.size());

        Element doOperationLevelPolicy = getOperationElement("doOperationLevelPolicy", operationMessages);
        assertEquals("#UsernameToken",
                     getOperationPolicyReferenceId(doOperationLevelPolicy, Constants.URI_POLICY_13_NS));

        Element doInputMessagePolicy = getOperationElement("doInputMessagePolicy", operationMessages);
        assertEquals("#UsernameToken",
                     getMessagePolicyReferenceId(doInputMessagePolicy, Type.INPUT, Constants.URI_POLICY_13_NS));
        assertNull(getMessagePolicyReferenceId(doInputMessagePolicy, Type.OUTPUT, Constants.URI_POLICY_13_NS));

        Element doOutputMessagePolicy = getOperationElement("doOutputMessagePolicy", operationMessages);
        assertEquals("#UsernameToken",
                     getMessagePolicyReferenceId(doOutputMessagePolicy, Type.OUTPUT,
                                                 Constants.URI_POLICY_13_NS));
        assertNull(getMessagePolicyReferenceId(doOutputMessagePolicy, Type.INPUT, Constants.URI_POLICY_13_NS));

        Element doNoPolicy = getOperationElement("doNoPolicy", operationMessages);
        assertNull(getMessagePolicyReferenceId(doNoPolicy, Type.INPUT, Constants.URI_POLICY_13_NS));
        assertNull(getMessagePolicyReferenceId(doNoPolicy, Type.OUTPUT, Constants.URI_POLICY_13_NS));

        // ensure that the policies are attached to the wsdl:definitions
        List<Element> policyMessages = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
                                                                    Constants.URI_POLICY_13_NS, "Policy");
        assertEquals(1, policyMessages.size());

        assertEquals("UsernameToken", getPolicyId(policyMessages.get(0)));

        Element exactlyOne = DOMUtils.getFirstChildWithName(policyMessages.get(0), "", "ExactlyOne");
        assertNull(exactlyOne);

        exactlyOne = DOMUtils.getFirstChildWithName(policyMessages.get(0), Constants.URI_POLICY_13_NS,
                                                    "ExactlyOne");
        assertNotNull(exactlyOne);
    }

    @org.junit.Test
    public void testJavaFirstWsdl() throws Exception {
        Document doc = loadWsdl("JavaFirstPolicyService");

        Element portType = DOMUtils.getFirstChildWithName(doc.getDocumentElement(), WSDL_NAMESPACE,
                                                          "portType");
        assertNotNull(portType);

        List<Element> operationMessages = DOMUtils.getChildrenWithName(portType, WSDL_NAMESPACE, "operation");
        assertEquals(5, operationMessages.size());

        Element operationOne = getOperationElement("doOperationOne", operationMessages);
        assertEquals("#InternalTransportAndUsernamePolicy",
                     getMessagePolicyReferenceId(operationOne, Type.INPUT, Constants.URI_POLICY_NS));
        Element operationTwo = getOperationElement("doOperationTwo", operationMessages);
        assertEquals("#TransportAndUsernamePolicy",
                     getMessagePolicyReferenceId(operationTwo, Type.INPUT, Constants.URI_POLICY_NS));
        Element operationThree = getOperationElement("doOperationThree", operationMessages);
        assertEquals("#InternalTransportAndUsernamePolicy",
                     getMessagePolicyReferenceId(operationThree, Type.INPUT, Constants.URI_POLICY_NS));
        Element operationFour = getOperationElement("doOperationFour", operationMessages);
        assertEquals("#TransportAndUsernamePolicy",
                     getMessagePolicyReferenceId(operationFour, Type.INPUT, Constants.URI_POLICY_NS));
        Element operationPing = getOperationElement("doPing", operationMessages);
        assertNull(getMessagePolicyReferenceId(operationPing, Type.INPUT, Constants.URI_POLICY_NS));

        List<Element> policyMessages = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
                                                                    Constants.URI_POLICY_NS, "Policy");

        assertEquals(2, policyMessages.size());

        // validate that both the internal and external policies are included
        assertEquals("TransportAndUsernamePolicy", getPolicyId(policyMessages.get(0)));
        assertEquals("InternalTransportAndUsernamePolicy", getPolicyId(policyMessages.get(1)));
    }

    private Document loadWsdl(String serviceName) throws Exception {
        HttpURLConnection connection = getHttpConnection("http://localhost:" + PORT + "/" + serviceName
                                                         + "?wsdl");
        InputStream is = connection.getInputStream();
        String wsdlContents = IOUtils.toString(is);

        // System.out.println(wsdlContents);
        return StaxUtils.read(new StringReader(wsdlContents));
    }

    private String getPolicyId(Element element) {
        return element.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI, PolicyConstants.WSU_ID_ATTR_NAME);
    }

    private Element getOperationElement(String operationName, List<Element> operationMessages) {
        Element operationElement = null;
        for (Element operation : operationMessages) {
            if (operationName.equals(operation.getAttributeNS(null, "name"))) {
                operationElement = operation;
                break;
            }
        }
        assertNotNull(operationElement);
        return operationElement;
    }

    private String getMessagePolicyReferenceId(Element operationElement, Type type, String policyNamespace) {
        Element messageElement = DOMUtils.getFirstChildWithName(operationElement, WSDL_NAMESPACE, type.name()
            .toLowerCase());
        assertNotNull(messageElement);
        Element policyReference = DOMUtils.getFirstChildWithName(messageElement, policyNamespace,
                                                                 "PolicyReference");
        if (policyReference != null) {
            return policyReference.getAttributeNS(null, "URI");
        }
        return null;
    }

    private String getOperationPolicyReferenceId(Element operationElement, String policyNamespace) {
        Element policyReference = DOMUtils.getFirstChildWithName(operationElement, policyNamespace,
                                                                 "PolicyReference");
        if (policyReference != null) {
            return policyReference.getAttributeNS(null, "URI");
        }
        return null;
    }
}

