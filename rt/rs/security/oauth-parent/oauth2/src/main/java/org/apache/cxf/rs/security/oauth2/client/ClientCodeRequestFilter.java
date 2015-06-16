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
package org.apache.cxf.rs.security.oauth2.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

@PreMatching
@Priority(Priorities.AUTHENTICATION + 1)
public class ClientCodeRequestFilter implements ContainerRequestFilter {
    @Context
    private MessageContext mc;
    
    private String scopes;
    private String completeUri;
    private String startUri;
    private String authorizationServiceUri;
    private OAuthClientUtils.Consumer consumer;
    private ClientCodeStateManager clientStateManager;
    private ClientTokenContextManager clientTokenContextManager;
    private WebClient accessTokenServiceClient;
    private boolean decodeRequestParameters;
    private long expiryThreshold;
    private String redirectUri;
    
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        checkSecurityContextStart(rc.getSecurityContext());
        UriInfo ui = rc.getUriInfo();
        String absoluteRequestUri = ui.getAbsolutePath().toString();
        
        boolean sameUriRedirect = false;
        if (completeUri == null) {
            String referer = rc.getHeaderString("Referer");
            if (referer != null && referer.startsWith(authorizationServiceUri)) {
                completeUri = absoluteRequestUri;
                sameUriRedirect = true;
            }
        }
        
        if (!sameUriRedirect && absoluteRequestUri.endsWith(startUri)) {
            ClientTokenContext request = getClientTokenContext(rc);
            if (request != null) {
                setClientCodeRequest(request);
                if (completeUri != null) {
                    rc.setRequestUri(URI.create(completeUri));
                }
                return;
            }
            Response codeResponse = createCodeResponse(rc,  ui);
            rc.abortWith(codeResponse);
        } else if (absoluteRequestUri.endsWith(completeUri)) {
            processCodeResponse(rc, ui);
            checkSecurityContextEnd(rc.getSecurityContext());
        }
    }

    protected void checkSecurityContextStart(SecurityContext sc) {
        checkSecurityContextEnd(sc);
    }
    private void checkSecurityContextEnd(SecurityContext sc) {
        if (sc == null || sc.getUserPrincipal() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    private Response createCodeResponse(ContainerRequestContext rc, UriInfo ui) {
        MultivaluedMap<String, String> redirectState = createRedirectState(rc, ui);
        String theState = redirectState != null ? redirectState.getFirst(OAuthConstants.STATE) : null;
        String redirectScope = redirectState != null ? redirectState.getFirst(OAuthConstants.SCOPE) : null;
        String theScope = redirectScope != null ? redirectScope : scopes;
        URI uri = OAuthClientUtils.getAuthorizationURI(authorizationServiceUri, 
                                             consumer.getKey(), 
                                             getAbsoluteRedirectUri(ui).toString(), 
                                             theState, 
                                             theScope);
        return Response.seeOther(uri).build();
    }

    private URI getAbsoluteRedirectUri(UriInfo ui) {
        if (redirectUri != null) {
            return URI.create(redirectUri);
        } else if (completeUri != null) {
            return completeUri.startsWith("http") ? URI.create(completeUri) 
                : ui.getBaseUriBuilder().path(completeUri).build();
        } else {
            return ui.getAbsolutePath();
        }
    }
    protected void processCodeResponse(ContainerRequestContext rc, UriInfo ui) {
        MultivaluedMap<String, String> params = toRequestState(rc, ui);
        String codeParam = params.getFirst(OAuthConstants.AUTHORIZATION_CODE_VALUE);
        ClientAccessToken at = null;
        if (codeParam != null) {
            AccessTokenGrant grant = new AuthorizationCodeGrant(codeParam, getAbsoluteRedirectUri(ui));
            at = OAuthClientUtils.getAccessToken(accessTokenServiceClient, consumer, grant);
        }
        ClientTokenContext tokenContext = initializeClientTokenContext(rc, at, params);
        if (at != null && clientTokenContextManager != null) {
            clientTokenContextManager.setClientTokenContext(mc, tokenContext);
        }
        setClientCodeRequest(tokenContext);
    }
    
    protected ClientTokenContext initializeClientTokenContext(ContainerRequestContext rc, 
                                                              ClientAccessToken at, 
                                                            MultivaluedMap<String, String> params) {
        ClientTokenContext tokenContext = createTokenContext(rc, at);
        ((ClientTokenContextImpl)tokenContext).setToken(at);
        if (clientStateManager != null) {
            MultivaluedMap<String, String> state = clientStateManager.fromRedirectState(mc, params);
            ((ClientTokenContextImpl)tokenContext).setState(state);
        }
        
        return tokenContext;
        
    }

    protected ClientTokenContext createTokenContext(ContainerRequestContext rc, ClientAccessToken at) {
        return new ClientTokenContextImpl();
    }
    
    private void setClientCodeRequest(ClientTokenContext request) {
        JAXRSUtils.getCurrentMessage().setContent(ClientTokenContext.class, request);
    }

    private MultivaluedMap<String, String> createRedirectState(ContainerRequestContext rc, UriInfo ui) {
        if (clientStateManager == null) {
            return null;
        }
        return clientStateManager.toRedirectState(mc, toRequestState(rc, ui));
    }

    private MultivaluedMap<String, String> toRequestState(ContainerRequestContext rc, UriInfo ui) {
        MultivaluedMap<String, String> requestState = new MetadataMap<String, String>();
        requestState.putAll(ui.getQueryParameters(decodeRequestParameters));
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), "UTF-8");
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body, 
                                            "UTF-8", decodeRequestParameters);
        }
        return requestState;
    }

    public void setScopeList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(s);
        }
        setScopes(sb.toString());
    }
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public void setStartUri(String relStartUri) {
        this.startUri = relStartUri;
    }

    public void setAuthorizationServiceUri(String authorizationServiceUri) {
        this.authorizationServiceUri = authorizationServiceUri;
    }

    public void setCompleteUri(String completeUri) {
        this.completeUri = completeUri;
    }

    public void setAccessTokenServiceClient(WebClient accessTokenServiceClient) {
        this.accessTokenServiceClient = accessTokenServiceClient;
    }

    public void setClientCodeStateManager(ClientCodeStateManager manager) {
        this.clientStateManager = manager;
    }
    public void setClientTokenContextManager(ClientTokenContextManager clientTokenContextManager) {
        this.clientTokenContextManager = clientTokenContextManager;
    }

    public OAuthClientUtils.Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(OAuthClientUtils.Consumer consumer) {
        this.consumer = consumer;
    }
 
    public void setDecodeRequestParameters(boolean decodeRequestParameters) {
        this.decodeRequestParameters = decodeRequestParameters;
    }

    protected ClientTokenContext getClientTokenContext(ContainerRequestContext rc) {
        ClientTokenContext ctx = null;
        if (clientTokenContextManager != null) {
            ctx = clientTokenContextManager.getClientTokenContext(mc);
            if (ctx != null) {
                ClientAccessToken newAt = refreshAccessTokenIfExpired(ctx.getToken());
                if (newAt != null) {
                    clientTokenContextManager.removeClientTokenContext(mc, ctx);
                    ClientTokenContext newCtx = initializeClientTokenContext(rc, newAt, ctx.getState());            
                    clientTokenContextManager.setClientTokenContext(mc, newCtx);
                    ctx = newCtx;
                }
            }
        }
        return ctx;
    }
    
    private ClientAccessToken refreshAccessTokenIfExpired(ClientAccessToken at) {
        if (at.getRefreshToken() != null
            && ((expiryThreshold > 0 && OAuthUtils.isExpired(at.getIssuedAt(), at.getExpiresIn() - expiryThreshold))
            || OAuthUtils.isExpired(at.getIssuedAt(), at.getExpiresIn()))) {
            return OAuthClientUtils.refreshAccessToken(accessTokenServiceClient, consumer, at);
        }
        return null;
    }

    public void setExpiryThreshold(long expiryThreshold) {
        this.expiryThreshold = expiryThreshold;
    }

    public void setRedirectUri(String redirectUri) {
        // Can be set to something like "postmessage" in some flows
        this.redirectUri = redirectUri;
    }
}
