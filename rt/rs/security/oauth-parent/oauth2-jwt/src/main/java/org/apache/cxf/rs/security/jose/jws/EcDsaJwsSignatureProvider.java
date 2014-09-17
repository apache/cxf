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
package org.apache.cxf.rs.security.jose.jws;

import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class EcDsaJwsSignatureProvider extends PrivateKeyJwsSignatureProvider {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.SHA256withECDSA.getJwtName(),
                      Algorithm.SHA384withECDSA.getJwtName(),
                      Algorithm.SHA512withECDSA.getJwtName())); 
    
    public EcDsaJwsSignatureProvider(ECPrivateKey key) {
        this(key, null);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, AlgorithmParameterSpec spec) {
        this(key, null, spec);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, SecureRandom random, AlgorithmParameterSpec spec) {
        super(key, random, spec, SUPPORTED_ALGORITHMS);
    }
}
