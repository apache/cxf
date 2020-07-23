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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

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
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DateUtil;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for renewing SAML Tokens with lifetime
 */
public class SAMLTokenRenewerLifetimeTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Renew SAML 2 token with a valid requested lifetime
     */
    @org.junit.Test
    public void testSaml2ValidLifetime() throws Exception {
        int requestedLifetime = 60;
        SAMLTokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);

        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenRenewer.setConditionsProvider(conditionsProvider);

        TokenRenewerParameters renewerParameters = createRenewerParameters();

        // Set expected lifetime to 1 minute
        Instant creationTime = Instant.now();
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        renewerParameters.getTokenRequirements().setLifetime(lifetime);

        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        // Create token.
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        renewTarget.setState(STATE.VALID);
        renewerParameters.getTokenRequirements().setRenewTarget(renewTarget);
        renewerParameters.setToken(renewTarget);

        assertTrue(samlTokenRenewer.canHandleToken(renewTarget));
        TokenRenewerResponse renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        long duration = Duration.between(renewerResponse.getCreated(), renewerResponse.getExpires()).getSeconds();
        assertEquals(requestedLifetime, duration);
    }


    /**
     * Renew SAML 2 token with a lifetime configured in SAMLTokenProvider
     * No specific lifetime requested
     */
    @org.junit.Test
    public void testSaml2ProviderLifetime() throws Exception {
        SAMLTokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);

        long providerLifetime = 10 * 600L;
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setLifetime(providerLifetime);
        samlTokenRenewer.setConditionsProvider(conditionsProvider);

        TokenRenewerParameters renewerParameters = createRenewerParameters();

        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        // Create token.
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        renewTarget.setState(STATE.VALID);
        renewerParameters.getTokenRequirements().setRenewTarget(renewTarget);
        renewerParameters.setToken(renewTarget);

        assertTrue(samlTokenRenewer.canHandleToken(renewTarget));
        TokenRenewerResponse renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        long duration = Duration.between(renewerResponse.getCreated(), renewerResponse.getExpires()).getSeconds();
        assertEquals(providerLifetime, duration);
    }


    /**
     * Renew SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetime() throws Exception {
        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);

        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenRenewer.setConditionsProvider(conditionsProvider);

        TokenRenewerParameters renewerParameters = createRenewerParameters();

        // Set expected lifetime to 35 minutes
        Instant creationTime = Instant.now();
        long requestedLifetime = 35 * 60L;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        renewerParameters.getTokenRequirements().setLifetime(lifetime);

        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        // Create token.
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        renewTarget.setState(STATE.VALID);
        renewerParameters.getTokenRequirements().setRenewTarget(renewTarget);
        renewerParameters.setToken(renewTarget);

        assertTrue(samlTokenRenewer.canHandleToken(renewTarget));
        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }

    /**
     * Renew SAML 2 token with a with a lifetime
     * which exceeds default maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededDefaultMaxLifetime() throws Exception {
        SAMLTokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);

        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenRenewer.setConditionsProvider(conditionsProvider);

        TokenRenewerParameters renewerParameters = createRenewerParameters();

        // Set expected lifetime to Default max lifetime plus 1
        Instant creationTime = Instant.now();
        long requestedLifetime = DefaultConditionsProvider.DEFAULT_MAX_LIFETIME + 1;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        renewerParameters.getTokenRequirements().setLifetime(lifetime);

        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        // Create token.
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        renewTarget.setState(STATE.VALID);
        renewerParameters.getTokenRequirements().setRenewTarget(renewTarget);
        renewerParameters.setToken(renewTarget);

        assertTrue(samlTokenRenewer.canHandleToken(renewTarget));
        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }

    /**
     * Renew SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     * Lifetime reduced to maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetimeButUpdated() throws Exception {

        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        samlTokenRenewer.setVerifyProofOfPossession(false);
        samlTokenRenewer.setAllowRenewalAfterExpiry(true);

        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setFailLifetimeExceedance(false);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenRenewer.setConditionsProvider(conditionsProvider);

        TokenRenewerParameters renewerParameters = createRenewerParameters();

        // Set expected lifetime to 35 minutes
        Instant creationTime = Instant.now();
        long requestedLifetime = 35 * 60L;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        renewerParameters.getTokenRequirements().setLifetime(lifetime);

        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        // Create token.
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        renewTarget.setState(STATE.VALID);
        renewerParameters.getTokenRequirements().setRenewTarget(renewTarget);
        renewerParameters.setToken(renewTarget);

        assertTrue(samlTokenRenewer.canHandleToken(renewTarget));
        TokenRenewerResponse renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        long duration = Duration.between(renewerResponse.getCreated(), renewerResponse.getExpires()).getSeconds();
        assertEquals(maxLifetime, duration);
    }


    private TokenRenewerParameters createRenewerParameters() throws WSSecurityException {
        TokenRenewerParameters parameters = new TokenRenewerParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        parameters.setTokenRequirements(tokenRequirements);
        parameters.setKeyRequirements(new KeyRequirements());

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

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

        parameters.setEncryptionProperties(new EncryptionProperties());

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
            Instant creationTime = Instant.now();
            Instant expirationTime = creationTime.plusNanos(ttlMs * 1000000L);

            Lifetime lifetime = new Lifetime();
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
