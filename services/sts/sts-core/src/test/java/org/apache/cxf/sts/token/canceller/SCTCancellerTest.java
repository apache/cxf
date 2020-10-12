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
package org.apache.cxf.sts.token.canceller;

import java.util.Properties;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
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
import org.apache.cxf.sts.token.provider.SCTProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.dom.message.token.SecurityContextToken;

import org.junit.BeforeClass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for cancelling a SecurityContextToken via the SCTCanceller.
 */
public class SCTCancellerTest {

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Get a (valid) SecurityContextToken and successfully cancel it.
     */
    @org.junit.Test
    public void testCancelToken() throws Exception {
        TokenCanceller sctCanceller = new SCTCanceller();
        sctCanceller.setVerifyProofOfPossession(false);
        TokenCancellerParameters cancellerParameters = createCancellerParameters();
        TokenRequirements tokenRequirements = cancellerParameters.getTokenRequirements();

        // Create a CancelTarget consisting of a SecurityContextToken
        TokenProviderResponse providerResponse = getSecurityContextToken();
        ReceivedToken cancelTarget = new ReceivedToken(providerResponse.getToken());
        tokenRequirements.setCancelTarget(cancelTarget);
        cancellerParameters.setToken(cancelTarget);

        assertTrue(sctCanceller.canHandleToken(cancelTarget));

        TokenCancellerResponse cancellerResponse = sctCanceller.cancelToken(cancellerParameters);
        assertNotNull(cancellerResponse);
        assertTrue(cancellerResponse.getToken().getState() == STATE.CANCELLED);

        // Try to cancel the token again - this should fail
        cancellerResponse = sctCanceller.cancelToken(cancellerParameters);
        assertNotNull(cancellerResponse);
        assertFalse(cancellerResponse.getToken().getState() == STATE.CANCELLED);
    }

    /**
     * Try to cancel an invalid SecurityContextToken
     */
    @org.junit.Test
    public void testCancelInvalidToken() throws Exception {
        TokenCanceller sctCanceller = new SCTCanceller();
        sctCanceller.setVerifyProofOfPossession(false);
        TokenCancellerParameters cancellerParameters = createCancellerParameters();
        TokenRequirements tokenRequirements = cancellerParameters.getTokenRequirements();

        // Create a CancelTarget consisting of a SecurityContextToken
        Document doc = DOMUtils.getEmptyDocument();
        SecurityContextToken sct = new SecurityContextToken(doc);
        ReceivedToken cancelTarget = new ReceivedToken(sct.getElement());
        tokenRequirements.setCancelTarget(cancelTarget);
        cancellerParameters.setToken(cancelTarget);

        assertTrue(sctCanceller.canHandleToken(cancelTarget));

        TokenCancellerResponse cancellerResponse = sctCanceller.cancelToken(cancellerParameters);
        assertNotNull(cancellerResponse);
        assertFalse(cancellerResponse.getToken().getState() == STATE.CANCELLED);
    }

    private TokenProviderResponse getSecurityContextToken() throws Exception {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        return sctTokenProvider.createToken(providerParameters);
    }

    private TokenCancellerParameters createCancellerParameters() throws WSSecurityException {
        TokenCancellerParameters parameters = new TokenCancellerParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(STSConstants.STATUS);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);
        parameters.setTokenStore(tokenStore);

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

        return parameters;
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
