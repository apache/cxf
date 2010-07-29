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


import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executor;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor.PolicyBasedWSS4JOutInterceptorInternal;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.util.WSSecurityUtil;
import org.junit.Test;


public class PolicyBasedWss4JInOutTest extends AbstractSecurityTest {
    private PolicyBuilder policyBuilder;

    @Test
    public void testSignedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_elements_policy.xml",
                null,
                SP12Constants.SIGNED_ELEMENTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_elements_policy.xml",
                SP12Constants.SIGNED_ELEMENTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_elements_policy.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
    }

    // TODO this test does not follow the traditional pattern as no server-side enforcement
    // of algorithm suites yet exists.  This support is blocked on WSS4J patches.  In the interim
    // the outbound side is tested ONLY.
    @Test
    public void testAsymmetricBindingAlgorithmSuitePolicy() throws Exception {
        runOutInterceptorAndValidateAsymmetricBinding("signed_elements_policy.xml");
        runOutInterceptorAndValidateAsymmetricBinding("signed_elements_Basic256Sha256_policy.xml");
    }

    @Test
    public void testSignedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_body.xml",
                "signed_parts_policy_body.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_parts_policy_header_namespace_only.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                "signed_parts_policy_header.xml",
                null,
                SP12Constants.SIGNED_PARTS,
                CoverageType.SIGNED);
    }
    
    @Test
    public void testSignedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_body.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header_namespace_only.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_namespace_only.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "signed_parts_policy_header_and_body.xml",
                SP12Constants.SIGNED_PARTS,
                null,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_and_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_elements_policy.xml",
                null,
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_elements_policy2.xml",
                null,
                SP12Constants.ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_elements_policy.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_elements_policy.xml",
                null,
                null,
                Arrays.asList(new QName[] {SP12Constants.ENCRYPTED_ELEMENTS}),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "encrypted_elements_policy2.xml",
                SP12Constants.ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_elements_policy2.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "content_encrypted_elements_policy.xml",
                null,
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testContentEncryptedElementsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "content_encrypted_elements_policy.xml",
                SP12Constants.CONTENT_ENCRYPTED_ELEMENTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "content_encrypted_elements_policy.xml",
                null,
                null,
                Arrays.asList(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testEncryptedPartsPolicyWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_body.xml",
                "encrypted_parts_policy_body.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_body_element.xml",
                "encrypted_parts_policy_body.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
        
        this.runInInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                "encrypted_parts_policy_header.xml",
                null,
                SP12Constants.ENCRYPTED_PARTS,
                CoverageType.ENCRYPTED);
    }
    
    @Test
    public void testEncryptedPartsPolicyWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_namespace_only.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
        
        this.runInInterceptorAndValidate(
                "encrypted_body_content.xml",
                "encrypted_parts_policy_header_and_body.xml",
                SP12Constants.ENCRYPTED_PARTS,
                null,
                CoverageType.ENCRYPTED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_and_body.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED));
    }
    
    @Test
    public void testSignedEncryptedPartsWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_encrypted_missing_enc_header.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS),
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testSignedEncryptedPartsWithCompleteCoverage() throws Exception {
        if (!checkUnrestrictedPoliciesInstalled()) {
            return;
        }
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_encrypted.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "signed_parts_policy_header_and_body_encrypted.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedSignedPartsWithIncompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content_signed_missing_signed_header.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                null,
                Arrays.asList(SP12Constants.SIGNED_PARTS),
                Arrays.asList(CoverageType.ENCRYPTED, CoverageType.SIGNED));
    }
    
    @Test
    public void testEncryptedSignedPartsWithCompleteCoverage() throws Exception {
        this.runInInterceptorAndValidate(
                "encrypted_body_content_signed.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS, 
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED, CoverageType.SIGNED));
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "encrypted_parts_policy_header_and_body_signed.xml",
                null,
                null,
                Arrays.asList(SP12Constants.ENCRYPTED_PARTS,
                        SP12Constants.SIGNED_PARTS),
                null,
                Arrays.asList(CoverageType.ENCRYPTED,
                        CoverageType.SIGNED));
    }
    
    @Test
    public void testProtectTokenAssertion() throws Exception {
        
        // ////////////////////////////////////////////////////
        // x509 Direct Ref Tests
        
        /* REVISIT
        No inbound validation is available for the PROTECT_TOKENS assertion.
        We cannot yet test inbound in the standard manner.  Since we can't
        test inbound, we can't test reound trip either and thus must take
        a different approach for now.
         
        this.runInInterceptorAndValidate(
                "signed_x509_direct_ref_token_prot.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                SP12Constants.PROTECT_TOKENS,
                null,
                CoverageType.SIGNED);

        this.runInInterceptorAndValidate(
                "signed_x509_direct_ref.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                null,
                SP12Constants.PROTECT_TOKENS,
                CoverageType.SIGNED);
        
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                null,
                null,
                Arrays.asList(new QName[] {SP12Constants.PROTECT_TOKENS }),
                null,
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));
        */
        
        // REVISIT
        // We test using a policy with ProtectTokens enabled on
        // the outbound but with a policy using a SignedElements policy
        // on the inbound to validate that the correct thing got signed.
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_direct_ref.xml",
                "protect_token_policy_asym_x509_direct_ref_complement.xml",
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.ASYMMETRIC_BINDING}),
                        null),
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.SIGNED_ELEMENTS}),
                        null),
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));
        
        // ////////////////////////////////////////////////////
        // x509 Issuer Serial Tests
        
        /* REVISIT
        No inbound validation is available for the PROTECT_TOKENS assertion.
        We cannot yet test inbound in the standard manner.  Since we can't
        test inbound, we can't test reound trip either and thus must take
        a different approach for now.
        
        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial_token_prot.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                SP12Constants.PROTECT_TOKENS,
                null,
                CoverageType.SIGNED);

        this.runInInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                null,
                SP12Constants.PROTECT_TOKENS,
                CoverageType.SIGNED);

        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                null,
                null,
                Arrays.asList(new QName[] { SP12Constants.PROTECT_TOKENS }),
                null,
                Arrays.asList(new CoverageType[] { CoverageType.SIGNED }));
        */
        
        // REVISIT
        // We test using a policy with ProtectTokens enabled on
        // the outbound but with a policy using a SignedElements policy
        // on the inbound to validate that the correct thing got signed.
        this.runAndValidate(
                "wsse-request-clean.xml",
                "protect_token_policy_asym_x509_issuer_serial.xml",
                "protect_token_policy_asym_x509_issuer_serial_complement.xml",
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.ASYMMETRIC_BINDING}),
                        null),
                new AssertionsHolder(
                        Arrays.asList(new QName[] {SP12Constants.SIGNED_ELEMENTS}),
                        null),
                Arrays.asList(new CoverageType[] {CoverageType.SIGNED }));

        // ////////////////////////////////////////////////////
        // x509 Key Identifier Tests

        // TODO: Tests for Key Identifier are needed but require that the
        // certificates used in the test cases be updated to version 3
        // according to WSS4J.
        
        // TODO: Tests for derived keys.
    }

    
    protected Bus createBus() throws BusException {
        Bus b = super.createBus();
        this.policyBuilder = 
            b.getExtension(PolicyBuilder.class);
        return b;
    }
    
    private void runAndValidate(String document, String policyDocument,
            List<QName> assertedOutAssertions, List<QName> notAssertedOutAssertions,
            List<QName> assertedInAssertions, List<QName> notAssertedInAssertions,
            List<CoverageType> types) throws Exception {
        
        this.runAndValidate(document, policyDocument, null,
                new AssertionsHolder(assertedOutAssertions, notAssertedOutAssertions),
                new AssertionsHolder(assertedInAssertions, notAssertedInAssertions),
                types);
    }
    
    private void runAndValidate(
            String document,
            String outPolicyDocument, String inPolicyDocument,
            AssertionsHolder outAssertions,
            AssertionsHolder inAssertions,
            List<CoverageType> types) throws Exception {
        
        final Element outPolicyElement = this.readDocument(outPolicyDocument)
                .getDocumentElement();
        final Element inPolicyElement;

        if (inPolicyDocument != null) {
            inPolicyElement = this.readDocument(inPolicyDocument)
                    .getDocumentElement();
        } else {
            inPolicyElement = outPolicyElement;
        }
            
        
        final Policy outPolicy = this.policyBuilder.getPolicy(outPolicyElement);
        final Policy inPolicy = this.policyBuilder.getPolicy(inPolicyElement);
        
        final Document originalDoc = this.readDocument(document);
        
        final Document inDoc = this.runOutInterceptorAndValidate(
                originalDoc, outPolicy, outAssertions.getAssertedAssertions(),
                outAssertions.getNotAssertedAssertions());
        
        // Can't use this method if you want output that is not mangled.
        // Such is the case when you want to capture output to use
        // as input to another test case.
        //DOMUtils.writeXml(inDoc, System.out);
        
        // Use this snippet if you need intermediate output for debugging.
        /*
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "no");
        t.transform(new DOMSource(inDoc), new StreamResult(System.out));
        */
        
        this.runInInterceptorAndValidate(inDoc,
                inPolicy, inAssertions.getAssertedAssertions(),
                inAssertions.getNotAssertedAssertions(), types);
    }
    
    private void runInInterceptorAndValidate(String document,
            String policyDocument, QName assertedInAssertion,
            QName notAssertedInAssertion, 
            CoverageType type) throws Exception {
        
        this.runInInterceptorAndValidate(
                document, policyDocument, 
                assertedInAssertion == null ? null 
                        : Arrays.asList(assertedInAssertion),
                notAssertedInAssertion == null ? null
                        : Arrays.asList(notAssertedInAssertion),
                Arrays.asList(type));
    }
    
    private void runInInterceptorAndValidate(String document,
            String policyDocument, List<QName> assertedInAssertions,
            List<QName> notAssertedInAssertions,
            List<CoverageType> types) throws Exception {
        
        final Policy policy = this.policyBuilder.getPolicy(
                this.readDocument(policyDocument).getDocumentElement());
        
        final Document doc = this.readDocument(document);
        
        this.runInInterceptorAndValidate(
                doc, policy, 
                assertedInAssertions,
                notAssertedInAssertions,
                types);
    }
    
    private void runInInterceptorAndValidate(Document document,
            Policy policy, List<QName> assertedInAssertions,
            List<QName> notAssertedInAssertions,
            List<CoverageType> types) throws Exception {
        
        final AssertionInfoMap aim = new AssertionInfoMap(policy);
        
        this.runInInterceptorAndValidateWss(document, aim, types);
        
        try {
            aim.checkEffectivePolicy(policy);
        } catch (PolicyException e) {
            // Expected but not relevant
        } finally {
            if (assertedInAssertions != null) {
                for (QName assertionType : assertedInAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        assertTrue(assertionType + " policy erroneously failed.",
                                ai.getAssertion().isAsserted(aim));
                    }
                }
            }
            
            if (notAssertedInAssertions != null) {
                for (QName assertionType : notAssertedInAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        assertFalse(assertionType + " policy erroneously asserted.",
                                ai.getAssertion().isAsserted(aim));
                    }
                }
            }
        }
    }
    
    private void runInInterceptorAndValidateWss(Document document, AssertionInfoMap aim,
            List<CoverageType> types) throws Exception {
        
        PolicyBasedWSS4JInInterceptor inHandler = 
            this.getInInterceptor(types);
            
        SoapMessage inmsg = this.getSoapMessageForDom(document, aim);

        inHandler.handleMessage(inmsg);
        
        for (CoverageType type : types) {
            switch(type) {
            case SIGNED:
                this.verifyWss4jSigResults(inmsg);
                break;
            case ENCRYPTED:
                this.verifyWss4jEncResults(inmsg);
                break;
            default:
                fail("Unsupported coverage type.");
            }
        }
    }
    
    private Document runOutInterceptorAndValidate(Document document, Policy policy,
            List<QName> assertedOutAssertions, 
            List<QName> notAssertedOutAssertions) throws Exception {
        
        AssertionInfoMap aim = new AssertionInfoMap(policy);
        
        final SoapMessage msg = 
            this.getOutSoapMessageForDom(document, aim); 
        
        this.getOutInterceptor().handleMessage(msg);
        
        try {
            aim.checkEffectivePolicy(policy);
        } catch (PolicyException e) {
            // Expected but not relevant
        } finally {
            if (assertedOutAssertions != null) {
                for (QName assertionType : assertedOutAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        assertTrue(assertionType + " policy erroneously failed.",
                                ai.getAssertion().isAsserted(aim));
                    }
                }
            }
            
            if (notAssertedOutAssertions != null) {
                for (QName assertionType : notAssertedOutAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        assertFalse(assertionType + " policy erroneously asserted.",
                                ai.getAssertion().isAsserted(aim));
                    }
                }
            }
        }
        
        return msg.getContent(SOAPMessage.class).getSOAPPart();
    }
    
    // TODO: This method can be removed when testAsymmetricBindingPolicyWithSignedElements
    // is cleaned up by adding server side enforcement of signature related algorithms.
    private void runOutInterceptorAndValidateAsymmetricBinding(String policyDoc) throws Exception {
        final Document originalDoc = this.readDocument("wsse-request-clean.xml");
        
        final Element outPolicyElement = 
                this.readDocument(policyDoc).getDocumentElement();
       
        final Policy outPolicy = this.policyBuilder.getPolicy(outPolicyElement);
        final AssertionInfoMap aim = new AssertionInfoMap(outPolicy);
        
        final Document signedDoc = this.runOutInterceptorAndValidate(
                originalDoc, outPolicy, Arrays.asList(SP12Constants.ASYMMETRIC_BINDING), null);
        
        this.verifySignatureAlgorithms(signedDoc, aim);
    }
    
    private PolicyBasedWSS4JOutInterceptorInternal getOutInterceptor() {
        return (new PolicyBasedWSS4JOutInterceptor()).createEndingInterceptor();
    }
    
    private PolicyBasedWSS4JInInterceptor getInInterceptor(List<CoverageType> types) {
        PolicyBasedWSS4JInInterceptor inHandler = new PolicyBasedWSS4JInInterceptor();
        String action = "";
        
        for (CoverageType type : types) {
            switch(type) {
            case SIGNED:
                action += " " + WSHandlerConstants.SIGNATURE;
                break;
            case ENCRYPTED:
                action += " " + WSHandlerConstants.ENCRYPT;
                break;
            default:
                fail("Unsupported coverage type.");
            }
        }
        inHandler.setProperty(WSHandlerConstants.ACTION, action);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, 
                "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE,
                "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, 
                TestPwdCallback.class.getName());
        
        return inHandler;
    }
    
    /**
     * Gets a SoapMessage, but with the needed SecurityConstants in the context propreties
     * so that it can be passed to PolicyBasedWSS4JOutInterceptor.
     *
     * @see #getSoapMessageForDom(Document, AssertionInfoMap)
     */
    private SoapMessage getOutSoapMessageForDom(Document doc, AssertionInfoMap aim)
        throws SOAPException {
        SoapMessage msg = this.getSoapMessageForDom(doc, aim);
        msg.put(SecurityConstants.SIGNATURE_PROPERTIES, "META-INF/cxf/outsecurity.properties");
        msg.put(SecurityConstants.ENCRYPT_PROPERTIES, "META-INF/cxf/outsecurity.properties");
        msg.put(SecurityConstants.CALLBACK_HANDLER, TestPwdCallback.class.getName());
        msg.put(SecurityConstants.SIGNATURE_USERNAME, "myalias");
        msg.put(SecurityConstants.ENCRYPT_USERNAME, "myalias");
        
        msg.getExchange().put(Endpoint.class, new MockEndpoint());
        msg.getExchange().put(Bus.class, this.bus);
        msg.put(Message.REQUESTOR_ROLE, true);
        
        return msg;
    }
    
    private SoapMessage getSoapMessageForDom(Document doc, AssertionInfoMap aim)
        throws SOAPException {
        
        SoapMessage msg = this.getSoapMessageForDom(doc);
        if (aim != null) {
            msg.put(AssertionInfoMap.class, aim);
        }
        
        return msg;
    }
    
    private void verifyWss4jSigResults(SoapMessage inmsg) {
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
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

        Vector<Object> protectionResults = new Vector<Object>();
        WSSecurityUtil.fetchAllActionResults(handlerResults.get(0).getResults(),
                WSConstants.ENCR, protectionResults);
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
    
    // TODO: This method can be removed when runOutInterceptorAndValidateAsymmetricBinding
    // is cleaned up by adding server side enforcement of signature related algorithms.
    private void verifySignatureAlgorithms(Document signedDoc, AssertionInfoMap aim) throws Exception { 
        final AssertionInfo assertInfo = aim.get(SP12Constants.ASYMMETRIC_BINDING).iterator().next();
        assertNotNull(assertInfo);
        
        final AsymmetricBinding binding = (AsymmetricBinding) assertInfo.getAssertion();
        final String expectedSignatureMethod = binding.getAlgorithmSuite().getAsymmetricSignature();
        final String expectedDigestAlgorithm = binding.getAlgorithmSuite().getDigest();
        final String expectedCanonAlgorithm  = binding.getAlgorithmSuite().getInclusiveC14n();
            
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        final NamespaceContext nsContext = this.getNamespaceContext();
        xpath.setNamespaceContext(nsContext);
        
        // Signature Algorithm
        final XPathExpression sigAlgoExpr = 
            xpath.compile("/s:Envelope/s:Header/wsse:Security/ds:Signature/ds:SignedInfo" 
                              + "/ds:SignatureMethod/@Algorithm");
        
        final String sigMethod =  (String) sigAlgoExpr.evaluate(signedDoc, XPathConstants.STRING);
        assertEquals(expectedSignatureMethod, sigMethod);
        
        // Digest Method Algorithm
        final XPathExpression digestAlgoExpr = xpath.compile(
            "/s:Envelope/s:Header/wsse:Security/ds:Signature/ds:SignedInfo/ds:Reference/ds:DigestMethod");
        
        final NodeList digestMethodNodes = 
            (NodeList) digestAlgoExpr.evaluate(signedDoc, XPathConstants.NODESET);
        
        for (int i = 0; i < digestMethodNodes.getLength(); i++) {
            Node node = (Node)digestMethodNodes.item(i);
            String digestAlgorithm = node.getAttributes().getNamedItem("Algorithm").getNodeValue();
            assertEquals(expectedDigestAlgorithm, digestAlgorithm);
        }
        
        // Canonicalization Algorithm
        final XPathExpression canonAlgoExpr =
            xpath.compile("/s:Envelope/s:Header/wsse:Security/ds:Signature/ds:SignedInfo" 
                              + "/ds:CanonicalizationMethod/@Algorithm");
        final String canonMethod =  (String) canonAlgoExpr.evaluate(signedDoc, XPathConstants.STRING);
        assertEquals(expectedCanonAlgorithm, canonMethod);
    }

    private static final class MockEndpoint extends 
        AbstractAttributedInterceptorProvider implements Endpoint {

        private static final long serialVersionUID = 1L;

        private EndpointInfo epi = new EndpointInfo();
        
        public MockEndpoint() {
            epi.setBinding(new BindingInfo(null, null));
        }
        
        
        public List<AbstractFeature> getActiveFeatures() {
            return null;
        }

        public Binding getBinding() {
            return null;
        }

        public EndpointInfo getEndpointInfo() {
            return this.epi;
        }

        public Executor getExecutor() {
            return null;
        }

        public MessageObserver getInFaultObserver() {
            return null;
        }

        public MessageObserver getOutFaultObserver() {
            return null;
        }

        public Service getService() {
            return null;
        }

        public void setExecutor(Executor executor) {   
        }

        public void setInFaultObserver(MessageObserver observer) {
        }

        public void setOutFaultObserver(MessageObserver observer) {            
        }
    }
    
    /**
     * A simple container used to reduce argument numbers to satisfy
     * project code conventions.
     */
    private static final class AssertionsHolder {
        private List<QName> assertedAssertions;
        private List<QName> notAssertedAssertions;
        
        public AssertionsHolder(List<QName> assertedAssertions,
                List<QName> notAssertedAssertions) {
            super();
            this.assertedAssertions = assertedAssertions;
            this.notAssertedAssertions = notAssertedAssertions;
        }
        
        public List<QName> getAssertedAssertions() {
            return this.assertedAssertions;
        }
        public List<QName> getNotAssertedAssertions() {
            return this.notAssertedAssertions;
        }
    }
}
