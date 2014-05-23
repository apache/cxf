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
package org.apache.cxf.rs.security.oauth2.jws;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenWriter;
import org.apache.cxf.rs.security.oauth2.jwt.jwk.JsonWebKey;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

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
     
    @Test
    public void testWriteJwsSignedByMacSpecExample() throws Exception {
        JwtHeaders headers = new JwtHeaders(Algorithm.HmacSHA256.getJwtName());
        JwsCompactProducer jws = initSpecJwtTokenWriter(headers);
        jws.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY));
        
        assertEquals(ENCODED_TOKEN_SIGNED_BY_MAC, jws.getSignedEncodedToken());
        
    }
    
    @Test
    public void testWriteReadJwsUnsigned() throws Exception {
        JwtHeaders headers = new JwtHeaders(JwtConstants.PLAIN_TEXT_ALGO);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("https://jwt-idp.example.com");
        claims.setSubject("mailto:mike@example.com");
        claims.setAudience("https://jwt-rp.example.net");
        claims.setNotBefore(1300815780);
        claims.setExpiryTime(1300819380);
        claims.setClaim("http://claims.example.com/member", true);
        
        JwsCompactProducer writer = new JwsCompactProducer(headers, claims);
        String signed = writer.getSignedEncodedToken();
        
        JwsCompactConsumer reader = new JwsCompactConsumer(signed);
        assertEquals(0, reader.getDecodedSignature().length);
        
        JwtToken token = reader.getJwtToken();
        assertEquals(new JwtToken(headers, claims), token);
    }

    @Test
    public void testReadJwsSignedByMacSpecExample() throws Exception {
        JwsCompactConsumer jws = new JwsCompactConsumer(ENCODED_TOKEN_SIGNED_BY_MAC);
        assertTrue(jws.verifySignatureWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY)));
        JwtToken token = jws.getJwtToken();
        JwtHeaders headers = token.getHeaders();
        assertEquals(JwtConstants.TYPE_JWT, headers.getType());
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
        JwtHeaders headers = new JwtHeaders(Algorithm.HmacSHA256.getJwtName());
        
        headers.setHeader(JwtConstants.HEADER_JSON_WEB_KEY, jsonWebKey);
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("joe");
        claims.setExpiryTime(1300819380);
        claims.setClaim("http://example.com/is_root", Boolean.TRUE);
        
        JwtToken token = new JwtToken(headers, claims);
        JwsCompactProducer jws = new JwsCompactProducer(token, getWriter());
        jws.signWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY));
        
        assertEquals(ENCODED_TOKEN_WITH_JSON_KEY_SIGNED_BY_MAC, jws.getSignedEncodedToken());
    }
    
    @Test
    public void testReadJwsWithJwkSignedByMac() throws Exception {
        JwsCompactConsumer jws = new JwsCompactConsumer(ENCODED_TOKEN_WITH_JSON_KEY_SIGNED_BY_MAC);
        assertTrue(jws.verifySignatureWith(new HmacJwsSignatureProvider(ENCODED_MAC_KEY)));
        JwtToken token = jws.getJwtToken();
        JwtHeaders headers = token.getHeaders();
        assertEquals(JwtConstants.TYPE_JWT, headers.getType());
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
        assertEquals(Integer.valueOf(1300819380), claims.getExpiryTime());
        assertEquals(Boolean.TRUE, claims.getClaim("http://example.com/is_root"));
    }
    
    @Test
    public void testWriteReadJwsSignedByPrivateKey() throws Exception {
        JwtHeaders headers = new JwtHeaders();
        headers.setAlgorithm(Algorithm.SHA256withRSA.getJwtName());
        JwsCompactProducer jws = initSpecJwtTokenWriter(headers);
        PrivateKey key = CryptoUtils.getRSAPrivateKey(RSA_MODULUS_ENCODED, RSA_PRIVATE_EXPONENT_ENCODED);
        jws.signWith(new PrivateKeyJwsSignatureProvider(key));
        
        assertEquals(ENCODED_TOKEN_SIGNED_BY_PRIVATE_KEY, jws.getSignedEncodedToken());
    }
    
    @Test
    public void testReadJwsSignedByPrivateKey() throws Exception {
        JwsCompactConsumer jws = new JwsCompactConsumer(ENCODED_TOKEN_SIGNED_BY_PRIVATE_KEY);
        RSAPublicKey key = CryptoUtils.getRSAPublicKey(RSA_MODULUS_ENCODED, RSA_PUBLIC_EXPONENT_ENCODED);
        assertTrue(jws.verifySignatureWith(new PublicKeyJwsSignatureVerifier(key)));
        JwtToken token = jws.getJwtToken();
        JwtHeaders headers = token.getHeaders();
        assertEquals(Algorithm.SHA256withRSA.getJwtName(), headers.getAlgorithm());
        validateSpecClaim(token.getClaims());
    }
    
    private JwsCompactProducer initSpecJwtTokenWriter(JwtHeaders headers) throws Exception {
        
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("joe");
        claims.setExpiryTime(1300819380);
        claims.setClaim("http://example.com/is_root", Boolean.TRUE);
        
        JwtToken token = new JwtToken(headers, claims);
        return new JwsCompactProducer(token, getWriter());
    }

    
    private JwtTokenWriter getWriter() {
        JwtTokenReaderWriter jsonWriter = new JwtTokenReaderWriter();
        jsonWriter.setFormat(true);
        return jsonWriter;
    }
}
