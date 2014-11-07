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
package org.apache.cxf.rs.security.oauth2.utils.crypto;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.AesGcmContentDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesGcmContentEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.DirectKeyJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.DirectKeyJweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

public final class JwtAccessTokenUtils {
    private JwtAccessTokenUtils() {
        
    }
    
    public static ServerAccessToken toAccessToken(JwtToken jwt, 
                                                  Client client,
                                                  SecretKey key) {
        ContentEncryptionAlgorithm contentEncryption = 
            new AesGcmContentEncryptionAlgorithm(key, null, Algorithm.A128GCM.getJwtName());
        JweEncryptionProvider jweEncryption = new DirectKeyJweEncryption(contentEncryption);
        return toAccessToken(jwt, client, jweEncryption);
        
    }
    
    public static ServerAccessToken toAccessToken(JwtToken jwt, 
                                                  Client client,
                                                  JweEncryptionProvider jweEncryption) {
        String jwtString = new JwsJwtCompactProducer(jwt)
                               .signWith(new NoneSignatureProvider());
        String tokenId = jweEncryption.encrypt(getBytes(jwtString), null);
        Long issuedAt = jwt.getClaims().getIssuedAt();
        Long notBefore = jwt.getClaims().getNotBefore();
        if (issuedAt == null) {
            issuedAt = System.currentTimeMillis();
            notBefore = null;
        }
        Long expiresIn = null;
        if (notBefore == null) {
            expiresIn = 3600L;
        } else {
            expiresIn = notBefore - issuedAt;
        }
        
        return new BearerAccessToken(client, tokenId, issuedAt, expiresIn);
        
    }
    public static JwtToken fromAccessTokenId(String tokenId, SecretKey key) {
        DirectKeyJweDecryption jweDecryption = 
            new DirectKeyJweDecryption(key, 
                new AesGcmContentDecryptionAlgorithm(Algorithm.A128GCM.getJwtName()));
        return fromAccessTokenId(tokenId, jweDecryption);
    }
    public static JwtToken fromAccessTokenId(String tokenId, JweDecryptionProvider jweDecryption) {
        String decrypted = jweDecryption.decrypt(tokenId).getContentText();
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(decrypted);
        return consumer.getJwtToken();
    }
    private static class NoneSignatureProvider implements JwsSignatureProvider {

        @Override
        public String getAlgorithm() {
            return "none";
        }

        @Override
        public JwsSignature createJwsSignature(JoseHeaders headers) {
            return new NoneJwsSignature();
        }
        
    }
    private static class NoneJwsSignature implements JwsSignature {

        @Override
        public void update(byte[] src, int off, int len) {
            // complete
        }

        @Override
        public byte[] sign() {
            return new byte[]{};
        }
        
    }
    private static byte[] getBytes(String str) {
        return StringUtils.toBytesUTF8(str);
    }
}
