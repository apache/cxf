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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

@Path("rp")
public class OidcRpAuthenticationService {
    private OidcRpStateManager stateManager;
    private String defaultLocation;
    private String tokenFormParameter = "idtoken"; 
    @Context
    private MessageContext mc; 
    private UserInfoClient userInfoClient;
    private Consumer consumer;
    
    public void setUserInfoClient(UserInfoClient userInfoClient) {
        this.userInfoClient = userInfoClient;
    }
    
    @POST
    @Path("complete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response completeScriptAuthentication(MultivaluedMap<String, String> map) {
        String idTokenParamValue = map.getFirst(tokenFormParameter);
        OidcClientTokenContextImpl ctx = new OidcClientTokenContextImpl();
        ctx.setIdToken(userInfoClient.getIdToken(idTokenParamValue, consumer.getKey()));
        return completeAuthentication(ctx);   
    }
    
    @GET
    @Path("complete")
    public Response completeAuthentication(@Context OidcClientTokenContext oidcContext) {
        String key = OAuthUtils.generateRandomTokenKey();
        stateManager.setTokenContext(key, oidcContext);
        URI redirectUri = null;
        String location = oidcContext.getState().getFirst("state");
        if (location == null) {
            String basePath = (String)mc.get("http.base.path");
            redirectUri = UriBuilder.fromUri(basePath).path(defaultLocation).build();
        } else {
            redirectUri = URI.create(location);
        }
        return Response.seeOther(redirectUri).header("Set-Cookie", 
                                                     "org.apache.cxf.websso.context=" + key + ";Path=/").build();
    }

    public void setDefaultLocation(String defaultLocation) {
        this.defaultLocation = defaultLocation;
    }

    public void setStateManager(OidcRpStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void setTokenFormParameter(String tokenFormParameter) {
        this.tokenFormParameter = tokenFormParameter;
    }
}
