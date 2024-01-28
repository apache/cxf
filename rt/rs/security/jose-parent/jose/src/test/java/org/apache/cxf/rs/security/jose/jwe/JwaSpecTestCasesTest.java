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


import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.HexUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;


import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * This class includes some of the test-cases in the appendix of the JWA spec -
 * https://tools.ietf.org/html/rfc7518#appendix-B
 */
public class JwaSpecTestCasesTest {

    private static final String K1 = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    private static final String K2 = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        + "202122232425262728292a2b2c2d2e2f";
    private static final String K3 = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        + "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f";

    private static final String IV = "1af38c2dc2b96ffdd86694092341bc04";

    private static final String P = "41206369706865722073797374656d206d757374206e6f74206265207265717569"
        + "72656420746f206265207365637265742c20616e64206974206d7573742062652061626c6520746f2066616c6c20"
        + "696e746f207468652068616e6473206f662074686520656e656d7920776974686f757420696e636f6e76656e6965"
        + "6e6365";

    private static final String E = "c80edfa32ddf39d5ef00c0b468834279a2e46a1b8049f792f76bfe54b903a9c9a9"
        + "4ac9b47ad2655c5f10f9aef71427e2fc6f9b3f399a221489f16362c703233609d45ac69864e3321cf82935ac4096"
        + "c86e133314c54019e8ca7980dfa4b9cf1b384c486f3a54c51078158ee5d79de59fbd34d848b3d69550a676463444"
        + "27ade54b8851ffb598f7f80074b9473c82e2db";
    private static final String E2 = "ea65da6b59e61edb419be62d19712ae5d303eeb50052d0dfd6697f77224c8edb0"
        + "00d279bdc14c1072654bd30944230c657bed4ca0c9f4a8466f22b226d1746214bf8cfc2400add9f5126e479663fc"
        + "90b3bed787a2f0ffcbf3904be2a641d5c2105bfe591bae23b1d7449e532eef60a9ac8bb6c6b01d35d49787bcd57e"
        + "f484927f280adc91ac0c4e79c7b11efc60054e3";
    private static final String E3 = "4affaaadb78c31c5da4b1b590d10ffbd3dd8d5d302423526912da037ecbcc7bd8"
        + "22c301dd67c373bccb584ad3e9279c2e6d12a1374b77f077553df829410446b36ebd97066296ae6427ea75c2e084"
        + "6a11a09ccf5370dc80bfecbad28c73f09b3a3b75e662a2594410ae496b2e2e6609e31e6e02cc837f053d21f37ff4"
        + "f51950bbe2638d09dd7a4930930806d0703b1f6";

    
    @Test
    public void testAes128CBCHMACSHA256() throws Exception {
        doTestSingleRecipient(P, E.getBytes(), ContentAlgorithm.A128CBC_HS256,
                              HexUtils.decode(IV.getBytes()), HexUtils.decode(K1.getBytes()));
    }

    @Test
    public void testAes182CBCHMACSHA384() throws Exception {
        doTestSingleRecipient(P, E2.getBytes(), ContentAlgorithm.A192CBC_HS384,
                              HexUtils.decode(IV.getBytes()), HexUtils.decode(K2.getBytes()));
    }

    @Test
    public void testAes256CBCHMACSHA512() throws Exception {
        doTestSingleRecipient(P, E3.getBytes(), ContentAlgorithm.A256CBC_HS512,
                              HexUtils.decode(IV.getBytes()), HexUtils.decode(K3.getBytes()));
    }

    private void doTestSingleRecipient(String text,
                                         byte[] expectedOutput,
                                         ContentAlgorithm contentEncryptionAlgo,
                                         final byte[] iv,
                                         final byte[] cek) throws Exception {
        JweHeaders headers = new JweHeaders(KeyAlgorithm.A128KW, contentEncryptionAlgo);

        headers.asMap().remove("alg");
        SecretKey cekKey = CryptoUtils.createSecretKeySpec(cek, "AES");
        JweEncryptionProvider jwe = JweUtils.getDirectKeyJweEncryption(cekKey, contentEncryptionAlgo);
        JweJsonProducer p = new JweJsonProducer(headers, HexUtils.decode(text.getBytes())) {
            protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
                JweEncryptionInput input = super.createEncryptionInput(jsonHeaders);
                input.setCek(cek);
                input.setIv(iv);
                return input;
            }
        };
        String jweJson = p.encryptWith(jwe);

        JsonMapObjectReaderWriter jsonReader = new JsonMapObjectReaderWriter();
        Map<String, Object> json = jsonReader.fromJson(jweJson);

        // Check IV matches
        byte[] outputIv = Base64UrlUtility.decode((String)json.get("iv"));
        assertArrayEquals(outputIv, iv);

        // Check CipherText matches
        byte[] cipherTextBytes = Base64UrlUtility.decode((String)json.get("ciphertext"));
        assertArrayEquals(cipherTextBytes, HexUtils.decode(expectedOutput));
    }
}
