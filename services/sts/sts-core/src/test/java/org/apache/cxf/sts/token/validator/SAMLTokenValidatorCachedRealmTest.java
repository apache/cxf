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
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for validating a SAML token with a realm against the cached tokens realm.
 */
public class SAMLTokenValidatorCachedRealmTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test a SAML Assertion with valid cached realm.
     */
    @org.junit.Test
    public void testValidCachedRealm() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, "A");
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        // Now set the SAMLRealmCodec implementation on the Validator
        SAMLRealmCodec samlRealmCodec = new IssuerSAMLRealmCodec();
        ((SAMLTokenValidator)samlTokenValidator).setSamlRealmCodec(samlRealmCodec);

        TokenValidatorResponse validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        assertEquals("A", validatorResponse.getTokenRealm());

    }

    /**
     * Test a SAML Assertion with invalid cached realm.
     */
    @org.junit.Test
    public void testInValidCachedRealm() throws Exception {
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();

        // Create a ValidateTarget consisting of a SAML Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, "A");
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);

        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);

        // Now set the SAMLRealmCodec implementation on the Validator
        SAMLRealmCodec samlRealmCodec = new CacheSAMLRealmCodec();
        ((SAMLTokenValidator)samlTokenValidator).setSamlRealmCodec(samlRealmCodec);

        TokenValidatorResponse validatorResponse = samlTokenValidator.validateToken(validatorParameters);
        assertNotNull(validatorResponse);
        assertNotNull(validatorResponse.getToken());
        assertTrue(validatorResponse.getToken().getState() == STATE.INVALID);
        assertNull(validatorResponse.getTokenRealm());

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
        String tokenType,
        Crypto crypto,
        String signatureUsername,
        CallbackHandler callbackHandler,
        String realm
    ) throws WSSecurityException {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(
                tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );
        providerParameters.setRealm(realm);

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
