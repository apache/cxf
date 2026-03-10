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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.StaticClaimsHandler;
import org.apache.cxf.sts.claims.StaticEndpointClaimsHandler;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Attribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A unit test for creating a SAML Tokens with various Attributes populated by a ClaimsHandler.
 */
public class SAMLClaimsTest {

    public static final String CLAIM_STATIC_COMPANY =
        "http://apache.org/claims/test/company";

    public static final String CLAIM_APPLICATION =
        "http://apache.org/claims/test/applicationId";

    private static final String CLAIM_STATIC_COMPANY_VALUE = "myc@mpany";

    private static final String CLAIM_APPLICATION_VALUE = "my@pplic@tion";

    private static final String APPLICATION_APPLIES_TO = "http://dummy-service.com/dummy";

    /**
     * Test the creation of a SAML2 Assertion with various Attributes set by a ClaimsHandler.
     */
    @org.junit.Test
    public void testSaml2Claims() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = createClaims();
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.EMAILADDRESS.toString()));
        assertTrue(tokenString.contains(ClaimTypes.FIRSTNAME.toString()));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
    }

    /**
     * Test the creation of a SAML2 Assertion with various Attributes set by a ClaimsHandler.
     * We have both a primary claim (sent in wst:RequestSecurityToken) and a secondary claim
     * (send in wst:RequestSecurityToken/wst:SecondaryParameters).
     */
    @org.junit.Test
    public void testSaml2MultipleClaims() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection primaryClaims = createClaims();
        providerParameters.setRequestedPrimaryClaims(primaryClaims);

        ClaimCollection secondaryClaims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.STREETADDRESS);
        secondaryClaims.add(claim);
        providerParameters.setRequestedSecondaryClaims(secondaryClaims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.EMAILADDRESS.toString()));
        assertTrue(tokenString.contains(ClaimTypes.FIRSTNAME.toString()));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
        assertTrue(tokenString.contains(ClaimTypes.STREETADDRESS.toString()));
    }

    /**
     * Test the creation of a SAML2 Assertion with various Attributes set by a ClaimsHandler.
     * We have both a primary claim (sent in wst:RequestSecurityToken) and a secondary claim
     * (send in wst:RequestSecurityToken/wst:SecondaryParameters), and both have the
     * same dialect in this test.
     */
    @org.junit.Test
    public void testSaml2MultipleClaimsSameDialect() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection primaryClaims = createClaims();
        primaryClaims.setDialect(ClaimTypes.URI_BASE);
        providerParameters.setRequestedPrimaryClaims(primaryClaims);

        ClaimCollection secondaryClaims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.STREETADDRESS);
        secondaryClaims.add(claim);
        secondaryClaims.setDialect(ClaimTypes.URI_BASE);
        providerParameters.setRequestedSecondaryClaims(secondaryClaims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.EMAILADDRESS.toString()));
        assertTrue(tokenString.contains(ClaimTypes.FIRSTNAME.toString()));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
        assertTrue(tokenString.contains(ClaimTypes.STREETADDRESS.toString()));
    }

    /**
     * Test the creation of a SAML2 Assertion with StaticClaimsHandler
     */
    @org.junit.Test
    public void testSaml2StaticClaims() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        StaticClaimsHandler claimsHandler = new StaticClaimsHandler();
        Map<String, String> staticClaimsMap = new HashMap<>();
        staticClaimsMap.put(CLAIM_STATIC_COMPANY, CLAIM_STATIC_COMPANY_VALUE);
        claimsHandler.setGlobalClaims(staticClaimsMap);
        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(CLAIM_STATIC_COMPANY);
        claims.add(claim);
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));

        SamlAssertionWrapper assertion = new SamlAssertionWrapper(token);
        List<Attribute> attributes = assertion.getSaml2().getAttributeStatements().get(0).getAttributes();
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get(0).getName(), CLAIM_STATIC_COMPANY);
        XMLObject valueObj = attributes.get(0).getAttributeValues().get(0);
        assertEquals(valueObj.getDOM().getTextContent(), CLAIM_STATIC_COMPANY_VALUE);
    }

    /**
     * Test the creation of a SAML2 Assertion with StaticEndpointClaimsHandler
     */
    @org.junit.Test
    public void testSaml2StaticEndpointClaims() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        StaticEndpointClaimsHandler claimsHandler = new StaticEndpointClaimsHandler();

        // Create claims map for specific application
        Map<String, String> endpointClaimsMap = new HashMap<>();
        endpointClaimsMap.put(CLAIM_APPLICATION, CLAIM_APPLICATION_VALUE);

        Map<String, Map<String, String>> staticClaims = new HashMap<>();
        staticClaims.put(APPLICATION_APPLIES_TO, endpointClaimsMap);
        claimsHandler.setEndpointClaims(staticClaims);

        claimsHandler.setSupportedClaims(Collections.singletonList(CLAIM_APPLICATION));

        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(CLAIM_APPLICATION);
        claims.add(claim);
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));

        SamlAssertionWrapper assertion = new SamlAssertionWrapper(token);
        List<Attribute> attributes = assertion.getSaml2().getAttributeStatements().get(0).getAttributes();
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get(0).getName(), CLAIM_APPLICATION);
        XMLObject valueObj = attributes.get(0).getAttributeValues().get(0);
        assertEquals(valueObj.getDOM().getTextContent(), CLAIM_APPLICATION_VALUE);
    }

    /**
     * Test the creation of a SAML2 Assertion with StaticEndpointClaimsHandler
     * but unknown AppliesTo value
     */
    @org.junit.Test
    public void testSaml2StaticEndpointClaimsUnknownAppliesTo() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE,
                    STSConstants.BEARER_KEY_KEYTYPE, APPLICATION_APPLIES_TO + "UNKNOWN");

        ClaimsManager claimsManager = new ClaimsManager();
        StaticEndpointClaimsHandler claimsHandler = new StaticEndpointClaimsHandler();

        // Create claims map for specific application
        Map<String, String> endpointClaimsMap = new HashMap<>();
        endpointClaimsMap.put(CLAIM_APPLICATION, CLAIM_APPLICATION_VALUE);

        Map<String, Map<String, String>> staticClaims = new HashMap<>();
        staticClaims.put(APPLICATION_APPLIES_TO, endpointClaimsMap);
        claimsHandler.setEndpointClaims(staticClaims);

        claimsHandler.setSupportedClaims(Collections.singletonList(CLAIM_APPLICATION));

        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(CLAIM_APPLICATION);
        claims.add(claim);
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected as the claim for the application can't be found due to unknown AppliesTo");
        } catch (Exception ex) {
            // expected on the wrong attribute provider
        }
    }

    /**
     * Test the creation of a SAML2 Assertion with various Attributes set by a ClaimsHandler.
     */
    @org.junit.Test
    public void testSaml2ClaimsInteger() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.MOBILEPHONE);
        claims.add(claim);
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.MOBILEPHONE.toString()));
    }

    /**
     * Here we have two ClaimsHandlers that can both add claims for the same claim type. By default we will
     * combine the Claims into one Attribute.
     */
    @org.junit.Test
    public void testSaml2CombinedClaimsMultipleClaimsHandlers() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        ClaimsHandler claimsHandler2 = new CustomClaimsHandler();
        ((CustomClaimsHandler) claimsHandler2).setRole("CustomRole");
        claimsManager.setClaimHandlers(Arrays.asList(claimsHandler, claimsHandler2));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();

        Claim claim = new Claim();
        claim.setClaimType(CustomClaimsHandler.ROLE_CLAIM);
        claims.add(claim);

        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));

        String requiredClaim = CustomClaimsHandler.ROLE_CLAIM;
        assertTrue(tokenString.contains(requiredClaim));
        assertTrue(tokenString.contains("DUMMY"));
        assertTrue(tokenString.contains("CustomRole"));
        // Check only one Role Claim
        assertEquals(tokenString.indexOf(requiredClaim), tokenString.lastIndexOf(requiredClaim));
    }

    /**
     * Here we have two ClaimsHandlers that can both add claims for the same claim type. We configure the
     * SAMLTokenProvider not to combine the claims unlike the test above.
     */
    @org.junit.Test
    public void testSaml2SeparateClaimsMultipleClaimsHandlers() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        ((SAMLTokenProvider) samlTokenProvider).setCombineClaimAttributes(false);
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        ClaimsHandler claimsHandler2 = new CustomClaimsHandler();
        ((CustomClaimsHandler) claimsHandler2).setRole("CustomRole");
        claimsManager.setClaimHandlers(Arrays.asList(claimsHandler, claimsHandler2));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();

        Claim claim = new Claim();
        claim.setClaimType(CustomClaimsHandler.ROLE_CLAIM);
        claims.add(claim);

        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));

        String requiredClaim = CustomClaimsHandler.ROLE_CLAIM;
        assertTrue(tokenString.contains(requiredClaim));
        assertTrue(tokenString.contains("DUMMY"));
        assertTrue(tokenString.contains("CustomRole"));
        // Check we have two Role Claims
        assertNotEquals(tokenString.indexOf(requiredClaim), tokenString.lastIndexOf(requiredClaim));
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType, String appliesTo
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

        if (appliesTo != null) {
            parameters.setAppliesToAddress(appliesTo);
        } else {
            parameters.setAppliesToAddress(APPLICATION_APPLIES_TO);
        }

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

    /**
     * Create a set of parsed Claims
     */
    private ClaimCollection createClaims() {
        ClaimCollection claims = new ClaimCollection();

        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.FIRSTNAME);
        claims.add(claim);

        claim = new Claim();
        claim.setClaimType(ClaimTypes.LASTNAME);
        claims.add(claim);

        claim = new Claim();
        claim.setClaimType(ClaimTypes.EMAILADDRESS);
        claims.add(claim);

        return claims;
    }

}
