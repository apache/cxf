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

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthServerJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class IdTokenResponseFilter extends AbstractOAuthServerJoseJwtProducer implements AccessTokenResponseFilter {
    private UserInfoProvider userInfoProvider;
    @Override
    public void process(ClientAccessToken ct, ServerAccessToken st) {
        
        String idToken = getProcessedIdToken(st);
        if (idToken != null) {
            ct.getParameters().put(OidcUtils.ID_TOKEN, idToken);
        } 
        
    }
    private String getProcessedIdToken(ServerAccessToken st) {
        if (userInfoProvider != null) {
            IdToken idToken = 
                userInfoProvider.getIdToken(st.getClient().getClientId(), st.getSubject(), st.getScopes());
            setAtHash(idToken, st);
            return super.processJwt(new JwtToken(idToken), st.getClient());
        } else if (st.getSubject().getProperties().containsKey(OidcUtils.ID_TOKEN)) {
            return st.getSubject().getProperties().get(OidcUtils.ID_TOKEN);
        } else if (st.getSubject() instanceof OidcUserSubject) {
            OidcUserSubject sub = (OidcUserSubject)st.getSubject();
            IdToken idToken = new IdToken(sub.getIdToken());
            setAtHash(idToken, st);
            return super.processJwt(new JwtToken(idToken), st.getClient());
        } else {
            return null;
        }
    }
    private void setAtHash(IdToken idToken, ServerAccessToken st) {
        Properties props = JwsUtils.loadSignatureOutProperties(false);
        SignatureAlgorithm sigAlgo = null;
        if (super.isSignWithClientSecret()) {
            sigAlgo = OAuthUtils.getClientSecretSignatureAlgorithm(props);
        } else {
            sigAlgo = JwsUtils.getSignatureAlgorithm(props, SignatureAlgorithm.RS256);
        }
        if (sigAlgo != SignatureAlgorithm.NONE) {
            String atHash = OidcUtils.calculateAccessTokenHash(st.getTokenKey(), sigAlgo);
            idToken.setAccessTokenHash(atHash);
        }
        
    }
    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }
    
}
