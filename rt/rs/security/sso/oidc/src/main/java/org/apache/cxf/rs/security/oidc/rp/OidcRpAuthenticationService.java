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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContextManager;

@Path("rp")
public class OidcRpAuthenticationService {
    private ClientTokenContextManager stateManager;
    private String defaultLocation;
    @Context
    private MessageContext mc;

    @POST
    @Path("signin")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response completeScriptAuthentication(@Context IdTokenContext idTokenContext) {
        OidcClientTokenContextImpl ctx = new OidcClientTokenContextImpl();
        ctx.setIdToken(idTokenContext.getIdToken());
        return completeAuthentication(ctx);
    }

    @GET
    @Path("complete")
    public Response completeAuthentication(@Context OidcClientTokenContext oidcContext) {
        stateManager.setClientTokenContext(mc, oidcContext);

        URI redirectUri = null;
        MultivaluedMap<String, String> state = oidcContext.getState();
        String location = state != null ? state.getFirst("state") : null;
        if (location == null && defaultLocation != null) {
            String basePath = (String)mc.get("http.base.path");
            redirectUri = UriBuilder.fromUri(basePath).path(defaultLocation).build();
        } else if (location != null) {
            redirectUri = URI.create(UrlUtils.urlDecode(location));
        }
        if (redirectUri != null) {
            return Response.seeOther(redirectUri).build();
        }
        return Response.ok(oidcContext).build();
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public void setClientTokenContextManager(ClientTokenContextManager manager) {
        this.stateManager = manager;
    }

}
