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

package org.apache.cxf.rs.security.oauth.services;

import java.security.Principal;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.rs.security.oauth.common.Client;
import org.apache.cxf.rs.security.oauth.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth.common.OAuthError;
import org.apache.cxf.rs.security.oauth.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth.grants.code.AuthorizationCodeGrantHandler;
import org.apache.cxf.rs.security.oauth.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth.utils.AuthorizationUtils;


@Path("/token")
public class AccessTokenService extends AbstractOAuthService {
    private static final String CLIENT_SECRET = "client_secret";
    private static final String GRANT_TYPE = "grant_type";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    
    private List<AccessTokenGrantHandler> grantHandlers;
    
    public void setGrantHandlers(List<AccessTokenGrantHandler> handlers) {
        grantHandlers = handlers;
    }
    
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response handleTokenRequest(MultivaluedMap<String, String> params) {
        Client client = authenticateClientIfNeeded(params);
        
        AccessTokenGrantHandler handler = findGrantHandler(params);
        if (handler == null) {
            return createErrorResponse(params, UNSUPPORTED_GRANT_TYPE);
        }
        
        ServerAccessToken serverToken = null;
        try {
            serverToken = handler.createAccessToken(client, params);
        } catch (OAuthServiceException ex) {
            // the error response is to be returned next
        }
        if (serverToken == null) {
            return createErrorResponse(params, INVALID_GRANT);
        }
        getDataProvider().persistAccessToken(serverToken);
        
        ClientAccessToken clientToken = new ClientAccessToken(serverToken.getTokenType(),
                                                              serverToken.getTokenKey());
        clientToken.setParameters(serverToken.getParameters());
        return Response.ok(clientToken).build();
    }
    
    private Client authenticateClientIfNeeded(MultivaluedMap<String, String> params) {
        Client client = null;
        SecurityContext sc = getMessageContext().getSecurityContext();
        
        if (params.containsKey(CLIENT_ID)) {
            // both client_id and client_secret are expected in the form payload
            client = getAndValidateClient(params.getFirst(CLIENT_ID),
                                          params.getFirst(CLIENT_SECRET));
        } else if (sc.getUserPrincipal() != null) {
            // client has already authenticated
            Principal p = sc.getUserPrincipal();
            String scheme = sc.getAuthenticationScheme();
            if ("Basic".equals(scheme)) {
                // section 2.3.1
                client = getClient(p.getName());
            } else {
                // section 2.3.2
                // the client has authenticated itself using some other scheme
                // in which case the mapping between the scheme and the client_id
                // should've been done, in which case the client_id is expected
                // on the current message
                Object clientIdProp = getMessageContext().get(CLIENT_ID);
                if (clientIdProp != null) {
                    client = getClient(clientIdProp.toString());
                }
            }
        } else {
            String[] parts = 
                AuthorizationUtils.getAuthorizationParts(getMessageContext());
            if ("Basic".equals(parts[0])) {
                String[] authInfo = AuthorizationUtils.getBasicAuthParts(parts[1]);
                client = getAndValidateClient(authInfo[0], authInfo[1]);
            }
        }
        
        if (client == null) {
            throw new WebApplicationException(401);
        }
        return client;
    }
    
    private Client getAndValidateClient(String clientId, String clientSecret) {
        Client client = getClient(clientId);
        if (clientSecret == null || !client.getClientId().equals(clientId) 
            || !client.getClientSecret().equals(clientSecret)) {
            throw new WebApplicationException(401);
        }
        return client;
    }
    
    protected AccessTokenGrantHandler findGrantHandler(MultivaluedMap<String, String> params) {
        String grantType = params.getFirst(GRANT_TYPE);        
        if (grantType != null) {
            for (AccessTokenGrantHandler handler : grantHandlers) {
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    return handler;
                }
            }
            if (grantHandlers.size() == 0) {
                AuthorizationCodeGrantHandler handler = new AuthorizationCodeGrantHandler();
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    return handler;
                }
            }
        }
        
        return null;
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String error) {
        OAuthError oauthError = new OAuthError(error);
        return Response.status(400).entity(oauthError).build();
    }
}
