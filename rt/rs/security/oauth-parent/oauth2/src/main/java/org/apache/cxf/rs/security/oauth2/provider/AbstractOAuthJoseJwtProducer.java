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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public abstract class AbstractOAuthJoseJwtProducer extends AbstractJoseJwtProducer {
    private boolean encryptWithClientCertificates;
    private boolean encryptWithClientSecret;
    private boolean signWithClientSecret;
    
    protected JwsSignatureProvider getInitializedSigProvider(Client c, boolean required) {
        if (signWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(c.getClientSecret());
            return JwsUtils.getHmacSignatureProvider(hmac, SignatureAlgorithm.HS256);
        } 
        return super.getInitializedSignatureProvider(required);
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider(Client c, boolean required) {
        JweEncryptionProvider theEncryptionProvider = null;
        if (encryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(c.getClientSecret());
            theEncryptionProvider = JweUtils.getDirectKeyJweEncryption(key, ContentAlgorithm.A128GCM);
        } else if (encryptWithClientCertificates) {
            X509Certificate cert = 
                (X509Certificate)CryptoUtils.decodeCertificate(c.getApplicationCertificates().get(0));
            theEncryptionProvider = JweUtils.createJweEncryptionProvider((RSAPublicKey)cert.getPublicKey(), 
                                                                         KeyAlgorithm.RSA_OAEP, 
                                                                         ContentAlgorithm.A128GCM, 
                                                                         null);
        }
        if (theEncryptionProvider == null) {
            theEncryptionProvider = super.getInitializedEncryptionProvider(required);
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
