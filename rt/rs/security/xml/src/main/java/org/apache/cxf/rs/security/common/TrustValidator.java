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
package org.apache.cxf.rs.security.common;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;

public class TrustValidator {
    public void validateTrust(Crypto crypto, X509Certificate cert, PublicKey publicKey)
        throws WSSecurityException {
        validateTrust(crypto, cert, publicKey, null);
    }

    public void validateTrust(Crypto crypto, X509Certificate cert, PublicKey publicKey,
                              Collection<Pattern> subjectCertConstraints)
        throws WSSecurityException {
        SignatureTrustValidator validator = new SignatureTrustValidator();
        RequestData data = new RequestData();
        data.setSigVerCrypto(crypto);
        data.setSubjectCertConstraints(subjectCertConstraints);

        Credential trustCredential = new Credential();
        trustCredential.setPublicKey(publicKey);
        if (cert != null) {
            trustCredential.setCertificates(new X509Certificate[]{cert});
        }
        validator.validate(trustCredential, data);
    }
}
