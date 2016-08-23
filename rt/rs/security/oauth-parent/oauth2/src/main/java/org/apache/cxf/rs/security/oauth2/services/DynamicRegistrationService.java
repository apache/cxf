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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;

@Path("register")
public class DynamicRegistrationService extends AbstractOAuthService {
    
    private OAuthDataProvider dataProvider;
    private String initialAccessToken;
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ClientRegistrationResponse register(ClientRegistrationRequest request) {
        
        return new ClientRegistrationResponse();
    }
    
    @GET
    @Produces("application/json")
    public ClientRegistrationResponse readClientRegistrationWithQuery(@QueryParam("client_id") String clientId) {
        
        return doReadClientRegistration(clientId);
    }
    
    @GET
    @Path("{clientId}")
    @Produces("application/json")
    public ClientRegistrationResponse readClientRegistrationWithPath(@PathParam("clientId") String clientId) {
        
        return doReadClientRegistration(clientId);
    }
    
    @PUT
    @Path("{clientId}")
    @Consumes("application/json")
    public Response updateClientRegistration(@PathParam("clientId") String clientId) {
        return Response.ok().build();
    }
    
    @DELETE
    @Path("{clientId}")
    public Response deleteClientRegistration(@PathParam("clientId") String clientId) {
        return Response.ok().build();
    }
    
    protected ClientRegistrationResponse doReadClientRegistration(String clientId) {
        return new ClientRegistrationResponse();
    }

    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.initialAccessToken = registrationAccessToken;
    }
    
    
}
