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
import java.security.interfaces.RSAPrivateKey;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenReaderWriter;


public class JweJwtCompactConsumer  {
    private JweCompactConsumer jweConsumer;
    private JweHeaders headers;
    public JweJwtCompactConsumer(String content) {
        jweConsumer = new JweCompactConsumer(content);
        headers = jweConsumer.getJweHeaders();
    }
    public JwtToken decryptWith(JsonWebKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key, 
                               headers.getContentEncryptionAlgorithm().getJwaName()));
    }
    public JwtToken decryptWith(RSAPrivateKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key, 
                               headers.getKeyEncryptionAlgorithm().getJwaName(),
                               headers.getContentEncryptionAlgorithm().getJwaName()));
    }
    public JwtToken decryptWith(SecretKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key, 
                               headers.getKeyEncryptionAlgorithm().getJwaName(),
                               headers.getContentEncryptionAlgorithm().getJwaName()));
    }
    public JwtToken decryptWith(JweDecryptionProvider jwe) {
        byte[] bytes = jwe.decrypt(jweConsumer.getJweDecryptionInput());
        JwtClaims claims = new JwtTokenReaderWriter().fromJsonClaims(toString(bytes));
        return new JwtToken(headers, claims);
    }
    private static String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
