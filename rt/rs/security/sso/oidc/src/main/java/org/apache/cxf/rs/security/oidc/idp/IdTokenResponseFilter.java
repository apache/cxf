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

import java.util.Collections;

import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AbstractOAuthServerJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class IdTokenResponseFilter extends AbstractOAuthServerJoseJwtProducer implements AccessTokenResponseFilter {
    private UserInfoProvider userInfoProvider;
    private String issuer;
    @Override
    public void process(ClientAccessToken ct, ServerAccessToken st) {
        
        // This may also be done directly inside a data provider code creating the server token
        if (userInfoProvider != null) {
            IdToken token = 
                userInfoProvider.getIdToken(st.getClient().getClientId(), st.getSubject(), st.getScopes());
            token.setIssuer(issuer);
            token.setAudiences(Collections.singletonList(st.getClient().getClientId()));
            
            String responseEntity = super.processJwt(new JwtToken(token), 
                                                     st.getClient());
            ct.getParameters().put(OidcUtils.ID_TOKEN, responseEntity);
        } else if (st.getSubject().getProperties().containsKey("id_token")) {
            ct.getParameters().put(OidcUtils.ID_TOKEN, 
                                   st.getSubject().getProperties().get("id_token"));
        }
        
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    public void setUserInfoProvider(UserInfoProvider userInfoProvider) {
        this.userInfoProvider = userInfoProvider;
    }
    
}
