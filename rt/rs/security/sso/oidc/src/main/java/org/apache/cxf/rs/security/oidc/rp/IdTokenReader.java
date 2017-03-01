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
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class IdTokenReader extends OidcClaimsValidator {
    private boolean requireAtHash = true;
    private boolean requireCodeHash;

    public IdToken getIdToken(ClientAccessToken at, String code, Consumer client) {
        JwtToken jwt = getIdJwtToken(at, code, client);
        return getIdTokenFromJwt(jwt);
    }

    public IdToken getIdToken(ClientAccessToken at, Consumer client) {
        return getIdToken(at, null, client);
    }
    public IdToken getIdToken(String idJwtToken, Consumer client) {
        JwtToken jwt = getIdJwtToken(idJwtToken, client);
        return getIdTokenFromJwt(jwt);
    }
    public JwtToken getIdJwtToken(ClientAccessToken at, String code, Consumer client) {
        String idJwtToken = at.getParameters().get(OidcUtils.ID_TOKEN);
        JwtToken jwt = getIdJwtToken(idJwtToken, client);
        OidcUtils.validateAccessTokenHash(at, jwt, requireAtHash);
        OidcUtils.validateCodeHash(code, jwt, requireCodeHash);
        return jwt;
    }
    public JwtToken getIdJwtToken(ClientAccessToken at, Consumer client) {
        return getIdJwtToken(at, null, client);
    }
    public JwtToken getIdJwtToken(String idJwtToken, Consumer client) {
        JwtToken jwt = getJwtToken(idJwtToken, client.getClientSecret());
        validateJwtClaims(jwt.getClaims(), client.getClientId(), true);
        return jwt;
    }
    private IdToken getIdTokenFromJwt(JwtToken jwt) {
        return new IdToken(jwt.getClaims().asMap());
    }
    public void setRequireAccessTokenHash(boolean require) {
        this.requireAtHash = require;
    }
    public void setRequireCodeHash(boolean require) {
        this.requireCodeHash = require;
    }
}
