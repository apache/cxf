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
package org.apache.cxf.sts.token.renewer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.Renewing;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.DateUtil;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for renewing a SAML token via the SAMLTokenRenewer.
 */
public class SAMLTokenRenewerTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Renew a valid SAML1 Assertion
     */
    @org.junit.Test
    public void renewValidSAML1Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, true, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        TokenRenewerResponse renewerResponse =
                samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        String oldId = new SamlAssertionWrapper(samlToken).getId();
        String newId = new SamlAssertionWrapper(renewerResponse.getToken()).getId();
        assertNotEquals(oldId, newId);

        // Now validate it again
        validateTarget = new ReceivedToken(renewerResponse.getToken());
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        // Now try to renew it again!
        renewerParameters.setToken(validatorResponse.getToken());

        samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());
    }

    /**
     * Renew a valid SAML1 Assertion. However, the issuer does not allow renewal
     */
    @org.junit.Test
    public void renewNotAllowedOfValidSAML1Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, false, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected on attempting to renew a token that was not allowed to be renewed");
        } catch (Exception ex) {
            // expected
        }
    }

    /**
     * Renew an expired SAML1 Assertion
     */
    @org.junit.Test
    public void renewExpiredSAML1Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        // Sleep to expire the token
        Thread.sleep(100);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected on an expired token, which is not allowed by default");
        } catch (Exception ex) {
            // expected
        }

        samlTokenRenewer.setAllowRenewalAfterExpiry(true);
        TokenRenewerResponse renewerResponse =
                samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        String oldId = new SamlAssertionWrapper(samlToken).getId();
        String newId = new SamlAssertionWrapper(renewerResponse.getToken()).getId();
        assertNotEquals(oldId, newId);

        // Now validate it again
        validateTarget = new ReceivedToken(renewerResponse.getToken());
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
    }

    /**
     * Renew an expired SAML2 Assertion
     */
    @org.junit.Test
    public void renewExpiredSAML2Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        // Sleep to expire the token
        Thread.sleep(100);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected on an expired token, which is not allowed by default");
        } catch (Exception ex) {
            // expected
        }

        samlTokenRenewer.setAllowRenewalAfterExpiry(true);
        TokenRenewerResponse renewerResponse =
                samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        String oldId = new SamlAssertionWrapper(samlToken).getId();
        String newId = new SamlAssertionWrapper(renewerResponse.getToken()).getId();
        assertNotEquals(oldId, newId);

        // Now validate it again
        validateTarget = new ReceivedToken(renewerResponse.getToken());
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
    }

    /**
     * Renew an expired SAML2 Assertion. However the issuer does not allow the renewal of expired
     * tokens.
     */
    @org.junit.Test
    public void renewExpiredNotAllowedSAML2Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        // Sleep to expire the token
        Thread.sleep(100);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure on attempting to renew an expired token, which is not allowed");
        } catch (Exception ex) {
            // expected
        }
    }


    /**
     * Renew an expired SAML2 Assertion that has expired greater than the maximum allowable time
     * for renewal.
     */
    @org.junit.Test
    public void renewTooFarExpiredSAML2Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        // Sleep to expire the token
        Thread.sleep(1500);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);
        ((SAMLTokenRenewer)samlTokenRenewer).setMaxExpiry(1L);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected as the token expired too long ago");
        } catch (STSException ex) {
            // Expected
        }
    }

    /**
     * Renew a valid SAML1 Assertion but sending a different AppliesTo address.
     */
    @org.junit.Test
    public void renewSAML1AssertionDifferentAppliesTo() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, true, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        assertTrue(samlTokenValidator.canHandleToken(validateTarget));

        TokenValidatorResponse validatorResponse =
                samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);

        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy2");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setMessageContext(validatorParameters.getMessageContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());

        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected on sending a different AppliesTo address");
        } catch (Exception ex) {
            // expected
        }
    }


    private TokenValidatorParameters createValidatorParameters() throws WSSecurityException {
        TokenValidatorParameters parameters = new TokenValidatorParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
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
            String tokenType, Crypto crypto, String signatureUsername,
            CallbackHandler callbackHandler, long ttlMs, boolean allowRenewing,
            boolean allowRenewingAfterExpiry
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
        TokenProviderParameters providerParameters =
            createProviderParameters(
                    tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );

        Renewing renewing = new Renewing();
        renewing.setAllowRenewing(allowRenewing);
        renewing.setAllowRenewingAfterExpiry(allowRenewingAfterExpiry);
        providerParameters.getTokenRequirements().setRenewing(renewing);

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
        String tokenType, String keyType, Crypto crypto,
        String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
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


}
