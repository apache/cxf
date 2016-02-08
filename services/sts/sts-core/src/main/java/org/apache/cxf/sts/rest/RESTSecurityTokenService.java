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

package org.apache.cxf.sts.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;

@Path("/token")
public interface RESTSecurityTokenService {

    enum Action {
        issue("issue"),
        validate("validate"),
        renew("renew"),
        cancel("cancel");
        private String value;

        private Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * @return Issues required token type with default token settings.
     */
    
    @GET
    @Path("{tokenType}")
    @Produces({
        MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON
    })
    Response getToken(@PathParam("tokenType") String tokenType, @QueryParam("keyType") String keyType,
        @QueryParam("claim") List<String> requestedClaims,
        @QueryParam("appliesTo") String appliesTo);
    
    @GET
    @Path("ws-trust/{tokenType}")
    @Produces({
        MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON
    })
    Response getTokenViaWSTrust(@PathParam("tokenType") String tokenType, @QueryParam("keyType") String keyType,
        @QueryParam("claim") List<String> requestedClaims, @QueryParam("appliesTo") String appliesTo);
    
    @POST
    @Produces({
        MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON
    })
    Response getToken(@QueryParam("action") @DefaultValue("issue") Action action, RequestSecurityTokenType request);

    /**
     * Same as {@link #getToken(Action, RequestSecurityTokenType)} with 'cancel' action.
     * 
     * @param request
     * @return
     */
    @DELETE
    @Path("/")
    @Produces({
        MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON
    })
    Response removeToken(RequestSecurityTokenType request);

    @POST
    @Path("KeyExchangeToken")
    @Produces({
        MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON
    })
    Response getKeyExchangeToken(RequestSecurityTokenType request);

}
