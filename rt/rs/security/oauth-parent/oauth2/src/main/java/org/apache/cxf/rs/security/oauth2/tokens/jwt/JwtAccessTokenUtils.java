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
package org.apache.cxf.rs.security.oauth2.tokens.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
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
            JweUtils.getDirectKeyJweEncryption(key, ContentAlgorithm.A128GCM.getJwaName());
        return encryptToAccessToken(jwt, client, jweEncryption);
        
    }
    public static ServerAccessToken encryptToAccessToken(JwtToken jwt, 
                                                  Client client,
                                                  JweEncryptionProvider jweEncryption) {
        String jwtString = new JwsJwtCompactProducer(jwt)
                               .signWith(new NoneJwsSignatureProvider());
        String tokenId = jweEncryption.encrypt(getBytes(jwtString), null);
        return toAccessToken(jwt, client, tokenId);
    }
    private static ServerAccessToken toAccessToken(JwtToken jwt, 
                                                   Client client,
                                                   String tokenId) {
        JwtClaims claims = jwt.getClaims();
        validateJwtSubjectAndAudience(claims, client);
        Long issuedAt = claims.getIssuedAt();
        Long notBefore = claims.getNotBefore();
        Long expiresIn = notBefore - issuedAt;
        
        return new BearerAccessToken(client, tokenId, issuedAt, expiresIn);
    }
    public static JwtToken decryptFromfromAccessToken(String tokenId, SecretKey key) {
        JweDecryption jweDecryption = JweUtils.getDirectKeyJweDecryption(key, ContentAlgorithm.A128GCM.getJwaName());
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
            JwsUtils.getRSAKeySignatureProvider(key, AlgorithmUtils.RS_SHA_256_ALGO);
        return signToAccessToken(jwt, client, jws);
       
    }
    public static ServerAccessToken signToAccessToken(JwtToken jwt, 
                                                      Client client,
                                                      JwsSignatureProvider jws) {
        String jwtString = new JwsJwtCompactProducer(jwt).signWith(jws);
        return toAccessToken(jwt, client, jwtString);
    }
    public static JwtToken verifyAccessToken(String tokenId, RSAPublicKey key) {
        JwsSignatureVerifier jws = JwsUtils.getRSAKeySignatureVerifier(key, AlgorithmUtils.RS_SHA_256_ALGO);
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
    public static void validateJwtClaims(JwtClaims claims, Client c) {
        validateJwtSubjectAndAudience(claims, c);
        JwtUtils.validateJwtTimeClaims(claims);
    }
    
    private static void validateJwtSubjectAndAudience(JwtClaims claims, Client c) {
        if (claims.getSubject() == null || !claims.getSubject().equals(c.getClientId())) {
            throw new SecurityException("Invalid subject");
        }
        // validate audience
        String aud = claims.getAudience();
        if (aud == null 
            || !c.getRegisteredAudiences().isEmpty() && !c.getRegisteredAudiences().contains(aud)) {
            throw new SecurityException("Invalid audience");
        }
        // TODO: the issuer is indirectly validated by validating the signature
        // but an extra check can be done
    }
    
    private static byte[] getBytes(String str) {
        return StringUtils.toBytesUTF8(str);
    }
}
