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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * OAuth2 Access Token Service implementation
 */
@Path("/token")
public class AccessTokenService extends AbstractTokenService {
    private List<AccessTokenGrantHandler> grantHandlers = new LinkedList<AccessTokenGrantHandler>();
    private List<AccessTokenResponseFilter> responseHandlers = new LinkedList<AccessTokenResponseFilter>();
    private List<String> audiences = new LinkedList<String>();
    
    /**
     * Sets the list of optional grant handlers
     * @param handlers the grant handlers
     */
    public void setGrantHandlers(List<AccessTokenGrantHandler> handlers) {
        grantHandlers = handlers;
    }
    
    /**
     * Sets a grant handler
     * @param handler the grant handler
     */
    public void setGrantHandler(AccessTokenGrantHandler handler) {
        grantHandlers.add(handler);
    }

    public void setResponseFilters(List<AccessTokenResponseFilter> handlers) {
        this.responseHandlers = handlers;
    }
    
    public void setResponseFilter(AccessTokenResponseFilter responseHandler) {
        responseHandlers.add(responseHandler);
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
        
        if (!OAuthUtils.isGrantSupportedForClient(client, 
                                                  isCanSupportPublicClients(),
                                                  params.getFirst(OAuthConstants.GRANT_TYPE))) {
            return createErrorResponse(params, OAuthConstants.UNAUTHORIZED_CLIENT);    
        }
        
        try {
            checkAudience(params);
        } catch (OAuthServiceException ex) {
            return super.createErrorResponseFromBean(ex.getError());
        }
        
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
            return handleException(ex, OAuthConstants.INVALID_GRANT);
        }
        if (serverToken == null) {
            return createErrorResponse(params, OAuthConstants.INVALID_GRANT);
        }
        
        // Extract the information to be of use for the client
        ClientAccessToken clientToken = OAuthUtils.toClientAccessToken(serverToken, isWriteOptionalParameters());
        processClientAccessToken(clientToken, serverToken);    
        // Return it to the client
        return Response.ok(clientToken)
                       .header(HttpHeaders.CACHE_CONTROL, "no-store")
                       .header("Pragma", "no-cache")
                        .build();
    }
    protected void processClientAccessToken(ClientAccessToken clientToken, ServerAccessToken serverToken) {
        for (AccessTokenResponseFilter filter : responseHandlers) {
            filter.process(clientToken, serverToken); 
        }
    }
    protected void checkAudience(MultivaluedMap<String, String> params) { 
        if (audiences.isEmpty()) {
            return;
        }
        
        String audienceParam = params.getFirst(OAuthConstants.CLIENT_AUDIENCE);
        if (audienceParam == null) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_REQUEST));
        }
        // must be URL
        try {
            new URL(audienceParam);
        } catch (MalformedURLException ex) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_REQUEST));
        }
        
        if (!audiences.contains(audienceParam)) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.ACCESS_DENIED));
        }
        
    }
    
    /**
     * Find the matching grant handler
     */
    protected AccessTokenGrantHandler findGrantHandler(MultivaluedMap<String, String> params) {    
        String grantType = params.getFirst(OAuthConstants.GRANT_TYPE);
                
        if (grantType != null) {
            for (AccessTokenGrantHandler handler : grantHandlers) {
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    return handler;
                }
            }
            // Lets try the well-known grant handlers
            if (super.getDataProvider() instanceof AuthorizationCodeDataProvider) {
                AuthorizationCodeGrantHandler handler = new AuthorizationCodeGrantHandler();
                if (handler.getSupportedGrantTypes().contains(grantType)) {
                    handler.setDataProvider((AuthorizationCodeDataProvider)super.getDataProvider());
                    return handler;
                }
            }
        }
        
        return null;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }
}