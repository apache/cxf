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

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.DirectKeyJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

public final class JwtAccessTokenUtils {
    private JwtAccessTokenUtils() {
        
    }
    public static ServerAccessToken encryptToAccessToken(JwtToken jwt, 
                                                  Client client,
                                                  SecretKey key) {
        JweEncryptionProvider jweEncryption = 
            JweUtils.getDirectKeyJweEncryption(key, Algorithm.A128GCM.getJwtName());
        return encryptToAccessToken(jwt, client, jweEncryption);
        
    }
    public static ServerAccessToken encryptToAccessToken(JwtToken jwt, 
                                                  Client client,
                                                  JweEncryptionProvider jweEncryption) {
        String jwtString = new JwsJwtCompactProducer(jwt)
                               .signWith(new NoneSignatureProvider());
        String tokenId = jweEncryption.encrypt(getBytes(jwtString), null);
        return toAccessToken(jwt, client, tokenId);
    }
    private static ServerAccessToken toAccessToken(JwtToken jwt, 
                                                   Client client,
                                                   String tokenId) {
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
    public static JwtToken decryptFromfromAccessToken(String tokenId, SecretKey key) {
        DirectKeyJweDecryption jweDecryption = JweUtils.getDirectKeyJweDecryption(key, Algorithm.A128GCM.getJwtName());
        return decryptFromAccessToken(tokenId, jweDecryption);
    }
    public static JwtToken decryptFromAccessToken(String tokenId, JweDecryptionProvider jweDecryption) {
        String decrypted = jweDecryption.decrypt(tokenId).getContentText();
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(decrypted);
        return consumer.getJwtToken();
    }
    public static ServerAccessToken signToAccessToken(JwtToken jwt, 
                                                      Client client,
                                                      RSAPrivateKey key) {
        JwsSignatureProvider jws = 
            JwsUtils.getRSAKeySignatureProvider(key, JoseConstants.RS_SHA_256_ALGO);
        return signToAccessToken(jwt, client, jws);
       
    }
    public static ServerAccessToken signToAccessToken(JwtToken jwt, 
                                                      Client client,
                                                      JwsSignatureProvider jws) {
        String jwtString = new JwsJwtCompactProducer(jwt).signWith(jws);
        return toAccessToken(jwt, client, jwtString);
    }
    public static JwtToken verifyAccessToken(String tokenId, RSAPublicKey key) {
        JwsSignatureVerifier jws = JwsUtils.getRSAKeySignatureVerifier(key, JoseConstants.RS_SHA_256_ALGO);
        return verifyAccessToken(tokenId, jws);
    }
    public static JwtToken verifyAccessToken(String tokenId, JwsSignatureVerifier jws) {
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(tokenId);
        if (consumer.verifySignatureWith(jws)) {
            return consumer.getJwtToken();
        } else {
            throw new SecurityException();
        }
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
