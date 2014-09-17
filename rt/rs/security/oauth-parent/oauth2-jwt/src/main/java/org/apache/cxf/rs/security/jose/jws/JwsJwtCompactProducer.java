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
package org.apache.cxf.rs.security.jose.jws;

import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtHeaders;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenWriter;

public class JwsJwtCompactProducer extends JwsCompactProducer {
    
    public JwsJwtCompactProducer(JwtToken token) {
        this(token, null);
    }
    public JwsJwtCompactProducer(JwtClaims claims) {
        this(new JwtToken(null, claims), null);
    }
    public JwsJwtCompactProducer(JwtHeaders headers, JwtClaims claims) {
        this(headers, claims, null);
    }
    public JwsJwtCompactProducer(JwtHeaders headers, JwtClaims claims, JwtTokenWriter w) {
        this(new JwtToken(headers, claims), w);
    }
    public JwsJwtCompactProducer(JwtToken token, JwtTokenWriter w) {
        super(token.getHeaders(), w, serializeClaims(token.getClaims(), w));
    }
    
    private static String serializeClaims(JwtClaims claims, JwtTokenWriter writer) {
        if (writer == null) {
            writer = new JwtTokenReaderWriter();
        }
        return writer.claimsToJson(claims);
    }
}
