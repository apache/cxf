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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;
import org.tomitribe.auth.signatures.Signature;

public class TomitribeSignatureCreator implements SignatureCreator {
    private final String signatureAlgorithmName;
    private final PrivateKey privateKey;
    private final String keyId;

    public TomitribeSignatureCreator(String signatureAlgorithmName, PrivateKey privateKey, String keyId) {
        this.signatureAlgorithmName = signatureAlgorithmName;
        this.privateKey = privateKey;
        this.keyId = keyId;
    }

    @Override
    public String createSignature(Map<String, List<String>> messageHeaders, String uri, String method)
            throws IOException {
        if (messageHeaders == null) {
            throw new IllegalArgumentException("message headers cannot be null");
        }

        List<String> headers = messageHeaders.keySet().stream().map(String::toLowerCase).collect(Collectors.toList());

        headers.add("(request-target)");

        if (keyId == null) {
            throw new IllegalArgumentException("key id cannot be null");
        }

        final Signature signature = new Signature(keyId, signatureAlgorithmName, null, headers);
        final org.tomitribe.auth.signatures.Signer signer =
                new org.tomitribe.auth.signatures.Signer(privateKey, signature);
        return signer.sign(method, uri, SignatureHeaderUtils.mapHeaders(messageHeaders)).toString();
    }

}
