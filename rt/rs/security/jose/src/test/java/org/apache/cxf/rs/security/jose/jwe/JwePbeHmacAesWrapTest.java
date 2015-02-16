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

import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JwePbeHmacAesWrapTest extends Assert {
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
        KeyEncryptionAlgorithm keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, JoseConstants.PBES2_HS256_A128KW_ALGO);
        JweEncryptionProvider encryption = new AesCbcHmacJweEncryption(Algorithm.A128CBC_HS256.getJwtName(),
                                                                       keyEncryption);
        String jweContent = encryption.encrypt(specPlainText.getBytes("UTF-8"), null);
        
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new AesCbcHmacJweDecryption(keyDecryption);
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
        
    }
    @Test
    public void testEncryptDecryptPbesHmacAesWrapAesGcm() throws Exception {
        final String specPlainText = "Live long and prosper.";
        JweHeaders headers = new JweHeaders();
        headers.setAlgorithm(JoseConstants.PBES2_HS256_A128KW_ALGO);
        headers.setContentEncryptionAlgorithm(Algorithm.A128GCM.getJwtName());
        final String password = "Thus from my lips, by yours, my sin is purged."; 
        KeyEncryptionAlgorithm keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, JoseConstants.PBES2_HS256_A128KW_ALGO);
        JweEncryptionProvider encryption = new JweEncryption(keyEncryption,
            new AesGcmContentEncryptionAlgorithm(Algorithm.A128GCM.getJwtName()));
        String jweContent = encryption.encrypt(specPlainText.getBytes("UTF-8"), null);
        PbesHmacAesWrapKeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        JweDecryptionProvider decryption = new JweDecryption(keyDecryption, 
                                               new AesGcmContentDecryptionAlgorithm(JoseConstants.A128GCM_ALGO));
        String decryptedText = decryption.decrypt(jweContent).getContentText();
        assertEquals(specPlainText, decryptedText);
        
    }
}

