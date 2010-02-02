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

package org.apache.cxf.ws.security.wss4j;


import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.junit.Test;


public class PolicyBasedWss4JInOutTest extends AbstractSecurityTest {
    private PolicyBuilder policyBuilder;
    
    protected Bus createBus() throws BusException {
        Bus b = super.createBus();
        this.policyBuilder = 
            b.getExtension(PolicyBuilder.class);
        return b;
    }
    @Test
    public void testSignedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runAndValidatePolicyNotAsserted(
                "signed_missing_signed_header.xml",
                "signed_elements_policy.xml",
                SP12Constants.SIGNED_ELEMENTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runAndValidatePolicyAsserted(
                "signed.xml",
                "signed_elements_policy.xml",
                SP12Constants.SIGNED_ELEMENTS,
                CoverageType.SIGNED);
    }

    @Test
    public void testSignedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runAndValidatePolicyNotAsserted(
                "signed_missing_signed_body.xml",
                "signed_parts_policy_body.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runAndValidatePolicyNotAsserted(
                "signed_missing_signed_header.xml",
                "signed_parts_policy_header_namespace_only.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runAndValidatePolicyNotAsserted(
                "signed_missing_signed_header.xml",
                "signed_parts_policy_header.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runAndValidatePolicyAsserted(
                "signed.xml",
                "signed_parts_policy_body.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runAndValidatePolicyAsserted(
                "signed.xml",
                "signed_parts_policy_header_namespace_only.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runAndValidatePolicyAsserted(
                "signed.xml",
                "signed_parts_policy_header.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runAndValidatePolicyAsserted(
                "signed.xml",
                "signed_parts_policy_header_and_body.xml",
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runAndValidatePolicyNotAsserted(
                "encrypted_missing_enc_header.xml",
                "encrypted_elements_policy.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyNotAsserted(
                "encrypted_body_content.xml",
                "encrypted_elements_policy2.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "encrypted_elements_policy.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyAsserted(
                "encrypted_body_element.xml",
                "encrypted_elements_policy2.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runAndValidatePolicyNotAsserted(
                "encrypted_body_element.xml",
                "content_encrypted_elements_policy.xml",
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "content_encrypted_elements_policy.xml",
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runAndValidatePolicyNotAsserted(
                "encrypted_missing_enc_body.xml",
                "encrypted_parts_policy_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyNotAsserted(
                "encrypted_body_element.xml",
                "encrypted_parts_policy_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyNotAsserted(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyNotAsserted(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runAndValidatePolicyAsserted(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_and_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
    }
    
    private void runAndValidatePolicyAsserted(String document,
            String policyDocument, QName assertionType,
            CoverageType type) throws Exception {
        Policy policy = this.policyBuilder.getPolicy(
                this.readDocument(policyDocument).getDocumentElement());
        
        AssertionInfoMap aim = new AssertionInfoMap(policy);
        
        this.runAndValidateWss(document, aim, type);
        
        try {
            aim.checkEffectivePolicy(policy);
            
        } catch (PolicyException e) {
            fail(assertionType + " policy erroneously failed.");
        }
    }
    
    private void runAndValidatePolicyNotAsserted(String document,
            String policyDocument, QName assertionType,
            CoverageType type) throws Exception {
        Policy policy = this.policyBuilder.getPolicy(
                this.readDocument(policyDocument).getDocumentElement());
        
        AssertionInfoMap aim = new AssertionInfoMap(policy);
        
        this.runAndValidateWss(document, aim, type);
        
        try {
            aim.checkEffectivePolicy(policy);
            fail(assertionType + " policy erroneously asserted.");
        } catch (PolicyException e) {
            Collection<AssertionInfo> ais = aim.get(assertionType);
            for (AssertionInfo ai : ais) {
                assertFalse(ai.getAssertion().isAsserted(aim));
            }
        }
    }
    
    private void runAndValidateWss(String document, AssertionInfoMap aim, CoverageType type)
        throws Exception {
        Document doc = readDocument(document);
        
        PolicyBasedWSS4JInInterceptor inHandler = 
            CoverageType.SIGNED.equals(type)
                    ? this.getInInterceptorForSignature()
                            : this.getInInterceptorForEncryption();

        SoapMessage inmsg = this.getSoapMessageForDom(doc, aim);

        inHandler.handleMessage(inmsg);
        
        if (CoverageType.SIGNED.equals(type)) {
            this.verifyWss4jSigResults(inmsg);
        } else {
            this.verifyWss4jEncResults(inmsg);
        }
    }
    
    private PolicyBasedWSS4JInInterceptor getInInterceptorForSignature() {
        PolicyBasedWSS4JInInterceptor inHandler = new PolicyBasedWSS4JInInterceptor();
        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, 
                "META-INF/cxf/insecurity.properties");
        
        return inHandler;
    }
    
    private PolicyBasedWSS4JInInterceptor getInInterceptorForEncryption() {
        PolicyBasedWSS4JInInterceptor inHandler = new PolicyBasedWSS4JInInterceptor();
        inHandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE,
                "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, 
                "org.apache.cxf.ws.security.wss4j.TestPwdCallback");
        
        return inHandler;
    }
    
    private SoapMessage getSoapMessageForDom(Document doc, AssertionInfoMap aim)
        throws SOAPException {
        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        part.setContent(new DOMSource(doc));
        saajMsg.saveChanges();
        
        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);
        if (aim != null) {
            inmsg.put(AssertionInfoMap.class, aim);
        }
        return inmsg;
    }
    
    private void verifyWss4jSigResults(SoapMessage inmsg) {
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
        X509Certificate certificate = (X509Certificate)result
            .get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
    }
    
    @SuppressWarnings("unchecked")
    private void verifyWss4jEncResults(SoapMessage inmsg) {
        //
        // There should be exactly 1 (WSS4J) HandlerResult
        //
        final List<WSHandlerResult> handlerResults = 
            (List<WSHandlerResult>) inmsg
                .get(WSHandlerConstants.RECV_RESULTS);
        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        //
        // This should contain exactly 1 protection result
        //
        final List<Object> protectionResults = (List<Object>) handlerResults
                .get(0).getResults();
        assertNotNull(protectionResults);
        //
        // This result should contain a reference to the decrypted element
        //
        final Map<String, Object> result = (Map<String, Object>) protectionResults
                .get(0);
        final List<WSDataRef> protectedElements = (List<WSDataRef>) result
                .get(WSSecurityEngineResult.TAG_DATA_REF_URIS);
        assertNotNull(protectedElements);
    }
}
