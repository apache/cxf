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

import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JwePbeHmacAesWrapTest {
    
    @Test
    public void testEncryptDecryptPbesHmacAesWrapA128CBCHS256() throws Exception {
        final String specPlainText = "Live long and prosper.";
        final String password = "Thus from my lips, by yours, my sin is purged.";
        KeyEncryptionProvider keyEncryption =
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, KeyAlgorithm.PBES2_HS256_A128KW);
        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                                       keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);

        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);

    }
    @Test
    public void testEncryptDecryptPbesHmacAesWrapAesGcm() throws Exception {
        final String specPlainText = "Live long and prosper.";
        JweHeaders headers = new JweHeaders();
        headers.setKeyEncryptionAlgorithm(KeyAlgorithm.PBES2_HS256_A128KW);
        headers.setContentEncryptionAlgorithm(ContentAlgorithm.A128GCM);
        final String password = "Thus from my lips, by yours, my sin is purged.";
        KeyEncryptionProvider keyEncryption =
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, KeyAlgorithm.PBES2_HS256_A128KW);
        JweEncryptionProvider encryption = new JweEncryption(keyEncryption,
            new AesGcmContentEncryptionAlgorithm(ContentAlgorithm.A128GCM));
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new JweDecryption(keyDecryption,
                                               new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A128GCM));
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);

    }

    @Test
    public void testDecryptWithInvalidLargeP2CValue() throws Exception {
        final String specPlainText = "Live long and prosper.";
        JweHeaders headers = new JweHeaders();
        headers.setKeyEncryptionAlgorithm(KeyAlgorithm.PBES2_HS256_A128KW);
        headers.setContentEncryptionAlgorithm(ContentAlgorithm.A128GCM);
        final String password = "Thus from my lips, by yours, my sin is purged.";
        KeyEncryptionProvider keyEncryption =
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, 1_500_000, KeyAlgorithm.PBES2_HS256_A128KW, false);
        JweEncryptionProvider encryption = new JweEncryption(keyEncryption,
            new AesGcmContentEncryptionAlgorithm(ContentAlgorithm.A128GCM));
        String jweContent = encryption.encrypt(specPlainText.getBytes(StandardCharsets.UTF_8), null);

        // 1. It should fail by default
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new JweDecryption(keyDecryption,
                                               new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A128GCM));
        try {
            decryption.decrypt(jweContent).getContentText();
            fail("Failure expected on a too large p2c value");
        } catch (JoseException ex) {
            // expected
        }

        // 2. Now we allow 1.5M iterations and it passes
        keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password, 1_500_000);
        decryption = new JweDecryption(keyDecryption,
                                               new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A128GCM));
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
    }
}
