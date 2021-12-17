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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtException;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContextManager;
import org.apache.cxf.rs.security.oidc.common.IdToken;

@PreMatching
@Priority(Priorities.AUTHENTICATION + 2)
public class OidcRpAuthenticationFilter implements ContainerRequestFilter {
    @Context
    private MessageContext mc;
    private ClientTokenContextManager stateManager;
    private String redirectUri;
    private String roleClaim;
    private boolean addRequestUriAsRedirectQuery;

    public void filter(ContainerRequestContext rc) {
        if (checkSecurityContext(rc)) {
            return;
        } else if (redirectUri != null) {
            final UriBuilder redirectBuilder;
            if (redirectUri.startsWith("/")) {
                String basePath = (String)mc.get("http.base.path");
                redirectBuilder = UriBuilder.fromUri(basePath).path(redirectUri);
            } else if (redirectUri.startsWith("http")) {
                redirectBuilder = UriBuilder.fromUri(URI.create(redirectUri));
            } else {
                redirectBuilder = rc.getUriInfo().getBaseUriBuilder().path(redirectUri);
            }
            if (addRequestUriAsRedirectQuery) {
                redirectBuilder.queryParam("state", rc.getUriInfo().getRequestUri().toString());
            }
            URI redirectAddress = redirectBuilder.build();
            rc.abortWith(Response.seeOther(redirectAddress)
                           .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                           .header("Pragma", "no-cache")
                           .build());
        } else {
            rc.abortWith(Response.status(401).build());
        }
    }
    protected boolean checkSecurityContext(ContainerRequestContext rc) {
        OidcClientTokenContext tokenContext = (OidcClientTokenContext)stateManager.getClientTokenContext(mc);
        if (tokenContext == null) {
            return false;
        }
        IdToken idToken = tokenContext.getIdToken();
        try {
            // If ID token has expired then the context is no longer valid
            JwtUtils.validateJwtExpiry(idToken, 0, idToken.getExpiryTime() != null);
        } catch (JwtException ex) {
            stateManager.removeClientTokenContext(new MessageContextImpl(JAXRSUtils.getCurrentMessage()));
            return false;
        }
        OidcClientTokenContextImpl newTokenContext = new OidcClientTokenContextImpl();
        newTokenContext.setToken(tokenContext.getToken());
        newTokenContext.setIdToken(idToken);
        newTokenContext.setUserInfo(tokenContext.getUserInfo());
        newTokenContext.setState(toRequestState(rc));
        JAXRSUtils.getCurrentMessage().setContent(ClientTokenContext.class, newTokenContext);

        OidcSecurityContext oidcSecCtx = new OidcSecurityContext(newTokenContext);
        oidcSecCtx.setRoleClaim(roleClaim);
        rc.setSecurityContext(oidcSecCtx);
        return true;
    }
    private MultivaluedMap<String, String> toRequestState(ContainerRequestContext rc) {
        MultivaluedMap<String, String> requestState = new MetadataMap<>();
        requestState.putAll(rc.getUriInfo().getQueryParameters(true));
        if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.isCompatible(rc.getMediaType())) {
            String body = FormUtils.readBody(rc.getEntityStream(), StandardCharsets.UTF_8.name());
            FormUtils.populateMapFromString(requestState, JAXRSUtils.getCurrentMessage(), body,
                                            StandardCharsets.UTF_8.name(), true);
            rc.setEntityStream(new ByteArrayInputStream(StringUtils.toBytesUTF8(body)));

        }
        return requestState;
    }
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    public void setClientTokenContextManager(ClientTokenContextManager manager) {
        this.stateManager = manager;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    public void setAddRequestUriAsRedirectQuery(boolean addRequestUriAsRedirectQuery) {
        this.addRequestUriAsRedirectQuery = addRequestUriAsRedirectQuery;
    }
}
