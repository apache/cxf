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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;

import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
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
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void testUnsignedSaml1Token() throws Exception {
        assertNull(testSaml1Token(false));
    }

    @Test
    public void testUnsignedSaml1TokenWithPrincipal() throws Exception {
        SecurityContext ctx = testSaml1Token(true);
        assertTrue(ctx.getUserPrincipal() instanceof SAMLTokenPrincipal);
    }

    private SecurityContext testSaml1Token(boolean allowUnsignedPrincipal) throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler());

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml1:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        if (allowUnsignedPrincipal) {
            inMessageProperties.put(SecurityConstants.ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, "true");
        }

        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_UNSIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assertFalse(receivedAssertion.isSigned());

        return message.get(SecurityContext.class);
    }

    @Test
    public void testSaml1TokenSignedSenderVouches() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML1CallbackHandler());
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml1:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_UNSIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assertFalse(receivedAssertion.isSigned());
    }

    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     */
    @Test
    public void testSaml2Token() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler());

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml2:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_UNSIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assertFalse(receivedAssertion.isSigned());
    }

    @Test
    public void testSaml2TokenSignedSenderVouches() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, new SAML2CallbackHandler());
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_UNSIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml2:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_UNSIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assertFalse(receivedAssertion.isSigned());
    }


    /**
     * This test creates a holder-of-key SAML1 Assertion, and sends it in the security header
     * to the provider.
     */
    @Test
    public void testSaml1TokenHOK() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml1:Assertion");

        try {
            makeInvocation(outProperties, xpaths, inProperties, Collections.emptyMap());
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);

        Message message = makeInvocation(outProperties, xpaths, inProperties, Collections.emptyMap());
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_SIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assertTrue(receivedAssertion.isSigned());

        actionResult = handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);
        assertNotNull(actionResult);
    }

    /**
     * This test creates a holder-of-key SAML2 Assertion, and sends it in the security header
     * to the provider.
     */
    @Test
    public void testSaml2TokenHOK() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION,
            ConfigurationConstants.SAML_TOKEN_SIGNED + " " + ConfigurationConstants.SIGNATURE
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml2:Assertion");

        try {
            makeInvocation(outProperties, xpaths, inProperties, Collections.emptyMap());
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSenderVouches(false);

        try {
            makeInvocation(outProperties, xpaths, inProperties, Collections.emptyMap());
            fail("Failure expected in SAML Validator");
        } catch (Fault ex) {
            // expected
        }
        validator.setRequireSAML1Assertion(false);

        Message message = makeInvocation(outProperties, xpaths, inProperties, Collections.emptyMap());
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_SIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assertTrue(receivedAssertion.isSigned());

        actionResult = handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);
        assertNotNull(actionResult);
    }

    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     * An single attribute is created for the roles but multiple attribute value elements.
     */
    @Test
    public void testSaml2TokenWithRoles() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        callbackHandler.setStatement(Statement.ATTR);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        validator.setRequireSenderVouches(false);
        validator.setRequireBearer(true);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml2:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_SIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assertTrue(receivedAssertion.isSigned());
    }

    /**
     * This test creates a SAML2 Assertion and sends it in the security header to the provider.
     * An attribute is created per role. There are several attributes with the same name.
     */
    @Test
    public void testSaml2TokenWithRolesSingleValue() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler(false);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        callbackHandler.setStatement(Statement.ATTR);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(false);
        validator.setRequireSenderVouches(false);
        validator.setRequireBearer(true);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml2:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_SIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml2() != null);
        assertTrue(receivedAssertion.isSigned());
    }

    /**
     * This test creates a SAML1 Assertion and sends it in the security header to the provider.
     */
    @Test
    public void testSaml1TokenWithRoles() throws Exception {
        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_UNSIGNED);
        outProperties.put(ConfigurationConstants.SIG_KEY_ID, "DirectReference");
        outProperties.put(ConfigurationConstants.USER, "alice");
        outProperties.put("password", "password");
        outProperties.put(ConfigurationConstants.SIG_PROP_FILE, "alice.properties");
        SAML1CallbackHandler callbackHandler = new SAML1CallbackHandler();
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_HOLDER_KEY);
        callbackHandler.setSignAssertion(true);
        callbackHandler.setStatement(Statement.ATTR);
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);

        outProperties.put(ConfigurationConstants.SAML_CALLBACK_REF, callbackHandler);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(
            ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED
        );
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        final Map<QName, Object> customMap = new HashMap<>();
        CustomSamlValidator validator = new CustomSamlValidator();
        validator.setRequireSAML1Assertion(true);
        validator.setRequireSenderVouches(false);
        validator.setRequireBearer(true);
        customMap.put(WSConstants.SAML_TOKEN, validator);
        customMap.put(WSConstants.SAML2_TOKEN, validator);
        inProperties.put(WSS4JInInterceptor.VALIDATOR_MAP, customMap);

        List<String> xpaths = Arrays.asList(
            "//wsse:Security",
            "//wsse:Security/saml1:Assertion");

        Map<String, String> inMessageProperties = new HashMap<>();
        inMessageProperties.put(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, "false");
        Message message = makeInvocation(outProperties, xpaths, inProperties, inMessageProperties);

        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));

        SecurityContext sc = message.get(SecurityContext.class);
        assertNotNull(sc);
        assertTrue(sc.isUserInRole("user"));
        assertTrue(sc.isUserInRole("admin"));

        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.ST_SIGNED).get(0);
        SamlAssertionWrapper receivedAssertion =
            (SamlAssertionWrapper) actionResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertTrue(receivedAssertion != null && receivedAssertion.getSaml1() != null);
        assertTrue(receivedAssertion.isSigned());
    }

    private SoapMessage makeInvocation(
        Map<String, Object> outProperties,
        List<String> xpaths,
        Map<String, Object> inProperties,
        Map<String, String> inMessageProperties
    ) throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        for (String key : outProperties.keySet()) {
            msg.put(key, outProperties.get(key));
        }

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

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
        StaxUtils.read(db, reader, false);

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(inProperties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        inmsg.put(SecurityConstants.SAML_ROLE_ATTRIBUTENAME, "role");
        for (String inMessageProperty : inMessageProperties.keySet()) {
            inmsg.put(inMessageProperty, inMessageProperties.get(inMessageProperty));
        }
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.handleMessage(inmsg);

        return inmsg;
    }

    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) throws Exception {
        return StaxUtils.toString(doc);
    }
}
