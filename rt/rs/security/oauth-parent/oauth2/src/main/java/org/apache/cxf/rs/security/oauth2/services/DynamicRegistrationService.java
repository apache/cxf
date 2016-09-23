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

import java.util.Collections;
import java.util.List;

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
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

@Path("register")
public class DynamicRegistrationService extends AbstractOAuthService {
    private static final String DEFAULT_APPLICATION_TYPE = "web";
    private static final Integer DEFAULT_CLIENT_ID_SIZE = 10;
    private ClientRegistrationProvider clientProvider;
    private String initialAccessToken;
    private int clientIdSizeInBytes = DEFAULT_CLIENT_ID_SIZE;
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ClientRegistrationResponse register(ClientRegistration request) {
        checkInitialAccessToken();
        Client client = createNewClient(request);
        createRegAccessToken(client);
        clientProvider.setClient(client);
        
        return fromClientToRegistrationResponse(client);
    }
    
    protected void checkInitialAccessToken() {
        if (initialAccessToken != null) {
            checkCurrentAccessToken(initialAccessToken);
        }
        
    }

    protected String createRegAccessToken(Client client) {
        //TODO: Passing AccessTokenRegistration to OAuthDataProvider may be needed
        String regAccessToken = OAuthUtils.generateRandomTokenKey();
        client.getProperties().put(ClientRegistrationResponse.REG_ACCESS_TOKEN, 
                                   regAccessToken);
        return regAccessToken;
    }
    protected void checkCurrentAccessToken(String accessToken) {
        String[] authParts = AuthorizationUtils.getAuthorizationParts(getMessageContext(), 
                             Collections.singleton(OAuthConstants.BEARER_AUTHORIZATION_SCHEME));
        if (authParts.length != 2 || !authParts[1].equals(accessToken)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    @GET
    @Produces("application/json")
    public ClientRegistration readClientRegistrationWithQuery(@QueryParam("client_id") String clientId) {
        return doReadClientRegistration(clientId);
    }
    
    @GET
    @Path("{clientId}")
    @Produces("application/json")
    public ClientRegistration readClientRegistrationWithPath(@PathParam("clientId") String clientId) {
        
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
        if (readClient(clientId) != null) {
            clientProvider.removeClient(clientId);    
        }
        
        return Response.ok().build();
    }
    
    protected ClientRegistrationResponse fromClientToRegistrationResponse(Client client) {
        ClientRegistrationResponse response = new ClientRegistrationResponse();
        response.setClientId(client.getClientId());
        response.setClientSecret(client.getClientSecret());
        response.setClientIdIssuedAt(client.getRegisteredAt());
        // TODO: consider making Client secret time limited
        response.setClientSecretExpiresAt(Long.valueOf(0));
        UriBuilder ub = getMessageContext().getUriInfo().getAbsolutePathBuilder();
        response.setRegistrationClientUri(ub.path(client.getClientId()).build().toString());
        
        response.setRegistrationAccessToken(client.getProperties()
                                            .get(ClientRegistrationResponse.REG_ACCESS_TOKEN));
        return response;
    }
    
    protected ClientRegistration doReadClientRegistration(String clientId) {
        Client client = readClient(clientId);
        return fromClientToClientRegistration(client);
    }

    protected ClientRegistration fromClientToClientRegistration(Client client) {
        return new ClientRegistration();
    }
    
    protected Client readClient(String clientId) {
        Client c = clientProvider.getClient(clientId);
        if (c == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        String regAccessToken = c.getProperties().get(ClientRegistrationResponse.REG_ACCESS_TOKEN);
        // Or check OAuthDataProvider.getAccessToken
        // if OAuthDataProvider.createAccessToken was used
        
        validateRegistrationAccessToken(regAccessToken);
        return c;
    }
    
    protected void validateRegistrationAccessToken(String accessToken) {
        checkCurrentAccessToken(accessToken);
    }

    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.initialAccessToken = registrationAccessToken;
    }
    
    protected Client createNewClient(ClientRegistration request) {
        // Client ID
        String clientId = generateClientId();
        
        // Client Name
        String clientName = request.getClientName();
        if (StringUtils.isEmpty(clientName)) {
            clientName = clientId;
        }
        
        List<String> grantTypes = request.getGrantTypes();
        
        // Client Type
        // https://tools.ietf.org/html/rfc7591 has no this property but
        // but http://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata does
        String appType = request.getApplicationType();
        if (appType == null) {
            appType = DEFAULT_APPLICATION_TYPE;
        }
        boolean isConfidential = DEFAULT_APPLICATION_TYPE.equals(appType) 
            && grantTypes != null && grantTypes.contains(OAuthConstants.AUTHORIZATION_CODE_GRANT);
        
        // Client Secret
        String clientSecret = isConfidential
            ? generateClientSecret(request)
            : null;

        Client newClient = new Client(clientId, clientSecret, isConfidential, clientName);
        
        if (grantTypes != null) {
            newClient.setAllowedGrantTypes(grantTypes);
        }    
        
        // Client Registration Time
        newClient.setRegisteredAt(System.currentTimeMillis() / 1000);
        
        // Client Redirect URIs
        List<String> redirectUris = request.getRedirectUris();
        if (redirectUris != null) {
            for (String uri : redirectUris) {
                validateRequestUri(uri, appType, grantTypes);
            }
            newClient.setRedirectUris(redirectUris);
        }
        
        // Client Scopes
        String scope = request.getScope();
        if (!StringUtils.isEmpty(scope)) {
            newClient.setRegisteredScopes(OAuthUtils.parseScope(scope));
        }
        // Client Application URI
        String clientUri = request.getClientUri();
        if (clientUri != null) {
            newClient.setApplicationWebUri(clientUri);
        }
        // Client Logo URI
        String clientLogoUri = request.getLogoUri();
        if (clientLogoUri != null) {
            newClient.setApplicationLogoUri(clientLogoUri);
        }
        
        //TODO: check other properties
        // Add more typed properties like tosUri, policyUri, etc to Client
        // or set them as Client extra properties
        
        return newClient;
    }

    protected void validateRequestUri(String uri, String appType, List<String> grantTypes) {
        // Web Clients using the OAuth Implicit Grant Type MUST only register URLs using the https scheme 
        // as redirect_uris; they MUST NOT use localhost as the hostname. Native Clients MUST only register
        // redirect_uris using custom URI schemes or URLs using the http: scheme with localhost as the hostname.
        // Authorization Servers MAY place additional constraints on Native Clients. Authorization Servers MAY 
        // reject Redirection URI values using the http scheme, other than the localhost case for Native Clients
    }

    public void setClientProvider(ClientRegistrationProvider clientProvider) {
        this.clientProvider = clientProvider;
    }
    
    protected String generateClientId() {
        return Base64UrlUtility.encode(
                   CryptoUtils.generateSecureRandomBytes(
                        getClientIdSizeInBytes()));
    }

    public int getClientIdSizeInBytes() {
        return clientIdSizeInBytes;
    }
    public void setClientIdSizeInBytes(int size) {
        clientIdSizeInBytes = size;
    }

    protected String generateClientSecret(ClientRegistration request) {
        return Base64UrlUtility.encode(
                   CryptoUtils.generateSecureRandomBytes(
                       getClientSecretSizeInBytes(request)));
    }

    protected int getClientSecretSizeInBytes(ClientRegistration request) {
        return 16;
    }
}
