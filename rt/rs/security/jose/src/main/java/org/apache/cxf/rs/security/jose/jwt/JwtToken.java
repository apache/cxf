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

import org.apache.cxf.rs.security.jose.JoseHeaders;



public class JwtToken {
    private JoseHeaders headers;
    private JwtClaims claims;
    public JwtToken(JwtClaims claims) {
        this(new JoseHeaders(), claims);
    }
    public JwtToken(JoseHeaders headers, JwtClaims claims) {
        this.headers = headers;
        this.claims = claims;
    }
    public JoseHeaders getHeaders() {
        return headers;
    }
    public JwtClaims getClaims() {
        return claims;
    }
    public Object getHeader(String name) {
        return headers.getHeader(name);
    }
    public Object getClaim(String name) {
        return claims.getClaim(name);
    }
    public int hashCode() { 
        return headers.hashCode() + 37 * claims.hashCode();
    }
    
    public boolean equals(Object obj) {
        return obj instanceof JwtToken 
            && ((JwtToken)obj).headers.equals(this.headers)
            && ((JwtToken)obj).claims.equals(this.claims);
    }
}
