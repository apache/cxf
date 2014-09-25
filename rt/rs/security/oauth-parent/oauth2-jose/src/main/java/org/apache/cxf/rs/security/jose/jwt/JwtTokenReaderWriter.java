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

import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;




public class JwtTokenReaderWriter extends JoseHeadersReaderWriter
    implements JwtTokenReader, JwtTokenWriter {
    

    @Override
    public String claimsToJson(JwtClaims claims) {
        return toJson(claims);
    }

    @Override
    public JwtTokenJson tokenToJson(JwtToken token) {
        return new JwtTokenJson(toJson(token.getHeaders()),
                                    toJson(token.getClaims()));
    }
    
    @Override
    public JwtClaims fromJsonClaims(String claimsJson) {
        JwtClaims claims = new JwtClaims();
        fromJsonInternal(claims, claimsJson);
        return claims;
        
    }
    
    private JwtToken fromJson(String headersJson, String claimsJson) {
        JwtHeaders headers = fromJsonHeaders(headersJson);
        JwtClaims claims = fromJsonClaims(claimsJson);
        return new JwtToken(headers, claims);
    }
    
    @Override
    public JwtToken fromJson(JwtTokenJson pair) {
        return fromJson(pair.getHeadersJson(), pair.getClaimsJson());
    }
    
    @Override
    public JwtHeaders fromJsonHeaders(String jsonHeaders) {
        return new JwtHeaders(super.fromJsonHeaders(jsonHeaders)); 
    }
}
