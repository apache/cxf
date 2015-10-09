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

import org.apache.cxf.rs.security.jose.AbstractJoseConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;

public abstract class AbstractJoseJwtConsumer extends AbstractJoseConsumer {
    private boolean jwsRequired = true;
    private boolean jweRequired;
    
    
    protected JwtToken getJwtToken(String wrappedJwtToken) {
        return getJwtToken(wrappedJwtToken, null, null);
    }
    protected JwtToken getJwtToken(String wrappedJwtToken,
                                   JweDecryptionProvider jweDecryptor,
                                   JwsSignatureVerifier theSigVerifier) {
        if (!isJwsRequired() && !isJweRequired()) {
            throw new JwtException("Unable to process JWT");
        }
        
        if (isJweRequired()) {
            if (jweDecryptor == null) {
                jweDecryptor = getInitializedDecryptionProvider();
            }
            if (jweDecryptor == null) {
                throw new JwtException("Unable to decrypt JWT");
            }
            
            if (!isJwsRequired()) {
                return new JweJwtCompactConsumer(wrappedJwtToken).decryptWith(jweDecryptor);    
            }
            wrappedJwtToken = jweDecryptor.decrypt(wrappedJwtToken).getContentText();
        }
        

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(wrappedJwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
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
        return super.getInitializedSignatureVerifier();
    }
    protected void validateToken(JwtToken jwt) {
    }
    public boolean isJwsRequired() {
        return jwsRequired;
    }

    public void setJwsRequired(boolean jwsRequired) {
        this.jwsRequired = jwsRequired;
    }

    public boolean isJweRequired() {
        return jweRequired;
    }

    public void setJweRequired(boolean jweRequired) {
        this.jweRequired = jweRequired;
    }
    
}
