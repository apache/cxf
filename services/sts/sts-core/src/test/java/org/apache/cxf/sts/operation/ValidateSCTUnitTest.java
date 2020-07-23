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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

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
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.SCTProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.SCTValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the validate operation to validate SecurityContextTokens.
 */
public class ValidateSCTUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    private static final QName QNAME_WST_STATUS =
        QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test to successfully validate a SecurityContextToken
     */
    @org.junit.Test
    public void testValidateSCT() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        validateOperation.setTokenStore(tokenStore);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SCTValidator());
        validateOperation.setTokenValidators(validatorList);

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        validateOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, STSConstants.STATUS
            );
        request.getAny().add(tokenType);

        // Get a SecurityContextToken via the SCTProvider
        TokenProviderResponse providerResponse = createSCT();
        Element sct = (Element)providerResponse.getToken();
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(sct);

        JAXBElement<ValidateTargetType> validateTargetType =
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, principal, msgCtx);
        assertTrue(validateResponse(response));

        // Now remove the token from the cache before validating again
        tokenStore.remove(tokenStore.getToken(providerResponse.getTokenId()).getId());
        assertNull(tokenStore.getToken(providerResponse.getTokenId()));
        response = validateOperation.validate(request, principal, msgCtx);
        assertFalse(validateResponse(response));
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

    /**
     * Return true if the response has a valid status, false otherwise
     */
    private boolean validateResponse(RequestSecurityTokenResponseType response) {
        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        for (Object requestObject : response.getAny()) {
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_WST_STATUS.equals(jaxbElement.getName())) {
                    StatusType status = (StatusType)jaxbElement.getValue();
                    if (STSConstants.VALID_CODE.equals(status.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    private TokenProviderResponse createSCT() throws WSSecurityException {
        TokenProvider sctTokenProvider = new SCTProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(STSUtils.TOKEN_TYPE_SCT_05_12);

        assertTrue(sctTokenProvider.canHandleToken(STSUtils.TOKEN_TYPE_SCT_05_12));
        TokenProviderResponse providerResponse = sctTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return providerResponse;
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


}
