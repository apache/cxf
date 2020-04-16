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

import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.message.token.SecurityContextToken;

import org.junit.BeforeClass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for creating SecurityContextTokens.
 */
public class SCTProviderTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Create a SecurityContextToken
     */
    @org.junit.Test
    public void testCreateSCT() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_12));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(ConversationConstants.WSC_NS_05_12));
        assertFalse(tokenString.contains(ConversationConstants.WSC_NS_05_02));
    }

    /**
     * Create a SecurityContextToken with a different namespace
     */
    @org.junit.Test
    public void testCreateSCTDifferentNamespace() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_02);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_02));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(ConversationConstants.WSC_NS_05_02));
        assertFalse(tokenString.contains(ConversationConstants.WSC_NS_05_12));
    }

    /**
     * Create a SecurityContextToken that returns (and doesn't return) Entropy
     */
    @org.junit.Test
    public void testCreateSCTReturnEntropy() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();
        assertTrue(((SCTProvider)sctTokenProvider).isReturnEntropy());

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_12));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertTrue(providerResponse.getEntropy() != null && providerResponse.getEntropy().length > 0);

        ((SCTProvider)sctTokenProvider).setReturnEntropy(false);
        providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertNull(providerResponse.getEntropy());
    }

    /**
     * Create a SecurityContextToken and test that it's stored in the cache
     */
    @org.junit.Test
    public void testCreateSCTCache() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_12));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        SecurityContextToken sctToken = new SecurityContextToken(token);
        String identifier = sctToken.getIdentifier();
        assertNotNull(tokenStore.getToken(identifier));
        assertNull(tokenStore.getToken(identifier + "1234"));
    }

    /**
     * Create a SecurityContextToken and test the KeySize
     */
    @org.junit.Test
    public void testCreateSCTKeySize() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_12));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertTrue(256L == providerResponse.getKeySize());

        // Test a custom KeySize
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();
        keyRequirements.setKeySize(192);
        providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(192L == providerResponse.getKeySize());

        // Test a bad KeySize - it will just use the default keysize
        keyRequirements.setKeySize(64);
        providerResponse = sctTokenProvider.createToken(providerParameters);
        assertTrue(256L == providerResponse.getKeySize());
    }


    private TokenProviderParameters createProviderParameters(String tokenType) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);

        parameters.setTokenStore(tokenStore);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setSignatureCrypto(crypto);
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
