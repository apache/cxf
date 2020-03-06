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
package org.apache.cxf.rs.security.jose.jwt;

import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;



public class JwtToken {
    private final JwsHeaders jwsHeaders;
    private final JweHeaders jweHeaders;
    private final JwtClaims claims;

    public JwtToken(JwtClaims claims) {
        this(new JwsHeaders(), new JweHeaders(), claims);
    }
    public JwtToken(JwsHeaders jwsHeaders, JwtClaims claims) {
        this(jwsHeaders, new JweHeaders(), claims);
    }
    public JwtToken(JweHeaders jweHeaders, JwtClaims claims) {
        this(new JwsHeaders(), jweHeaders, claims);
    }
    public JwtToken(JwsHeaders jwsHeaders, JweHeaders jweHeaders, JwtClaims claims) {
        this.jwsHeaders = jwsHeaders;
        this.jweHeaders = jweHeaders;
        this.claims = claims;
    }
    public JwsHeaders getJwsHeaders() {
        return jwsHeaders;
    }
    public JweHeaders getJweHeaders() {
        return jweHeaders;
    }
    public JwtClaims getClaims() {
        return claims;
    }
    public Object getJwsHeader(String name) {
        return jwsHeaders.getHeader(name);
    }
    public Object getJweHeader(String name) {
        return jweHeaders.getHeader(name);
    }
    public Object getClaim(String name) {
        return claims.getClaim(name);
    }
    public int hashCode() {
        return jwsHeaders.hashCode() + 37 * claims.hashCode() + 37 * jweHeaders.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof JwtToken
            && ((JwtToken)obj).jwsHeaders.equals(this.jwsHeaders)
            && ((JwtToken)obj).jweHeaders.equals(this.jweHeaders)
            && ((JwtToken)obj).claims.equals(this.claims);
    }
}
