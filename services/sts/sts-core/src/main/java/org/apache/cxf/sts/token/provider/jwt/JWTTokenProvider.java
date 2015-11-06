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

package org.apache.cxf.sts.token.provider.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.SAMLRealm;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * A TokenProvider implementation that provides a JWT Token.
 */
public class JWTTokenProvider implements TokenProvider {
    
    public static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final Logger LOG = LogUtils.getL7dLogger(JWTTokenProvider.class);
    
    private boolean signToken = true;
    private Map<String, SAMLRealm> realmMap = new HashMap<>();
    private JWTClaimsProvider jwtClaimsProvider = new DefaultJWTClaimsProvider();
    
    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType.
     */
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }
    
    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType in a given realm.
     */
    public boolean canHandleToken(String tokenType, String realm) {
        if (realm != null && !realmMap.containsKey(realm)) {
            return false;
        }
        return JWT_TOKEN_TYPE.equals(tokenType);
    }
    
    /**
     * Create a token given a TokenProviderParameters
     */
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        //KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Handling token of type: " + tokenRequirements.getTokenType());
        }
        
        // Get the claims
        JWTClaimsProviderParameters jwtClaimsProviderParameters = new JWTClaimsProviderParameters();
        jwtClaimsProviderParameters.setProviderParameters(tokenParameters);
        
        JwtClaims claims = jwtClaimsProvider.getJwtClaims(jwtClaimsProviderParameters);
        
        /*
        if (signToken) {
            STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
            signToken(assertion, samlRealm, stsProperties, tokenParameters.getKeyRequirements());
        }
        */
        
        try {
            /*
            Document doc = DOMUtils.createDocument();
            SamlAssertionWrapper assertion = createSamlToken(tokenParameters, secret, doc);
            Element token = assertion.toDOM(doc);
            
            // set the token in cache (only if the token is signed)
            byte[] signatureValue = assertion.getSignatureValue();
            if (tokenParameters.getTokenStore() != null && signatureValue != null
                && signatureValue.length > 0) {
                DateTime validTill = null;
                if (assertion.getSamlVersion().equals(SAMLVersion.VERSION_20)) {
                    validTill = assertion.getSaml2().getConditions().getNotOnOrAfter();
                } else {
                    validTill = assertion.getSaml1().getConditions().getNotOnOrAfter();
                }
                
                SecurityToken securityToken = 
                    CacheUtils.createSecurityTokenForStorage(token, assertion.getId(), 
                        validTill.toDate(), tokenParameters.getPrincipal(), tokenParameters.getRealm(),
                        tokenParameters.getTokenRequirements().getRenewing());
                CacheUtils.storeTokenInCache(
                    securityToken, tokenParameters.getTokenStore(), signatureValue);
            }
            */
            
            JwtToken token = new JwtToken(claims);
            
            Properties signingProperties = new Properties();
            signingProperties.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "none");
            
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(token);
            jws.setSignatureProperties(signingProperties);
            String tokenData = jws.getSignedEncodedJws();
            
            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(tokenData);
            
            response.setTokenId(claims.getTokenId());
            
            if (claims.getIssuedAt() > 0) {
                response.setCreated(new Date(claims.getIssuedAt() * 1000L));
            }
            if (claims.getExpiryTime() > 0) {
                response.setExpires(new Date(claims.getExpiryTime() * 1000L));
            }
            
            /*response.setEntropy(entropyBytes);
            if (keySize > 0) {
                response.setKeySize(keySize);
            }
            response.setComputedKey(computedKey);
            */
            LOG.fine("JWT Token successfully created");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.WARNING, "", e);
            throw new STSException("Can't serialize JWT token", e, STSException.REQUEST_FAILED);
        }
    }
    
    /**
     * Return whether the provided token will be signed or not. Default is true.
     */
    public boolean isSignToken() {
        return signToken;
    }

    /**
     * Set whether the provided token will be signed or not. Default is true.
     */
    public void setSignToken(boolean signToken) {
        this.signToken = signToken;
    }
    
    /**
     * Set the map of realm->SAMLRealm for this token provider
     * @param realms the map of realm->SAMLRealm for this token provider
     */
    public void setRealmMap(Map<String, SAMLRealm> realms) {
        this.realmMap = realms;
    }
    
    /**
     * Get the map of realm->SAMLRealm for this token provider
     * @return the map of realm->SAMLRealm for this token provider
     */
    public Map<String, SAMLRealm> getRealmMap() {
        return realmMap;
    }

    public JWTClaimsProvider getJwtClaimsProvider() {
        return jwtClaimsProvider;
    }

    public void setJwtClaimsProvider(JWTClaimsProvider jwtClaimsProvider) {
        this.jwtClaimsProvider = jwtClaimsProvider;
    }
    
}
