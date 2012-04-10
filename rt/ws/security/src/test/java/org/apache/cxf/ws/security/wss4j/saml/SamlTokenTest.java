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
package org.apache.cxf.ws.security.wss4j.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.AbstractSecurityTest;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.saml.AbstractSAMLCallbackHandler.Statement;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.ws.security.util.WSSecurityUtil;
import org.junit.Test;


/**
 * Some tests for creating and processing (signed) SAML Assertions.
 */
public class SamlTokenTest extends AbstractSecurityTest {

    public SamlTokenTest() {
    }

    /**
     * This test creates a SAML1 Assertion and sends it in the security header to the provider. 
     */
    @Test
    public void testSaml1Token() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.saml.SAML1CallbackHandler"
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml1:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assert !receivedAssertion.isSigned();
    }
    
    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     */
    @Test
    public void testSaml2Token() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_CLASS, 
            "org.apache.cxf.ws.security.wss4j.saml.SAML2CallbackHandler"
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml2:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert !receivedAssertion.isSigned();
    }
    
    /**
     * This test creates a sender-vouches SAML1 Assertion, where the Assertion and SOAP body are 
     * signed, and sends it in the security header to the provider.
     */
    @Test
    public void testSaml1TokenSignedSenderVouches() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler()
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SAML_TOKEN_UNSIGNED + " " + WSHandlerConstants.SIGNATURE
        );
        inProperties.put(WSHandlerConstants.SIG_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml1:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assert !receivedAssertion.isSigned();
        
        actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.SIGN);
        assertTrue(actionResult != null);
    }
    
    /**
     * This test creates a sender-vouches SAML2 Assertion, where the Assertion and SOAP body are 
     * signed, and sends it in the security header to the provider.
     */
    @Test
    public void testSaml2TokenSignedSenderVouches() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        outProperties.put("password", "myAliasPassword");
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler()
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SAML_TOKEN_UNSIGNED + " " + WSHandlerConstants.SIGNATURE
        );
        inProperties.put(WSHandlerConstants.SIG_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml2:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert !receivedAssertion.isSigned();
        
        actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.SIGN);
        assertTrue(actionResult != null);
    }
    
    
    /**
     * This test creates a holder-of-key SAML1 Assertion, and sends it in the security header 
     * to the provider.
     */
    @Test
    public void testSaml1TokenHOK() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_hok.properties");
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(WSHandlerConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "alice.properties");
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, callbackHandler
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SAML_TOKEN_SIGNED + " " + WSHandlerConstants.SIGNATURE
        );
        inProperties.put(WSHandlerConstants.SIG_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml1:Assertion");
        
        try {
            makeInvocation(outProperties, xpaths, inProperties);
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_SIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assert receivedAssertion.isSigned();
        
        actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.SIGN);
        assertTrue(actionResult != null);
    }
    
    /**
     * This test creates a holder-of-key SAML2 Assertion, and sends it in the security header 
     * to the provider.
     */
    @Test
    public void testSaml2TokenHOK() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_SIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_hok.properties");
        outProperties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(WSHandlerConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "alice.properties");
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, callbackHandler
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SAML_TOKEN_SIGNED + " " + WSHandlerConstants.SIGNATURE
        );
        inProperties.put(WSHandlerConstants.SIG_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml2:Assertion");
        
        try {
            makeInvocation(outProperties, xpaths, inProperties);
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);
        
        try {
            makeInvocation(outProperties, xpaths, inProperties);
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSAML1Assertion(false);

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_SIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert receivedAssertion.isSigned();
        
        actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.SIGN);
        assertTrue(actionResult != null);
    }
    
    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     * An single attribute is created for the roles but multiple attribute value elements.
     */
    @Test
    public void testSaml2TokenWithRoles() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(Statement.ATTR);
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, callbackHandler
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml2:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert !receivedAssertion.isSigned();
    }
    
    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     * An attribute is created per role. There are several attributes with the same name.
     */
    @Test
    public void testSaml2TokenWithRolesSingleValue() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler(false);
        callbackHandler.setStatement(Statement.ATTR);
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, callbackHandler
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml2:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assert !receivedAssertion.isSigned();
    }
    
    /**
     * This test creates a SAML1 Assertion and sends it in the security header to the provider.
     */
    @Test
    //@Ignore
    public void testSaml1TokenWithRoles() throws Exception {
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(WSHandlerConstants.SAML_PROP_FILE, "saml_sv.properties");
        
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setStatement(Statement.ATTR);
        outProperties.put(
            WSHandlerConstants.SAML_CALLBACK_REF, callbackHandler
        );
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<QName, Object>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSSecurityEngine.SAML_TOKEN, validator);
        customMap.put(WSSecurityEngine.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);
        
        
        List<String> xpaths = new ArrayList<String>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/saml1:Assertion");

        Message message = makeInvocation(outProperties, xpaths, inProperties);
        final List<WSHandlerResult> handlerResults = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        
        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));
        
        WSSecurityEngineResult actionResult =
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.ST_UNSIGNED);
        AssertionWrapper receivedAssertion = 
            (AssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assert !receivedAssertion.isSigned();
    }
    
    private SoapMessage makeInvocation(
        Map<String, Object> outProperties,
        List<String> xpaths,
        Map<String, Object> inProperties
    ) throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        part.setContent(new DOMSource(doc));
        saajMsg.saveChanges();

        msg.setContent(SOAPMessage.class, saajMsg);

        for (String key : outProperties.keySet()) {
            msg.put(key, outProperties.get(key));
        }

        handler.handleMessage(msg);

        doc = part;

        for (String xpath : xpaths) {
            assertValid(xpath, doc);
        }

        byte[] docbytes = getMessageBytes(doc);
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(docbytes));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());
        doc = StaxUtils.read(db, reader, false);

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(inProperties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        inmsg.put(SecurityConstants.SAML_ROLE_ATTRIBUTENAME, "role");
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.handleMessage(inmsg);

        return inmsg;
    }
    
    private byte[] getMessageBytes(Document doc) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter byteArrayWriter = StaxUtils.createXMLStreamWriter(outputStream);
        StaxUtils.writeDocument(doc, byteArrayWriter, false);
        byteArrayWriter.flush();
        return outputStream.toByteArray();
    }

    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) {
        return XMLUtils.toString(doc);
    }
}
