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
package org.apache.cxf.rs.security.oauth2.jwe;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;

public class DirectKeyJweEncryption extends AbstractJweEncryption {
    public DirectKeyJweEncryption(SecretKey cek, byte[] iv) {
        this(new JweHeaders(Algorithm.toJwtName(cek.getAlgorithm(),
                                                cek.getEncoded().length * 8)), cek.getEncoded(), iv);
    }
    public DirectKeyJweEncryption(JweHeaders headers, byte[] cek, byte[] iv) {
        this(headers, new AesGcmContentEncryptionAlgorithm(cek, iv));
    }
    public DirectKeyJweEncryption(JweHeaders headers, ContentEncryptionAlgorithm ceAlgo) {
        super(headers, ceAlgo, new DirectKeyEncryptionAlgorithm());
    }
    protected byte[] getProvidedContentEncryptionKey() {
        return validateCek(super.getProvidedContentEncryptionKey());
    }
    private static byte[] validateCek(byte[] cek) {
        if (cek == null) {
            // to prevent the cek from being auto-generated which 
            // does not make sense for the direct key case
            throw new NullPointerException("CEK must not be null");
        }
        return cek;
    }
}
