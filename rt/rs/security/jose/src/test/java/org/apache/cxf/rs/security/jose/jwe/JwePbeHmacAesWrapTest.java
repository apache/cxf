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

import java.security.Security;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JwePbeHmacAesWrapTest extends Assert {
    private static final Boolean SKIP_AES_GCM_TESTS = isJava6();
    
    private static boolean isJava6() {
        String version = System.getProperty("java.version");
        return 1.6D == Double.parseDouble(version.substring(0, 3));    
    }
    @Before
    public void registerBouncyCastleIfNeeded() throws Exception {
        Security.addProvider(new BouncyCastleProvider());    
    }
    @After
    public void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    @Test
    public void testEncryptDecryptPbesHmacAesWrapA128CBCHS256() throws Exception {
        final String specPlainText = "Live long and prosper.";
        final String password = "Thus from my lips, by yours, my sin is purged."; 
        KeyEncryptionProvider keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, KeyAlgorithm.PBES2_HS256_A128KW);
        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                                       keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes("UTF-8"), null);
        
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
        
    }
    @Test
    public void testEncryptDecryptPbesHmacAesWrapAesGcm() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        final String specPlainText = "Live long and prosper.";
        JweHeaders headers = new JweHeaders();
        headers.setAlgorithm(AlgorithmUtils.PBES2_HS256_A128KW_ALGO);
        headers.setContentEncryptionAlgorithm(ContentAlgorithm.A128GCM.getJwaName());
        final String password = "Thus from my lips, by yours, my sin is purged."; 
        KeyEncryptionProvider keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, KeyAlgorithm.PBES2_HS256_A128KW);
        JweEncryptionProvider encryption = new JweEncryption(keyEncryption,
            new AesGcmContentEncryptionAlgorithm(ContentAlgorithm.A128GCM));
        String jweContent = encryption.encrypt(specPlainText.getBytes("UTF-8"), null);
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new JweDecryption(keyDecryption, 
                                               new AesGcmContentDecryptionAlgorithm(ContentAlgorithm.A128GCM));
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
        
    }
}

