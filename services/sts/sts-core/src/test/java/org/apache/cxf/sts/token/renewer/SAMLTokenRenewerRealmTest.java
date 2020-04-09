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
import java.util.HashMap;
import java.util.Map;
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
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.sts.token.validator.IssuerSAMLRealmCodec;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
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

/**
 * Some unit tests for renewing a SAML token in different realms.
 */
public class SAMLTokenRenewerRealmTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test a SAML 1.1 Assertion created in realm "A".
     */
    @org.junit.Test
    public void testRealmA() throws Exception {
        // Create a RenewTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, crypto, "mystskey",
                callbackHandler
            );

        Element samlToken = createSAMLAssertion(providerParameters, "A", 50, true, true);
        // Sleep to expire the token
        Thread.sleep(100);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        SAMLRealmCodec samlRealmCodec = new IssuerSAMLRealmCodec();
        ((SAMLTokenValidator)samlTokenValidator).setSamlRealmCodec(samlRealmCodec);

        TokenValidatorParameters validatorParameters = createValidatorParameters();
        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        tokenRequirements.setValidateTarget(renewTarget);
        validatorParameters.setToken(renewTarget);

        // Validate the token
        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);
        assertEquals("A", validatorResponse.getTokenRealm());

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
        Map<String, RealmProperties> samlRealms = getSamlRealms();
        ((SAMLTokenRenewer)samlTokenRenewer).setRealmMap(samlRealms);
        String realm = validatorResponse.getTokenRealm();
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken(), realm));

        TokenRenewerResponse renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        // Now validate it again
        ReceivedToken validateTarget = new ReceivedToken(renewerResponse.getToken());
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
    }

    /**
     * Test a SAML 1.1 Assertion created in realm "B".
     */
    @org.junit.Test
    public void testRealmB() throws Exception {
        // Create a RenewTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, crypto, "mystskey",
                callbackHandler
            );

        Element samlToken = createSAMLAssertion(providerParameters, "B", 50, true, true);
        // Sleep to expire the token
        Thread.sleep(100);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        SAMLRealmCodec samlRealmCodec = new IssuerSAMLRealmCodec();
        ((SAMLTokenValidator)samlTokenValidator).setSamlRealmCodec(samlRealmCodec);

        TokenValidatorParameters validatorParameters = createValidatorParameters();
        ReceivedToken renewTarget = new ReceivedToken(samlToken);
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        tokenRequirements.setValidateTarget(renewTarget);
        validatorParameters.setToken(renewTarget);

        // Validate the token
        TokenValidatorResponse validatorResponse =
            samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.EXPIRED);
        assertEquals("B", validatorResponse.getTokenRealm());

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
        Map<String, RealmProperties> samlRealms = getSamlRealms();
        ((SAMLTokenRenewer)samlTokenRenewer).setRealmMap(samlRealms);
        String realm = validatorResponse.getTokenRealm();
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken(), realm));

        TokenRenewerResponse renewerResponse = samlTokenRenewer.renewToken(renewerParameters);
        assertNotNull(renewerResponse);
        assertNotNull(renewerResponse.getToken());

        // Now validate it again
        ReceivedToken validateTarget = new ReceivedToken(renewerResponse.getToken());
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
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
        stsProperties.setIssuer("STS-2");
        parameters.setStsProperties(stsProperties);

        parameters.setTokenStore(tokenStore);

        return parameters;
    }

    private Element createSAMLAssertion(
        TokenProviderParameters providerParameters,
        String realm,
        long ttlMs,
        boolean allowRenewing,
        boolean allowRenewingAfterExpiry
    ) throws WSSecurityException {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        ((SAMLTokenProvider)samlTokenProvider).setConditionsProvider(conditionsProvider);

        providerParameters.setRealm(realm);

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

        // Create Realms
        Map<String, RealmProperties> samlRealms = getSamlRealms();
        ((SAMLTokenProvider)samlTokenProvider).setRealmMap(samlRealms);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private Map<String, RealmProperties> getSamlRealms() {
        // Create Realms
        Map<String, RealmProperties> samlRealms = new HashMap<>();
        RealmProperties samlRealm = new RealmProperties();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new RealmProperties();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        return samlRealms;
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
