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

import javax.ws.rs.core.MultivaluedMap;

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
    
    private CodeVerifierTransformer codeVerifierTransformer;
    private boolean expectCodeVerifierForPublicClients;
    
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
            if (expectedRedirectUri == null || !providedRedirectUri.equals(expectedRedirectUri)) {
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
        if (!compareCodeVerifierWithChallenge(client, clientCodeVerifier, clientCodeChallenge)) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }
        
        return doCreateAccessToken(client, 
                                   grant.getSubject(), 
                                   getSingleGrantType(),
                                   grant.getRequestedScopes(),
                                   grant.getApprovedScopes(),
                                   grant.getAudience(),
                                   clientCodeVerifier,
                                   grant.getNonce());
    }
    
    private boolean compareCodeVerifierWithChallenge(Client c, String clientCodeVerifier, 
                                                     String clientCodeChallenge) {
        if (clientCodeChallenge == null && clientCodeChallenge == null 
            && (c.isConfidential() || !expectCodeVerifierForPublicClients)) {
            return true;
        } else if (clientCodeChallenge != null && clientCodeChallenge == null 
            || clientCodeChallenge == null && clientCodeChallenge != null) {
            return false;
        } else {
            String transformedCodeVerifier = codeVerifierTransformer == null 
                ? clientCodeVerifier : codeVerifierTransformer.transformCodeVerifier(clientCodeVerifier); 
            return clientCodeChallenge.equals(transformedCodeVerifier);
        }
    }

    public void setCodeVerifierTransformer(CodeVerifierTransformer codeVerifier) {
        this.codeVerifierTransformer = codeVerifier;
    }

    public void setExpectCodeVerifierForPublicClients(boolean expectCodeVerifierForPublicClients) {
        this.expectCodeVerifierForPublicClients = expectCodeVerifierForPublicClients;
    }
}
