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
package org.apache.cxf.rs.security.oidc.idp;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;

public abstract class AbstractJwsJweProducer {
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    private boolean encryptWithClientCertificates;
    private boolean encryptWithClientSecret;
    private boolean signWithClientSecret;
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider(Client c, boolean required) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        
        if (signWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(c.getClientSecret());
            return JwsUtils.getHmacSignatureProvider(hmac, AlgorithmUtils.HMAC_SHA_256_ALGO);
        } else {
            return JwsUtils.loadSignatureProvider(required);
        }
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider(Client c, boolean required) {
        if (encryptionProvider != null) {
            return encryptionProvider;    
        }
        JweEncryptionProvider theEncryptionProvider = null;
        if (encryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(c.getClientSecret());
            theEncryptionProvider = JweUtils.getDirectKeyJweEncryption(key, AlgorithmUtils.A128GCM_ALGO);
        } else if (encryptWithClientCertificates) {
            X509Certificate cert = 
                (X509Certificate)CryptoUtils.decodeCertificate(c.getApplicationCertificates().get(0));
            theEncryptionProvider = JweUtils.createJweEncryptionProvider((RSAPublicKey)cert.getPublicKey(), 
                                                                         AlgorithmUtils.RSA_OAEP_ALGO, 
                                                                         AlgorithmUtils.A128GCM_ALGO, 
                                                                         null);
        }
        if (theEncryptionProvider == null) {
            theEncryptionProvider = JweUtils.loadEncryptionProvider(required);
        }
        return theEncryptionProvider;
        
    }

    public void setEncryptWithClientCertificates(boolean encryptWithClientCertificates) {
        if (encryptWithClientSecret) {
            throw new SecurityException();
        }
        this.encryptWithClientCertificates = encryptWithClientCertificates;
    }
    public void setEncryptWithClientSecret(boolean encryptWithClientSecret) {
        if (signWithClientSecret || encryptWithClientCertificates) {
            throw new SecurityException();
        }
        this.encryptWithClientSecret = encryptWithClientSecret;
    }
    public void setSignWithClientSecret(boolean signWithClientSecret) {
        if (encryptWithClientSecret) {
            throw new SecurityException();
        }
        this.signWithClientSecret = signWithClientSecret;
    }
}
