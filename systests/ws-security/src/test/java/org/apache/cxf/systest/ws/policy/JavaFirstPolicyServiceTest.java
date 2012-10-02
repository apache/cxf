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
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.policy.server.JavaFirstPolicyServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Constants;

import org.junit.BeforeClass;

public class JavaFirstPolicyServiceTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(JavaFirstPolicyServer.class);
    static final String PORT2 = allocatePort(JavaFirstPolicyServer.class, 2);
    
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
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testJavaFirstWsdl() throws Exception {
        HttpURLConnection connection = getHttpConnection("http://localhost:" + PORT2
                                                         + "/JavaFirstPolicyService?wsdl");
        InputStream is = connection.getInputStream();
        String wsdlContents = IOUtils.toString(is);

        Document doc = DOMUtils.readXml(new StringReader(wsdlContents));

        Element portType = DOMUtils.getFirstChildWithName(doc.getDocumentElement(), WSDL_NAMESPACE, "portType");
        assertNotNull(portType);
        
        List<Element> operationMessages = DOMUtils.getChildrenWithName(portType, WSDL_NAMESPACE, "operation");
        assertEquals(5, operationMessages.size());
        
        Element operationOne = getOperationMessage("doOperationOne", operationMessages);
        assertEquals("#InternalTransportAndUsernamePolicy", getPolicyReferenceId(operationOne));
        Element operationTwo = getOperationMessage("doOperationTwo", operationMessages);
        assertEquals("#TransportAndUsernamePolicy", getPolicyReferenceId(operationTwo));
        Element operationThree = getOperationMessage("doOperationThree", operationMessages);
        assertEquals("#InternalTransportAndUsernamePolicy", getPolicyReferenceId(operationThree));
        Element operationFour = getOperationMessage("doOperationFour", operationMessages);
        assertEquals("#TransportAndUsernamePolicy", getPolicyReferenceId(operationFour));
        Element operationPing = getOperationMessage("doPing", operationMessages);
        assertNull(getPolicyReferenceId(operationPing));
        
        List<Element> policyMessages = DOMUtils.getChildrenWithName(doc.getDocumentElement(), 
                                                                    Constants.URI_POLICY_NS, "Policy");
        assertEquals(2, policyMessages.size());
        
        // validate that both the internal and external policies are included
        assertEquals("TransportAndUsernamePolicy", getPolicyId(policyMessages.get(0)));
        assertEquals("InternalTransportAndUsernamePolicy", getPolicyId(policyMessages.get(1)));
    }
    
    private String getPolicyId(Element element) {
        return element.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                     PolicyConstants.WSU_ID_ATTR_NAME);
    }
    
    private Element getOperationMessage(String operationName, List<Element> operationMessages) {
        Element operationElement = null;
        for (Element operation : operationMessages) {
            if (operationName.equals(operation.getAttribute("name"))) {
                operationElement = operation;
                break;
            }
        }
        assertNotNull(operationElement);
        return operationElement;
    }
    
    private String getPolicyReferenceId(Element operationMessage) {
        Element inputMessage = DOMUtils.getFirstChildWithName(operationMessage, WSDL_NAMESPACE, "input");
        assertNotNull(inputMessage);
        Element policyReference = DOMUtils.getFirstChildWithName(inputMessage, Constants.URI_POLICY_NS, 
                                                                 "PolicyReference");
        if (policyReference != null) {
            return policyReference.getAttribute("URI");
        } else {
            return null;
        }
    }
}
