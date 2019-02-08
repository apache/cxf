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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.StaticClaimsHandler;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.jwt.DefaultJWTClaimsProvider;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A unit test for creating JWT Tokens with various claims populated by a ClaimsHandler.
 */
public class JWTClaimsTest {

    public static final URI CLAIM_STATIC_COMPANY =
        URI.create("http://apache.org/claims/test/company");

    public static final URI CLAIM_APPLICATION =
        URI.create("http://apache.org/claims/test/applicationId");

    private static final String CLAIM_STATIC_COMPANY_VALUE = "myc@mpany";

    private static final String APPLICATION_APPLIES_TO = "http://dummy-service.com/dummy";

    /**
     * Test the creation of a JWTToken with various claims set by a ClaimsHandler.
     */
    @org.junit.Test
    public void testJWTClaims() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = createClaims();
        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim(ClaimTypes.EMAILADDRESS.toString()), "alice@cxf.apache.org");
        assertEquals(jwt.getClaim(ClaimTypes.FIRSTNAME.toString()), "alice");
        assertEquals(jwt.getClaim(ClaimTypes.LASTNAME.toString()), "doe");
    }

    /**
     * Test the creation of a JWTToken with various claims set by a ClaimsHandler.
     * We have both a primary claim (sent in wst:RequestSecurityToken) and a secondary claim
     * (send in wst:RequestSecurityToken/wst:SecondaryParameters).
     */
    @org.junit.Test
    public void testJWTMultipleClaims() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

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

        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim(ClaimTypes.EMAILADDRESS.toString()), "alice@cxf.apache.org");
        assertEquals(jwt.getClaim(ClaimTypes.FIRSTNAME.toString()), "alice");
        assertEquals(jwt.getClaim(ClaimTypes.LASTNAME.toString()), "doe");
        assertEquals(jwt.getClaim(ClaimTypes.STREETADDRESS.toString()), "1234 1st Street");
    }

    /**
     * Test the creation of a JWTToken with various claims set by a ClaimsHandler.
     * We have both a primary claim (sent in wst:RequestSecurityToken) and a secondary claim
     * (send in wst:RequestSecurityToken/wst:SecondaryParameters), and both have the
     * same dialect in this test.
     */
    @org.junit.Test
    public void testJWTMultipleClaimsSameDialect() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

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

        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim(ClaimTypes.EMAILADDRESS.toString()), "alice@cxf.apache.org");
        assertEquals(jwt.getClaim(ClaimTypes.FIRSTNAME.toString()), "alice");
        assertEquals(jwt.getClaim(ClaimTypes.LASTNAME.toString()), "doe");
        assertEquals(jwt.getClaim(ClaimTypes.STREETADDRESS.toString()), "1234 1st Street");
    }

    /**
     * Test the creation of a JWTToken with StaticClaimsHandler
     */
    @org.junit.Test
    public void testJWTStaticClaims() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        StaticClaimsHandler claimsHandler = new StaticClaimsHandler();
        Map<String, String> staticClaimsMap = new HashMap<>();
        staticClaimsMap.put(CLAIM_STATIC_COMPANY.toString(), CLAIM_STATIC_COMPANY_VALUE);
        claimsHandler.setGlobalClaims(staticClaimsMap);
        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(CLAIM_STATIC_COMPANY);
        claims.add(claim);
        providerParameters.setRequestedPrimaryClaims(claims);

        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim(CLAIM_STATIC_COMPANY.toString()), CLAIM_STATIC_COMPANY_VALUE);
    }

    @org.junit.Test
    public void testJWTRoleUsingURI() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();

        URI role = URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");

        Claim claim = new Claim();
        claim.setClaimType(role);
        claims.add(claim);

        providerParameters.setRequestedPrimaryClaims(claims);

        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim(role.toString()), "DUMMY");
    }

    @org.junit.Test
    public void testJWTRoleUsingCustomReturnType() throws Exception {
        TokenProvider tokenProvider = new JWTTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE, null);

        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection claims = new ClaimCollection();

        URI role = URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");

        Claim claim = new Claim();
        claim.setClaimType(role);
        claims.add(claim);

        providerParameters.setRequestedPrimaryClaims(claims);

        Map<String, String> claimTypeMap = new HashMap<>();
        claimTypeMap.put(role.toString(), "roles");
        DefaultJWTClaimsProvider claimsProvider = new DefaultJWTClaimsProvider();
        claimsProvider.setClaimTypeMap(claimTypeMap);
        ((JWTTokenProvider)tokenProvider).setJwtClaimsProvider(claimsProvider);

        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        String token = (String)providerResponse.getToken();
        assertNotNull(token);

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        assertEquals(jwt.getClaim("roles"), "DUMMY");
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String appliesTo
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
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
