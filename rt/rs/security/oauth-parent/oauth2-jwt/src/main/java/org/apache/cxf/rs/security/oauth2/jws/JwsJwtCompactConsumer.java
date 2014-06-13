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
package org.apache.cxf.rs.security.oauth2.jws;

import org.apache.cxf.rs.security.oauth2.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenJson;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReader;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;

public class JwsJwtCompactConsumer extends JwsCompactConsumer {
    private JwtToken token;
    public JwsJwtCompactConsumer(String encodedJws) {
        this(encodedJws, null, null);
    }
    public JwsJwtCompactConsumer(String encodedJws, JwsSignatureProperties props) {
        this(encodedJws, props, null);
    }
    public JwsJwtCompactConsumer(String encodedJws, JwtTokenReader r) {
        this(encodedJws, null, r);
    }
    public JwsJwtCompactConsumer(String encodedJws, JwsSignatureProperties props, JwtTokenReader r) {
        super(encodedJws, props, r == null ? new JwtTokenReaderWriter() : r);
    }
    public JwtTokenJson getDecodedJsonToken() {
        return new JwtTokenJson(getDecodedJsonHeaders(), getDecodedJwsPayload());
    }
    public JwtClaims getJwtClaims() {
        return getJwtToken().getClaims();
    }
    public JwtToken getJwtToken() {
        if (token == null) {
            token = ((JwtTokenReaderWriter)getReader()).fromJson(
                new JwtTokenJson(getDecodedJsonHeaders(), getDecodedJwsPayload()));
        }
        return token;
    }
    
}
