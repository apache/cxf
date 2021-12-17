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

package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.AbstractGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Authorization Code Grant Handler
 */
public class AuthorizationCodeGrantHandler extends AbstractGrantHandler {

    private List<CodeVerifierTransformer> codeVerifierTransformers = Collections.emptyList();
    private boolean expectCodeVerifierForPublicClients;
    private boolean requireCodeVerifier;

    public AuthorizationCodeGrantHandler() {
        super(OAuthConstants.AUTHORIZATION_CODE_GRANT);
    }

    public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
        throws OAuthServiceException {

        // Get the grant representation from the provider
        String codeValue = params.getFirst(OAuthConstants.AUTHORIZATION_CODE_VALUE);
        ServerAuthorizationCodeGrant grant =
            ((AuthorizationCodeDataProvider)getDataProvider()).removeCodeGrant(codeValue);
        if (grant == null) {
            return null;
        }
        // check it has not expired, the client ids are the same
        if (OAuthUtils.isExpired(grant.getIssuedAt(), grant.getExpiresIn())) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        if (!grant.getClient().getClientId().equals(client.getClientId())) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        // redirect URIs must match too
        String expectedRedirectUri = grant.getRedirectUri();
        String providedRedirectUri = params.getFirst(OAuthConstants.REDIRECT_URI);
        if (providedRedirectUri != null) {
            if (!providedRedirectUri.equals(expectedRedirectUri)) {
                throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
            }
        } else if (expectedRedirectUri == null && !isCanSupportPublicClients()
            || expectedRedirectUri != null
                && (client.getRedirectUris().size() != 1
                || !client.getRedirectUris().contains(expectedRedirectUri))) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
        }

        String clientCodeVerifier = params.getFirst(OAuthConstants.AUTHORIZATION_CODE_VERIFIER);
        String clientCodeChallenge = grant.getClientCodeChallenge();
        String clientCodeChallengeMethod = grant.getClientCodeChallengeMethod();
        if (!compareCodeVerifierWithChallenge(client, clientCodeVerifier,
                clientCodeChallenge, clientCodeChallengeMethod)) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        List<String> audiences = getAudiences(client, params, grant.getAudience());
        return doCreateAccessToken(client, grant, getSingleGrantType(), clientCodeVerifier, audiences);
    }

    protected List<String> getAudiences(Client client, MultivaluedMap<String, String> params,
                                        String grantAudience) {
        String clientAudience = params.getFirst(OAuthConstants.CLIENT_AUDIENCE);
        if (client.getRegisteredAudiences().isEmpty() && clientAudience == null && grantAudience == null) {
            return Collections.emptyList();
        }
        // if the audience was approved at the grant creation time and the audience is also
        // sent to the token endpoint then both values must match
        if (grantAudience != null && clientAudience != null && !grantAudience.equals(clientAudience)) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
        }
        return getAudiences(client, clientAudience == null ? grantAudience : clientAudience);
    }

    private ServerAccessToken doCreateAccessToken(Client client,
                                                  ServerAuthorizationCodeGrant grant,
                                                  String requestedGrant,
                                                  String codeVerifier,
                                                  List<String> audiences) {
        if (grant.isPreauthorizedTokenAvailable()) {
            ServerAccessToken token = getPreAuthorizedToken(client,
                                                            grant.getSubject(),
                                                            requestedGrant,
                                                            grant.getRequestedScopes(),
                                                            getAudiences(client, grant.getAudience()));
            if (token != null) {
                if (grant.getNonce() != null) {
                    JAXRSUtils.getCurrentMessage().getExchange().put(OAuthConstants.NONCE, grant.getNonce());
                }
                return token;
            }
            // the grant was issued based on the authorization time check confirming the
            // token was available but it has expired by now or been removed then
            // creating a completely new token can be wrong - though this needs to be reviewed
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        // Make sure the client supports the authorization code in cases where
        // the implicit/hybrid service was initiating the code grant processing flow

        if (!client.getAllowedGrantTypes().isEmpty() && !client.getAllowedGrantTypes().contains(requestedGrant)) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        // Delegate to the data provider to create the one
        AccessTokenRegistration reg = new AccessTokenRegistration();
        reg.setGrantCode(grant.getCode());
        reg.setClient(client);
        reg.setGrantType(requestedGrant);
        reg.setSubject(grant.getSubject());
        reg.setRequestedScope(grant.getRequestedScopes());
        reg.setNonce(grant.getNonce());
        if (grant.getApprovedScopes() != null) {
            reg.setApprovedScope(grant.getApprovedScopes());
        } else {
            reg.setApprovedScope(Collections.emptyList());
        }
        reg.setAudiences(audiences);
        reg.setResponseType(grant.getResponseType());
        reg.setClientCodeVerifier(codeVerifier);
        reg.getExtraProperties().putAll(grant.getExtraProperties());
        return getDataProvider().createAccessToken(reg);
    }

    private boolean compareCodeVerifierWithChallenge(Client c, String clientCodeVerifier,
                                                     String clientCodeChallenge, String clientCodeChallengeMethod) {
        if (clientCodeChallenge == null && clientCodeVerifier == null) {
            if (requireCodeVerifier) {
                return false;
            }
            return c.isConfidential() || !expectCodeVerifierForPublicClients;
        } else if (clientCodeChallenge != null && clientCodeVerifier == null
            || clientCodeChallenge == null && clientCodeVerifier != null) {
            return false;
        } else {
            CodeVerifierTransformer codeVerifierTransformer = null;
            if (!codeVerifierTransformers.isEmpty() && clientCodeChallengeMethod != null) {
                codeVerifierTransformer = codeVerifierTransformers.stream()
                        .filter(t -> clientCodeChallengeMethod.equals(t.getChallengeMethod()))
                        .findAny()
                        .orElse(null);
                // If we have configured codeVerifierTransformers then we must have a match
                if (codeVerifierTransformer == null) {
                    return false;
                }
            }
            // Fall back to plain
            if (codeVerifierTransformer == null) {
                codeVerifierTransformer = new PlainCodeVerifier();
            }
            String transformedCodeVerifier = codeVerifierTransformer.transformCodeVerifier(clientCodeVerifier);
            return clientCodeChallenge.equals(transformedCodeVerifier);
        }
    }

    public void setCodeVerifierTransformer(CodeVerifierTransformer codeVerifier) {
        setCodeVerifierTransformers(codeVerifier == null ? null : Collections.singletonList(codeVerifier));
    }

    public void setCodeVerifierTransformers(List<CodeVerifierTransformer> codeVerifierTransformers) {
        if (codeVerifierTransformers == null) {
            this.codeVerifierTransformers = Collections.emptyList();
        }
        this.codeVerifierTransformers = new ArrayList<>(codeVerifierTransformers);
    }

    /**
     * Require a code verifier for public clients only.
     * @param expectCodeVerifierForPublicClients require a code verifier for public clients only.
     */
    public void setExpectCodeVerifierForPublicClients(boolean expectCodeVerifierForPublicClients) {
        this.expectCodeVerifierForPublicClients = expectCodeVerifierForPublicClients;
    }

    /**
     * Require a code verifier (PKCE). This will override any value set for expectCodeVerifierForPublicClients
     * @param requireCodeVerifier require a code verifier
     */
    public void setRequireCodeVerifier(boolean requireCodeVerifier) {
        this.requireCodeVerifier = requireCodeVerifier;
    }
}
