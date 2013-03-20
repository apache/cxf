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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.policy.server.JavaFirstPolicyServer;
import org.apache.cxf.systest.ws.wssec11.client.UTPasswordCallback;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

import org.junit.BeforeClass;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JavaFirstPolicyServiceTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(JavaFirstPolicyServer.class);
    static final String PORT2 = allocatePort(JavaFirstPolicyServer.class, 2);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch",
        // run the server in the same process
        // set this to false to fork
                   launchServer(JavaFirstPolicyServer.class, true));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }
    
    @org.junit.Test
    public void testUsernameTokenInterceptorNoPasswordValidation() {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/policy/client/javafirstclient.xml");
        
        JavaFirstAttachmentPolicyService svc = 
            (JavaFirstAttachmentPolicyService) ctx.getBean("JavaFirstAttachmentPolicyServiceClient");
        
        Client client = ClientProxy.getClient(svc);
        client.getEndpoint().getEndpointInfo().setAddress(
                                "http://localhost:" + PORT + "/JavaFirstAttachmentPolicyService");
       
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor();
        client.getEndpoint().getOutInterceptors().add(wssOut);
        
        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doInputMessagePolicy();
        
        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        wssOut.setProperties(getNoPasswordProperties("alice"));
        
        try {
            svc.doInputMessagePolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
    
    @org.junit.Test
    public void testUsernameTokenPolicyValidatorNoPasswordValidation() {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext("org/apache/cxf/systest/ws/policy/client/javafirstclient.xml");
        
        SslUsernamePasswordAttachmentService svc = 
            (SslUsernamePasswordAttachmentService) ctx.getBean("SslUsernamePasswordAttachmentServiceClient");
        
        Client client = ClientProxy.getClient(svc);
        client.getEndpoint().getEndpointInfo().setAddress(
                                "https://localhost:" + PORT2 + "/SslUsernamePasswordAttachmentService");
       
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor();
        client.getEndpoint().getOutInterceptors().add(wssOut);
        
        // just some basic sanity tests first to make sure that auth is working where password is provided.
        wssOut.setProperties(getPasswordProperties("alice", "password"));
        svc.doSslAndUsernamePasswordPolicy();
        
        wssOut.setProperties(getPasswordProperties("alice", "passwordX"));
        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
        
        wssOut.setProperties(getNoPasswordProperties("alice"));
        
        try {
            svc.doSslAndUsernamePasswordPolicy();
            fail("Expected authentication failure");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
    
    private Map<String, Object> getPasswordProperties(String username, String password) {
        UTPasswordCallback callback = new UTPasswordCallback();
        callback.setAliasPassword(username, password);
        
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProps.put(WSHandlerConstants.PW_CALLBACK_REF, callback);
        outProps.put(WSHandlerConstants.USER, username);
        return outProps;
    }
    
    private Map<String, Object> getNoPasswordProperties(String username) {
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_NONE);
        outProps.put(WSHandlerConstants.USER, username);
        return outProps;
    }
<<<<<<< HEAD
=======

    @org.junit.Test
    public void testJavaFirstWsdl() throws Exception {
        Document doc = loadWsdl("JavaFirstPolicyService");

        Element portType = DOMUtils.getFirstChildWithName(doc.getDocumentElement(), WSDL_NAMESPACE, "portType");
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
        HttpURLConnection connection = getHttpConnection("http://localhost:" + PORT
                                                         + "/" + serviceName + "?wsdl");
        InputStream is = connection.getInputStream();
        String wsdlContents = IOUtils.toString(is);
        
        //System.out.println(wsdlContents);
        return DOMUtils.readXml(new StringReader(wsdlContents));
    }
    
    private String getPolicyId(Element element) {
        return element.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                     PolicyConstants.WSU_ID_ATTR_NAME);
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
        Element messageElement = DOMUtils.getFirstChildWithName(operationElement, WSDL_NAMESPACE, 
                                                              type.name().toLowerCase());
        assertNotNull(messageElement);
<<<<<<< HEAD
        Element policyReference = DOMUtils.getFirstChildWithName(messageElement, policyNamespace, 
=======
        Element policyReference = DOMUtils.getFirstChildWithName(messageElement, policyNamespace,
                                                                 "PolicyReference");
        if (policyReference != null) {
            return policyReference.getAttributeNS(null, "URI");
        } else {
            return null;
        }
    }

    private String getOperationPolicyReferenceId(Element operationElement, String policyNamespace) {
        Element policyReference = DOMUtils.getFirstChildWithName(operationElement, policyNamespace,
>>>>>>> 4c6a8d2... Merged revisions 1458926 via  git cherry-pick from
                                                                 "PolicyReference");
        if (policyReference != null) {
            return policyReference.getAttributeNS(null, "URI");
        } else {
            return null;
        }
    }
>>>>>>> ba4d301... Merged revisions 1458929 via  git cherry-pick from
}
