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
package org.apache.cxf.sts.token.validator.jwt;

import java.security.KeyStore;
import java.security.Principal;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;

/**
 * Validate a SAML Assertion. It is valid if it was issued and signed by this STS.
 */
public class JWTTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JWTTokenValidator.class);
    private int clockOffset;
    private int ttl;
    private JWTRoleParser roleParser;
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        if (token instanceof String) {
            try {
                JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer((String)token);
                if (jwtConsumer.getJwtToken() != null) {
                    return true;
                }
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating JWT Token");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);
        
        String token = (String)validateTarget.getToken();
        if (token == null) {
            return response;
        }
        
        if (token.split("\\.").length != 3) {
            LOG.log(Level.WARNING, "JWT Token appears not to be signed. Validation has failed");
            return response;
        }
        
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        JwtToken jwt = jwtConsumer.getJwtToken();
        
        // Verify the signature
        Properties verificationProperties = new Properties();
        
        Crypto signatureCrypto = stsProperties.getSignatureCrypto();
        String alias = stsProperties.getSignatureUsername();
        
        if (alias != null) {
            verificationProperties.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, alias);
        }
        
        if (!(signatureCrypto instanceof Merlin)) {
            throw new STSException("Can't get the keystore", STSException.REQUEST_FAILED);
        }
        KeyStore keystore = ((Merlin)signatureCrypto).getKeyStore();
        verificationProperties.put(JoseConstants.RSSEC_KEY_STORE, keystore);
        
        JwsSignatureVerifier signatureVerifier = 
            JwsUtils.loadSignatureVerifier(verificationProperties, jwt.getJwsHeaders());
        
        if (!jwtConsumer.verifySignatureWith(signatureVerifier)) {
            return response;
        }
        
        try {
            validateToken(jwt);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "JWT token validation failed", ex);
            return response;
        }
        
        
        /*
        // Get the realm of the SAML token
        String tokenRealm = null;
        if (samlRealmCodec != null) {
            tokenRealm = samlRealmCodec.getRealmFromToken(assertion);
            // verify the realm against the cached token
            if (secToken != null) {
                Map<String, Object> props = secToken.getProperties();
                if (props != null) {
                    String cachedRealm = (String)props.get(STSConstants.TOKEN_REALM);
                    if (cachedRealm != null && !tokenRealm.equals(cachedRealm)) {
                        return response;
                    }
                }
            }
        }
        response.setTokenRealm(tokenRealm);
        */

        if (isVerifiedWithAPublicKey(jwt)) {
            Principal principal = new SimplePrincipal(jwt.getClaims().getSubject());
            response.setPrincipal(principal);
            
            // Parse roles from the validated token
            if (roleParser != null) {
                Set<Principal> roles = 
                    roleParser.parseRolesFromToken(principal, null, jwt);
                response.setRoles(roles);
            }
        }

        validateTarget.setState(STATE.VALID);
        LOG.fine("JWT Token successfully validated");

        return response;
    }
    
    private boolean isVerifiedWithAPublicKey(JwtToken jwt) {
        String alg = (String)jwt.getJwsHeader(JoseConstants.HEADER_ALGORITHM);
        SignatureAlgorithm sigAlg = SignatureAlgorithm.getAlgorithm(alg);
        return SignatureAlgorithm.isPublicKeyAlgorithm(sigAlg);
    }
    
    protected void validateToken(JwtToken jwt) {
        // If we have no issued time then we need to have an expiry
        boolean expiredRequired = jwt.getClaims().getIssuedAt() == null;
        JwtUtils.validateJwtExpiry(jwt.getClaims(), clockOffset, expiredRequired);
        
        JwtUtils.validateJwtNotBefore(jwt.getClaims(), clockOffset, false);
        
        // If we have no expiry then we must have an issued at
        boolean issuedAtRequired = jwt.getClaims().getExpiryTime() == null;
        JwtUtils.validateJwtIssuedAt(jwt.getClaims(), ttl, clockOffset, issuedAtRequired);
    }

    public int getClockOffset() {
        return clockOffset;
    }

    public void setClockOffset(int clockOffset) {
        this.clockOffset = clockOffset;
    }
    
    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
    
    public JWTRoleParser getRoleParser() {
        return roleParser;
    }

    public void setRoleParser(JWTRoleParser roleParser) {
        this.roleParser = roleParser;
    }
}
