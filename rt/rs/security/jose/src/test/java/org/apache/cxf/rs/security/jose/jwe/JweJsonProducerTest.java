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
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JweJsonProducerTest extends Assert {
    @BeforeClass
    public static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
            Cipher.getInstance(Algorithm.AES_CBC_ALGO_JAVA);
        } catch (Throwable t) {
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testSingleRecipient() throws Exception {
        final String text = "The true sign of intelligence is not knowledge but imagination.";
        RSAPublicKey publicKey = CryptoUtils.getRSAPublicKey(JweCompactReaderWriterTest.RSA_MODULUS_ENCODED_A1, 
                                                             JweCompactReaderWriterTest.RSA_PUBLIC_EXPONENT_ENCODED_A1);
        JweHeaders headers = new JweHeaders(Algorithm.RSA_OAEP.getJwtName(),
                                            JoseConstants.A128GCM_ALGO);
        JweEncryptionProvider jwe = JweUtils.createJweEncryptionProvider(publicKey, headers);
        JweJsonProducer p = new JweJsonProducer(headers, StringUtils.toBytesUTF8(text));
        String jweJws = p.encryptWith(jwe);
        assertNotNull(jweJws);
        
    }
}

