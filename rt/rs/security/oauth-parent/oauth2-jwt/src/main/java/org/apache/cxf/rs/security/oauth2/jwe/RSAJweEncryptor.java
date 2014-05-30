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

import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;

public class RSAJweEncryptor extends JweEncryptor {
    public RSAJweEncryptor(RSAPublicKey publicKey, String contentEncryptionAlgo) {
        super(new JweHeaders(Algorithm.RSA_OAEP_ALGO.getJwtName(),
                             contentEncryptionAlgo), publicKey);
    }
    public RSAJweEncryptor(RSAPublicKey publicKey, JweHeaders headers, byte[] cek, byte[] iv) {
        this(publicKey, headers, cek, iv, DEFAULT_AUTH_TAG_LENGTH, true);
    }
    public RSAJweEncryptor(RSAPublicKey publicKey, SecretKey secretKey, String secretKeyJwtAlgorithm,
                           byte[] iv) {
        this(publicKey, 
             new JweHeaders(Algorithm.RSA_OAEP_ALGO.getJwtName(), secretKeyJwtAlgorithm),
             secretKey.getEncoded(), iv, DEFAULT_AUTH_TAG_LENGTH, true);
    }
    
    public RSAJweEncryptor(RSAPublicKey publicKey, JweHeaders headers, byte[] cek, byte[] iv, 
                           int authTagLen, boolean wrap) {
        this(publicKey, headers, cek, iv, authTagLen, wrap, null);
    }
    
    public RSAJweEncryptor(RSAPublicKey publicKey, JweHeaders headers, byte[] cek, byte[] iv, 
                              JwtHeadersWriter writer) {
        this(publicKey, headers, cek, iv, DEFAULT_AUTH_TAG_LENGTH, true, null);
    }
    public RSAJweEncryptor(RSAPublicKey publicKey, JweHeaders headers, byte[] cek, byte[] iv, 
                              int authTagLen, boolean wrap, JwtHeadersWriter writer) {
        super(headers, publicKey, cek, iv, authTagLen, wrap, writer);
    }
    
}
