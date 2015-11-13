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

import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
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
            Properties props = JwsUtils.loadSignatureInProperties(false);
            SignatureAlgorithm sigAlgo = SignatureAlgorithm.getAlgorithm(
                props.getProperty(OAuthConstants.CLIENT_SECRET_SIGNATURE_ALGORITHM));
            sigAlgo = sigAlgo != null ? sigAlgo : SignatureAlgorithm.HS256;
            if (AlgorithmUtils.isHmacSign(sigAlgo)) {
                return JwsUtils.getHmacSignatureVerifier(clientSecret, sigAlgo);
            }
        }
        return null;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider(String clientSecret) {
        JweDecryptionProvider theDecryptionProvider = null;
        if (decryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(clientSecret);
            Properties props = JweUtils.loadEncryptionInProperties(false);
            ContentAlgorithm ctAlgo = ContentAlgorithm.getAlgorithm(
                props.getProperty(OAuthConstants.CLIENT_SECRET_ENCRYPTION_ALGORITHM));
            ctAlgo = ctAlgo != null ? ctAlgo : ContentAlgorithm.A128GCM;
            theDecryptionProvider = JweUtils.getDirectKeyJweDecryption(key, ctAlgo);
        }
        return theDecryptionProvider;
        
    }

    public void setDecryptWithClientSecret(boolean decryptWithClientSecret) {
        this.decryptWithClientSecret = verifyWithClientSecret;
    }
    public void setVerifyWithClientSecret(boolean verifyWithClientSecret) {
        this.verifyWithClientSecret = verifyWithClientSecret;
    }
}
