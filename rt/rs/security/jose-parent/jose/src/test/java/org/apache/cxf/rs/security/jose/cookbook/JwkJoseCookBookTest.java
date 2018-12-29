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
package org.apache.cxf.rs.security.jose.cookbook;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JwkJoseCookBookTest {

    private static final String EC_X_COORDINATE_VALUE = "AHKZLLOsCOzz5cY97ewNUajB957y-C-U88c3v13nmGZx6sYl_oJXu9"
        + "A5RkTKqjqvjyekWF-7ytDyRXYgCF5cj0Kt";
    private static final String EC_Y_COORDINATE_VALUE = "AdymlHvOiLxXkEhayXQnNCvDX4h9htZaCJN34kfmC6pV5OhQHiraVy"
        + "SsUdaQkAgDPrwQrJmbnX9cwlGfP-HqHZR1";
    private static final String EC_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String EC_CURVE_VALUE = "P-521";
    private static final String EC_PRIVATE_KEY_VALUE = "AAhRON2r9cqXX1hg-RoI6R1tX5p2rUAYdmpHZoC1XNM56KtscrX6zb"
        + "KipQrCW9CGZH3T4ubpnoTKLDYJ_fF3_rJt";
    private static final String RSA_MODULUS_VALUE = "n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT"
        + "-O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV"
        + "wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-"
        + "oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde"
        + "3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC"
        + "LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g"
        + "HdrNP5zw";
    private static final String RSA_PUBLIC_EXP_VALUE = "AQAB";
    private static final String RSA_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String RSA_PRIVATE_EXP_VALUE = "bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e"
        + "iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld"
        + "Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b"
        + "MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU"
        + "6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj"
        + "d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc"
        + "OpBrQzwQ";
    private static final String RSA_FIRST_PRIME_FACTOR_VALUE = "3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR"
        + "aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG"
        + "peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8"
        + "bUq0k";
    private static final String RSA_SECOND_PRIME_FACTOR_VALUE = "uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT"
        + "8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an"
        + "V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0"
        + "s7pFc";
    private static final String RSA_FIRST_PRIME_CRT_VALUE = "B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q"
        + "1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn"
        + "-RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX"
        + "59ehik";
    private static final String RSA_SECOND_PRIME_CRT_VALUE = "CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr"
        + "AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK"
        + "bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK"
        + "T1cYF8";
    private static final String RSA_FIRST_CRT_COEFFICIENT_VALUE =
          "3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N"
        + "ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh"
        + "jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP"
        + "z8aaI4";
    private static final String SIGN_SECRET_VALUE = "hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg";
    private static final String SIGN_KID_VALUE = "018c0ae5-4d9b-471b-bfd6-eef314bc7037";
    private static final String ENCRYPTION_SECRET_VALUE = "AAPapAv4LbFbiVawEjagUBluYqN5rhna-8nuldDvOx8";
    private static final String ENCRYPTION_KID_VALUE = "1e571774-2e08-40da-8308-e8d68773842d";
    @Test
    public void testPublicSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        assertEquals(2, keys.size());
        JsonWebKey ecKey = keys.get(0);
        assertEquals(6, ecKey.asMap().size());
        validatePublicEcKey(ecKey);
        JsonWebKey rsaKey = keys.get(1);
        assertEquals(5, rsaKey.asMap().size());
        validatePublicRsaKey(rsaKey);
    }
    @Test
    public void testPublicSetAsMap() throws Exception {
        JsonWebKeys jwks = readKeySet("cookbookPublicSet.txt");
        Map<KeyType, List<JsonWebKey>> keysMap = jwks.getKeyTypeMap();
        assertEquals(2, keysMap.size());
        List<JsonWebKey> rsaKeys = keysMap.get(KeyType.RSA);
        assertEquals(1, rsaKeys.size());
        assertEquals(5, rsaKeys.get(0).asMap().size());
        validatePublicRsaKey(rsaKeys.get(0));
        List<JsonWebKey> ecKeys = keysMap.get(KeyType.EC);
        assertEquals(1, ecKeys.size());
        assertEquals(6, ecKeys.get(0).asMap().size());
        validatePublicEcKey(ecKeys.get(0));
    }
    @Test
    public void testPrivateSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
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
    public void testSecretSetAsList() throws Exception {
        JsonWebKeys jwks = readKeySet("cookbookSecretSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        assertEquals(2, keys.size());
        JsonWebKey signKey = keys.get(0);
        assertEquals(5, signKey.asMap().size());
        validateSecretSignKey(signKey);
        JsonWebKey encKey = keys.get(1);
        assertEquals(5, encKey.asMap().size());
        validateSecretEncKey(encKey);
    }
    private void validateSecretSignKey(JsonWebKey key) {
        assertEquals(SIGN_SECRET_VALUE, key.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        assertEquals(SIGN_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.OCTET, key.getKeyType());
        assertEquals(AlgorithmUtils.HMAC_SHA_256_ALGO, key.getAlgorithm());
    }
    private void validateSecretEncKey(JsonWebKey key) {
        assertEquals(ENCRYPTION_SECRET_VALUE, key.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        assertEquals(ENCRYPTION_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.OCTET, key.getKeyType());
        assertEquals(AlgorithmUtils.A256GCM_ALGO, key.getAlgorithm());
    }
    private void validatePublicRsaKey(JsonWebKey key) {
        assertEquals(RSA_MODULUS_VALUE, key.getProperty(JsonWebKey.RSA_MODULUS));
        assertEquals(RSA_PUBLIC_EXP_VALUE, key.getProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertEquals(RSA_KID_VALUE, key.getKeyId());
        assertEquals(KeyType.RSA, key.getKeyType());
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
        assertEquals(PublicKeyUse.SIGN, key.getPublicKeyUse());
    }
    private void validatePrivateEcKey(JsonWebKey key) {
        validatePublicEcKey(key);
        assertEquals(EC_PRIVATE_KEY_VALUE, key.getProperty(JsonWebKey.EC_PRIVATE_KEY));
    }
    public JsonWebKeys readKeySet(String fileName) throws Exception {
        InputStream is = JwkJoseCookBookTest.class.getResourceAsStream(fileName);
        String s = IOUtils.readStringFromStream(is);
        return JwkUtils.readJwkSet(s);
    }
    public JsonWebKey readKey(String key) throws Exception {
        return JwkUtils.readJwkKey(key);
    }
}