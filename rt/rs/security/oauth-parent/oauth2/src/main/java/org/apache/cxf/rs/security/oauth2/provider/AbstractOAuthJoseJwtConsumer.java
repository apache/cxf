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
package org.apache.cxf.rs.security.oauth2.provider;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public abstract class AbstractOAuthJoseJwtConsumer extends AbstractJoseJwtConsumer {
   
    private boolean decryptWithClientSecret;
    private boolean verifyWithClientSecret;
    
    protected JwtToken getJwtToken(String wrappedJwtToken, String clientSecret) {
        return getJwtToken(wrappedJwtToken, 
                           getInitializedDecryptionProvider(clientSecret),
                           getInitializedSignatureVerifier(clientSecret));
    }
    
    protected JwsSignatureVerifier getInitializedSignatureVerifier(String clientSecret) {
        if (verifyWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(clientSecret);
            return JwsUtils.getHmacSignatureVerifier(hmac, SignatureAlgorithm.HS256);
        }
        return null;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider(String clientSecret) {
        JweDecryptionProvider theDecryptionProvider = null;
        if (decryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(clientSecret);
            theDecryptionProvider = JweUtils.getDirectKeyJweDecryption(key, ContentAlgorithm.A128GCM);
        }
        return theDecryptionProvider;
        
    }

    public void setDecryptWithClientSecret(boolean decryptWithClientSecret) {
        if (verifyWithClientSecret) {
            throw new SecurityException();
        }
        this.decryptWithClientSecret = verifyWithClientSecret;
    }
    public void setVerifyWithClientSecret(boolean verifyWithClientSecret) {
        if (verifyWithClientSecret) {
            throw new SecurityException();
        }
        this.verifyWithClientSecret = verifyWithClientSecret;
    }
}
