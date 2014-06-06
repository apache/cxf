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

import java.security.Key;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public class WrappedKeyJweEncryptor extends AbstractJweEncryptor {
    private Key cekEncryptionKey;
    private boolean wrap;
    public WrappedKeyJweEncryptor(JweHeaders headers, Key cekEncryptionKey) {
        this(headers, cekEncryptionKey, null, null);
    }
    public WrappedKeyJweEncryptor(JweHeaders headers, Key cekEncryptionKey, byte[] cek, byte[] iv) {
        this(headers, cekEncryptionKey, cek, iv, DEFAULT_AUTH_TAG_LENGTH, true);
    }
    public WrappedKeyJweEncryptor(JweHeaders headers, Key cekEncryptionKey, byte[] cek, byte[] iv, 
                                   int authTagLen, boolean wrap) {
        this(headers, cekEncryptionKey, cek, iv, authTagLen, wrap, null);
    }
    
    public WrappedKeyJweEncryptor(JweHeaders headers, Key cekEncryptionKey, byte[] cek, byte[] iv, int authTagLen, 
                                   boolean wrap, JwtHeadersWriter writer) {
        super(headers, cek, iv, authTagLen, writer);
        this.cekEncryptionKey = cekEncryptionKey;
        this.wrap = wrap;
    }
    protected byte[] getContentEncryptionKey() {
        byte[] theCek = super.getContentEncryptionKey();
        if (theCek == null) {
            String algo = getContentEncryptionAlgo();
            theCek = CryptoUtils.getSecretKey(algo, Algorithm.valueOf(algo).getKeySizeBits()).getEncoded();
        }
        return theCek;
    }
    protected byte[] getEncryptedContentEncryptionKey(byte[] theCek) {
        KeyProperties secretKeyProperties = new KeyProperties(getContentEncryptionKeyEncryptionAlgo());
        if (!wrap) {
            return CryptoUtils.encryptBytes(theCek, cekEncryptionKey, secretKeyProperties);
        } else {
            return CryptoUtils.wrapSecretKey(theCek, getContentEncryptionAlgo(), cekEncryptionKey, 
                                             secretKeyProperties.getKeyAlgo());
        }
    }
    protected String getContentEncryptionKeyEncryptionAlgo() {
        return Algorithm.toJavaName(getJweHeaders().getKeyEncryptionAlgorithm());
    }
}
