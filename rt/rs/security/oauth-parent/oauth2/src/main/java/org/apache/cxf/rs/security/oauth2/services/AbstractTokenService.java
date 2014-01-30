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

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class AbstractTokenService extends AbstractOAuthService {
    private boolean canSupportPublicClients;
    private boolean writeCustomErrors;
    
    /**
     * Make sure the client is authenticated
     */
    protected Client authenticateClientIfNeeded(MultivaluedMap<String, String> params) {
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
            reportInvalidClient();
        }
        return client;
    }
    
    // Get the Client and check the id and secret
    protected Client getAndValidateClient(String clientId, String clientSecret) {
        Client client = getClient(clientId);
        if (canSupportPublicClients 
            && !client.isConfidential() 
            && client.getClientSecret() == null 
            && clientSecret == null) {
            return client;
        }
        if (clientSecret == null || client.getClientSecret() == null 
            || !client.getClientId().equals(clientId) 
            || !client.getClientSecret().equals(clientSecret)) {
            throw new NotAuthorizedException(Response.status(401).build());
        }
        return client;
    }
    
    protected Response handleException(OAuthServiceException ex, String error) {
        OAuthError customError = ex.getError();
        if (writeCustomErrors && customError != null) {
            return createErrorResponseFromBean(customError);
        } else {
            return createErrorResponseFromBean(new OAuthError(error));
        }
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String error) {
        return createErrorResponseFromBean(new OAuthError(error));
    }
    
    protected Response createErrorResponseFromBean(OAuthError errorBean) {
        return Response.status(400).entity(errorBean).build();
    }
    
    /**
     * Get the {@link Client} reference
     * @param clientId the provided client id
     * @return Client the client reference 
     * @throws {@link javax.ws.rs.WebApplicationException} if no matching Client is found
     */
    protected Client getClient(String clientId) {
        if (clientId == null) {
            reportInvalidRequestError("Client ID is null");
            return null;
        }
        Client client = null;
        try {
            client = getValidClient(clientId);
        } catch (OAuthServiceException ex) {
            if (ex.getError() != null) {
                reportInvalidClient(ex.getError());
                return null;
            }
        }
        if (client == null) {
            reportInvalidClient();
        }
        return client;
    }
    
    protected void reportInvalidClient() {
        reportInvalidClient(new OAuthError(OAuthConstants.INVALID_CLIENT));
    }
    
    protected void reportInvalidClient(OAuthError error) {
        ResponseBuilder rb = Response.status(401);
        throw new NotAuthorizedException(rb.type(MediaType.APPLICATION_JSON_TYPE).entity(error).build());
    }
    
    public void setCanSupportPublicClients(boolean support) {
        this.canSupportPublicClients = support;
    }

    public boolean isCanSupportPublicClients() {
        return canSupportPublicClients;
    }
    
    public void setWriteCustomErrors(boolean writeCustomErrors) {
        this.writeCustomErrors = writeCustomErrors;
    }
}
