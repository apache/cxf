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

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * OAuth2 Access Token Service implementation
 */
@Path("/token")
public class AccessTokenService extends AbstractOAuthService {
    private List<AccessTokenGrantHandler> grantHandlers = Collections.emptyList();
    private boolean writeOptionalParameters = true;
    private boolean writeCustomErrors;
    
    public void setWriteOptionalParameters(boolean write) {
        writeOptionalParameters = write;
    }
    
    public void setWriteCustomErrors(boolean write) {
        writeCustomErrors = write;
    }
    
    /**
     * Sets the list of optional grant handlers
     * @param handlers the grant handlers
     */
    public void setGrantHandlers(List<AccessTokenGrantHandler> handlers) {
        grantHandlers = handlers;
    }
    
    /**
     * Processes an access token request
     * @param params the form parameters representing the access token grant 
     * @return Access Token or the error 
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response handleTokenRequest(MultivaluedMap<String, String> params) {
        
        // Make sure the client is authenticated
        Client client = authenticateClientIfNeeded(params);
        
        // Find the grant handler
        AccessTokenGrantHandler handler = findGrantHandler(params);
        if (handler == null) {
            return createErrorResponse(params, OAuthConstants.UNSUPPORTED_GRANT_TYPE);
        }
        
        // Create the access token
        ServerAccessToken serverToken = null;
        try {
            serverToken = handler.createAccessToken(client, params);
        } catch (OAuthServiceException ex) {
            OAuthError customError = ex.getError();
            if (writeCustomErrors && customError != null) {
                return createErrorResponseFromBean(customError);
            }

        }
        if (serverToken == null) {
            return createErrorResponse(params, OAuthConstants.INVALID_GRANT);
        }
        
        // Extract the information to be of use for the client
        ClientAccessToken clientToken = new ClientAccessToken(serverToken.getTokenType(),
                                                              serverToken.getTokenKey());
        if (writeOptionalParameters) {
            clientToken.setExpiresIn(serverToken.getLifetime());
            List<OAuthPermission> perms = serverToken.getScopes();
            if (!perms.isEmpty()) {
                clientToken.setApprovedScope(OAuthUtils.convertPermissionsToScope(perms));    
            }
            clientToken.setParameters(serverToken.getParameters());
        }
        
        //TODO: also set a refresh token if any
        
        // Return it to the client
        return Response.ok(clientToken)
                       .header(HttpHeaders.CACHE_CONTROL, "no-store")
                       .header("Pragma", "no-cache")
                        .build();
    }
    
    /**
     * Make sure the client is authenticated
     */
    private Client authenticateClientIfNeeded(MultivaluedMap<String, String> params) {
        Client client = null;
        SecurityContext sc = getMessageContext().getSecurityContext();
        
        if (params.containsKey(OAuthConstants.CLIENT_ID)) {
            // both client_id and client_secret are expected in the form payload
            client = getAndValidateClient(params.getFirst(OAuthConstants.CLIENT_ID),
                                          params.getFirst(OAuthConstants.CLIENT_SECRET));
        } else if (sc.getUserPrincipal() != null) {
            // client has already authenticated
            Principal p = sc.getUserPrincipal();
            String scheme = sc.getAuthenticationScheme();
            if (OAuthConstants.BASIC_SCHEME.equalsIgnoreCase(scheme)) {
                // section 2.3.1
                client = getClient(p.getName());
            } else {
                // section 2.3.2
                // the client has authenticated itself using some other scheme
                // in which case the mapping between the scheme and the client_id
                // should've been done and the client_id is expected
                // on the current message
                Object clientIdProp = getMessageContext().get(OAuthConstants.CLIENT_ID);
                if (clientIdProp != null) {
                    client = getClient(clientIdProp.toString());
                    // TODO: consider matching client.getUserSubject().getLoginName() 
                    // against principal.getName() ?
                }
            }
        } else {
            // the client id and secret are expected to be in the Basic scheme data
            String[] parts = 
                AuthorizationUtils.getAuthorizationParts(getMessageContext());
            if (OAuthConstants.BASIC_SCHEME.equalsIgnoreCase(parts[0])) {
                String[] authInfo = AuthorizationUtils.getBasicAuthParts(parts[1]);
                client = getAndValidateClient(authInfo[0], authInfo[1]);
            }
        }
        
        if (client == null) {
            throw new WebApplicationException(401);
        }
        return client;
    }
    
    // Get the Client and check the id and secret
    private Client getAndValidateClient(String clientId, String clientSecret) {
        Client client = getClient(clientId);
        if (clientSecret == null || !client.getClientId().equals(clientId) 
            || !client.getClientSecret().equals(clientSecret)) {
            throw new WebApplicationException(401);
        }
        return client;
    }
    
    /**
     * Find the mathcing grant handler
     */
    protected AccessTokenGrantHandler findGrantHandler(MultivaluedMap<String, String> params) {
        String grantType = params.getFirst(OAuthConstants.GRANT_TYPE);        
        if (grantType != null) {
            for (AccessTokenGrantHandler handler : grantHandlers) {
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    return handler;
                }
            }
            // Lets try the default grant handler
            if (grantHandlers.size() == 0) {
                AuthorizationCodeGrantHandler handler = new AuthorizationCodeGrantHandler();
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    handler.setDataProvider(
                            (AuthorizationCodeDataProvider)super.getDataProvider());
                    return handler;
                }
            }
        }
        
        return null;
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String error) {
        return createErrorResponseFromBean(new OAuthError(error));
    }
    
    protected Response createErrorResponseFromBean(OAuthError errorBean) {
        return Response.status(400).entity(errorBean).build();
    }
}
