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

import java.security.KeyStore;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSPasswordCallback;

/**
 * A TokenProvider implementation that provides a JWT Token.
 */
public class JWTTokenProvider implements TokenProvider {
    
    public static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final Logger LOG = LogUtils.getL7dLogger(JWTTokenProvider.class);
    
    private boolean signToken = true;
    private Map<String, RealmProperties> realmMap = new HashMap<>();
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
            
            String tokenData = signToken(token, null, tokenParameters.getStsProperties(), 
                      tokenParameters.getTokenRequirements());
            
            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(tokenData);
            
            response.setTokenId(claims.getTokenId());
            
            if (claims.getIssuedAt() > 0) {
                response.setCreated(new Date(claims.getIssuedAt() * 1000L));
            }
            if (claims.getExpiryTime() > 0) {
                response.setExpires(new Date(claims.getExpiryTime() * 1000L));
            }
            
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
     * Set the map of realm->RealmProperties for this token provider
     * @param realms the map of realm->RealmProperties for this token provider
     */
    public void setRealmMap(Map<String, ? extends RealmProperties> realms) {
        this.realmMap.clear();
        this.realmMap.putAll(realms);
    }
    
    /**
     * Get the map of realm->RealmProperties for this token provider
     * @return the map of realm->RealmProperties for this token provider
     */
    public Map<String, RealmProperties> getRealmMap() {
        return Collections.unmodifiableMap(realmMap);
    }

    public JWTClaimsProvider getJwtClaimsProvider() {
        return jwtClaimsProvider;
    }

    public void setJwtClaimsProvider(JWTClaimsProvider jwtClaimsProvider) {
        this.jwtClaimsProvider = jwtClaimsProvider;
    }
    
    private String signToken(
        JwtToken token, 
        RealmProperties jwtRealm,
        STSPropertiesMBean stsProperties,
        TokenRequirements tokenRequirements
    ) throws Exception {
        
        Properties signingProperties = new Properties();
        
        if (signToken) {
            // Initialise signature objects with defaults of STSPropertiesMBean
            Crypto signatureCrypto = stsProperties.getSignatureCrypto();
            CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
            SignatureProperties signatureProperties = stsProperties.getSignatureProperties();
            String alias = stsProperties.getSignatureUsername();

            if (jwtRealm != null) {
                // If SignatureCrypto configured in realm then
                // callbackhandler and alias of STSPropertiesMBean is ignored
                if (jwtRealm.getSignatureCrypto() != null) {
                    LOG.fine("SAMLRealm signature keystore used");
                    signatureCrypto = jwtRealm.getSignatureCrypto();
                    callbackHandler = jwtRealm.getCallbackHandler();
                    alias = jwtRealm.getSignatureAlias();
                }
                // SignatureProperties can be defined independently of SignatureCrypto
                if (jwtRealm.getSignatureProperties() != null) {
                    signatureProperties = jwtRealm.getSignatureProperties();
                }
            }

            // Get the signature algorithm to use - for now we don't allow the client to ask
            // for a particular signature algorithm, as with SAML
            String signatureAlgorithm = signatureProperties.getSignatureAlgorithm();
            try {
                SignatureAlgorithm.getAlgorithm(signatureAlgorithm);
            } catch (IllegalArgumentException ex) {
                signatureAlgorithm = SignatureAlgorithm.RS256.name();
            }

            // If alias not defined, get the default of the SignatureCrypto
            if ((alias == null || "".equals(alias)) && (signatureCrypto != null)) {
                alias = signatureCrypto.getDefaultX509Identifier();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Signature alias is null so using default alias: " + alias);
                }
            }
            // Get the password
            WSPasswordCallback[] cb = {new WSPasswordCallback(alias, WSPasswordCallback.SIGNATURE)};
            callbackHandler.handle(cb);
            String password = cb[0].getPassword();

            signingProperties.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, signatureAlgorithm);
            signingProperties.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, alias);
            signingProperties.put(JoseConstants.RSSEC_KEY_PSWD, password);
            
            if (!(signatureCrypto instanceof Merlin)) {
                throw new STSException("Can't get the keystore", STSException.REQUEST_FAILED);
            }
            KeyStore keystore = ((Merlin)signatureCrypto).getKeyStore();
            signingProperties.put(JoseConstants.RSSEC_KEY_STORE, keystore);
            
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(token);
            jws.setSignatureProperties(signingProperties);
            
            Message m = PhaseInterceptorChain.getCurrentMessage();
            JwsSignatureProvider sigProvider = 
                JwsUtils.loadSignatureProvider(m, signingProperties, token.getJwsHeaders(), false);
            token.getJwsHeaders().setSignatureAlgorithm(sigProvider.getAlgorithm());
            
            return jws.signWith(sigProvider);
        } else {
            signingProperties.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "none");
            
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(token);
            jws.setSignatureProperties(signingProperties);
            return jws.getSignedEncodedJws();
        }
        
    }

}
