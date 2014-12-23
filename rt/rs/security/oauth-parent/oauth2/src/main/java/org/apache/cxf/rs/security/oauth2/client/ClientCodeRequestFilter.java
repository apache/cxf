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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

@PreMatching
@Priority(Priorities.AUTHENTICATION + 1)
public class ClientCodeRequestFilter implements ContainerRequestFilter {

    private String scopes;
    private String relRedirectUri;
    private String startUri;
    private String authorizationServiceUri;
    private OAuthClientUtils.Consumer consumer;
    private ClientCodeStateProvider clientStateProvider;
    private ClientCodeRequestProvider clientRequestProvider;
    private WebClient accessTokenService;
    
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        SecurityContext sc = rc.getSecurityContext();
        if (sc == null || sc.getUserPrincipal() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        UriInfo ui = rc.getUriInfo();
        if (ui.getPath().endsWith(startUri)) {
            if (clientRequestProvider != null) {
                ClientCodeRequest request = clientRequestProvider.getCodeRequest(sc, ui);
                if (request != null) {
                    setClientCodeRequest(request);
                    rc.setRequestUri(URI.create(relRedirectUri));
                    return;
                }
            }
            Response codeResponse = createCodeResponse(rc, sc, ui);
            rc.abortWith(codeResponse);
        } else if (ui.getPath().endsWith(relRedirectUri)) {
            processCodeResponse(rc, sc, ui);
        }
    }

    private Response createCodeResponse(ContainerRequestContext rc, SecurityContext sc, UriInfo ui) {
        URI uri = OAuthClientUtils.getAuthorizationURI(authorizationServiceUri, 
                                             consumer.getKey(), 
                                             getAbsoluteRedirectUri(ui).toString(), 
                                             createRequestState(rc, sc, ui), 
                                             scopes);
        return Response.seeOther(uri).build();
    }

    private URI getAbsoluteRedirectUri(UriInfo ui) {
        return ui.getBaseUriBuilder().path(relRedirectUri).build();
    }
    private void processCodeResponse(ContainerRequestContext rc, SecurityContext sc, UriInfo ui) {
        MultivaluedMap<String, String> params = ui.getQueryParameters();
        String codeParam = params.getFirst(OAuthConstants.AUTHORIZATION_CODE_VALUE);
        AccessTokenGrant grant = new AuthorizationCodeGrant(codeParam, getAbsoluteRedirectUri(ui));
        ClientAccessToken at = OAuthClientUtils.getAccessToken(accessTokenService, 
                                                               consumer, 
                                                               grant);
        MultivaluedMap<String, String> state = null;
        String stateParam = params.getFirst(OAuthConstants.STATE);
        if (clientStateProvider != null) {
            state = clientStateProvider.toState(sc, ui, stateParam);
        }
        ClientCodeRequest request = new ClientCodeRequest();
        request.setToken(at);
        request.setState(state);
        request.setUserName(sc.getUserPrincipal().getName());
        if (clientStateProvider != null) {
            clientRequestProvider.setCodeRequest(sc, ui, request);
        }
        setClientCodeRequest(request);
    }
    
    private void setClientCodeRequest(ClientCodeRequest request) {
        JAXRSUtils.getCurrentMessage().setContent(ClientCodeRequest.class, request);
    }

    private String createRequestState(ContainerRequestContext rc, SecurityContext sc, UriInfo ui) {
        if (clientStateProvider == null) {
            return null;
        }
        MultivaluedMap<String, String> state = new MetadataMap<String, String>();
        state.putAll(ui.getQueryParameters(false));
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), "UTF-8");
            FormUtils.populateMapFromString(state, JAXRSUtils.getCurrentMessage(), body, "UTF-8", false);
        }
        return clientStateProvider.toString(sc, ui, state);
    }

    public void setScopeList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(s);
        }
        setScopeString(sb.toString());
    }
    public void setScopeString(String scopesString) {
        this.scopes = scopesString;
    }

    public void setStartUri(String startUri) {
        this.startUri = startUri;
    }

    public void setAuthorizationServiceUri(String authorizationServiceUri) {
        this.authorizationServiceUri = authorizationServiceUri;
    }

    public void setConsumer(OAuthClientUtils.Consumer consumer) {
        this.consumer = consumer;
    }

    public void setRelativeRedirectUri(String redirectUri) {
        this.relRedirectUri = redirectUri;
    }

    public void setAccessTokenService(WebClient accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    public void setClientStateProvider(ClientCodeStateProvider clientStateProvider) {
        this.clientStateProvider = clientStateProvider;
    }
    public void setClientRequestProvider(ClientCodeRequestProvider clientRequestProvider) {
        this.clientRequestProvider = clientRequestProvider;
    }

}
