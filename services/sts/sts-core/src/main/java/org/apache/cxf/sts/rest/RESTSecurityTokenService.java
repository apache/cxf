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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;

@Path("/token")
public interface RESTSecurityTokenService {

    enum Action {
        issue("issue"),
        validate("validate"),
        renew("renew"),
        cancel("cancel");
        private String value;

        Action(String value) {
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
    @Produces(MediaType.APPLICATION_XML)
    Response getXMLToken(@PathParam("tokenType") String tokenType, @QueryParam("keyType") String keyType,
        @QueryParam("claim") List<String> requestedClaims,
        @QueryParam("appliesTo") String appliesTo,
        @QueryParam("wstrustResponse") @DefaultValue("false") boolean wstrustResponse);

    @GET
    @Path("{tokenType}")
    @Produces("application/json;qs=0.8")
    Response getJSONToken(@PathParam("tokenType") @DefaultValue("jwt") String tokenType,
        @QueryParam("keyType") String keyType,
        @QueryParam("claim") List<String> requestedClaims,
        @QueryParam("appliesTo") String appliesTo);

    @GET
    @Path("{tokenType}")
    @Produces("text/plain;qs=0.9")
    Response getPlainToken(@PathParam("tokenType") String tokenType, @QueryParam("keyType") String keyType,
        @QueryParam("claim") List<String> requestedClaims,
        @QueryParam("appliesTo") String appliesTo);

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
