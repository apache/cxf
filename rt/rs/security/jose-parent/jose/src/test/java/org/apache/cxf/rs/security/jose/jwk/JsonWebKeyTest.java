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
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonWebKeyTest {
    private static final String RSA_MODULUS_VALUE = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
        + "VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf"
        + "0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt"
        + "-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw";
    private static final String RSA_PUBLIC_EXP_VALUE = "AQAB";
    private static final String RSA_PRIVATE_EXP_VALUE = "X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9M7d"
        + "x5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqijwp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ4"
        + "6pRUohsXywbReAdYaMwFs9tv8d_cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBznbJSzFHK66"
        + "jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFzme1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q";
    private static final String RSA_FIRST_PRIME_FACTOR_VALUE = "83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQ"
        + "BQxtPVnwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqVWlWEh6dN36GVZYk93N8Bc9vY41xy8B9"
        + "RzzOGVQzXvNEvn7O0nVbfs";
    private static final String RSA_SECOND_PRIME_FACTOR_VALUE = "3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3"
        + "vobLyumqjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgxkIdrecRezsZ-1kYd_s1qDbxtkDEgfA"
        + "ITAG9LUnADun4vIcb6yelxk";
    private static final String RSA_FIRST_PRIME_CRT_VALUE = "G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0o"
        + "imYwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2VF402XRugKDTP8akYhFo5tAA77Qe_NmtuYZc3C3m3I24G2GvR5sSDxUyAN2zq8Lfn9EUm"
        + "s6rY3Ob8YeiKkTiBj0";
    private static final String RSA_SECOND_PRIME_CRT_VALUE = "s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6hu"
        + "UUvMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvW"
        + "rX-L18txXw494Q_cgk";
    private static final String RSA_FIRST_CRT_COEFFICIENT_VALUE = "GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfm"
        + "t0FoYzgUIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rxyR8O55XLSe3SPmRfKwZI6yU24ZxvQKF"
        + "YItdldUKGzO6Ia6zTKhAVRU";
    private static final String RSA_KID_VALUE = "2011-04-29";
    private static final String EC_CURVE_VALUE = JsonWebKey.EC_CURVE_P256;
    private static final String EC_X_COORDINATE_VALUE = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4";
    private static final String EC_Y_COORDINATE_VALUE = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM";
    private static final String EC_PRIVATE_KEY_VALUE = "870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE";
    private static final String EC_KID_VALUE = "1";
    private static final String AES_SECRET_VALUE = "GawgguFyGrWKav7AX4VKUg";
    private static final String AES_KID_VALUE = "AesWrapKey";
    private static final String HMAC_SECRET_VALUE = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3"
        + "Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final String HMAC_KID_VALUE = "HMACKey";

    @Test
    public void testPublicSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("jwkPublicSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        assertEquals(3, keys.size());

        JsonWebKey ecKey = keys.get(0);
        assertEquals(6, ecKey.asMap().size());
        validatePublicEcKey(ecKey);
        JsonWebKey rsaKey = keys.get(1);
        assertEquals(5, rsaKey.asMap().size());
        validatePublicRsaKey(rsaKey);
        JsonWebKey rsaKeyCert = keys.get(2);
        assertEquals(3, rsaKeyCert.asMap().size());
        assertEquals(3, rsaKeyCert.getX509Chain().size());
        List<X509Certificate> certs = JwkUtils.toX509CertificateChain(rsaKeyCert);
        assertEquals(3, certs.size());
    }

    @Test
    public void testPublicSetAsMap() throws Exception {
        JsonWebKeys jwks = readKeySet("jwkPublicSet.txt");
        Map<String, JsonWebKey> keysMap = jwks.getKeyIdMap();
        assertEquals(3, keysMap.size());

        JsonWebKey rsaKey = keysMap.get(RSA_KID_VALUE);
        assertEquals(5, rsaKey.asMap().size());
        validatePublicRsaKey(rsaKey);
        JsonWebKey ecKey = keysMap.get(EC_KID_VALUE);
        assertEquals(6, ecKey.asMap().size());
        validatePublicEcKey(ecKey);
    }

    @Test
    public void testPrivateSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("jwkPrivateSet.txt");
        validatePrivateSet(jwks);
    }
    private void validatePrivateSet(JsonWebKeys jwks) throws Exception {
        List<JsonWebKey> keys = jwks.getKeys();
        assertEquals(2, keys.size());

        JsonWebKey ecKey = keys.get(0);
        assertEquals(7, ecKey.asMap().size());
        validatePrivateEcKey(ecKey);
        JsonWebKey rsaKey = keys.get(1);
        assertEquals(11, rsaKey.asMap().size());
        validatePrivateRsaKey(rsaKey);
    }
    @Test
    public void testEncryptDecryptPrivateSet() throws Exception {
        final String password = "Thus from my lips, by yours, my sin is purged.";

        JsonWebKeys jwks = readKeySet("jwkPrivateSet.txt");
        validatePrivateSet(jwks);
        String encryptedKeySet = JwkUtils.encryptJwkSet(jwks, password.toCharArray());
        JweCompactConsumer c = new JweCompactConsumer(encryptedKeySet);
        assertEquals("jwk-set+json", c.getJweHeaders().getContentType());
        assertEquals(KeyAlgorithm.PBES2_HS256_A128KW, c.getJweHeaders().getKeyEncryptionAlgorithm());
        assertEquals(ContentAlgorithm.A128CBC_HS256, c.getJweHeaders().getContentEncryptionAlgorithm());
        assertNotNull(c.getJweHeaders().getHeader("p2s"));
        assertNotNull(c.getJweHeaders().getHeader("p2c"));
        jwks = JwkUtils.decryptJwkSet(encryptedKeySet, password.toCharArray());
        validatePrivateSet(jwks);

    }
    @Test
    public void testEncryptDecryptPrivateKey() throws Exception {
        final String password = "Thus from my lips, by yours, my sin is purged.";
        final String key = "{\"kty\":\"oct\"," + "\"alg\":\"A128KW\"," + "\"k\":\"GawgguFyGrWKav7AX4VKUg\","
                           + "\"kid\":\"AesWrapKey\"}";

        JsonWebKey jwk = readKey(key);
        validateSecretAesKey(jwk);
        String encryptedKey = JwkUtils.encryptJwkKey(jwk, password.toCharArray());
        JweCompactConsumer c = new JweCompactConsumer(encryptedKey);
        assertEquals("jwk+json", c.getJweHeaders().getContentType());
        assertEquals(KeyAlgorithm.PBES2_HS256_A128KW, c.getJweHeaders().getKeyEncryptionAlgorithm());
        assertEquals(ContentAlgorithm.A128CBC_HS256, c.getJweHeaders().getContentEncryptionAlgorithm());
        assertNotNull(c.getJweHeaders().getHeader("p2s"));
        assertNotNull(c.getJweHeaders().getHeader("p2c"));
        jwk = JwkUtils.decryptJwkKey(encryptedKey, password.toCharArray());
        validateSecretAesKey(jwk);

    }

    @Test
    public void testSecretSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("jwkSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        assertEquals(2, keys.size());
        JsonWebKey aesKey = keys.get(0);
        assertEquals(4, aesKey.asMap().size());
        validateSecretAesKey(aesKey);
        JsonWebKey hmacKey = keys.get(1);
        assertEquals(4, hmacKey.asMap().size());
        validateSecretHmacKey(hmacKey);
    }

    private void validateSecretAesKey(JsonWebKey key) {
        assertEquals(AES_SECRET_VALUE, key.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        assertEquals(AES_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.OCTET, key.getKeyType());
        assertEquals(AlgorithmUtils.A128KW_ALGO, key.getAlgorithm());
    }
    private void validateSecretHmacKey(JsonWebKey key) {
        assertEquals(HMAC_SECRET_VALUE, key.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        assertEquals(HMAC_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.OCTET, key.getKeyType());
        assertEquals(AlgorithmUtils.HMAC_SHA_256_ALGO, key.getAlgorithm());
    }

    private void validatePublicRsaKey(JsonWebKey key) {
        assertEquals(RSA_MODULUS_VALUE, key.getProperty(JsonWebKey.RSA_MODULUS));
        assertEquals(RSA_PUBLIC_EXP_VALUE, key.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertEquals(RSA_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.RSA, key.getKeyType());
        assertEquals(AlgorithmUtils.RS_SHA_256_ALGO, key.getAlgorithm());
    }
    private void validatePrivateRsaKey(JsonWebKey key) {
        validatePublicRsaKey(key);
        assertEquals(RSA_PRIVATE_EXP_VALUE, key.getProperty(JsonWebKey.RSA_PRIVATE_EXP));
        assertEquals(RSA_FIRST_PRIME_FACTOR_VALUE, key.getProperty(JsonWebKey.RSA_FIRST_PRIME_FACTOR));
        assertEquals(RSA_SECOND_PRIME_FACTOR_VALUE, key.getProperty(JsonWebKey.RSA_SECOND_PRIME_FACTOR));
        assertEquals(RSA_FIRST_PRIME_CRT_VALUE, key.getProperty(JsonWebKey.RSA_FIRST_PRIME_CRT));
        assertEquals(RSA_SECOND_PRIME_CRT_VALUE, key.getProperty(JsonWebKey.RSA_SECOND_PRIME_CRT));
        assertEquals(RSA_FIRST_CRT_COEFFICIENT_VALUE, key.getProperty(JsonWebKey.RSA_FIRST_CRT_COEFFICIENT));
    }
    private void validatePublicEcKey(JsonWebKey key) {
        assertEquals(EC_X_COORDINATE_VALUE, key.getProperty(JsonWebKey.EC_X_COORDINATE));
        assertEquals(EC_Y_COORDINATE_VALUE, key.getProperty(JsonWebKey.EC_Y_COORDINATE));
        assertEquals(EC_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.EC, key.getKeyType());
        assertEquals(EC_CURVE_VALUE, key.getProperty(JsonWebKey.EC_CURVE));
        assertEquals(PublicKeyUse.ENCRYPT, key.getPublicKeyUse());
    }
    private void validatePrivateEcKey(JsonWebKey key) {
        validatePublicEcKey(key);
        assertEquals(EC_PRIVATE_KEY_VALUE, key.getProperty(JsonWebKey.EC_PRIVATE_KEY));
    }

    public JsonWebKeys readKeySet(String fileName) throws Exception {
        InputStream is = JsonWebKeyTest.class.getResourceAsStream(fileName);
        String s = IOUtils.readStringFromStream(is);
        return JwkUtils.readJwkSet(s);
    }
    public JsonWebKey readKey(String key) throws Exception {
        return JwkUtils.readJwkKey(key);
    }
}