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

import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenWriter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.Assert;
import org.junit.Test;

public class JwsCompactReaderWriterTest extends Assert {
    
    public static final String ENCODED_TOKEN_SIGNED_BY_MAC = 
        "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9"
        + ".eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
        + ".dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    
    
    private static final String ENCODED_MAC_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    
    private static final String ENCODED_TOKEN_WITH_JSON_KEY_SIGNED_BY_MAC = 
        "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIU"
        + "zI1NiIsDQogImp3ayI6eyJrdHkiOiJvY3QiLA0KICJrZXlfb3BzIjpbDQogInNpZ24iLA0KICJ2ZXJpZnkiDQogXX19"
        + ".eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
        + ".8cFZqb15gEDYRZqSzUu23nQnKNynru1ADByRPvmmOq8";
    
    private static final String RSA_MODULUS_ENCODED = "ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddx"
        + "HmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMs"
        + "D1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSH"
        + "SXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdV"
        + "MTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8"
        + "NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ";
    private static final String RSA_PUBLIC_EXPONENT_ENCODED = "AQAB";
    private static final String RSA_PRIVATE_EXPONENT_ENCODED = 
        "Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97I"
        + "jlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0"
        + "BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn"
        + "439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYT"
        + "CBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLh"
        + "BOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ";
    private static final String ENCODED_TOKEN_SIGNED_BY_PRIVATE_KEY =
        "eyJhbGciOiJSUzI1NiJ9"
        + "."
        + "eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFt"
        + "cGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
        + "."
        + "cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7"
        + "AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4"
        + "BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K"
        + "0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqv"
        + "hJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrB"
        + "p0igcN_IoypGlUPQGe77Rw";
     
    private static final String EC_PRIVATE_KEY_ENCODED = 
        "jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI";
    private static final String EC_X_POINT_ENCODED = 
        "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU";
    private static final String EC_Y_POINT_ENCODED = 
        "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0";
    @Test
    public void testWriteJwsSignedByMacSpecExample() throws Exception {
        JoseHeaders headers = new JoseHeaders();
        headers.setType(JoseConstants.TYPE_JWT);
        headers.setAlgorithm(Algorithm.HmacSHA256.getJwtName());
        JwsCompactProducer jws = initSpecJwtTokenWriter(headers);
        jws.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY, Algorithm.HmacSHA256.getJwtName()));
        
        assertEquals(ENCODED_TOKEN_SIGNED_BY_MAC, jws.getSignedEncodedJws());
        
    }
    
    @Test
    public void testWriteReadJwsUnsigned() throws Exception {
        JoseHeaders headers = new JoseHeaders();
        headers.setType(JoseConstants.TYPE_JWT);
        headers.setAlgorithm(JoseConstants.PLAIN_TEXT_ALGO);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://jwt-idp.example.com");
        claims.setSubject("mailto:mike@example.com");
        claims.setAudience("https://jwt-rp.example.net");
        claims.setNotBefore(1300815780L);
        claims.setExpiryTime(1300819380L);
        claims.setClaim("http://claims.example.com/member", true);
        
        JwsCompactProducer writer = new JwsJwtCompactProducer(headers, claims);
        String signed = writer.getSignedEncodedJws();
        
        JwsJwtCompactConsumer reader = new JwsJwtCompactConsumer(signed);
        assertEquals(0, reader.getDecodedSignature().length);
        
        JwtToken token = reader.getJwtToken();
        assertEquals(new JwtToken(headers, claims), token);
    }

    @Test
    public void testReadJwsSignedByMacSpecExample() throws Exception {
        JwsJwtCompactConsumer jws = new JwsJwtCompactConsumer(ENCODED_TOKEN_SIGNED_BY_MAC);
        assertTrue(jws.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                                        Algorithm.HmacSHA256.getJwtName())));
        JwtToken token = jws.getJwtToken();
        JoseHeaders headers = token.getHeaders();
        assertEquals(JoseConstants.TYPE_JWT, headers.getType());
        assertEquals(Algorithm.HmacSHA256.getJwtName(), headers.getAlgorithm());
        validateSpecClaim(token.getClaims());
    }
    
    @Test
    public void testWriteJwsWithJwkSignedByMac() throws Exception {
        JsonWebKey key = new JsonWebKey();
        key.setKeyType(JsonWebKey.KEY_TYPE_OCTET);
        key.setKeyOperation(Arrays.asList(
            new String[]{JsonWebKey.KEY_OPER_SIGN, JsonWebKey.KEY_OPER_VERIFY}));
        doTestWriteJwsWithJwkSignedByMac(key);
    }
    
    @Test
    public void testWriteJwsWithJwkAsMapSignedByMac() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(JsonWebKey.KEY_TYPE, JsonWebKey.KEY_TYPE_OCTET);
        map.put(JsonWebKey.KEY_OPERATIONS,
                new String[]{JsonWebKey.KEY_OPER_SIGN, JsonWebKey.KEY_OPER_VERIFY});
        doTestWriteJwsWithJwkSignedByMac(map);
    }
    
    private void doTestWriteJwsWithJwkSignedByMac(Object jsonWebKey) throws Exception {
        JoseHeaders headers = new JoseHeaders();
        headers.setType(JoseConstants.TYPE_JWT);
        headers.setAlgorithm(Algorithm.HmacSHA256.getJwtName());
        headers.setHeader(JoseConstants.HEADER_JSON_WEB_KEY, jsonWebKey);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("joe");
        claims.setExpiryTime(1300819380L);
        claims.setClaim("http://example.com/is_root", Boolean.TRUE);
        
        JwtToken token = new JwtToken(headers, claims);
        JwsCompactProducer jws = new JwsJwtCompactProducer(token, getWriter());
        jws.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY, Algorithm.HmacSHA256.getJwtName()));
        
        assertEquals(ENCODED_TOKEN_WITH_JSON_KEY_SIGNED_BY_MAC, jws.getSignedEncodedJws());
    }
    
    @Test
    public void testReadJwsWithJwkSignedByMac() throws Exception {
        JwsJwtCompactConsumer jws = new JwsJwtCompactConsumer(ENCODED_TOKEN_WITH_JSON_KEY_SIGNED_BY_MAC);
        assertTrue(jws.verifySignatureWith(new HmacJwsSignatureVerifier(ENCODED_MAC_KEY,
                                                                        Algorithm.HmacSHA256.getJwtName())));
        JwtToken token = jws.getJwtToken();
        JoseHeaders headers = token.getHeaders();
        assertEquals(JoseConstants.TYPE_JWT, headers.getType());
        assertEquals(Algorithm.HmacSHA256.getJwtName(), headers.getAlgorithm());
        
        JsonWebKey key = headers.getJsonWebKey();
        assertEquals(JsonWebKey.KEY_TYPE_OCTET, key.getKeyType());
        List<String> keyOps = key.getKeyOperation();
        assertEquals(2, keyOps.size());
        assertEquals(JsonWebKey.KEY_OPER_SIGN, keyOps.get(0));
        assertEquals(JsonWebKey.KEY_OPER_VERIFY, keyOps.get(1));
        
        validateSpecClaim(token.getClaims());
    }
    
    private void validateSpecClaim(JwtClaims claims) {
        assertEquals("joe", claims.getIssuer());
        assertEquals(Long.valueOf(1300819380), claims.getExpiryTime());
        assertEquals(Boolean.TRUE, claims.getClaim("http://example.com/is_root"));
    }
    
    @Test
    public void testWriteJwsSignedByPrivateKey() throws Exception {
        JoseHeaders headers = new JoseHeaders();
        headers.setAlgorithm(Algorithm.SHA256withRSA.getJwtName());
        JwsCompactProducer jws = initSpecJwtTokenWriter(headers);
        PrivateKey key = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED, RSA_PRIVATE_EXPONENT_ENCODED);
        jws.signWith(new PrivateKeyJwsSignatureProvider(key, Algorithm.SHA256withRSA.getJwtName()));
        
        assertEquals(ENCODED_TOKEN_SIGNED_BY_PRIVATE_KEY, jws.getSignedEncodedJws());
    }
    @Test
    public void testJwsPsSha() throws Exception {
        Security.addProvider(new BouncyCastleProvider());    
        try {
            JoseHeaders outHeaders = new JoseHeaders();
            outHeaders.setAlgorithm(JoseConstants.PS_SHA_256_ALGO);
            JwsCompactProducer producer = initSpecJwtTokenWriter(outHeaders);
            PrivateKey privateKey = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED, RSA_PRIVATE_EXPONENT_ENCODED);
            String signed = producer.signWith(
                new PrivateKeyJwsSignatureProvider(privateKey, JoseConstants.PS_SHA_256_ALGO));
            
            JwsJwtCompactConsumer jws = new JwsJwtCompactConsumer(signed);
            RSAPublicKey key = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED, RSA_PUBLIC_EXPONENT_ENCODED);
            assertTrue(jws.verifySignatureWith(new PublicKeyJwsSignatureVerifier(key, 
                                                                                 JoseConstants.PS_SHA_256_ALGO)));
            JwtToken token = jws.getJwtToken();
            JoseHeaders inHeaders = token.getHeaders();
            assertEquals(JoseConstants.PS_SHA_256_ALGO, inHeaders.getAlgorithm());
            validateSpecClaim(token.getClaims());
        } finally {
            Security.removeProvider(BouncyCastleProvider.class.getName());
        }
    }
    
    @Test
    public void testWriteReadJwsSignedByESPrivateKey() throws Exception {
        JoseHeaders headers = new JoseHeaders();
        headers.setAlgorithm(Algorithm.SHA256withECDSA.getJwtName());
        JwsCompactProducer jws = initSpecJwtTokenWriter(headers);
        ECPrivateKey privateKey = CryptoUtils.getECPrivateKey(JsonWebKey.EC_CURVE_P256,
                                                              EC_PRIVATE_KEY_ENCODED);
        jws.signWith(new EcDsaJwsSignatureProvider(privateKey, Algorithm.SHA256withECDSA.getJwtName()));
        String signedJws = jws.getSignedEncodedJws();
        
        ECPublicKey publicKey = CryptoUtils.getECPublicKey(JsonWebKey.EC_CURVE_P256,
                                                           EC_X_POINT_ENCODED, 
                                                           EC_Y_POINT_ENCODED);
        JwsJwtCompactConsumer jwsConsumer = new JwsJwtCompactConsumer(signedJws);
        assertTrue(jwsConsumer.verifySignatureWith(new EcDsaJwsSignatureVerifier(publicKey,
                                                   Algorithm.SHA256withECDSA.getJwtName())));
        JwtToken token = jwsConsumer.getJwtToken();
        JoseHeaders headersReceived = token.getHeaders();
        assertEquals(Algorithm.SHA256withECDSA.getJwtName(), headersReceived.getAlgorithm());
        validateSpecClaim(token.getClaims());
    }
    
    @Test
    public void testReadJwsSignedByPrivateKey() throws Exception {
        JwsJwtCompactConsumer jws = new JwsJwtCompactConsumer(ENCODED_TOKEN_SIGNED_BY_PRIVATE_KEY);
        RSAPublicKey key = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED, RSA_PUBLIC_EXPONENT_ENCODED);
        assertTrue(jws.verifySignatureWith(new PublicKeyJwsSignatureVerifier(key, 
                                                                             JoseConstants.RS_SHA_256_ALGO)));
        JwtToken token = jws.getJwtToken();
        JoseHeaders headers = token.getHeaders();
        assertEquals(Algorithm.SHA256withRSA.getJwtName(), headers.getAlgorithm());
        validateSpecClaim(token.getClaims());
    }
    
    private JwsCompactProducer initSpecJwtTokenWriter(JoseHeaders headers) throws Exception {
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("joe");
        claims.setExpiryTime(1300819380L);
        claims.setClaim("http://example.com/is_root", Boolean.TRUE);
        
        JwtToken token = new JwtToken(headers, claims);
        return new JwsJwtCompactProducer(token, getWriter());
    }

    
    private JwtTokenWriter getWriter() {
        JwtTokenReaderWriter jsonWriter = new JwtTokenReaderWriter();
        jsonWriter.setFormat(true);
        return jsonWriter;
    }
}
