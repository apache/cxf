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
package org.apache.cxf.sts.rs.api;

import java.util.List;

import javax.ws.rs.Consumes;
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

import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    info = @Info(title = "SecurityTokenService REST interface", version = "1")
)
@SecurityScheme(description = "The JWT token",
    in = SecuritySchemeIn.HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "Bearer",
    bearerFormat = "JWT")
/**
 * Here possible to declare roles that should have access to the endpoints
 * @DeclareRoles({"user", "admin"})
 */
@Path("/{realm}")
public interface RealmSecurityTokenService {

    @GET
    @Path("/token/{tokenType}")
    @Produces(MediaType.APPLICATION_XML)
    @Operation(
        summary = "Return XML token according requested token type and key type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response getXMLToken(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @PathParam("tokenType")
        @Parameter(name = "tokenType", description = "Type of token", required = true, example = "saml")
        String tokenType,
        @QueryParam("keyType")
        @Parameter(name = "keyType", description = "Type of key", required = true, example = "PublicKey")
        String keyType,
        @QueryParam("claim")
        @Parameter(name = "claims", description = "List of claim for requested token", example = "roles, emailaddress")
        List<String> claims,
        @QueryParam("appliesTo")
        @Parameter(name = "appliesTo", description = "URL of requested token audience",
            example = "https://localhost:8443/test")
        String appliesTo,
        @QueryParam("wstrustResponse")
        @Parameter(name = "wstrustResponse", description = "Flag that shows that if response wstrust or now",
            example = "false")
        @DefaultValue("false")
        boolean wstrustResponse);

    @GET
    @Path("/token/{tokenType}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Return JWT token in JSON according requested token type and key type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response getJSONToken(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @PathParam("tokenType")
        @Parameter(name = "tokenType", description = "Type of token", required = true, example = "jwt")
        String tokenType,
        @QueryParam("keyType")
        @Parameter(name = "keyType", description = "Type of key", required = true, example = "Bearer")
        String keyType,
        @QueryParam("claim")
        @Parameter(name = "claims", description = "List of claim for requested token", example = "roles, emailaddress")
        List<String> claims,
        @QueryParam("appliesTo")
        @Parameter(name = "appliesTo", description = "URL of requested token audience",
            example = "https://localhost:8443/test")
        String appliesTo);

    @GET
    @Path("/token/{tokenType}")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Return JWT token in plain text format according requested token type and key type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response getPlainToken(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @PathParam("tokenType")
        @Parameter(name = "tokenType", description = "Type of token", required = true, example = "jwt")
        String tokenType,
        @QueryParam("keyType")
        @Parameter(name = "keyType", description = "Type of key", required = true, example = "Bearer")
        String keyType,
        @QueryParam("claim")
        @Parameter(name = "claims", description = "List of claim for requested token", example = "roles, emailaddress")
        List<String> claims,
        @QueryParam("appliesTo")
        @Parameter(name = "appliesTo", description = "URL of requested token audience",
            example = "https://localhost:8443/test")
        String appliesTo);

    @POST
    @Path("/token")
    @Consumes({"application/xml", "application/json"})
    @Produces({"application/xml", "application/json"})
    @Operation(
        summary = "Return token according requested token type and key type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response getToken(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @RequestBody(description = "Object GetTokenRequest describes requested token parameters", required = true)
        GetTokenRequest request);

    @POST
    @Path("/token/validate")
    @Produces({"application/xml", "application/json"})
    @Operation(
        summary = "Validate incoming token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return result of validation"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response validate(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @RequestBody(description = "Object TokenRequest represents token for validation", required = true)
        TokenRequest request);

    @POST
    @Path("/token/renew")
    @Produces({"application/xml", "application/json"})
    @Operation(
        summary = "Renew incoming token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return new token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response renew(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @RequestBody(description = "Object TokenRequest represents token for renewal", required = true)
        TokenRequest request);

    @DELETE
    @Path("/token")
    @Produces({"application/xml", "application/json"})
    @Operation(
        summary = "Cancel incoming token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token is cancelled"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response remove(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @RequestBody(description = "Object TokenRequest represents token for canceling", required = true)
        TokenRequest request);

    @POST
    @Path("/token/exchange")
    @Produces({"application/xml", "application/json"})
    @Operation(
        summary = "Return exchange token according requested token type and key type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested token"),
            @ApiResponse(responseCode = "401", description = "You are not authorized to access the endpoint")
        }
    )
    Response getKeyExchangeToken(
        @PathParam("realm")
        @Parameter(name = "realm", description = "Name of realm", required = true)
        String realm,
        @RequestBody(description = "Object GetTokenRequest describes requested access token parameters",
            required = true)
        GetTokenRequest request);

    @GET
    @PathParam("realm")
    @Path("/jwk/keys")
    @Produces({"application/json"})
    @Operation(
        summary = "Return JWK public key for requested realm",
        responses = {
            @ApiResponse(responseCode = "200", description = "Return requested public JWK"),
            @ApiResponse(responseCode = "500",
                description = "In case wrong configuration of RS security for requested realm")
        }
    )
    JsonWebKeys getPublicVerificationKeys(
            @PathParam("realm")
            @Parameter(name = "realm", description = "Name of realm", required = true)
            String realm);

}
