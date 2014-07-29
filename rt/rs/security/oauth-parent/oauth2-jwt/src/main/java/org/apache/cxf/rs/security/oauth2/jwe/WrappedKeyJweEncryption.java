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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class WrappedKeyJweEncryption extends AbstractJweEncryption {
    private AtomicInteger providedCekUsageCount;
    public WrappedKeyJweEncryption(JweHeaders headers, 
                                   KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this(headers, null, null, keyEncryptionAlgorithm);
    }
    public WrappedKeyJweEncryption(JweHeaders headers, byte[] cek, 
                                   byte[] iv, KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this(headers, cek, iv, DEFAULT_AUTH_TAG_LENGTH, keyEncryptionAlgorithm, null);
    }
    public WrappedKeyJweEncryption(JweHeaders headers, 
                                   byte[] cek, 
                                   byte[] iv, 
                                   int authTagLen, 
                                   KeyEncryptionAlgorithm keyEncryptionAlgorithm,
                                   JwtHeadersWriter writer) {
        super(headers, cek, iv, authTagLen, keyEncryptionAlgorithm, writer);
        if (cek != null) {
            providedCekUsageCount = new AtomicInteger();
        }
    }
    protected byte[] getContentEncryptionKey() {
        byte[] theCek = super.getContentEncryptionKey();
        if (theCek == null) {
            String algoJava = getContentEncryptionAlgoJava();
            String algoJwt = getContentEncryptionAlgoJwt();
            theCek = CryptoUtils.getSecretKey(Algorithm.stripAlgoProperties(algoJava), 
                Algorithm.valueOf(algoJwt).getKeySizeBits()).getEncoded();
        } else if (providedCekUsageCount.addAndGet(1) > 1) {
            throw new SecurityException();
        }
        return theCek;
    }
    
}
