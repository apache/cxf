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

import java.security.Key;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class DirectKeyDecryptionAlgorithm implements KeyDecryptionProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(DirectKeyDecryptionAlgorithm.class);
    private final byte[] contentDecryptionKey;

    public DirectKeyDecryptionAlgorithm(Key contentDecryptionKey) {
        this(contentDecryptionKey.getEncoded());
    }
    public DirectKeyDecryptionAlgorithm(String encodedContentDecryptionKey) {
        this(CryptoUtils.decodeSequence(encodedContentDecryptionKey));
    }
    public DirectKeyDecryptionAlgorithm(byte[] contentDecryptionKey) {
        this.contentDecryptionKey = contentDecryptionKey;
    }
    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        validateKeyEncryptionKey(jweDecryptionInput);
        return contentDecryptionKey.clone();
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.DIRECT;
    }
    protected void validateKeyEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        byte[] encryptedCEK = jweDecryptionInput.getEncryptedCEK();
        if (encryptedCEK != null && encryptedCEK.length > 0) {
            LOG.warning("Unexpected content encryption key");
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
    }
}