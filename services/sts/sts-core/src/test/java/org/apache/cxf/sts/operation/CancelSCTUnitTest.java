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
import java.util.Collections;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.canceller.SCTCanceller;
import org.apache.cxf.sts.token.canceller.TokenCanceller;
import org.apache.cxf.sts.token.provider.SCTProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.CancelTargetType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the cancel operation to cancel SecurityContextTokens.
 */
public class CancelSCTUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    private static final QName QNAME_REQ_TOKEN_CANCELLED =
        QNameConstants.WS_TRUST_FACTORY.createRequestedTokenCancelled(null).getName();

    private static TokenStore tokenStore;

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test to successfully cancel a SecurityContextToken
     */
    @org.junit.Test
    public void testCancelSCT() throws Exception {
        TokenCancelOperation cancelOperation = new TokenCancelOperation();
        cancelOperation.setTokenStore(tokenStore);

        // Add Token Canceller
        TokenCanceller sctCanceller = new SCTCanceller();
        sctCanceller.setVerifyProofOfPossession(false);
        cancelOperation.setTokenCancellers(Collections.singletonList(sctCanceller));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        cancelOperation.setStsProperties(stsProperties);

        // Get a SecurityContextToken via the SCTProvider
        TokenProviderResponse providerResponse = createSCT();
        Element sct = (Element)providerResponse.getToken();
        CancelTargetType cancelTarget = new CancelTargetType();
        cancelTarget.setAny(sct);

        // Mock up a request
        JAXBElement<CancelTargetType> cancelTargetType =
            new JAXBElement<CancelTargetType>(
                QNameConstants.CANCEL_TARGET, CancelTargetType.class, cancelTarget
            );
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        request.getAny().add(cancelTargetType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Cancel a token
        RequestSecurityTokenResponseType response =
            cancelOperation.cancel(request, principal, msgCtx);
        assertTrue(validateResponse(response));

        // Now try to cancel again
        try {
            cancelOperation.cancel(request, principal, msgCtx);
        } catch (STSException ex) {
            // expected
        }
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
     * Return true if the response has a valid RequestTokenCancelled element, false otherwise
     */
    private boolean validateResponse(RequestSecurityTokenResponseType response) {
        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());

        for (Object requestObject : response.getAny()) {
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_REQ_TOKEN_CANCELLED.equals(jaxbElement.getName())) {
                    return true;
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
