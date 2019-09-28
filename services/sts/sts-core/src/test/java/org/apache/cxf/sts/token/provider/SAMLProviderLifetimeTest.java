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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.DateUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for creating SAML Tokens with lifetime
 */
public class SAMLProviderLifetimeTest {

    /**
     * Issue SAML 2 token with a valid requested lifetime
     */
    @org.junit.Test
    public void testSaml2ValidLifetime() throws Exception {

        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 1 minute
        Lifetime lifetime = new Lifetime();
        Instant creationTime = Instant.now();
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        long duration = Duration.between(providerResponse.getCreated(), providerResponse.getExpires()).getSeconds();
        assertEquals(requestedLifetime, duration);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }

    /**
     *
     * As specified in ws-trust
     * "If this attribute isn't specified, then the current time is used as an initial period."
     * if creation time is not specified, we use current time instead.
     *
     */
    @org.junit.Test
    public void saml2LifetimeWithoutCreated() throws WSSecurityException {
        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 1 minute
        Lifetime lifetime = new Lifetime();
        Instant expirationTime = Instant.now().plusSeconds(requestedLifetime);

        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getExpires().toEpochMilli(), expirationTime.toEpochMilli());
    }



    /**
     * Issue SAML 2 token with a lifetime configured in SAMLTokenProvider
     * No specific lifetime requested
     */
    @org.junit.Test
    public void testSaml2ProviderLifetime() throws Exception {

        long providerLifetime = 10 * 600L;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setLifetime(providerLifetime);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        long duration = Duration.between(providerResponse.getCreated(), providerResponse.getExpires()).getSeconds();
        assertEquals(providerLifetime, duration);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }


    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetime() throws Exception {

        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 35 minutes
        Instant creationTime = Instant.now();
        long requestedLifetime = 35 * 60L;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }

    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds default maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededDefaultMaxLifetime() throws Exception {

        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to Default max lifetime plus 1
        Instant creationTime = Instant.now();
        long requestedLifetime = DefaultConditionsProvider.DEFAULT_MAX_LIFETIME + 1;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }

    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     * Lifetime reduced to maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetimeButUpdated() throws Exception {

        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setFailLifetimeExceedance(false);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 35 minutes
        Instant creationTime = Instant.now();
        long requestedLifetime = 35 * 60L;
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        long duration = Duration.between(providerResponse.getCreated(), providerResponse.getExpires()).getSeconds();
        assertEquals(maxLifetime, duration);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }

    /**
     * Issue SAML 2 token with a near future Created Lifetime. This should pass as we allow a future
     * dated Lifetime up to 60 seconds to avoid clock skew problems.
     */
    @org.junit.Test
    public void testSaml2NearFutureCreatedLifetime() throws Exception {

        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 1 minute
        Instant creationTime = Instant.now();
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);
        creationTime = creationTime.plusSeconds(10L);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        long duration = Duration.between(providerResponse.getCreated(), providerResponse.getExpires()).getSeconds();
        assertEquals(50, duration);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }

    /**
     * Issue SAML 2 token with a future Created Lifetime. This should fail as we only allow a future
     * dated Lifetime up to 60 seconds to avoid clock skew problems.
     */
    @org.junit.Test
    public void testSaml2FarFutureCreatedLifetime() throws Exception {

        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 1 minute
        Instant creationTime = Instant.now().plusSeconds(120L);
        Instant expirationTime = creationTime.plusSeconds(requestedLifetime);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        lifetime.setExpires(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a Created Element too far in the future");
        } catch (STSException ex) {
            // expected
        }

        // Now allow this sort of Created Element
        conditionsProvider.setFutureTimeToLive(60L * 60L);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }

    /**
     * Issue SAML 2 token with no Expires element. This will be rejected, but will default to the
     * configured TTL and so the request will pass.
     */
    @org.junit.Test
    public void testSaml2NoExpires() throws Exception {

        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        conditionsProvider.setFutureTimeToLive(180L);
        samlTokenProvider.setConditionsProvider(conditionsProvider);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );

        // Set expected lifetime to 1 minute
        Instant creationTime = Instant.now().plusSeconds(120L);

        Lifetime lifetime = new Lifetime();
        lifetime.setCreated(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        providerParameters.getTokenRequirements().setLifetime(lifetime);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        long duration = Duration.between(providerResponse.getCreated(), providerResponse.getExpires()).getSeconds();
        assertEquals(conditionsProvider.getLifetime(), duration);
        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
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
