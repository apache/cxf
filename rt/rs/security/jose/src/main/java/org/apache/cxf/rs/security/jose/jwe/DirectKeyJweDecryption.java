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

import java.security.Key;

public class DirectKeyJweDecryption extends AbstractJweDecryption {
    public DirectKeyJweDecryption(Key contentDecryptionKey, 
                                  ContentDecryptionAlgorithm cipherProps) {    
        this(new DirectKeyDecryptionAlgorithm(contentDecryptionKey),
             cipherProps);
    }
    protected DirectKeyJweDecryption(DirectKeyDecryptionAlgorithm direct,
                                     ContentDecryptionAlgorithm cipherProps) {    
        super(direct, cipherProps);
    }
    protected static class DirectKeyDecryptionAlgorithm implements KeyDecryptionAlgorithm {
        private byte[] contentDecryptionKey;
        public DirectKeyDecryptionAlgorithm(Key contentDecryptionKey) {    
            this(contentDecryptionKey.getEncoded());
        }
        public DirectKeyDecryptionAlgorithm(byte[] contentDecryptionKey) {    
            this.contentDecryptionKey = contentDecryptionKey;
        }
        @Override
        public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
            validateKeyEncryptionKey(jweDecryptionInput);
            return contentDecryptionKey;
        }
        @Override
        public String getAlgorithm() {
            return null;
        }
        protected void validateKeyEncryptionKey(JweDecryptionInput jweDecryptionInput) {
            byte[] encryptedCEK = jweDecryptionInput.getEncryptedCEK();
            if (encryptedCEK != null && encryptedCEK.length > 0) {
                throw new SecurityException();
            }
        }
    }
}
