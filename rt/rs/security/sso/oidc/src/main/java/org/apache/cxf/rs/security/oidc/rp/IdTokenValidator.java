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
package org.apache.cxf.rs.security.oidc.rp;

import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class IdTokenValidator extends AbstractTokenValidator {
    private boolean requireAtHash = true;
    
    public IdToken getIdToken(ClientAccessToken at, String clientId) {
        JwtToken jwt = getIdJwtToken(at, clientId);
        return getIdTokenFromJwt(jwt);
    }
    public IdToken getIdToken(String idJwtToken, String clientId) {
        JwtToken jwt = getIdJwtToken(idJwtToken, clientId);
        return getIdTokenFromJwt(jwt);
    }
    public JwtToken getIdJwtToken(ClientAccessToken at, String clientId) {
        String idJwtToken = at.getParameters().get(OidcUtils.ID_TOKEN);
        JwtToken jwt = getIdJwtToken(idJwtToken, clientId); 
        OidcUtils.validateAccessTokenHash(at, jwt, requireAtHash);
        return jwt;
    }
    public JwtToken getIdJwtToken(String idJwtToken, String clientId) {
        JwtToken jwt = getJwtToken(idJwtToken, null, false);
        validateJwtClaims(jwt.getClaims(), clientId, true);
        return jwt;
    }
    public IdToken getIdTokenFromJwt(JwtToken jwt) {
        //TODO: do the extra validation if needed
        return new IdToken(jwt.getClaims().asMap());
    }
    public void setRequireAtHash(boolean requireAtHash) {
        this.requireAtHash = requireAtHash;
    }
}
