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
package org.apache.cxf.rs.security.oidc.rp;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContextManager;

@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class OidcRpAuthenticationFilter implements ContainerRequestFilter {
    @Context
    private MessageContext mc;
    private ClientTokenContextManager stateManager;
    private String redirectUri;
    
    public void filter(ContainerRequestContext rc) {
        if (checkSecurityContext(rc)) {
            return;
        } else {
            URI redirectAddress = null;
            if (redirectUri.startsWith("/")) {
                String basePath = (String)mc.get("http.base.path");
                redirectAddress = UriBuilder.fromUri(basePath).path(redirectUri).build();
            } else if (redirectUri.startsWith("http")) {
                redirectAddress = URI.create(redirectUri);
            } else {
                UriBuilder ub = rc.getUriInfo().getBaseUriBuilder().path(redirectUri);
                redirectAddress = ub.build();
            }
            rc.abortWith(Response.seeOther(redirectAddress)
                           .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                           .header("Pragma", "no-cache") 
                           .build());
        }
    }
    protected boolean checkSecurityContext(ContainerRequestContext rc) {
        OidcClientTokenContext tokenContext = (OidcClientTokenContext)stateManager.getClientTokenContext(mc);
        if (tokenContext == null) {
            return false;
        }
        OidcClientTokenContextImpl newTokenContext = new OidcClientTokenContextImpl();
        newTokenContext.setToken(tokenContext.getToken());
        newTokenContext.setIdToken(tokenContext.getIdToken());
        newTokenContext.setUserInfo(tokenContext.getUserInfo());
        newTokenContext.setState(toRequestState(rc));
        JAXRSUtils.getCurrentMessage().setContent(ClientTokenContext.class, newTokenContext);
        rc.setSecurityContext(new OidcSecurityContext(newTokenContext));
        return true;
    }
    private MultivaluedMap<String, String> toRequestState(ContainerRequestContext rc) {
        MultivaluedMap<String, String> requestState = new MetadataMap<String, String>();
        requestState.putAll(rc.getUriInfo().getQueryParameters(true));
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), StandardCharsets.UTF_8.name());
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body, 
                                            StandardCharsets.UTF_8.name(), true);
        }
        return requestState;
    }
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    public void setClientTokenContextManager(ClientTokenContextManager manager) {
        this.stateManager = manager;
    }
}
