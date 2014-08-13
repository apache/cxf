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

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;


public abstract class AbstractContentEncryptionAlgorithm extends AbstractContentEncryptionCipherProperties
    implements ContentEncryptionAlgorithm {
    private static final int DEFAULT_IV_SIZE = 128;
    private byte[] cek;
    private byte[] iv;
    private AtomicInteger providedIvUsageCount;
    protected AbstractContentEncryptionAlgorithm(SecretKey key, byte[] iv) { 
        this(key.getEncoded(), iv);    
    }
    protected AbstractContentEncryptionAlgorithm(byte[] cek, byte[] iv) { 
        this.cek = cek;
        this.iv = iv;
        if (iv != null && iv.length > 0) {
            providedIvUsageCount = new AtomicInteger();
        }    
    }
    
    public byte[] getContentEncryptionKey(JweHeaders headers) {
        return cek;
    }
    public byte[] getInitVector() {
        if (iv == null) {
            return CryptoUtils.generateSecureRandomBytes(getIvSize() / 8);
        } else if (iv.length > 0 && providedIvUsageCount.addAndGet(1) > 1) {
            throw new SecurityException();
        } else {
            return iv;
        }
    }
    protected int getIvSize() { 
        return DEFAULT_IV_SIZE;
    }
}
