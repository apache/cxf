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

import java.security.interfaces.RSAPrivateKey;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;

public class RSAJweEncryptor extends JweEncryptor {
    public RSAJweEncryptor(RSAPrivateKey privateKey, JweHeaders headers, byte[] cek, byte[] iv) {
        this(privateKey, headers, cek, iv, 128, true);
    }
    public RSAJweEncryptor(RSAPrivateKey privateKey, SecretKey secretKey, byte[] iv) {
        this(privateKey, 
             new JweHeaders(Algorithm.RSA_OAEP_ALGO.getJwtName(),
                            Algorithm.toJwtName(secretKey.getAlgorithm())), 
             secretKey.getEncoded(), iv, 128, true);
    }
    
    public RSAJweEncryptor(RSAPrivateKey privateKey, JweHeaders headers, byte[] cek, byte[] iv, 
                           int authTagLen, boolean wrap) {
        this(privateKey, headers, cek, iv, authTagLen, wrap, null);
    }
    
    public RSAJweEncryptor(RSAPrivateKey privateKey, JweHeaders headers, byte[] cek, byte[] iv, 
                              JwtHeadersWriter writer) {
        this(privateKey, headers, cek, iv, 128, true, null);
    }
    public RSAJweEncryptor(RSAPrivateKey privateKey, JweHeaders headers, byte[] cek, byte[] iv, 
                              int authTagLen, boolean wrap, JwtHeadersWriter writer) {
        super(headers, privateKey, cek, iv, authTagLen, wrap, writer);
    }
    
}
