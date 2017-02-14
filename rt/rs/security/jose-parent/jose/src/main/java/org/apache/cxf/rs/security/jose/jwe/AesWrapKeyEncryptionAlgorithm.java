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

import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class AesWrapKeyEncryptionAlgorithm extends AbstractWrapKeyEncryptionAlgorithm {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<>(
        Arrays.asList(KeyAlgorithm.A128KW.getJwaName(),
                      KeyAlgorithm.A192KW.getJwaName(),
                      KeyAlgorithm.A256KW.getJwaName()));
    public AesWrapKeyEncryptionAlgorithm(String encodedKey, KeyAlgorithm keyAlgoJwt) {
        this(CryptoUtils.decodeSequence(encodedKey), keyAlgoJwt);
    }
    public AesWrapKeyEncryptionAlgorithm(byte[] keyBytes, KeyAlgorithm keyAlgoJwt) {
        this(CryptoUtils.createSecretKeySpec(keyBytes, keyAlgoJwt.getJavaName()),
             keyAlgoJwt);
    }
    public AesWrapKeyEncryptionAlgorithm(SecretKey key, KeyAlgorithm keyAlgoJwt) {
        super(key, keyAlgoJwt, SUPPORTED_ALGORITHMS);
    }



}
