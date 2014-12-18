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
package demo.jaxrs.server;

import java.net.URI;
import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.common.UserInfo;
import org.apache.cxf.rs.security.oidc.rp.IdTokenValidator;

@Path("/service")
public class BigQueryService {

    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders httpHeaders;

    private String authorizationServiceUri;
    private WebClient accessTokenServiceClient;
    private WebClient userInfoServiceClient;
    private IdTokenValidator tokenValidator;
    private Consumer consumer;

    @GET
    @Path("/oidc/rp/start")
    public Response startUserAuthentication() {
        URI indexUri = uriInfo.getBaseUriBuilder().path("index.html").build();
        return Response.seeOther(indexUri).build();
    }

    @POST
    @Path("/oidc/rp/complete")
    @Consumes("application/octet-stream")
    @Produces("application/xml,application/json,text/html")
    public Response completeUserAuthentication(String code) {
        return doCompleteBigQuery(code, null, true);
    }

    @GET
    @Path("/bigquery")
    public Response startBiqQuery() {

        StringBuilder scopes = new StringBuilder();
        scopes.append("openid email profile");
        // Add application specific scopes if any

        URI loc = OAuthClientUtils.getAuthorizationURI(authorizationServiceUri,
                consumer.getKey(), getRedirectUri(), uriInfo.getAbsolutePath()
                        .toString(), scopes.toString());

        Response r = Response.seeOther(loc).build();
        return r;
    }

    @GET
    @Path("/bigquery/complete")
    @Produces("application/xml,application/json,text/html")
    public Response completeBigQuery(@QueryParam("code") String code,
            @QueryParam("state") String state) {
        return doCompleteBigQuery(code, state, false);
    }

    private Response doCompleteBigQuery(String code, String state,
            boolean postMessage) {

        // Get the access token
        ClientAccessToken at = getClientAccessToken(consumer, code, postMessage);

        // Expect and validate id_token
        IdToken idToken = tokenValidator.getIdTokenFromJwt(at,
                consumer.getKey());

        // Get User Profile if needed
        UserInfo userInfo = getUserInfo(at, idToken);

        // Complete the request, use 'at' to access some other user's API,
        // return the response to the user
        ResponseBuilder rb = Response.ok().type("application/json");
        Response r = rb.entity(
                "{\"email\":\"" + userInfo.getProperty("email") + "\"}")
                .build();
        return r;
    }

    public void setAccessTokenServiceClient(WebClient accessTokenServiceClient) {
        this.accessTokenServiceClient = accessTokenServiceClient;
    }

    private String getRedirectUri() {
        return uriInfo.getBaseUriBuilder().path("/service/bigquery/complete")
                .build().toString();
    }

    private ClientAccessToken getClientAccessToken(Consumer consumer,
            String code, boolean postMessage) {
        AccessTokenGrant grant = new AuthorizationCodeGrant(code);
        String redirectUri = postMessage ? "postmessage" : getRedirectUri();
        return OAuthClientUtils.getAccessToken(accessTokenServiceClient,
                consumer, grant, Collections.singletonMap(
                        OAuthConstants.REDIRECT_URI, redirectUri), false);
    }

    private UserInfo getUserInfo(ClientAccessToken at, IdToken idToken) {
        if (userInfoServiceClient != null) {
            OAuthClientUtils.setAuthorizationHeader(userInfoServiceClient, at);
            return userInfoServiceClient.get(UserInfo.class);
        }
        return null;
    }

    public void setUserInfoServiceClient(WebClient userInfoServiceClient) {
        this.userInfoServiceClient = userInfoServiceClient;
    }

    public void setIdTokenValidator(IdTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    public void setAuthorizationServiceUri(String authorizationServiceUri) {
        this.authorizationServiceUri = authorizationServiceUri;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

}
