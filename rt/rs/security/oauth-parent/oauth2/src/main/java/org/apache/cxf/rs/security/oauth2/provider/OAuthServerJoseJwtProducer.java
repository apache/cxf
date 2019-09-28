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

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class OAuthServerJoseJwtProducer extends OAuthJoseJwtProducer {
    private boolean encryptWithClientCertificates;

    public String processJwt(JwtToken jwt, Client client) {
        return processJwt(jwt,
                         getInitializedEncryptionProvider(client),
                         getInitializedSignatureProvider(client));
    }

    protected JweEncryptionProvider getInitializedEncryptionProvider(Client c) {
        JweEncryptionProvider theEncryptionProvider = null;
        if (encryptWithClientCertificates && c != null && !c.getApplicationCertificates().isEmpty()) {
            X509Certificate cert =
                (X509Certificate)CryptoUtils.decodeCertificate(c.getApplicationCertificates().get(0));
            theEncryptionProvider = JweUtils.createJweEncryptionProvider(cert.getPublicKey(),
                                                                         KeyAlgorithm.RSA_OAEP,
                                                                         ContentAlgorithm.A128GCM,
                                                                         null);
        }
        if (theEncryptionProvider == null && c != null && c.getClientSecret() != null) {
            theEncryptionProvider = super.getInitializedEncryptionProvider(c.getClientSecret());
        }
        return theEncryptionProvider;

    }

    protected JwsSignatureProvider getInitializedSignatureProvider(Client c) {
        if (c == null) {
            return null;
        }
        return super.getInitializedSignatureProvider(c.getClientSecret());
    }

    public void setEncryptWithClientCertificates(boolean encryptWithClientCertificates) {
        if (isEncryptWithClientSecret()) {
            throw new SecurityException();
        }
        this.encryptWithClientCertificates = encryptWithClientCertificates;
    }

}
