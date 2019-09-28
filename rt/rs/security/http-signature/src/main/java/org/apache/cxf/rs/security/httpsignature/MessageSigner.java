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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cxf.rs.security.httpsignature.provider.KeyProvider;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

public class MessageSigner {
    private final SignatureCreator signatureCreator;

    public MessageSigner(String signatureAlgorithmName,
                         KeyProvider keyProvider,
                         String keyId) {
        this(signatureAlgorithmName, keyProvider, keyId, Collections.emptyList());
    }

    public MessageSigner(String signatureAlgorithmName,
                         KeyProvider keyProvider,
                         String keyId,
                         List<String> headersToSign) {
        this.signatureCreator = new TomitribeSignatureCreator(
                Objects.requireNonNull(signatureAlgorithmName),
                Objects.requireNonNull(keyProvider),
                Objects.requireNonNull(keyId),
                headersToSign);
    }

    public MessageSigner(KeyProvider keyProvider,
                         String keyId) {
        this(keyProvider, keyId, Collections.emptyList());
    }

    public MessageSigner(KeyProvider keyProvider,
                         String keyId,
                         List<String> headersToSign) {
        this(DefaultSignatureConstants.SIGNING_ALGORITHM,
                keyProvider,
                keyId,
                headersToSign);
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

}
