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

import java.util.Properties;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
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
    @Override
    public void process(ClientAccessToken ct, ServerAccessToken st) {
        if (st.getResponseType() != null
            && OidcUtils.CODE_AT_RESPONSE_TYPE.equals(st.getResponseType())) {
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
            return super.processJwt(new JwtToken(idToken), st.getClient());
        } else if (st.getSubject().getProperties().containsKey(OidcUtils.ID_TOKEN)) {
            return st.getSubject().getProperties().get(OidcUtils.ID_TOKEN);
        } else if (st.getSubject() instanceof OidcUserSubject) {
            OidcUserSubject sub = (OidcUserSubject)st.getSubject();
            IdToken idToken = new IdToken(sub.getIdToken());
            idToken.setAudience(st.getClient().getClientId());
            idToken.setAuthorizedParty(st.getClient().getClientId());
            // if this token was refreshed then the cloned IDToken might need to have its
            // issuedAt and expiry time properties adjusted if it proves to be necessary
            setAtHashAndNonce(idToken, st);
            return super.processJwt(new JwtToken(idToken), st.getClient());
        } else {
            return null;
        }
    }
    private void setAtHashAndNonce(IdToken idToken, ServerAccessToken st) {
        String rType = st.getResponseType();
        boolean atHashRequired = idToken.getAccessTokenHash() == null
            && (rType == null || !rType.equals(OidcUtils.ID_TOKEN_RESPONSE_TYPE));
        boolean cHashRequired = idToken.getAuthorizationCodeHash() == null && st.getGrantCode() != null 
            && rType != null 
            && (rType.equals(OidcUtils.CODE_ID_TOKEN_AT_RESPONSE_TYPE)
                || rType.equals(OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE));
        
        if (atHashRequired || cHashRequired) {
            Properties props = JwsUtils.loadSignatureOutProperties(false);
            SignatureAlgorithm sigAlgo = null;
            if (super.isSignWithClientSecret()) {
                sigAlgo = OAuthUtils.getClientSecretSignatureAlgorithm(props);
            } else {
                sigAlgo = JwsUtils.getSignatureAlgorithm(props, SignatureAlgorithm.RS256);
            }
            if (sigAlgo != SignatureAlgorithm.NONE) {
                if (atHashRequired) {
                    String atHash = OidcUtils.calculateAccessTokenHash(st.getTokenKey(), sigAlgo);
                    idToken.setAccessTokenHash(atHash);
                }
                if (cHashRequired) {
                    String cHash = OidcUtils.calculateAuthorizationCodeHash(st.getGrantCode(), sigAlgo);
                    idToken.setAuthorizationCodeHash(cHash);
                }
            }
        }
        Message m = JAXRSUtils.getCurrentMessage();
        if (m != null && m.getExchange().containsKey(OAuthConstants.NONCE)) {
            idToken.setNonce((String)m.getExchange().get(OAuthConstants.NONCE));
        } else if (st.getNonce() != null) {
            idToken.setNonce(st.getNonce());
        }
        
    }
    public void setIdTokenProvider(IdTokenProvider idTokenProvider) {
        this.idTokenProvider = idTokenProvider;
    }
    
}
