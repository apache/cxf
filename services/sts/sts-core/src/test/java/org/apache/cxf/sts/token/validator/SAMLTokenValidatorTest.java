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
package org.apache.cxf.sts.token.validator;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.claims.ClaimsAttributeStatementProvider;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DateUtil;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating a SAML token via the SAMLTokenValidator.
 */
public class SAMLTokenValidatorTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test a valid SAML 1.1 Assertion
     */
    @org.junit.Test
    public void testValidSAML1Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    /**
     * Test a valid SAML 2 Assertion
     */
    @org.junit.Test
    public void testValidSAML2Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
    }

    /**
     * Test a SAML 1.1 Assertion that is configured with the ClaimsAttributeStatementProvider,
     * but does not contain any claims. In older versions of the STS, this generated an invalid
     * SAML Assertion.
     */
    @org.junit.Test
    public void testSAML1AssertionWithClaims() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        validatorParameters.setTokenStore(null);
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertionWithClaimsProvider(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
    }

    /**
     * Test a SAML 1.1 Assertion with an invalid signature
     */
    @org.junit.Test
    public void testInvalidSignatureSAML1Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEveCryptoProperties());
        CallbackHandler callbackHandler = new EveCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "eve", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        // Set tokenstore to null so that issued token is not found in the cache
        validatorParameters.setTokenStore(null);

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    /**
     * Test a SAML 2 Assertion with an invalid signature
     */
    @org.junit.Test
    public void testInvalidSignatureSAML2Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEveCryptoProperties());
        CallbackHandler callbackHandler = new EveCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "eve", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        // Set tokenstore to null so that issued token is not found in the cache
        validatorParameters.setTokenStore(null);

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }


    /**
     * Test a SAML 1.1 Assertion with an invalid condition
     */
    @org.junit.Test
    public void testInvalidConditionSAML1Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));
        Thread.sleep(100);
        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);
    }

    /**
     * Test a SAML 2.0 Assertion with an invalid condition
     */
    @org.junit.Test
    public void testInvalidConditionSAML2Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));
        Thread.sleep(100);
        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);
    }


    /**
     * Test a SAML 1.1 Assertion using Certificate Constraints
     */
    @org.junit.Test
    public void testSAML1AssertionCertConstraints() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        validatorParameters.setTokenStore(null);

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));
        ((SAMLTokenValidator)samlTokenValidator).setSubjectConstraints(Arrays.asList(
            "XYZ",
            ".*CN=www.sts.com.*"));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        ((SAMLTokenValidator)samlTokenValidator).setSubjectConstraints(Collections.singletonList(
            "XYZ"));
        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
    }

    @org.junit.Test
    public void testSAML2AssertionWithRolesNoCaching() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertionWithRoles(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                                         callbackHandler, "manager");
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        // Disable caching
        validatorParameters.setTokenStore(null);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
        Set<Principal> roles = validatorResponse.getRoles();
        assertTrue(roles != null && !roles.isEmpty());
        assertEquals("manager", roles.iterator().next().getName());
    }

    @org.junit.Test
    public void testSAML2AssertionWithRolesCaching() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertionWithRoles(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                                         callbackHandler, "employee");
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
        Set<Principal> roles = validatorResponse.getRoles();
        assertTrue(roles != null && !roles.isEmpty());
        assertEquals("employee", roles.iterator().next().getName());
    }

    /**
     * Test an invalid SAML 2 Assertion
     */
    @org.junit.Test
    public void testInvalidSAML2Assertion() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        // Replace "alice" with "bob".
        Element nameID =
            (Element)samlToken.getElementsByTagNameNS(WSS4JConstants.SAML2_NS, "NameID").item(0);
        nameID.setTextContent("bob");

        // Now validate again
        validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() != STATE.VALID);
    }

    @org.junit.Test
    public void testSAML2SubjectWithComment() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        String principalName = "alice<!---->o=example.com";
        Element samlToken =
            createSAMLAssertion(principalName, WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto,
                                "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        Principal principal = validatorResponse.getPrincipal();
        assertTrue(principal != null && principal.getName() != null);
        assertEquals(principalName, principal.getName());
    }

    private TokenValidatorParameters createValidatorParameters() throws WSSecurityException {
        TokenValidatorParameters parameters = new TokenValidatorParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(STSConstants.STATUS);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);
        parameters.setTokenStore(tokenStore);

        return parameters;
    }

    private Element createSAMLAssertion(
        String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        return createSAMLAssertion("alice", tokenType, crypto, signatureUsername, callbackHandler);
    }

    private Element createSAMLAssertion(
        String subjectName, String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(
                 subjectName, tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private Element createSAMLAssertionWithRoles(
        String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler,
        String role
    ) throws WSSecurityException {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(
                "alice", tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        claim.addValue(role);
        claims.add(claim);

        providerParameters.setRequestedPrimaryClaims(claims);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private Element createSAMLAssertionWithClaimsProvider(
        String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        AttributeStatementProvider statementProvider = new ClaimsAttributeStatementProvider();
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(statementProvider));
        TokenProviderParameters providerParameters =
            createProviderParameters(
                "alice", tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private Element createSAMLAssertion(
            String tokenType, Crypto crypto, String signatureUsername,
            CallbackHandler callbackHandler, long ttlMs
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
        TokenProviderParameters providerParameters =
            createProviderParameters(
                "alice", tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );

        if (ttlMs != 0) {
            Lifetime lifetime = new Lifetime();
            Instant creationTime = Instant.now();
            Instant expirationTime = creationTime.plusNanos(ttlMs * 1000000L);

            lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
            lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

            providerParameters.getTokenRequirements().setLifetime(lifetime);
        }

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private TokenProviderParameters createProviderParameters(
        String subjectName, String tokenType, String keyType, Crypto crypto,
        String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal(subjectName));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername(signatureUsername);
        stsProperties.setCallbackHandler(callbackHandler);
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());
        parameters.setTokenStore(tokenStore);

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    private Properties getEveCryptoProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "evespass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "eve.jks");

        return properties;
    }

    public class EveCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof WSPasswordCallback) { // CXF
                    WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                    if ("eve".equals(pc.getIdentifier())) {
                        pc.setPassword("evekpass");
                        break;
                    }
                }
            }
        }
    }


}
