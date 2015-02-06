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


public class DirectKeyJweEncryption extends AbstractJweEncryption {
    
    public DirectKeyJweEncryption(ContentEncryptionAlgorithm ceAlgo) {
        this(ceAlgo, new DirectKeyEncryptionAlgorithm());
    }
    protected DirectKeyJweEncryption(ContentEncryptionAlgorithm ceAlgo,
                                     DirectKeyEncryptionAlgorithm direct) {
        super(ceAlgo, direct);
    }
    @Override
    protected byte[] getProvidedContentEncryptionKey(JweHeaders headers) {
        return validateCek(super.getProvidedContentEncryptionKey(headers));
    }
    private static byte[] validateCek(byte[] cek) {
        if (cek == null) {
            // to prevent the cek from being auto-generated which 
            // does not make sense for the direct key case
            throw new NullPointerException("CEK must not be null");
        }
        return cek;
    }
    protected static class DirectKeyEncryptionAlgorithm implements KeyEncryptionAlgorithm {
        public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] theCek) {
            if (headers.getKeyEncryptionAlgorithm() != null) {
                throw new SecurityException();
            }
            return new byte[0];
        }
        protected void checkKeyEncryptionAlgorithm(JweHeaders headers) {
            if (headers.getKeyEncryptionAlgorithm() != null) {
                throw new SecurityException();
            }
        }
        @Override
        public String getAlgorithm() {
            return null;
        }
    }
}
