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
package org.apache.cxf.sts.operation;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.Renewing;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RenewTargetType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.DateUtil;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the renew operation to renew SAML tokens.
 */
public class RenewSamlUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test to successfully renew a valid Saml 1.1 token
     */
    @org.junit.Test
    public void testRenewValidSaml1Token() throws Exception {
        TokenRenewOperation renewOperation = new TokenRenewOperation();
        renewOperation.setTokenStore(tokenStore);

        // Add Token Renewer
        TokenRenewer tokenRenewer = new SAMLTokenRenewer();
        tokenRenewer.setVerifyProofOfPossession(false);
        renewOperation.setTokenRenewers(Collections.singletonList(tokenRenewer));

        // Add Token Validator
        renewOperation.setTokenValidators(Collections.singletonList(
            new SAMLTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        renewOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.BEARER_KEY_KEYTYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, true, false
            );

        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        RenewTargetType renewTarget = new RenewTargetType();
        renewTarget.setAny(samlToken);

        JAXBElement<RenewTargetType> renewTargetType =
            new JAXBElement<RenewTargetType>(
                QNameConstants.RENEW_TARGET, RenewTargetType.class, renewTarget
            );
        request.getAny().add(renewTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Renew a token
        RequestSecurityTokenResponseType response =
            renewOperation.renew(request, principal, msgCtx);
        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
    }


    /**
     * Test to successfully renew an expired Saml 1.1 token.
     */
    @org.junit.Test
    public void testRenewExpiredSaml1Token() throws Exception {
        TokenRenewOperation renewOperation = new TokenRenewOperation();
        renewOperation.setTokenStore(tokenStore);

        // Add Token Renewer
        TokenRenewer tokenRenewer = new SAMLTokenRenewer();
        tokenRenewer.setVerifyProofOfPossession(false);
        tokenRenewer.setAllowRenewalAfterExpiry(true);
        renewOperation.setTokenRenewers(Collections.singletonList(tokenRenewer));

        // Add Token Validator
        renewOperation.setTokenValidators(Collections.singletonList(
            new SAMLTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        renewOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.BEARER_KEY_KEYTYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        RenewTargetType renewTarget = new RenewTargetType();
        renewTarget.setAny(samlToken);

        JAXBElement<RenewTargetType> renewTargetType =
            new JAXBElement<RenewTargetType>(
                QNameConstants.RENEW_TARGET, RenewTargetType.class, renewTarget
            );
        request.getAny().add(renewTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Renew a token
        RequestSecurityTokenResponseType response =
            renewOperation.renew(request, principal, msgCtx);

        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
    }

    /**
     * Test to successfully renew an expired Saml 2 token.
     */
    @org.junit.Test
    public void testRenewExpiredSaml2Token() throws Exception {
        TokenRenewOperation renewOperation = new TokenRenewOperation();
        renewOperation.setTokenStore(tokenStore);

        // Add Token Renewer
        TokenRenewer tokenRenewer = new SAMLTokenRenewer();
        tokenRenewer.setVerifyProofOfPossession(false);
        tokenRenewer.setAllowRenewalAfterExpiry(true);
        renewOperation.setTokenRenewers(Collections.singletonList(tokenRenewer));

        // Add Token Validator
        renewOperation.setTokenValidators(Collections.singletonList(
            new SAMLTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        renewOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.BEARER_KEY_KEYTYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        RenewTargetType renewTarget = new RenewTargetType();
        renewTarget.setAny(samlToken);

        JAXBElement<RenewTargetType> renewTargetType =
            new JAXBElement<RenewTargetType>(
                QNameConstants.RENEW_TARGET, RenewTargetType.class, renewTarget
            );
        request.getAny().add(renewTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Renew a token
        RequestSecurityTokenResponseType response =
            renewOperation.renew(request, principal, msgCtx);

        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }

    /**
     * Test to successfully renew an expired Saml 2 token and sending no TokenType.
     */
    @org.junit.Test
    public void testRenewExpiredSaml2TokenNoCacheNoTokenType() throws Exception {
        TokenRenewOperation renewOperation = new TokenRenewOperation();
        renewOperation.setTokenStore(tokenStore);

        // Add Token Renewer
        TokenRenewer tokenRenewer = new SAMLTokenRenewer();
        tokenRenewer.setVerifyProofOfPossession(false);
        tokenRenewer.setAllowRenewalAfterExpiry(true);
        renewOperation.setTokenRenewers(Collections.singletonList(tokenRenewer));

        // Add Token Validator
        renewOperation.setTokenValidators(Collections.singletonList(
            new SAMLTokenValidator()));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        renewOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(
                WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50, true, true
            );
        // Sleep to expire the token
        Thread.sleep(100);

        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        RenewTargetType renewTarget = new RenewTargetType();
        renewTarget.setAny(samlToken);

        JAXBElement<RenewTargetType> renewTargetType =
            new JAXBElement<RenewTargetType>(
                QNameConstants.RENEW_TARGET, RenewTargetType.class, renewTarget
            );
        request.getAny().add(renewTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Renew a token
        RequestSecurityTokenResponseType response =
            renewOperation.renew(request, principal, msgCtx);

        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }


    /*
     * Create a security context object
     */
    private SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {
            public Principal getUserPrincipal() {
                return p;
            }
            public boolean isUserInRole(String role) {
                return false;
            }
        };
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


}
