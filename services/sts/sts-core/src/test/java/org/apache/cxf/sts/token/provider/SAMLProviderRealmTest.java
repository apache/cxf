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
package org.apache.cxf.sts.token.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for creating SAML Tokens via the SAMLTokenProvider in different realms
 */
public class SAMLProviderRealmTest {

    /**
     * Test that a SAML 1.1 Bearer Assertion is created in specific realms.
     */
    @org.junit.Test
    public void testRealms() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        providerParameters.setRealm("A");

        // Create Realms
        Map<String, RealmProperties> samlRealms = new HashMap<>();
        RealmProperties samlRealm = new RealmProperties();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new RealmProperties();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        ((SAMLTokenProvider)samlTokenProvider).setRealmMap(samlRealms);

        // Realm "A"
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, "A"));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));

        // Realm "B"
        providerParameters.setRealm("B");
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, "B"));
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));

        // Default Realm
        providerParameters.setRealm(null);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, null));
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"STS\""));
    }

    @org.junit.Test
    public void testRealmsUsingOldRealmClass() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        providerParameters.setRealm("A");

        // Create Realms
        Map<String, org.apache.cxf.sts.token.realm.RealmProperties> samlRealms =
            new HashMap<>();
        org.apache.cxf.sts.token.realm.RealmProperties samlRealm = new org.apache.cxf.sts.token.realm.RealmProperties();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new org.apache.cxf.sts.token.realm.RealmProperties();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        ((SAMLTokenProvider)samlTokenProvider).setRealmMap(samlRealms);

        // Realm "A"
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, "A"));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));

        // Realm "B"
        providerParameters.setRealm("B");
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, "B"));
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));

        // Default Realm
        providerParameters.setRealm(null);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE, null));
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"STS\""));
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType
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
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

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
