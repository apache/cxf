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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServerJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class IdTokenResponseFilter extends OAuthServerJoseJwtProducer implements AccessTokenResponseFilter {
    private IdTokenProvider idTokenProvider;
    private WebClient keyServiceClient;
    @Override
    public void process(ClientAccessToken ct, ServerAccessToken st) {
        if (st.getResponseType() != null
            && OidcUtils.CODE_AT_RESPONSE_TYPE.equals(st.getResponseType())
            && OAuthConstants.IMPLICIT_GRANT.equals(st.getGrantType())) {
            // token post-processing as part of the current hybrid (implicit) flow
            // so no id_token is returned now - however when the code gets exchanged later on
            // this filter will add id_token to the returned access token
            return;
        }
        // Only add an IdToken if the client has the "openid" scope
        if (ct.getApprovedScope() == null || !ct.getApprovedScope().contains(OidcUtils.OPENID_SCOPE)) {
            return;
        }
        String idToken = getProcessedIdToken(st);
        if (idToken != null) {
            ct.getParameters().put(OidcUtils.ID_TOKEN, idToken);
        }

    }
    private String getProcessedIdToken(ServerAccessToken st) {
        if (idTokenProvider != null) {
            IdToken idToken =
                idTokenProvider.getIdToken(st.getClient().getClientId(), st.getSubject(),
                                           OAuthUtils.convertPermissionsToScopeList(st.getScopes()));
            setAtHashAndNonce(idToken, st);
            return processJwt(new JwtToken(idToken), st.getClient());
        } else if (st.getSubject().getProperties().containsKey(OidcUtils.ID_TOKEN)) {
            return st.getSubject().getProperties().get(OidcUtils.ID_TOKEN);
        } else if (st.getSubject() instanceof OidcUserSubject) {
            OidcUserSubject sub = (OidcUserSubject)st.getSubject();
            if (sub.getIdToken() != null) {
                IdToken idToken = new IdToken(sub.getIdToken());
                idToken.setAudience(st.getClient().getClientId());
                idToken.setAuthorizedParty(st.getClient().getClientId());
                // if this token was refreshed then the cloned IDToken might need to have its
                // issuedAt and expiry time properties adjusted
                if (OAuthConstants.REFRESH_TOKEN_GRANT.equals(st.getGrantType())) {
                    final long iat = st.getIssuedAt();
                    idToken.setExpiryTime(iat + (idToken.getExpiryTime() - idToken.getIssuedAt()));
                    idToken.setIssuedAt(iat);
                }
                setAtHashAndNonce(idToken, st);
                return processJwt(new JwtToken(idToken), st.getClient());
            }
        }
        return null;

    }
    private void setAtHashAndNonce(IdToken idToken, ServerAccessToken st) {
        String rType = st.getResponseType();
        boolean atHashRequired = idToken.getAccessTokenHash() == null
            && (rType == null || !rType.equals(OidcUtils.ID_TOKEN_RESPONSE_TYPE));
        boolean cHashRequired = idToken.getAuthorizationCodeHash() == null
            && rType != null
            && (rType.equals(OidcUtils.CODE_ID_TOKEN_AT_RESPONSE_TYPE)
                || rType.equals(OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE));

        Message m = JAXRSUtils.getCurrentMessage();
        if (atHashRequired || cHashRequired) {
            Properties props = JwsUtils.loadSignatureOutProperties(false);
            final SignatureAlgorithm sigAlgo;
            if (super.isSignWithClientSecret()) {
                sigAlgo = OAuthUtils.getClientSecretSignatureAlgorithm(props);
            } else {
                sigAlgo = JwsUtils.getSignatureAlgorithm(props, SignatureAlgorithm.RS256);
            }
            if (sigAlgo != SignatureAlgorithm.NONE) {
                if (atHashRequired) {
                    String tokenKey = st.getEncodedToken() != null ? st.getEncodedToken() : st.getTokenKey();
                    String atHash = OidcUtils.calculateAccessTokenHash(tokenKey, sigAlgo);
                    idToken.setAccessTokenHash(atHash);
                }
                if (cHashRequired) {
                    // c_hash can be returned from either Authorization or Token endpoints
                    String code;
                    if (st.getGrantCode() != null) {
                        // This is a token endpoint, the code has been exchanged for a token
                        code = st.getGrantCode();
                    } else {
                        // Authorization endpoint: hybrid flow, implicit part
                        code = (String)m.getExchange().get(OAuthConstants.AUTHORIZATION_CODE_VALUE);
                    }
                    if (code != null) {
                        idToken.setAuthorizationCodeHash(OidcUtils.calculateAuthorizationCodeHash(code, sigAlgo));
                    }
                }
            }
        }

        if (m != null && m.getExchange().containsKey(OAuthConstants.NONCE)) {
            idToken.setNonce((String)m.getExchange().get(OAuthConstants.NONCE));
        } else if (st.getNonce() != null) {
            idToken.setNonce(st.getNonce());
        }

    }
    public void setIdTokenProvider(IdTokenProvider idTokenProvider) {
        this.idTokenProvider = idTokenProvider;
    }
    @Override
    public String processJwt(JwtToken jwt, Client client) {
        if (keyServiceClient != null) {
            List<String> opers = new LinkedList<>();
            if (super.isJwsRequired()) {
                opers.add(JsonWebKey.KEY_OPER_SIGN);
            }
            if (super.isJweRequired()) {
                opers.add(JsonWebKey.KEY_OPER_ENCRYPT);
            }
            // the form request can be supported too
            keyServiceClient.resetQuery();
            keyServiceClient.query(JsonWebKey.KEY_OPERATIONS, opers);
            //TODO: OIDC core talks about various security algorithm preferences
            // that may be set during the client registrations, they can be passed along too
            return keyServiceClient.post(jwt, String.class);
        }
        return super.processJwt(jwt, client);
    }
    public void setKeyServiceClient(WebClient keyServiceClient) {
        this.keyServiceClient = keyServiceClient;
    }
}
