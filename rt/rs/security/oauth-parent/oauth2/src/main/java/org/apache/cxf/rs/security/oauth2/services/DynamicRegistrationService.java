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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

@Path("register")
public class DynamicRegistrationService {
    private static final String DEFAULT_APPLICATION_TYPE = "web";
    private static final Integer DEFAULT_CLIENT_ID_SIZE = 10;
    private ClientRegistrationProvider clientProvider;
    private String initialAccessToken;
    private int clientIdSizeInBytes = DEFAULT_CLIENT_ID_SIZE;
    private MessageContext mc;
    private boolean supportRegistrationAccessTokens = true;
    private String userRole;

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response register(ClientRegistration request) {
        checkInitialAuthentication();
        Client client = createNewClient(request);
        createRegAccessToken(client);
        clientProvider.setClient(client);

        return Response.status(201).entity(fromClientToRegistrationResponse(client)).build();
    }

    protected void checkInitialAuthentication() {
        if (initialAccessToken != null) {
            String accessToken = getRequestAccessToken();
            if (!initialAccessToken.equals(accessToken)) {
                throw ExceptionUtils.toNotAuthorizedException(null, null);
            }
        } else {
            checkSecurityContext();
        }

    }


    protected void checkSecurityContext() {
        SecurityContext sc = mc.getSecurityContext();
        if (sc.getUserPrincipal() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        if (userRole != null && !sc.isUserInRole(userRole)) {
            throw ExceptionUtils.toForbiddenException(null, null);
        }
    }

    protected String createRegAccessToken(Client client) {
        String regAccessToken = OAuthUtils.generateRandomTokenKey();
        client.getProperties().put(ClientRegistrationResponse.REG_ACCESS_TOKEN,
                                   regAccessToken);
        return regAccessToken;
    }
    protected void checkRegistrationAccessToken(Client c, String accessToken) {
        String regAccessToken = c.getProperties().get(ClientRegistrationResponse.REG_ACCESS_TOKEN);

        if (regAccessToken == null || !regAccessToken.equals(accessToken)) {
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
    @Produces("application/json")
    public ClientRegistration updateClientRegistration(@PathParam("clientId") String clientId,
        ClientRegistration request) {
        Client client = readClient(clientId);
        fromClientRegistrationToClient(request, client);
        clientProvider.setClient(client);
        return fromClientToClientRegistration(client);
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
        if (client.getClientSecret() != null) {
            response.setClientSecret(client.getClientSecret());
            // TODO: consider making Client secret time limited
            response.setClientSecretExpiresAt(Long.valueOf(0));
        }
        response.setClientIdIssuedAt(client.getRegisteredAt());
        response.setGrantTypes(client.getAllowedGrantTypes());
        UriBuilder ub = getMessageContext().getUriInfo().getAbsolutePathBuilder();

        if (supportRegistrationAccessTokens) {
            // both registration access token and uri are either included or excluded
            response.setRegistrationClientUri(
                ub.path(client.getClientId()).build().toString());

            response.setRegistrationAccessToken(
                client.getProperties().get(ClientRegistrationResponse.REG_ACCESS_TOKEN));
        }
        return response;
    }

    protected ClientRegistration doReadClientRegistration(String clientId) {
        Client client = readClient(clientId);
        return fromClientToClientRegistration(client);
    }

    protected ClientRegistration fromClientToClientRegistration(Client c) {
        ClientRegistration reg = new ClientRegistration();
        reg.setClientName(c.getApplicationName());
        reg.setGrantTypes(c.getAllowedGrantTypes());
        reg.setApplicationType(c.isConfidential() ? "web" : "native");
        if (!c.getRedirectUris().isEmpty()) {
            reg.setRedirectUris(c.getRedirectUris());
        }
        if (!c.getRegisteredScopes().isEmpty()) {
            reg.setScope(OAuthUtils.convertListOfScopesToString(c.getRegisteredScopes()));
        }
        if (c.getApplicationWebUri() != null) {
            reg.setClientUri(c.getApplicationWebUri());
        }
        if (c.getApplicationLogoUri() != null) {
            reg.setLogoUri(c.getApplicationLogoUri());
        }
        if (!c.getRegisteredAudiences().isEmpty()) {
            reg.setResourceUris(c.getRegisteredAudiences());
        }
        if (c.getTokenEndpointAuthMethod() != null) {
            reg.setTokenEndpointAuthMethod(c.getTokenEndpointAuthMethod());
            if (OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS.equals(c.getTokenEndpointAuthMethod())) {
                String subjectDn = c.getProperties().get(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN);
                if (subjectDn != null) {
                    reg.setProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, subjectDn);
                }
                String issuerDn = c.getProperties().get(OAuthConstants.TLS_CLIENT_AUTH_ISSUER_DN);
                if (issuerDn != null) {
                    reg.setProperty(OAuthConstants.TLS_CLIENT_AUTH_ISSUER_DN, issuerDn);
                }
            }
        }

        return reg;
    }

    protected Client readClient(String clientId) {
        String accessToken = getRequestAccessToken();

        Client c = clientProvider.getClient(clientId);
        if (c == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        checkRegistrationAccessToken(c, accessToken);
        return c;
    }


    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public void setInitialAccessToken(String initialAccessToken) {
        this.initialAccessToken = initialAccessToken;
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
        if (grantTypes == null) {
            grantTypes = Collections.singletonList(OAuthConstants.AUTHORIZATION_CODE_GRANT);
        }

        String tokenEndpointAuthMethod = request.getTokenEndpointAuthMethod();
        //TODO: default is expected to be set to OAuthConstants.TOKEN_ENDPOINT_AUTH_BASIC

        boolean passwordRequired = isPasswordRequired(grantTypes, tokenEndpointAuthMethod);

        // Application Type
        // https://tools.ietf.org/html/rfc7591 has no this property but
        // but http://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata does
        String appType = request.getApplicationType();
        if (appType == null) {
            appType = DEFAULT_APPLICATION_TYPE;
        }
        boolean isConfidential = DEFAULT_APPLICATION_TYPE.equals(appType)
            && (passwordRequired
                || OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS.equals(tokenEndpointAuthMethod));

        // Client Secret
        String clientSecret = passwordRequired ? generateClientSecret(request) : null;

        Client newClient = new Client(clientId, clientSecret, isConfidential, clientName);

        newClient.setAllowedGrantTypes(grantTypes);

        newClient.setTokenEndpointAuthMethod(tokenEndpointAuthMethod);
        if (OAuthConstants.TOKEN_ENDPOINT_AUTH_TLS.equals(tokenEndpointAuthMethod)) {
            String subjectDn = (String)request.getProperty(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN);
            if (subjectDn != null) {
                newClient.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, subjectDn);
            }
            String issuerDn = (String)request.getProperty(OAuthConstants.TLS_CLIENT_AUTH_ISSUER_DN);
            if (issuerDn != null) {
                newClient.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_ISSUER_DN, issuerDn);
            }
        }
        // Client Registration Time
        newClient.setRegisteredAt(System.currentTimeMillis() / 1000L);

        fromClientRegistrationToClient(request, newClient);

        SecurityContext sc = mc.getSecurityContext();
        if (sc != null && sc.getUserPrincipal() != null && sc.getUserPrincipal().getName() != null) {
            UserSubject subject = new UserSubject(sc.getUserPrincipal().getName());
            newClient.setResourceOwnerSubject(subject);
        }

        newClient.setRegisteredDynamically(true);
        return newClient;
    }

    protected void fromClientRegistrationToClient(ClientRegistration request, Client client) {
        final List<String> grantTypes = client.getAllowedGrantTypes();

        // Client Redirect URIs
        List<String> redirectUris = request.getRedirectUris();
        if (redirectUris != null) {
            String appType = request.getApplicationType();
            if (appType == null) {
                appType = DEFAULT_APPLICATION_TYPE;
            }
            for (String uri : redirectUris) {
                validateRequestUri(uri, appType, grantTypes);
            }
            client.setRedirectUris(redirectUris);
        }

        if (client.getRedirectUris().isEmpty()
            && (grantTypes.contains(OAuthConstants.AUTHORIZATION_CODE_GRANT)
                || grantTypes.contains(OAuthConstants.IMPLICIT_GRANT))) {
            // Throw an error as we need a redirect URI for these grants.
            OAuthError error =
                new OAuthError(OAuthConstants.INVALID_REQUEST, "A Redirection URI is required");
            reportInvalidRequestError(error);
        }

        // Client Resource Audience URIs
        List<String> resourceUris = request.getResourceUris();
        if (resourceUris != null) {
            client.setRegisteredAudiences(resourceUris);
        }

        // Client Scopes
        String scope = request.getScope();
        if (!StringUtils.isEmpty(scope)) {
            client.setRegisteredScopes(OAuthUtils.parseScope(scope));
        }
        // Client Application URI
        String clientUri = request.getClientUri();
        if (clientUri != null) {
            client.setApplicationWebUri(clientUri);
        }
        // Client Logo URI
        String clientLogoUri = request.getLogoUri();
        if (clientLogoUri != null) {
            client.setApplicationLogoUri(clientLogoUri);
        }

        //TODO: check other properties
        // Add more typed properties like tosUri, policyUri, etc to Client
        // or set them as Client extra properties
    }


    protected boolean isPasswordRequired(List<String> grantTypes, String tokenEndpointAuthMethod) {
        if (grantTypes.contains(OAuthConstants.IMPLICIT_GRANT)) {
            return false;
        }
        if (tokenEndpointAuthMethod == null) {
            return true;
        }

        return !OAuthConstants.TOKEN_ENDPOINT_AUTH_NONE.equals(tokenEndpointAuthMethod)
            && (OAuthConstants.TOKEN_ENDPOINT_AUTH_BASIC.equals(tokenEndpointAuthMethod)
                || OAuthConstants.TOKEN_ENDPOINT_AUTH_POST.equals(tokenEndpointAuthMethod));
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

    protected String getRequestAccessToken() {
        // This call will throw 401 if no given authorization scheme exists
        return AuthorizationUtils.getAuthorizationParts(getMessageContext(),
                    Collections.singleton(OAuthConstants.BEARER_AUTHORIZATION_SCHEME))[1];
    }
    protected int getClientSecretSizeInBytes(ClientRegistration request) {
        return 32;
    }

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
    }

    public MessageContext getMessageContext() {
        return mc;
    }

    public void setSupportRegistrationAccessTokens(boolean supportRegistrationAccessTokens) {
        this.supportRegistrationAccessTokens = supportRegistrationAccessTokens;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    private void reportInvalidRequestError(OAuthError entity) {
        reportInvalidRequestError(entity, MediaType.APPLICATION_JSON_TYPE);
    }

    private void reportInvalidRequestError(OAuthError entity, MediaType mt) {
        ResponseBuilder rb = JAXRSUtils.toResponseBuilder(400);
        if (mt != null) {
            rb.type(mt);
        }
        throw ExceptionUtils.toBadRequestException(null, rb.entity(entity).build());
    }
}
