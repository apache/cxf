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

import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;

public class RSAJweEncryption extends WrappedKeyJweEncryption {
    public RSAJweEncryption(RSAPublicKey publicKey, 
                            String keyEncryptionJwtAlgo,
                            String contentEncryptionJwtAlgo) {
        super(new JweHeaders(keyEncryptionJwtAlgo,
                             contentEncryptionJwtAlgo), 
              new RSAOaepKeyEncryptionAlgorithm(publicKey, keyEncryptionJwtAlgo));
    }
    public RSAJweEncryption(RSAPublicKey publicKey, JweHeaders headers, byte[] cek, byte[] iv) {
        this(publicKey, headers, cek, iv, true, null);
    }
    public RSAJweEncryption(RSAPublicKey publicKey, 
                            String keyEncryptionJwtAlgo,
                            SecretKey secretKey, 
                            String secretKeyJwtAlgo,
                            byte[] iv) {
        this(publicKey, 
             new JweHeaders(keyEncryptionJwtAlgo, secretKeyJwtAlgo),
             secretKey != null ? secretKey.getEncoded() : null, iv, true, null);
    }
    
    public RSAJweEncryption(RSAPublicKey publicKey, 
                            JweHeaders headers, 
                            byte[] cek, 
                            byte[] iv, 
                            boolean wrap,
                            JwtHeadersWriter writer) {
        this(new RSAOaepKeyEncryptionAlgorithm(publicKey, wrap), headers, cek, iv, writer);
    }
    public RSAJweEncryption(RSAOaepKeyEncryptionAlgorithm keyEncryptionAlgorithm, JweHeaders headers, byte[] cek, 
                            byte[] iv, JwtHeadersWriter writer) {
        super(headers, cek, iv, keyEncryptionAlgorithm, writer);
    }
    
}
