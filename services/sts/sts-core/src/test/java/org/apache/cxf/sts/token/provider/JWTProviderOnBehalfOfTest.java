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

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.Assert;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for creating JWT Tokens with an OnBehalfOf element.
 */
public class JWTProviderOnBehalfOfTest {

    /**
     * Create a JWT Token with OnBehalfOf from a UsernameToken
     */
    @org.junit.Test
    public void testJWTOnBehalfOfUsernameToken() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();

        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue("bob");
        usernameToken.setUsername(username);
        JAXBElement<UsernameTokenType> usernameTokenType =
            new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );

        TokenProviderParameters providerParameters =
            createProviderParameters(
                JWTTokenProvider.JWT_TOKEN_TYPE, usernameTokenType
            );
        //Principal must be set in ReceivedToken/OnBehalfOf
        providerParameters.getTokenRequirements().getOnBehalfOf().setPrincipal(
                new CustomTokenPrincipal(username.getValue()));

        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        // Validate the token
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("bob", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
    }

    /**
     * Create a JWT Token with OnBehalfOf from a SAML Assertion
     */
    @org.junit.Test
    public void testJWTOnBehalfOfAssertion() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();

        String user = "alice";
        Element saml1Assertion = getSAMLAssertion(user);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                JWTTokenProvider.JWT_TOKEN_TYPE, saml1Assertion
            );
        //Principal must be set in ReceivedToken/OnBehalfOf
        providerParameters.getTokenRequirements().getOnBehalfOf().setPrincipal(
                new CustomTokenPrincipal(user));

        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        // Validate the token
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals(user, jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
    }

    private Element getSAMLAssertion(String user) throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, null);
        providerParameters.getKeyRequirements().setKeyType(STSConstants.BEARER_KEY_KEYTYPE);
        providerParameters.setPrincipal(new CustomTokenPrincipal(user));
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, Object onBehalfOf
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);

        if (onBehalfOf != null) {
            ReceivedToken onBehalfOfToken = new ReceivedToken(onBehalfOf);
            onBehalfOfToken.setState(STATE.VALID);
            tokenRequirements.setOnBehalfOf(onBehalfOfToken);

        }
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
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
