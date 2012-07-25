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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import org.apache.cxf.jaxrs.ext.MessageContext;
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
    
    
    private MessageContext mc;

    private List<AccessTokenValidator> tokenHandlers = Collections.emptyList();
    private Set<String> supportedSchemes = new HashSet<String>();
    private OAuthDataProvider dataProvider;
    
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
        return mc;
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
    protected AccessTokenValidation getAccessTokenValidation() {
        AccessTokenValidation accessTokenV = null;
        if (dataProvider == null && tokenHandlers.isEmpty()) {
            throw new WebApplicationException(500);
        }
        
        // Get the scheme and its data, Bearer only is supported by default
        // WWW-Authenticate with the list of supported schemes will be sent back 
        // if the scheme is not accepted
        String[] authParts = AuthorizationUtils.getAuthorizationParts(mc, supportedSchemes);
        String authScheme = authParts[0];
        String authSchemeData = authParts[1];
        
        // Get the registered handler capable of processing the token
        AccessTokenValidator handler = findTokenValidator(authScheme);
        if (handler != null) {
            try {
                // Convert the HTTP Authorization scheme data into a token
                accessTokenV = handler.validateAccessToken(mc, authScheme, authSchemeData);
            } catch (OAuthServiceException ex) {
                AuthorizationUtils.throwAuthorizationFailure(
                    Collections.singleton(authScheme));
            }
        }
        // Default processing if no registered providers available
        ServerAccessToken localAccessToken = null;
        if (accessTokenV == null && dataProvider != null && authScheme.equals(DEFAULT_AUTH_SCHEME)) {
            try {
                localAccessToken = dataProvider.getAccessToken(authSchemeData);
            } catch (OAuthServiceException ex) {
                // to be handled next
            }
            if (localAccessToken == null) {
                AuthorizationUtils.throwAuthorizationFailure(
                    Collections.singleton(authScheme));
            }
            accessTokenV = new AccessTokenValidation(localAccessToken);
        }
        if (accessTokenV == null) {
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes);
        }
        // Check if token is still valid
        if (OAuthUtils.isExpired(accessTokenV.getTokenIssuedAt(), accessTokenV.getTokenLifetime())) {
            if (localAccessToken != null) {
                dataProvider.removeAccessToken(localAccessToken);
            }
            AuthorizationUtils.throwAuthorizationFailure(supportedSchemes);
        }
        return accessTokenV;
    }
    
    
}
