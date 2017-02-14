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

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class OAuthServerJoseJwtConsumer extends OAuthJoseJwtConsumer {
    private boolean verifyWithClientCertificates;

    public JwtToken getJwtToken(String wrappedJwtToken, Client client) {
        return getJwtToken(wrappedJwtToken,
                           getInitializedDecryptionProvider(client),
                           getInitializedSignatureVerifier(client));
    }

    protected JweDecryptionProvider getInitializedDecryptionProvider(Client c) {
        if (c == null) {
            return null;
        }
        return super.getInitializedDecryptionProvider(c.getClientSecret());
    }

    protected JwsSignatureVerifier getInitializedSignatureVerifier(Client c) {
        JwsSignatureVerifier theSignatureVerifier = null;
        if (verifyWithClientCertificates && c != null && !c.getApplicationCertificates().isEmpty()) {
            X509Certificate cert =
                (X509Certificate)CryptoUtils.decodeCertificate(c.getApplicationCertificates().get(0));
            theSignatureVerifier = JwsUtils.getPublicKeySignatureVerifier(cert.getPublicKey(),
                                                                          SignatureAlgorithm.RS256);
        }
        if (theSignatureVerifier == null && c != null && c.getClientSecret() != null) {
            theSignatureVerifier = super.getInitializedSignatureVerifier(c.getClientSecret());
        }
        return theSignatureVerifier;
    }

    public void setVerifyWithClientCertificates(boolean verifyWithClientCertificates) {
        if (isVerifyWithClientSecret()) {
            throw new SecurityException();
        }
        this.verifyWithClientCertificates = verifyWithClientCertificates;
    }

}
