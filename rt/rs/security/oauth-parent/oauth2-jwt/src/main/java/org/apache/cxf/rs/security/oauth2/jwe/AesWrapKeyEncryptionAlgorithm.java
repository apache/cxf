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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AesWrapKeyEncryptionAlgorithm extends AbstractWrapKeyEncryptionAlgorithm {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.A128KW.getJwtName(),
                      Algorithm.A192KW.getJwtName(),
                      Algorithm.A256KW.getJwtName()));
    public AesWrapKeyEncryptionAlgorithm(String encodedKey, String keyAlgoJwt) {    
        this(CryptoUtils.decodeSequence(encodedKey), keyAlgoJwt);
    }
    public AesWrapKeyEncryptionAlgorithm(byte[] keyBytes, String keyAlgoJwt) {
        this(CryptoUtils.createSecretKeySpec(keyBytes, Algorithm.toJavaName(keyAlgoJwt)),
             keyAlgoJwt);
    }
    public AesWrapKeyEncryptionAlgorithm(SecretKey key, String keyAlgoJwt) {
        super(key, keyAlgoJwt, SUPPORTED_ALGORITHMS);
    }
    
    
    
}
