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
package org.apache.cxf.rs.security.oauth2.client;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadata;
import org.apache.cxf.rs.security.oauth2.tokens.hawk.HawkAuthorizationScheme;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;

/**
 * The utility class for simplifying working with OAuth servers
 */
public final class OAuthClientUtils {
    private OAuthClientUtils() {

    }

    /**
     * Builds a complete URI for redirecting to OAuth Authorization Service
     * @param authorizationServiceURI the service endpoint address
     * @param clientId client registration id
     * @param redirectUri the uri the authorization code will be posted to
     * @param state the client state, example the key or the encrypted token
     *              representing the info about the current end user's request
     * @scope scope the optional scope; if not specified then the authorization
     *              service will allocate the default scope
     * @return authorization service URI
     */
    public static URI getAuthorizationURI(String authorizationServiceURI,
                                          String clientId,
                                          String redirectUri,
                                          String state,
                                          String scope) {
        UriBuilder ub = getAuthorizationURIBuilder(authorizationServiceURI,
                                                   clientId,
                                                   redirectUri,
                                                   state,
                                                   scope);
        return ub.build();
    }

    public static UriBuilder getAuthorizationURIBuilder(String authorizationServiceURI,
                                          String clientId,
                                          String redirectUri,
                                          String state,
                                          String scope) {
        UriBuilder ub = getAuthorizationURIBuilder(authorizationServiceURI,
                                                   clientId,
                                                   scope);
        if (redirectUri != null) {
            ub.queryParam(OAuthConstants.REDIRECT_URI, redirectUri);
        }
        if (state != null) {
            ub.queryParam(OAuthConstants.STATE, state);
        }
        return ub;
    }

    /**
     * Creates the builder for building OAuth AuthorizationService URIs
     * @param authorizationServiceURI the service endpoint address
     * @param clientId client registration id
     * @param scope the optional scope; if not specified then the authorization
     *              service will allocate the default scope
     * @return the builder
     */
    public static UriBuilder getAuthorizationURIBuilder(String authorizationServiceURI,
                                                 String clientId,
                                                 String scope) {
        UriBuilder ub = UriBuilder.fromUri(authorizationServiceURI);
        if (clientId != null) {
            ub.queryParam(OAuthConstants.CLIENT_ID, clientId);
        }
        if (scope != null) {
            ub.queryParam(OAuthConstants.SCOPE, scope);
        }
        ub.queryParam(OAuthConstants.RESPONSE_TYPE, OAuthConstants.CODE_RESPONSE_TYPE);
        return ub;
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client
     * @param grant {@link AccessTokenGrant} grant
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant) throws OAuthServiceException {

        return getAccessToken(accessTokenService, consumer, grant, true);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * @param accessTokenServiceUri the AccessToken endpoint address
     * @param consumer {@link Consumer} representing the registered client
     * @param grant {@link AccessTokenGrant} grant
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(String accessTokenServiceUri,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader)
        throws OAuthServiceException {
        OAuthJSONProvider provider = new OAuthJSONProvider();
        WebClient accessTokenService =
            WebClient.create(accessTokenServiceUri, Collections.singletonList(provider));
        accessTokenService.accept("application/json");
        return getAccessToken(accessTokenService, consumer, grant, setAuthorizationHeader);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client.
     * @param grant {@link AccessTokenGrant} grant
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   boolean setAuthorizationHeader) {
        return getAccessToken(accessTokenService, consumer, grant, null, setAuthorizationHeader);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param grant {@link AccessTokenGrant} grant
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   AccessTokenGrant grant)
        throws OAuthServiceException {
        return getAccessToken(accessTokenService, null, grant, null, false);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param grant {@link AccessTokenGrant} grant
     * @param extraParams extra parameters
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   AccessTokenGrant grant,
                                                   Map<String, String> extraParams)
        throws OAuthServiceException {
        return getAccessToken(accessTokenService, null, grant, extraParams, false);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client.
     * @param grant {@link AccessTokenGrant} grant
     * @param extraParams extra parameters
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   Map<String, String> extraParams,
                                                   boolean setAuthorizationHeader)
        throws OAuthServiceException {
        return getAccessToken(accessTokenService, consumer, grant, extraParams,
                              null, setAuthorizationHeader);
    }
    public static ClientAccessToken refreshAccessToken(WebClient accessTokenService,
                                                       ClientAccessToken at) {
        return refreshAccessToken(accessTokenService, null, at, null, true);
    }
    public static ClientAccessToken refreshAccessToken(WebClient accessTokenService,
                                                       Consumer consumer,
                                                       ClientAccessToken at) {
        return refreshAccessToken(accessTokenService, consumer, at, null, true);
    }
    public static ClientAccessToken refreshAccessToken(WebClient accessTokenService,
                                                       Consumer consumer,
                                                       ClientAccessToken at,
                                                       String scope,
                                                       boolean setAuthorizationHeader)
        throws OAuthServiceException {
        RefreshTokenGrant grant = new RefreshTokenGrant(at.getRefreshToken(), scope);
        return getAccessToken(accessTokenService, consumer, grant, null,
                              at.getTokenType(), setAuthorizationHeader);
    }

    /**
     * Obtains the access token from OAuth AccessToken Service
     * using the initialized web client
     * @param accessTokenService the AccessToken client
     * @param consumer {@link Consumer} representing the registered client.
     * @param grant {@link AccessTokenGrant} grant
     * @param extraParams extra parameters
     * @param defaultTokenType default expected token type - some early
     *        well-known OAuth2 services do not return a required token_type parameter
     * @param setAuthorizationHeader if set to true then HTTP Basic scheme
     *           will be used to pass client id and secret, otherwise they will
     *           be passed in the form payload
     * @return {@link ClientAccessToken} access token
     * @throws OAuthServiceException
     */
    public static ClientAccessToken getAccessToken(WebClient accessTokenService,
                                                   Consumer consumer,
                                                   AccessTokenGrant grant,
                                                   Map<String, String> extraParams,
                                                   String defaultTokenType,
                                                   boolean setAuthorizationHeader)
        throws OAuthServiceException {

        if (accessTokenService == null) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }

        Form form = new Form(grant.toMap());
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                form.param(entry.getKey(), entry.getValue());
            }
        }
        if (consumer != null) {
            boolean secretAvailable = !StringUtils.isEmpty(consumer.getClientSecret());
            if (setAuthorizationHeader && secretAvailable) {
                accessTokenService.replaceHeader(HttpHeaders.AUTHORIZATION,
                    DefaultBasicAuthSupplier.getBasicAuthHeader(consumer.getClientId(), consumer.getClientSecret()));
            } else {
                form.param(OAuthConstants.CLIENT_ID, consumer.getClientId());
                if (secretAvailable) {
                    form.param(OAuthConstants.CLIENT_SECRET, consumer.getClientSecret());
                }
            }
        } else {
            // in this case the AccessToken service is expected to find a mapping between
            // the authenticated credentials and the client registration id
        }
        Response response = accessTokenService.form(form);
        final Map<String, String> map;
        try {
            map = response.getMediaType() == null
                    || response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)
                            ? new OAuthJSONProvider().readJSONResponse((InputStream) response.getEntity())
                            : Collections.emptyMap();
        } catch (Exception ex) {
            throw new ResponseProcessingException(response, ex);
        }
        if (200 == response.getStatus()) {
            ClientAccessToken token = fromMapToClientToken(map, defaultTokenType);
            if (token == null) {
                throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
            }
            return token;
        } else if (response.getStatus() >= 400 && map.containsKey(OAuthConstants.ERROR_KEY)) {
            OAuthError error = new OAuthError(map.get(OAuthConstants.ERROR_KEY),
                                              map.get(OAuthConstants.ERROR_DESCRIPTION_KEY));
            error.setErrorUri(map.get(OAuthConstants.ERROR_URI_KEY));
            throw new OAuthServiceException(error);
        }
        throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
    }

    public static ClientAccessToken fromMapToClientToken(Map<String, String> map) {
        return fromMapToClientToken(map, null);
    }

    public static ClientAccessToken fromMapToClientToken(Map<String, String> map,
                                                         String defaultTokenType) {
        final String tokenKey = map.remove(OAuthConstants.ACCESS_TOKEN);
        if (tokenKey != null) {

            String tokenType = map.remove(OAuthConstants.ACCESS_TOKEN_TYPE);
            if (tokenType == null) {
                tokenType = defaultTokenType;
            }
            if (tokenType != null) {
                ClientAccessToken token = new ClientAccessToken(tokenType, tokenKey);

                String refreshToken = map.remove(OAuthConstants.REFRESH_TOKEN);
                if (refreshToken != null) {
                    token.setRefreshToken(refreshToken);
                }
                String expiresInStr = map.remove(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN);
                if (expiresInStr != null) {
                    token.setExpiresIn(Long.parseLong(expiresInStr));
                }
                String issuedAtStr = map.remove(OAuthConstants.ACCESS_TOKEN_ISSUED_AT);
                token.setIssuedAt(issuedAtStr != null ? Long.parseLong(issuedAtStr)
                                                      : System.currentTimeMillis() / 1000);
                String scope = map.remove(OAuthConstants.SCOPE);
                if (scope != null) {
                    token.setApprovedScope(scope);
                }

                token.setParameters(map);
                return token;
            }
        }

        return null;
    }


    /**
     * Creates OAuth Authorization header with Bearer scheme
     * @param accessToken the access token
     * @return the header value
     */
    public static String createAuthorizationHeader(ClientAccessToken accessToken)
        throws OAuthServiceException {
        return createAuthorizationHeader(accessToken, null);
    }

    /**
     * Creates OAuth Authorization header with the scheme that
     * may require an access to the current HTTP request properties
     * @param accessToken the access token
     * @param httpProps http request properties, can be null for Bearer tokens
     * @return the header value
     */
    public static String createAuthorizationHeader(ClientAccessToken accessToken,
                                                   HttpRequestProperties httpProps)
        throws OAuthServiceException {
        StringBuilder sb = new StringBuilder();
        appendTokenData(sb, accessToken, httpProps);
        return sb.toString();
    }
    public static void setAuthorizationHeader(WebClient wc, ClientAccessToken accessToken) {
        setAuthorizationHeader(wc, accessToken, null);
    }
    public static void setAuthorizationHeader(WebClient wc,
                                              ClientAccessToken accessToken,
                                              String httpVerb) {
        wc.replaceHeader(HttpHeaders.AUTHORIZATION,
                         createAuthorizationHeader(accessToken,
                                                   new HttpRequestProperties(wc, httpVerb)));
    }

    public static AuthorizationMetadata getAuthorizationMetadata(String issuerURL) {
        Response response = WebClient.create(issuerURL).path("/.well-known/oauth-authorization-server")
            .accept(MediaType.APPLICATION_JSON).get();
        if (Status.OK.getStatusCode() != response.getStatus()) {
            throw ExceptionUtils.toWebApplicationException(response);
        }
        return new AuthorizationMetadata(new JsonMapObjectReaderWriter().fromJson(response.readEntity(String.class)));
    }

    private static void appendTokenData(StringBuilder sb,
                                        ClientAccessToken token,
                                        HttpRequestProperties httpProps)
        throws OAuthServiceException {
        // this should all be handled by token specific serializers
        String tokenType = token.getTokenType().toLowerCase();
        if (OAuthConstants.BEARER_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            sb.append(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
            sb.append(' ');
            sb.append(token.getTokenKey());
        } else if (OAuthConstants.HAWK_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            if (httpProps == null) {
                throw new IllegalArgumentException("MAC scheme requires HTTP Request properties");
            }
            HawkAuthorizationScheme macAuthData = new HawkAuthorizationScheme(httpProps, token);
            String macAlgo = token.getParameters().get(OAuthConstants.HAWK_TOKEN_ALGORITHM);
            String macKey = token.getParameters().get(OAuthConstants.HAWK_TOKEN_KEY);
            sb.append(macAuthData.toAuthorizationHeader(macAlgo, macKey));
        } else {
            throw new ProcessingException(new OAuthServiceException("Unsupported token type"));
        }

    }
}
