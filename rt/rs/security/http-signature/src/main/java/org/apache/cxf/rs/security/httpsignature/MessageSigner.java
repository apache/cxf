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
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

public class MessageSigner {
    private final String digestAlgorithmName;
    private SignatureCreator signatureCreator;

    public MessageSigner(String signatureAlgorithmName,
                         String digestAlgorithmName,
                         PrivateKey privateKey,
                         String keyId) {
        this.digestAlgorithmName = Objects.requireNonNull(digestAlgorithmName);
        this.signatureCreator = new TomitribeSignatureCreator(
                Objects.requireNonNull(signatureAlgorithmName),
                Objects.requireNonNull(privateKey),
                Objects.requireNonNull(keyId));
    }

    public MessageSigner(PrivateKey privateKey,
                         String keyId) {
        this(DefaultSignatureConstants.SIGNING_ALGORITHM,
                DefaultSignatureConstants.DIGEST_ALGORITHM,
                privateKey,
                keyId);
    }

    public void sign(Map<String, List<String>> messageHeaders,
                     String uri,
                     String method) throws IOException {
        SignatureHeaderUtils.inspectMessageHeaders(messageHeaders);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(method);

        messageHeaders.put("Signature", Collections.singletonList(signatureCreator.createSignature(messageHeaders,
                uri,
                method
        )));
    }

    public void sign(Map<String, List<String>> messageHeaders,
                     String uri,
                     String method,
                     String messageBody) throws IOException {
        SignatureHeaderUtils.inspectMessageHeaders(messageHeaders);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(method);
        Objects.requireNonNull(messageBody);

        messageHeaders.put("Digest",
                Collections.singletonList(SignatureHeaderUtils
                        .createDigestHeader(messageBody, digestAlgorithmName)));

        messageHeaders.put("Signature",
                Collections.singletonList(signatureCreator.createSignature(messageHeaders,
                        uri,
                        method
        )));
    }

}
