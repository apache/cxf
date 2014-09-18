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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;


public class AesGcmContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.A128GCM.getJwtName(),
                      Algorithm.A192GCM.getJwtName(),
                      Algorithm.A256GCM.getJwtName()));
    private static final int DEFAULT_IV_SIZE = 96;
    public AesGcmContentEncryptionAlgorithm(String algo) {
        this((byte[])null, null, algo);
    }
    public AesGcmContentEncryptionAlgorithm(String encodedCek, String encodedIv, String algo) {
        this((byte[])CryptoUtils.decodeSequence(encodedCek), CryptoUtils.decodeSequence(encodedIv), algo);
    }
    public AesGcmContentEncryptionAlgorithm(SecretKey key, byte[] iv, String algo) { 
        this(key.getEncoded(), iv, algo);    
    }
    public AesGcmContentEncryptionAlgorithm(byte[] cek, byte[] iv, String algo) { 
        super(cek, iv, checkAlgorithm(algo));    
    }
    protected int getIvSize() { 
        return DEFAULT_IV_SIZE;
    }
    private static String checkAlgorithm(String algo) {
        if (SUPPORTED_ALGORITHMS.contains(algo)) {       
            return algo;
        }
        throw new SecurityException();
    }
}