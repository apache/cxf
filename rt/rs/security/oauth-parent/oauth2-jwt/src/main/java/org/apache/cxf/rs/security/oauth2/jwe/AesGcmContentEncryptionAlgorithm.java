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

import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;


public class AesGcmContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {
    private static final int DEFAULT_IV_SIZE = 96;
    public AesGcmContentEncryptionAlgorithm() {
        this((byte[])null, null);
    }
    public AesGcmContentEncryptionAlgorithm(String encodedCek, String encodedIv) {
        this((byte[])CryptoUtils.decodeSequence(encodedCek), CryptoUtils.decodeSequence(encodedIv));
    }
    public AesGcmContentEncryptionAlgorithm(SecretKey key, byte[] iv) { 
        this(key.getEncoded(), iv);    
    }
    public AesGcmContentEncryptionAlgorithm(byte[] cek, byte[] iv) { 
        super(cek, iv);    
    }
    protected int getIvSize() { 
        return DEFAULT_IV_SIZE;
    }
}
