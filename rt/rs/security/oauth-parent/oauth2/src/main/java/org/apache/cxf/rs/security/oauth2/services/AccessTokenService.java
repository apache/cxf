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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
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
    private List<AccessTokenGrantHandler> grantHandlers = new LinkedList<>();
    private List<AccessTokenResponseFilter> responseHandlers = new LinkedList<>();

    /**
     * Sets the list of optional grant handlers
     * @param handlers the grant handlers
     */
    public void setGrantHandlers(List<AccessTokenGrantHandler> handlers) {
        grantHandlers = handlers;
    }

    @Override
    protected void injectContextIntoOAuthProviders() {
        super.injectContextIntoOAuthProviders();
        for (AccessTokenGrantHandler grantHandler : grantHandlers) {
            OAuthUtils.injectContextIntoOAuthProvider(getMessageContext(), grantHandler);
        }
    }

    /**
     * Sets a grant handler
     * @param handler the grant handler
     */
    public void setGrantHandler(AccessTokenGrantHandler handler) {
        setGrantHandlers(Collections.singletonList(handler));
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
            LOG.log(Level.FINE, "The grant type {} is not supported for the client",
                     params.getFirst(OAuthConstants.GRANT_TYPE));
            return createErrorResponse(params, OAuthConstants.UNAUTHORIZED_CLIENT);
        }

        try {
            checkAudience(client, params);
        } catch (OAuthServiceException ex) {
            return super.createErrorResponseFromBean(ex.getError());
        }

        // Find the grant handler
        AccessTokenGrantHandler handler = findGrantHandler(params);
        if (handler == null) {
            LOG.fine("No Grant Handler found");
            return createErrorResponse(params, OAuthConstants.UNSUPPORTED_GRANT_TYPE);
        }

        // Create the access token
        final ServerAccessToken serverToken;
        try {
            serverToken = handler.createAccessToken(client, params);
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LOG.log(Level.FINE, "Error creating the access token", ex);
            // This is done to bypass a Check-Style
            // restriction on a number of return statements
            OAuthServiceException oauthEx = ex instanceof OAuthServiceException
                ? (OAuthServiceException)ex : new OAuthServiceException(ex);
            return handleException(oauthEx, OAuthConstants.INVALID_GRANT);
        }
        if (serverToken == null) {
            LOG.fine("No access token was created");
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
    protected void checkAudience(Client c, MultivaluedMap<String, String> params) {
        String audienceParam = params.getFirst(OAuthConstants.CLIENT_AUDIENCE);
        if (!OAuthUtils.validateAudience(audienceParam, c.getRegisteredAudiences())) {
            LOG.log(Level.FINE, "Error validating the audience parameter. Supplied audience {0} "
                    + "does not match with the registered audiences {1}",
                    new Object[] {audienceParam, c.getRegisteredAudiences() });
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
                    handler.setDataProvider(super.getDataProvider());
                    return handler;
                }
            }
        }

        return null;
    }
}
