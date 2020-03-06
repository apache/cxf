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
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;


public class JweJwtCompactConsumer  {
    private final JweCompactConsumer jweConsumer;
    private final JweHeaders headers;

    public JweJwtCompactConsumer(String content) {
        jweConsumer = new JweCompactConsumer(content);
        headers = jweConsumer.getJweHeaders();
    }
    public JwtToken decryptWith(JsonWebKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key,
                               headers.getContentEncryptionAlgorithm()));
    }
    public JwtToken decryptWith(PrivateKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key,
                               headers.getKeyEncryptionAlgorithm(),
                               headers.getContentEncryptionAlgorithm()));
    }
    public JwtToken decryptWith(SecretKey key) {
        return decryptWith(JweUtils.createJweDecryptionProvider(key,
                               headers.getKeyEncryptionAlgorithm(),
                               headers.getContentEncryptionAlgorithm()));
    }
    public JwtToken decryptWith(JweDecryptionProvider jwe) {
        byte[] bytes = jwe.decrypt(jweConsumer.getJweDecryptionInput());
        JwtClaims claims = JwtUtils.jsonToClaims(new String(bytes, StandardCharsets.UTF_8));
        return new JwtToken(headers, claims);
    }

    public JweHeaders getHeaders() {
        return headers;
    }
}
