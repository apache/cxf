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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.apache.cxf.helpers.CastUtils;
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
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor.PolicyBasedWSS4JOutInterceptorInternal;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.util.WSSecurityUtil;

public abstract class AbstractPolicySecurityTest extends AbstractSecurityTest {
    protected PolicyBuilder policyBuilder;

    protected Bus createBus() throws BusException {
        Bus b = super.createBus();
        this.policyBuilder = 
            b.getExtension(PolicyBuilder.class);
        return b;
    }
    
    protected void runAndValidate(String document, String policyDocument,
            List<QName> assertedOutAssertions, List<QName> notAssertedOutAssertions,
            List<QName> assertedInAssertions, List<QName> notAssertedInAssertions,
            List<CoverageType> types) throws Exception {
        
        this.runAndValidate(document, policyDocument, null,
                new AssertionsHolder(assertedOutAssertions, notAssertedOutAssertions),
                new AssertionsHolder(assertedInAssertions, notAssertedInAssertions),
                types);
    }
    
    protected void runAndValidate(
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
    
    protected void runInInterceptorAndValidate(String document,
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
    
    protected void runInInterceptorAndValidate(String document,
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
    
    protected void runInInterceptorAndValidate(Document document,
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
                        checkAssertion(aim, assertionType, ai, true);
                    }
                }
            }
            
            if (notAssertedInAssertions != null) {
                for (QName assertionType : notAssertedInAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        checkAssertion(aim, assertionType, ai, false);
                    }
                }
            }
        }
    }
    
    private void checkAssertion(AssertionInfoMap aim, 
                                QName name,
                                AssertionInfo inf,
                                boolean asserted) {
        boolean pass = true;
        Collection<AssertionInfo> ail = aim.getAssertionInfo(name);
        for (AssertionInfo ai : ail) {
            if (ai.getAssertion().equal(inf.getAssertion())
                && !ai.isAsserted() && !inf.getAssertion().isOptional()) {
                pass = false;                    
            }
        }
        if (asserted) {
            assertTrue(name + " policy erroneously failed.", pass);
        } else {
            assertFalse(name + " policy erroneously asserted.", pass);            
        }
    }
    
    protected void runInInterceptorAndValidateWss(Document document, AssertionInfoMap aim,
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
    
    protected Document runOutInterceptorAndValidate(Document document, Policy policy,
            List<QName> assertedOutAssertions, 
            List<QName> notAssertedOutAssertions) throws Exception {
        
        AssertionInfoMap aim = new AssertionInfoMap(policy);
        
        final SoapMessage msg = 
            this.getOutSoapMessageForDom(document, aim);
        
        return this.runOutInterceptorAndValidate(msg, policy, aim,
                assertedOutAssertions, notAssertedOutAssertions);       
    }    
        
    
    protected Document runOutInterceptorAndValidate(SoapMessage msg, Policy policy,
            AssertionInfoMap aim,
            List<QName> assertedOutAssertions, 
            List<QName> notAssertedOutAssertions) throws Exception {
        
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
                        checkAssertion(aim, assertionType, ai, true);
                    }
                }
            }
            
            if (notAssertedOutAssertions != null) {
                for (QName assertionType : notAssertedOutAssertions) {
                    Collection<AssertionInfo> ais = aim.get(assertionType);
                    assertNotNull(ais);
                    for (AssertionInfo ai : ais) {
                        checkAssertion(aim, assertionType, ai, false);
                    }
                }
            }
        }
        
        return msg.getContent(SOAPMessage.class).getSOAPPart();
    }
    
    // TODO: This method can be removed or reduced when testSignedElementsWithIssuedSAMLToken is
    // cleaned up.
    protected void runOutInterceptorAndValidateSamlTokenAttached(String policyDoc) throws Exception {
        // create the request message
        final Document document = this.readDocument("wsse-request-clean.xml");
        final Element outPolicyElement = 
            this.readDocument(policyDoc).getDocumentElement();
        final Policy policy = this.policyBuilder.getPolicy(outPolicyElement);
        
        AssertionInfoMap aim = new AssertionInfoMap(policy);        
        SoapMessage msg = this.getOutSoapMessageForDom(document, aim);
        
        // add an "issued" assertion into the message exchange
        Element issuedAssertion = 
            this.readDocument("example-sts-issued-saml-assertion.xml").getDocumentElement();
        
        String assertionId = issuedAssertion.getAttributeNode("AssertionID").getNodeValue();
        
        SecurityToken issuedToken = 
            new SecurityToken(assertionId, issuedAssertion, null);
        
        Properties cryptoProps = new Properties();
        URL url = ClassLoader.getSystemResource("outsecurity.properties");
        cryptoProps.load(url.openStream());
        Crypto crypto = CryptoFactory.getInstance(cryptoProps);
        String alias = cryptoProps.getProperty("org.apache.ws.security.crypto.merlin.keystore.alias");
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(alias);
        issuedToken.setX509Certificate(crypto.getX509Certificates(cryptoType)[0], crypto);
        
        msg.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, 
                issuedToken.getId());
        msg.getExchange().put(SecurityConstants.TOKEN_ID, issuedToken.getId());
        
        TokenStore tokenStore = new MemoryTokenStore();
        msg.getExchange().get(Endpoint.class).getEndpointInfo()
            .setProperty(TokenStore.class.getName(), tokenStore);
        tokenStore.add(issuedToken);
        
        // fire the interceptor and verify results
        final Document signedDoc = this.runOutInterceptorAndValidate(
                msg, policy, aim, null, null);
        
        verifySignatureCoversAssertion(signedDoc, assertionId);
    }
    
    protected PolicyBasedWSS4JOutInterceptorInternal getOutInterceptor() {
        return PolicyBasedWSS4JOutInterceptor.INSTANCE.createEndingInterceptor();
    }
    
    protected PolicyBasedWSS4JInInterceptor getInInterceptor(List<CoverageType> types) {
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
                "insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE,
                "insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, 
                TestPwdCallback.class.getName());
        inHandler.setProperty(WSHandlerConstants.IS_BSP_COMPLIANT, "false");
        
        return inHandler;
    }
    
    /**
     * Gets a SoapMessage, but with the needed SecurityConstants in the context properties
     * so that it can be passed to PolicyBasedWSS4JOutInterceptor.
     *
     * @see #getSoapMessageForDom(Document, AssertionInfoMap)
     */
    protected SoapMessage getOutSoapMessageForDom(Document doc, AssertionInfoMap aim)
        throws SOAPException {
        SoapMessage msg = this.getSoapMessageForDom(doc, aim);
        msg.put(SecurityConstants.SIGNATURE_PROPERTIES, "outsecurity.properties");
        msg.put(SecurityConstants.ENCRYPT_PROPERTIES, "outsecurity.properties");
        msg.put(SecurityConstants.CALLBACK_HANDLER, TestPwdCallback.class.getName());
        msg.put(SecurityConstants.SIGNATURE_USERNAME, "myalias");
        msg.put(SecurityConstants.ENCRYPT_USERNAME, "myalias");
        
        msg.getExchange().put(Endpoint.class, new MockEndpoint());
        msg.getExchange().put(Bus.class, this.bus);
        msg.put(Message.REQUESTOR_ROLE, true);
        
        return msg;
    }
    
    protected SoapMessage getSoapMessageForDom(Document doc, AssertionInfoMap aim)
        throws SOAPException {
        
        SoapMessage msg = this.getSoapMessageForDom(doc);
        if (aim != null) {
            msg.put(AssertionInfoMap.class, aim);
        }
        
        return msg;
    }
    
    protected void verifyWss4jSigResults(SoapMessage inmsg) {
        WSSecurityEngineResult result = 
            (WSSecurityEngineResult) inmsg.get(WSS4JInInterceptor.SIGNATURE_RESULT);
        assertNotNull(result);
    }
    
    protected void verifyWss4jEncResults(SoapMessage inmsg) {
        //
        // There should be exactly 1 (WSS4J) HandlerResult
        //
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)inmsg.get(WSHandlerConstants.RECV_RESULTS));
        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);

        List<WSSecurityEngineResult> protectionResults = new Vector<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(handlerResults.get(0).getResults(),
                WSConstants.ENCR, protectionResults);
        assertNotNull(protectionResults);
        
        //
        // This result should contain a reference to the decrypted element
        //
        final Map<String, Object> result = (Map<String, Object>) protectionResults
                .get(0);
        final List<WSDataRef> protectedElements = 
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        assertNotNull(protectedElements);
    }
    
    // TODO: This method can be removed when runOutInterceptorAndValidateAsymmetricBinding
    // is cleaned up by adding server side enforcement of signature related algorithms.
    // See https://issues.apache.org/jira/browse/WSS-222
    protected void verifySignatureAlgorithms(Document signedDoc, AssertionInfoMap aim) throws Exception { 
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
    
    // TODO: This method can be removed when runOutInterceptorAndValidateSamlTokenAttached
    // is cleaned up.
    protected void verifySignatureCoversAssertion(Document signedDoc, String assertionId) throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        final NamespaceContext nsContext = this.getNamespaceContext();
        xpath.setNamespaceContext(nsContext);
        
        // Find the SecurityTokenReference for the assertion
        final XPathExpression strExpr = xpath.compile(
            "/s:Envelope/s:Header/wsse:Security/wsse:SecurityTokenReference/wsse:KeyIdentifier");
        
        final NodeList strKeyIdNodes = 
            (NodeList) strExpr.evaluate(signedDoc, XPathConstants.NODESET);
        
        String strId = null;
        for (int i = 0; i < strKeyIdNodes.getLength(); i++) {
            Node keyIdNode = (Node) strKeyIdNodes.item(i);
            String strKey = keyIdNode.getTextContent();
            if (strKey.equals(assertionId)) {
                Node strNode = (Node) keyIdNode.getParentNode();
                strId = strNode.getAttributes().
                    getNamedItemNS(nsContext.getNamespaceURI("wsu"), "Id").getNodeValue();
                break;
            }
        }
        assertNotNull("SecurityTokenReference for " + assertionId + " not found in security header.", strId);
        
        // Verify STR is included in the signature references
        final XPathExpression sigRefExpr = xpath.compile(
            "/s:Envelope/s:Header/wsse:Security/ds:Signature/ds:SignedInfo/ds:Reference");
        
        final NodeList sigReferenceNodes = 
            (NodeList) sigRefExpr.evaluate(signedDoc, XPathConstants.NODESET);
        
        boolean foundStrReference = false;
        for (int i = 0; i < sigReferenceNodes.getLength(); i++) {
            Node sigRefNode = (Node) sigReferenceNodes.item(i);
            String sigRefURI = sigRefNode.getAttributes().getNamedItem("URI").getNodeValue();
            if (sigRefURI.equals("#" + strId)) {
                foundStrReference = true;
                break;
            }
        }
        
        assertTrue("SecurityTokenReference for " + assertionId + " is not signed.", foundStrReference);
    }
    
    protected static final class MockEndpoint extends 
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
    protected static final class AssertionsHolder {
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
