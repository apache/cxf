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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public abstract class AbstractAccessTokenValidator {

    private static final String DEFAULT_AUTH_SCHEME = OAuthConstants.BEARER_AUTHORIZATION_SCHEME;


    protected Set<String> supportedSchemes = new HashSet<>();
    protected String realm;

    private MessageContext mc;
    private List<AccessTokenValidator> tokenHandlers = Collections.emptyList();
    private OAuthDataProvider dataProvider;

    private int maxValidationDataCacheSize;
    private ConcurrentHashMap<String, AccessTokenValidation> accessTokenValidations =
        new ConcurrentHashMap<>();
    private JoseJwtConsumer jwtTokenConsumer;
    private boolean persistJwtEncoding = true;

    public void setTokenValidator(AccessTokenValidator validator) {
        setTokenValidators(Collections.singletonList(validator));
    }

    public void setTokenValidators(List<AccessTokenValidator> validators) {
        tokenHandlers = validators;
        for (AccessTokenValidator handler : validators) {
            supportedSchemes.addAll(handler.getSupportedAuthorizationSchemes());
        }
    }

    public void setDataProvider(OAuthDataProvider provider) {
        dataProvider = provider;
    }

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
    }

    public MessageContext getMessageContext() {
        return mc != null ? mc : new MessageContextImpl(PhaseInterceptorChain.getCurrentMessage());
    }

    protected AccessTokenValidator findTokenValidator(String authScheme) {
        for (AccessTokenValidator handler : tokenHandlers) {
            List<String> handlerSchemes = handler.getSupportedAuthorizationSchemes();
            if (handlerSchemes.size() == 1 && OAuthConstants.ALL_AUTH_SCHEMES.equals(handlerSchemes.get(0))
                || handlerSchemes.contains(authScheme)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * Get the access token
     */
    protected AccessTokenValidation getAccessTokenValidation(String authScheme, String authSchemeData,
                                                             MultivaluedMap<String, String> extraProps) {
        if (dataProvider == null && tokenHandlers.isEmpty()) {
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        AccessTokenValidation accessTokenV = null;
        if (maxValidationDataCacheSize > 0) {
            accessTokenV = accessTokenValidations.get(authSchemeData);
        }
        ServerAccessToken localAccessToken = null;
        if (accessTokenV == null) {
            // Get the registered handler capable of processing the token
            AccessTokenValidator handler = findTokenValidator(authScheme);
            if (handler != null) {
                try {
                    // Convert the HTTP Authorization scheme data into a token
                    accessTokenV = handler.validateAccessToken(getMessageContext(), authScheme, authSchemeData,
                                                               extraProps);
                } catch (RuntimeException ex) {
                    AuthorizationUtils.throwAuthorizationFailure(Collections.singleton(authScheme), realm);
                }
            }
            // Default processing if no registered providers available
            if (accessTokenV == null && dataProvider != null && authScheme.equals(DEFAULT_AUTH_SCHEME)) {
                try {
                    String cacheKey = authSchemeData;
                    if (!persistJwtEncoding) {
                        JoseJwtConsumer theConsumer =
                            jwtTokenConsumer == null ? new JoseJwtConsumer() : jwtTokenConsumer;
                        JwtToken token = theConsumer.getJwtToken(authSchemeData);
                        cacheKey = token.getClaims().getTokenId();
                    }

                    localAccessToken = dataProvider.getAccessToken(cacheKey);
                } catch (JwtException | OAuthServiceException ex) {
                    // to be handled next
                }
                if (localAccessToken == null) {
                    AuthorizationUtils.throwAuthorizationFailure(
                        Collections.singleton(authScheme), realm);
                }
                accessTokenV = new AccessTokenValidation(localAccessToken);
            }
        }
        if (accessTokenV == null) {
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }
        // Check if token is still valid
        if (OAuthUtils.isExpired(accessTokenV.getTokenIssuedAt(), accessTokenV.getTokenLifetime())) {
            if (localAccessToken != null) {
                removeAccessToken(localAccessToken);
            } else if (maxValidationDataCacheSize > 0) {
                accessTokenValidations.remove(authSchemeData);
            }
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }

        // Check nbf property
        if (accessTokenV.getTokenNotBefore() > 0
            && accessTokenV.getTokenNotBefore() > System.currentTimeMillis() / 1000L) {
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes, realm);
        }
        if (maxValidationDataCacheSize > 0) {
            if (accessTokenValidations.size() >= maxValidationDataCacheSize) {
                // or delete the ones expiring sooner than others, etc
                accessTokenValidations.clear();
            }
            accessTokenValidations.put(authSchemeData, accessTokenV);
        }
        return accessTokenV;
    }

    protected void removeAccessToken(ServerAccessToken at) {
        dataProvider.revokeToken(at.getClient(),
                                 at.getTokenKey(),
                                 OAuthConstants.ACCESS_TOKEN);
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setMaxValidationDataCacheSize(int maxValidationDataCacheSize) {
        this.maxValidationDataCacheSize = maxValidationDataCacheSize;
    }

    public JoseJwtConsumer getJwtTokenConsumer() {
        return jwtTokenConsumer;
    }

    public void setJwtTokenConsumer(JoseJwtConsumer jwtTokenConsumer) {
        this.jwtTokenConsumer = jwtTokenConsumer;
    }

    public boolean isPersistJwtEncoding() {
        return persistJwtEncoding;
    }

    public void setPersistJwtEncoding(boolean persistJwtEncoding) {
        this.persistJwtEncoding = persistJwtEncoding;
    }


}
