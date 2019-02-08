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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;

/**
 * Default OAuth service implementation
 */
@Path("/")
public class OAuthDefaultServices {

    private AuthorizationRequestService authorizeService =
        new AuthorizationRequestService();
    private AccessTokenService accessTokenService =
        new AccessTokenService();
    private RequestTokenService requestTokenService =
        new RequestTokenService();

    public OAuthDefaultServices() {
    }

    @Context
    public void setMessageContext(MessageContext mc) {
        this.authorizeService.setMessageContext(mc);
        this.accessTokenService.setMessageContext(mc);
        this.requestTokenService.setMessageContext(mc);
    }

    public void setAuthorizationService(AuthorizationRequestService service) {
        this.authorizeService = service;
    }

    public void setAccessTokenService(AccessTokenService service) {
        this.accessTokenService = service;
    }

    public void setRequestTokenservice(RequestTokenService service) {
        this.requestTokenService = service;
    }

    @POST
    @Path("/initiate")
    @Produces("application/x-www-form-urlencoded")
    public Response getRequestToken() {
        return requestTokenService.getRequestToken();
    }

    @GET
    @Path("/initiate")
    @Produces("application/x-www-form-urlencoded")
    public Response getRequestTokenWithGET() {
        return requestTokenService.getRequestToken();
    }

    @GET
    @Path("/authorize")
    @Produces({"application/xhtml+xml", "text/html", "application/xml", "application/json" })
    public Response authorize() {
        return authorizeService.authorize();
    }

    @GET
    @Path("/authorize/decision")
    public Response authorizeDecision() {
        return authorizeService.authorizeDecision();
    }

    @POST
    @Path("/authorize/decision")
    @Consumes("application/x-www-form-urlencoded")
    public Response authorizeDecisionForm() {
        return authorizeService.authorizeDecision();
    }

    @GET
    @Path("/token")
    @Produces("application/x-www-form-urlencoded")
    public Response getAccessTokenWithGET() {
        return accessTokenService.getAccessToken();
    }

    @POST
    @Path("/token")
    @Produces("application/x-www-form-urlencoded")
    public Response getAccessToken() {
        return accessTokenService.getAccessToken();
    }
}
