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
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;


public class JwsJwtCompactProducer extends JwsCompactProducer {
    
    public JwsJwtCompactProducer(JwtToken token) {
        this(token, null);
    }
    public JwsJwtCompactProducer(JwtClaims claims) {
        this(new JwtToken(null, claims), null);
    }
    public JwsJwtCompactProducer(JoseHeaders headers, JwtClaims claims) {
        this(new JwtToken(headers, claims), null);
    }
    protected JwsJwtCompactProducer(JwtToken token, JwtTokenReaderWriter w) {
        super(new JwsHeaders(token.getHeaders()), w, JwtUtils.claimsToJson(token.getClaims(), w));
    }
    
    
}
