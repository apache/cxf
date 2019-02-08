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
package org.apache.cxf.rs.security.jose.jwe;

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class AesGcmWrapKeyEncryptionAlgorithm extends AbstractWrapKeyEncryptionAlgorithm {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<>(
        Arrays.asList(KeyAlgorithm.A128GCMKW.getJwaName(),
                      KeyAlgorithm.A192GCMKW.getJwaName(),
                      KeyAlgorithm.A256GCMKW.getJwaName()));
    public AesGcmWrapKeyEncryptionAlgorithm(String encodedKey, KeyAlgorithm keyAlgoJwt) {
        this(CryptoUtils.decodeSequence(encodedKey), keyAlgoJwt);
    }
    public AesGcmWrapKeyEncryptionAlgorithm(byte[] keyBytes, KeyAlgorithm keyAlgoJwt) {
        this(CryptoUtils.createSecretKeySpec(keyBytes, AlgorithmUtils.AES),
             keyAlgoJwt);
    }
    public AesGcmWrapKeyEncryptionAlgorithm(SecretKey key, KeyAlgorithm keyAlgoJwt) {
        super(key, keyAlgoJwt, true, SUPPORTED_ALGORITHMS);
    }

    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        byte[] wrappedKeyAndTag = super.getEncryptedContentEncryptionKey(headers, cek);
        byte[] wrappedKey = new byte[wrappedKeyAndTag.length - 128 / 8];
        System.arraycopy(wrappedKeyAndTag, 0, wrappedKey, 0, wrappedKeyAndTag.length - 128 / 8);
        String encodedTag = Base64UrlUtility.encodeChunk(wrappedKeyAndTag,
                                                         wrappedKeyAndTag.length - 128 / 8, 128 / 8);
        headers.setHeader("tag", encodedTag);

        // Cleanup
        Arrays.fill(wrappedKeyAndTag, (byte) 0);
        return wrappedKey;
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweHeaders headers) {
        byte[] iv = CryptoUtils.generateSecureRandomBytes(96 / 8);
        String encodedIv = Base64UrlUtility.encode(iv);
        headers.setHeader("iv", encodedIv);
        return CryptoUtils.getContentEncryptionCipherSpec(128, iv);
    }
}
