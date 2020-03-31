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

import org.apache.cxf.rs.security.jose.common.AbstractJoseConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;

public class JoseJwtConsumer extends AbstractJoseConsumer {
    
    private int clockOffset;
    private int ttl;

    public JwtToken getJwtToken(String wrappedJwtToken) {
        return getJwtToken(wrappedJwtToken, null, null);
    }
    public JwtToken getJwtToken(String wrappedJwtToken,
                                   JweDecryptionProvider theDecryptor,
                                   JwsSignatureVerifier theSigVerifier) {
        super.checkProcessRequirements();

        JweHeaders jweHeaders = new JweHeaders();
        if (isJweRequired()) {
            JweJwtCompactConsumer jwtConsumer = new JweJwtCompactConsumer(wrappedJwtToken);

            if (theDecryptor == null) {
                theDecryptor = getInitializedDecryptionProvider(jwtConsumer.getHeaders());
            }
            if (theDecryptor == null) {
                throw new JwtException("Unable to decrypt JWT");
            }

            if (!isJwsRequired()) {
                return jwtConsumer.decryptWith(theDecryptor);
            }

            JweDecryptionOutput decOutput = theDecryptor.decrypt(wrappedJwtToken);
            wrappedJwtToken = decOutput.getContentText();
            jweHeaders = decOutput.getHeaders();
        }

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(wrappedJwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        // Store the encryption headers as well
        jwt = new JwtToken(jwt.getJwsHeaders(), jweHeaders, jwt.getClaims());

        if (isJwsRequired()) {
            if (theSigVerifier == null) {
                theSigVerifier = getInitializedSignatureVerifier(jwt);
            }
            if (theSigVerifier == null) {
                throw new JwtException("Unable to validate JWT");
            }

            if (!jwtConsumer.verifySignatureWith(theSigVerifier)) {
                throw new JwtException("Invalid Signature");
            }
        }

        validateToken(jwt);
        return jwt;
    }

    protected JwsSignatureVerifier getInitializedSignatureVerifier(JwtToken jwt) {
        return getInitializedSignatureVerifier(jwt.getJwsHeaders());
    }

    protected void validateToken(JwtToken jwt) {
    }
    
    public int getClockOffset() {
        return clockOffset;
    }

    public void setClockOffset(int clockOffset) {
        this.clockOffset = clockOffset;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
}
