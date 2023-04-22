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
package org.apache.cxf.systest.sts.delegation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.systest.sts.common.CommonCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests for sending a SAML Token OnBehalfOf/ActAs to the STS. The STS is set up with
 * two endpoints, one requiring a UsernameToken over TLS, the other just requiring TLS
 * without client authentication (insecure, but used as part of the test process) with a
 * SAML DelegationHandler.
 */
public class SAMLDelegationTest extends AbstractBusClientServerTestBase {

    private static final String STSPORT = allocatePort(STSServer.class);

    private static final String SAML2_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String PUBLIC_KEY_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String BEARER_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private static final String DEFAULT_ADDRESS =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }

    @org.junit.Test
    public void testSAMLOnBehalfOf() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token from the UT endpoint first
        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, bus,
                                 DEFAULT_ADDRESS, "Transport_UT_Port");
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Use the first token as OnBehalfOf to get another token

        // First try with the UT endpoint. This should fail as there is no Delegation Handler.
        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, token.getToken(), bus,
                                     DEFAULT_ADDRESS, true, "Transport_UT_Port");
            fail("Failure expected on no delegation handler");
        } catch (Exception ex) {
            // expected
        }

        // Now send to the Transport endpoint.
        SecurityToken token2 =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, token.getToken(), bus,
                                 DEFAULT_ADDRESS, true, "Transport_Port");
        assertEquals(SAML2_TOKEN_TYPE, token2.getTokenType());
        assertNotNull(token2.getToken());
    }

    @org.junit.Test
    public void testSAMLActAs() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token from the UT endpoint first
        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, bus,
                                 DEFAULT_ADDRESS, "Transport_UT_Port");
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Use the first token as ActAs to get another token

        // First try with the UT endpoint. This should fail as there is no Delegation Handler.
        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, token.getToken(), bus,
                                     DEFAULT_ADDRESS, false, "Transport_UT_Port");
            fail("Failure expected on no delegation handler");
        } catch (Exception ex) {
            // expected
        }

        // Now send to the Transport endpoint.
        SecurityToken token2 =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, token.getToken(), bus,
                                 DEFAULT_ADDRESS, false, "Transport_Port");
        assertEquals(SAML2_TOKEN_TYPE, token2.getTokenType());
        assertNotNull(token2.getToken());
    }

    @org.junit.Test
    public void testTransportForgedDelegationToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new CommonCallbackHandler();

        // Create SAML token
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE,
                                crypto, "eve", callbackHandler, "alice", "a-issuer");

        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, samlToken, bus,
                                 DEFAULT_ADDRESS, true, "Transport_Port");
            fail("Failure expected on a forged delegation token");
        } catch (Exception ex) {
            // expected
        }

        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, samlToken, bus,
                                 DEFAULT_ADDRESS, false, "Transport_Port");
            fail("Failure expected on a forged delegation token");
        } catch (Exception ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testTransportUnsignedDelegationToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Create SAML token
        Element samlToken =
            createUnsignedSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE,
                                "alice", "a-issuer");

        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, samlToken, bus,
                                 DEFAULT_ADDRESS, true, "Transport_Port");
            fail("Failure expected on a unsigned delegation token");
        } catch (Exception ex) {
            // expected
        }

        try {
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, samlToken, bus,
                                 DEFAULT_ADDRESS, false, "Transport_Port");
            fail("Failure expected on a unsigned delegation token");
        } catch (Exception ex) {
            // expected
        }
    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Bus bus,
        String endpointAddress,
        String wsdlPort
    ) throws Exception {
        return requestSecurityToken(tokenType, keyType, null, bus, endpointAddress, true, wsdlPort);
    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Element supportingToken,
        Bus bus,
        String endpointAddress,
        boolean onBehalfOf,
        String wsdlPort
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");

        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        if (wsdlPort != null) {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}" + wsdlPort);
        } else {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(SecurityConstants.CALLBACK_HANDLER,
                       "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        properties.put(SecurityConstants.IS_BSP_COMPLIANT, "false");

        if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            properties.put(SecurityConstants.STS_TOKEN_USERNAME, "myclientkey");
            properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
            stsClient.setUseCertificateForConfirmationKeyInfo(true);
        }
        if (supportingToken != null) {
            if (onBehalfOf) {
                stsClient.setOnBehalfOf(supportingToken);
            } else {
                stsClient.setActAs(supportingToken);
            }
        }

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);

        return stsClient.requestSecurityToken(endpointAddress);
    }

    /*
     * Mock up an SAML assertion element
     */
    private Element createSAMLAssertion(
        String tokenType, String keyType, Crypto crypto, String signatureUsername,
        CallbackHandler callbackHandler, String user, String issuer
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();

        TokenProviderParameters providerParameters =
            createProviderParameters(
                tokenType, keyType, crypto, signatureUsername, callbackHandler, user, issuer
            );

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private Element createUnsignedSAMLAssertion(
        String tokenType, String keyType, String user, String issuer
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setSignToken(false);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                tokenType, keyType, null, null, null, user, issuer
            );

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType, Crypto crypto,
        String signatureUsername, CallbackHandler callbackHandler,
        String username, String issuer
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal(username));
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
        stsProperties.setIssuer(issuer);
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", "evespass");
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", "eve.jks");

        return properties;
    }

}
