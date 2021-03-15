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
package org.apache.cxf.systest.sts.issueunit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
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
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.systest.sts.common.CommonCallbackHandler;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.mex.MetadataExchange;
import org.apache.cxf.ws.mex.model._2004_09.Metadata;
import org.apache.cxf.ws.mex.model._2004_09.MetadataSection;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the CXF STSClient Issue Binding.
 */
public class IssueUnitTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String SAML1_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    private static final String SAML2_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String SYMMETRIC_KEY_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/SymmetricKey";
    private static final String PUBLIC_KEY_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String BEARER_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private static final String DEFAULT_ADDRESS =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new STSServer("cxf-transport.xml")));
    }

    @org.junit.Test
    public void testRetrieveWSMEX() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get Metadata
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setBindingId(SoapBindingConstants.SOAP11_BINDING_ID);
        proxyFac.setAddress("https://localhost:" + STSPORT + "/SecurityTokenService/Transport/mex");
        MetadataExchange exc = proxyFac.create(MetadataExchange.class);
        Metadata metadata = exc.get2004();

        // Parse response (as per the STSClient)
        Definition definition = null;
        // Parse the MetadataSections into WSDL definition + associated schemas
        for (MetadataSection s : metadata.getMetadataSection()) {
            if ("http://schemas.xmlsoap.org/wsdl/".equals(s.getDialect())) {
                definition =
                    bus.getExtension(WSDLManager.class).getDefinition((Element)s.getAny());
            }
        }
        assertNotNull(definition);
    }

    /**
     * Test the Symmetric Key SAML1 case
     */
    @org.junit.Test
    public void testSymmetricKeySaml1() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        SecurityToken token =
            requestSecurityToken(SAML1_TOKEN_TYPE, SYMMETRIC_KEY_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertTrue(token.getSecret() != null && token.getSecret().length > 0);
        assertEquals(SAML1_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() != null && assertion.getSaml2() == null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getSecret());
    }

    /**
     * Test the Public Key SAML2 case
     */
    @org.junit.Test
    public void testPublicKeySaml2() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, PUBLIC_KEY_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertTrue(token.getSecret() == null && token.getX509Certificate() != null);
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() == null && assertion.getSaml2() != null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(OpenSAMLUtil.isMethodHolderOfKey(confirmMethod));
        SAMLKeyInfo subjectKeyInfo = assertion.getSubjectKeyInfo();
        assertNotNull(subjectKeyInfo.getCerts());
    }

    /**
     * Test the Bearer SAML1 case
     */
    @org.junit.Test
    public void testBearerSaml1() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        SecurityToken token =
            requestSecurityToken(SAML1_TOKEN_TYPE, BEARER_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertEquals(SAML1_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() != null && assertion.getSaml2() == null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod != null && confirmMethod.contains("bearer"));
    }

    /**
     * Test the Bearer Sender Vouches SAML2 case
     */
    @org.junit.Test
    public void testBearerSVSaml2() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        SecurityToken token =
            requestSecurityToken(
                SAML2_TOKEN_TYPE, BEARER_KEYTYPE, null, bus, DEFAULT_ADDRESS, null, null, null, null
            );
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() == null && assertion.getSaml2() != null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertNotNull(confirmMethod);
    }

    /**
     * Test that a request with no AppliesTo can be created by the CXF STS client.
     */
    @org.junit.Test
    public void testNoAppliesTo() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        try {
            requestSecurityToken(SAML1_TOKEN_TYPE, BEARER_KEYTYPE, bus, null);
            // fail("Failure expected on no AppliesTo value");
        } catch (Exception ex) {
            // expected
        }
    }

    /**
     * Test the Bearer SAML1 case with a Context Attribute
     */
    @org.junit.Test
    public void testBearerSaml1Context() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        String context = "AuthenticationContext";
        SecurityToken token =
            requestSecurityToken(SAML1_TOKEN_TYPE, BEARER_KEYTYPE, bus, DEFAULT_ADDRESS, context);
        assertEquals(SAML1_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() != null && assertion.getSaml2() == null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod != null && confirmMethod.contains("bearer"));
    }

    /**
     * Test the Bearer SAML1 case with a Lifetime element
     */
    @org.junit.Test
    public void testBearerSaml1Lifetime() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        // Get a token
        SecurityToken token =
            requestSecurityTokenTTL(SAML1_TOKEN_TYPE, BEARER_KEYTYPE, bus, DEFAULT_ADDRESS);
        assertEquals(SAML1_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() != null && assertion.getSaml2() == null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod != null && confirmMethod.contains("bearer"));

    }

    @org.junit.Test
    public void testSAMLinWSSecToOtherRealm() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new CommonCallbackHandler();

        //Create SAML token
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                    callbackHandler, null, "alice", "a-issuer");

        String id = null;
        QName elName = DOMUtils.getElementQName(samlToken);
        if (elName.equals(new QName(WSS4JConstants.SAML_NS, "Assertion"))
            && samlToken.hasAttributeNS(null, "AssertionID")) {
            id = samlToken.getAttributeNS(null, "AssertionID");
        } else if (elName.equals(new QName(WSS4JConstants.SAML2_NS, "Assertion"))
            && samlToken.hasAttributeNS(null, "ID")) {
            id = samlToken.getAttributeNS(null, "ID");
        }
        if (id == null) {
            id = samlToken.getAttributeNS(WSS4JConstants.WSU_NS, "Id");
        }

        SecurityToken wstoken = new SecurityToken(id, samlToken, null, null);
        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.TOKEN, wstoken);
        properties.put(SecurityConstants.TOKEN_ID, wstoken.getId());

        // Get a token

        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, null,
                    bus, DEFAULT_ADDRESS, null, properties, "b-issuer", "Transport_SAML_Port");

        /*
        SecurityToken token =
                requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, null,
                        bus, DEFAULT_ADDRESS, null, properties, "b-issuer", null);
                        */
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        List<WSSecurityEngineResult> results = processToken(token);
        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod != null && confirmMethod.contains("bearer"));

        assertEquals("b-issuer", assertion.getIssuerString());
        String subjectName = assertion.getSaml2().getSubject().getNameID().getValue();
        assertEquals("Subject must be ALICE instead of " + subjectName, "ALICE", subjectName);

    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Bus bus,
        String endpointAddress
    ) throws Exception {
        return requestSecurityToken(tokenType, keyType, null, bus, endpointAddress, null, null, null, null);
    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Bus bus,
        String endpointAddress,
        String context
    ) throws Exception {
        return requestSecurityToken(tokenType, keyType, null, bus, endpointAddress, context, null, null, null);
    }

    // CHECKSTYLE:OFF
    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Element supportingToken,
        Bus bus,
        String endpointAddress,
        String context,
        Map<String, Object> msgProperties,
        String realmUri,
        String wsdlPort
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        if (realmUri != null) {
            stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/" + realmUri
                                      + "/Transport?wsdl");
        } else {
            stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        }
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        if (wsdlPort != null) {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}" + wsdlPort);
        } else {
            stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");
        }

        Map<String, Object> properties = msgProperties;
        if (properties == null) {
            properties = new HashMap<>();
            properties.put(SecurityConstants.USERNAME, "alice");
            properties.put(
                SecurityConstants.CALLBACK_HANDLER,
                "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
            );
        }
        properties.put(SecurityConstants.IS_BSP_COMPLIANT, "false");

        if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            properties.put(SecurityConstants.STS_TOKEN_USERNAME, "myclientkey");
            properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
            stsClient.setUseCertificateForConfirmationKeyInfo(true);
        }
        if (supportingToken != null) {
            stsClient.setOnBehalfOf(supportingToken);
        }
        if (context != null) {
            stsClient.setContext(context);
        }

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);

        return stsClient.requestSecurityToken(endpointAddress);
    }
    // CHECKSTYLE:ON

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    /*
     * Mock up an SAML assertion element
     */
    private Element createSAMLAssertion(
        String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler,
        Map<String, RealmProperties> realms, String user, String issuer
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername,
                callbackHandler, user, issuer
            );
        if (realms != null) {
            providerParameters.setRealm("A");
        }
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

        parameters.setAppliesToAddress(
            "https://localhost:" + STSPORT + "/SecurityTokenService/b-issuer/Transport");

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

    private SecurityToken requestSecurityTokenTTL(
            String tokenType,
            String keyType,
            Bus bus,
            String endpointAddress
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES, "clientKeystore.properties");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
        properties.put(SecurityConstants.IS_BSP_COMPLIANT, "false");

        if (PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            properties.put(SecurityConstants.STS_TOKEN_USERNAME, "myclientkey");
            properties.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
            stsClient.setUseCertificateForConfirmationKeyInfo(true);
        }
        stsClient.setEnableLifetime(true);
        stsClient.setTtl(60 * 30);

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken(endpointAddress);
    }

    private List<WSSecurityEngineResult> processToken(SecurityToken token) throws Exception {
        RequestData requestData = new RequestData();
        requestData.setDisableBSPEnforcement(true);
        CallbackHandler callbackHandler = new org.apache.cxf.systest.sts.common.CommonCallbackHandler();
        requestData.setCallbackHandler(callbackHandler);
        Crypto crypto = CryptoFactory.getInstance("serviceKeystore.properties");
        requestData.setDecCrypto(crypto);
        requestData.setSigVerCrypto(crypto);
        requestData.setWsDocInfo(new WSDocInfo(token.getToken().getOwnerDocument()));

        Processor processor = new SAMLTokenProcessor();
        return processor.handleToken(token.getToken(), requestData);
    }
}
