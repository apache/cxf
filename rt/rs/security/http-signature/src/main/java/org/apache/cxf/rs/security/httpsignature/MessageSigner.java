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
package org.apache.cxf.rs.security.httpsignature;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.tomitribe.auth.signatures.Signature;

public class MessageSigner {
    private final String digestAlgorithmName;
    private final String signatureAlgorithmName;

    /**
     * Message signer using standard digest and signing algorithm
     */
    public MessageSigner() {
        this(DefaultSignatureConstants.SIGNING_ALGORITHM, DefaultSignatureConstants.DIGEST_ALGORITHM);
    }

    public MessageSigner(String signatureAlgorithmName, String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
        this.signatureAlgorithmName = signatureAlgorithmName;
    }

    public void sign(Map<String, List<String>> messageHeaders,
                     String messageBody,
                     PrivateKey privateKey,
                     String keyId) throws NoSuchAlgorithmException, IOException {
        if (messageBody != null) {
            messageHeaders.put("Digest",
                    Collections.singletonList(SignatureHeaderUtils
                            .createDigestHeader(messageBody, digestAlgorithmName)));
        }

        String method = SignatureHeaderUtils.getMethod(messageHeaders);
        String uri = SignatureHeaderUtils.getUri(messageHeaders);

        messageHeaders.put("Signature", Collections.singletonList(createSignature(messageHeaders,
                privateKey,
                keyId,
                uri,
                method
        )));
    }

    private String createSignature(Map<String, List<String>> messageHeaders,
                                   PrivateKey privateKey,
                                   String keyId,
                                   String uri,
                                   String method) throws IOException {
        if (messageHeaders == null) {
            throw new IllegalArgumentException("Message headers cannot be null.");
        }

        String[] headerKeysForSignature =
                messageHeaders.keySet().stream().map(String::toLowerCase).toArray(String[]::new);

        if (headerKeysForSignature.length == 0) {
            throw new IllegalArgumentException("There has to be a value in the list of header keys for signature");
        }

        if (keyId == null) {
            throw new IllegalArgumentException("Key id cannot be null.");
        }

        final Signature signature = new Signature(keyId, signatureAlgorithmName, null, headerKeysForSignature);
        final org.tomitribe.auth.signatures.Signer signer =
                new org.tomitribe.auth.signatures.Signer(privateKey, signature);
        return signer.sign(method, uri, SignatureHeaderUtils.mapHeaders(messageHeaders)).toString();
    }
}
