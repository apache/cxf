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
package org.apache.cxf.rs.security.jose.jwk;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JwkUtilsTest {
    private static final String RSA_KEY = "{"
      + "\"kty\": \"RSA\","
      + "\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
      +      "VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn6"
      +      "4tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FD"
      +      "W2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n9"
      +      "1CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINH"
      +      "aQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
      + "\"e\": \"AQAB\","
      + "\"alg\": \"RS256\","
      + "\"kid\": \"2011-04-29\""
      + "}";
    private static final String EC_256_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"CEuRLUISufhcjrj-32N0Bvl3KPMiHH9iSw4ohN9jxrA\","
        + "\"y\": \"EldWz_iXSK3l_S7n4w_t3baxos7o9yqX0IjzG959vHc\","
        + "\"crv\": \"P-256\""
        + "}";
    private static final String EC_384_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"2jCG5DmKUql9YPn7F2C-0ljWEbj8O8-vn5Ih1k7Wzb-y3NpBLiG1BiRa392b1kcQ\","
        + "\"y\": \"7Ragi9rT-5tSzaMbJlH_EIJl6rNFfj4V4RyFM5U2z4j1hesX5JXa8dWOsE-5wPIl\","
        + "\"crv\": \"P-384\""
        + "}";
    private static final String EC_521_KEY = "{"
        + "\"kty\": \"EC\","
        + "\"x\": \"Aeq3uMrb3iCQEt0PzSeZMmrmYhsKP5DM1oMP6LQzTFQY9-F3Ab45xiK4AJxltXEI-87g3gRwId88hTyHgq180JDt\","
        + "\"y\": \"ARA0lIlrZMEzaXyXE4hjEkc50y_JON3qL7HSae9VuWpOv_2kit8p3pyJBiRb468_U5ztLT7FvDvtimyS42trhDTu\","
        + "\"crv\": \"P-521\""
        + "}";
    private static final String OCTET_KEY_1 = "{"
        + "\"kty\": \"oct\","
        + "\"k\": \"ZW8Eg8TiwoT2YamLJfC2leYpLgLmUAh_PcMHqRzBnMg\""
        + "}";
    private static final String OCTET_KEY_2 = "{"
        + "\"kty\": \"oct\","
        + "\"k\": \"NGbwp1rC4n85A1SaNxoHow\""
        + "}";
    @Test
    public void testRsaKeyModulus() throws Exception {
        JsonWebKey jwk = JwkUtils.readJwkKey(RSA_KEY);
        String modulus = jwk.getStringProperty(JsonWebKey.RSA_MODULUS);
        assertEquals(256, JoseUtils.decode(modulus).length);

        RSAPublicKey pk = JwkUtils.toRSAPublicKey(jwk);
        JsonWebKey jwk2 = JwkUtils.fromRSAPublicKey(pk, jwk.getAlgorithm());
        String modulus2 = jwk2.getStringProperty(JsonWebKey.RSA_MODULUS);
        assertEquals(256, JoseUtils.decode(modulus2).length);
        assertEquals(modulus2, modulus);
    }
    @Test
    public void testFromToPrivateRsaKey() throws Exception {
        RSAPrivateKey privateKey1 =
            (RSAPrivateKey)KeyManagementUtils.loadPrivateKey("org/apache/cxf/rs/security/jose/jws/alice.jks",
                                              "password",
                                              "alice",
                                              "password",
                                              null);
        JsonWebKey jwk1 = JwkUtils.fromRSAPrivateKey(privateKey1, KeyAlgorithm.RSA_OAEP_256.getJwaName());
        assertNotNull(jwk1.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertNotNull(jwk1.getProperty(JsonWebKey.RSA_PRIVATE_EXP));
        RSAPrivateKey privateKey2 = JwkUtils.toRSAPrivateKey(jwk1);
        assertEquals(privateKey2, privateKey1);

    }
    @Test
    public void testFromToPublicRsaKey() throws Exception {
        RSAPublicKey publicKey1 =
            (RSAPublicKey)KeyManagementUtils.loadPublicKey("org/apache/cxf/rs/security/jose/jws/alice.jks",
                                              "password",
                                              "alice",
                                              null);
        JsonWebKey jwk1 = JwkUtils.fromRSAPublicKey(publicKey1, KeyAlgorithm.RSA_OAEP_256.getJwaName());
        assertNotNull(jwk1.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertNull(jwk1.getProperty(JsonWebKey.RSA_PRIVATE_EXP));
        RSAPublicKey publicKey2 = JwkUtils.toRSAPublicKey(jwk1);
        assertEquals(publicKey2, publicKey1);

    }
    @Test
    public void testFromToPublicRsaKey2() throws Exception {
        BigInteger n = new BigInteger(
            "525569531153621228164069013206963023039121751335221395180741421479892725873020691336158448746650762107595"
            + "8352148531548486906896903886764928450353366890712125983926472500064566992690642117517954169974907061547"
            + "3353190040609042090075291281955112293781438730376121249764205272939686534594208819023639183157456093565"
            + "4148815673814517535941780340023556224072529306118783149589148262622268860151306096159642808944513667279"
            + "4704664637866917427597486905443676772669967766269923280637049233876979061993814679654208850149406432368"
            + "2161337544093644200063709176660451323844399667162451308704624790051211834667782115390754507376506824717"
            + "9938484919159962066058375588059543574624283546151162925649987580839763809787286157381728046746195701379"
            + "0902293850442561995774628930418082115864728330723111110174368232384797709242627319756376556142528218939"
            + "7783875183123336240582938265783686836202210705597100765098627429017295706176890505466946207401105614189"
            + "2784165813507235148683348014201150784998715061575093867666453332433607035581378251824779499939486011300"
            + "7245546797308586043310145338620953330797301627631794650975659295961069452157705404946866414340860434286"
            + "65874725802069389719375237126155948350679342167596471110676954951640992376889874630989205394080379", 
            10);
        BigInteger e = new BigInteger("65537", 10);
        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(n, e);
        
        JsonWebKey jwk1 = JwkUtils.fromRSAPublicKey(publicKey, KeyAlgorithm.RSA_OAEP_256.getJwaName());
        assertNotNull(jwk1.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertNull(jwk1.getProperty(JsonWebKey.RSA_PRIVATE_EXP));
        RSAPublicKey privateKey2 = JwkUtils.toRSAPublicKey(jwk1);
        assertEquals(privateKey2, publicKey);

    }
    @Test
    public void testToPrivateRsaKeyWithoutE() throws Exception {
        RSAPrivateKey privateKey1 =
            (RSAPrivateKey)KeyManagementUtils.loadPrivateKey("org/apache/cxf/rs/security/jose/jws/alice.jks",
                                              "password",
                                              "alice",
                                              "password",
                                              null);
        JsonWebKey jwk1 = JwkUtils.fromRSAPrivateKey(privateKey1, KeyAlgorithm.RSA_OAEP_256.getJwaName());
        assertNotNull(jwk1.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        jwk1.asMap().remove(JsonWebKey.RSA_PUBLIC_EXP);
        try {
            JwkUtils.toRSAPrivateKey(jwk1);
            fail("JWK without the public exponent can not be converted to RSAPrivateKey");
        } catch (JoseException ex) {
            // expected
        }
    }
    @Test
    public void testRsaKeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(RSA_KEY);
        assertEquals("NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs", thumbprint);
    }
    @Test
    public void testOctetKey1Thumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(OCTET_KEY_1);
        assertEquals("7WWD36NF4WCpPaYtK47mM4o0a5CCeOt01JXSuMayv5g", thumbprint);
    }
    @Test
    public void testOctetKey2Thumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(OCTET_KEY_2);
        assertEquals("5_qb56G0OJDw-lb5mkDaWS4MwuY0fatkn9LkNqUHqMk", thumbprint);
    }
    @Test
    public void testEc256KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_256_KEY);
        assertEquals("j4UYwo9wrtllSHaoLDJNh7MhVCL8t0t8cGPPzChpYDs", thumbprint);
    }
    @Test
    public void testEc384KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_384_KEY);
        assertEquals("vZtaWIw-zw95JNzzURg1YB7mWNLlm44YZDZzhrPNetM", thumbprint);
    }
    @Test
    public void testEc521KeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(EC_521_KEY);
        assertEquals("rz4Ohmpxg-UOWIWqWKHlOe0bHSjNUFlHW5vwG_M7qYg", thumbprint);
    }
    
    @Test
    public void testLoadPublicJwkSet() throws Exception {
        final Properties props = new Properties();
        props.setProperty(JoseConstants.RSSEC_KEY_STORE_FILE, "unavailable");
        try {
            JwkUtils.loadPublicJwkSet(null, props);
            fail();
        } catch (JwkException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testEcLeadingZeros() throws Exception {
        try (InputStream inputStream = this.getClass().getResourceAsStream("cert.pem")) {
            ECPublicKey publicKey = (ECPublicKey) CertificateFactory.getInstance("X.509")
                    .generateCertificate(inputStream).getPublicKey();
            JsonWebKey jwk = JwkUtils.fromECPublicKey(publicKey, "P-256");
            String x = (String)jwk.getProperty(JsonWebKey.EC_X_COORDINATE);
            String y = (String)jwk.getProperty(JsonWebKey.EC_Y_COORDINATE);
            int xLength = Base64UrlUtility.decode(x).length;
            int yLength = Base64UrlUtility.decode(y).length;
            assertEquals(xLength, yLength);
        }
    }

}