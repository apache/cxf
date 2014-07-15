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
package org.apache.cxf.rs.security.oauth2.jwe;

import javax.crypto.Cipher;

public class JweEncryption {
    private Cipher cipher;
    private int authTagLen;
    private JweHeaders headers;
    private byte[] contentEncryptionKey;
    private byte[] iv;
    private boolean compressionSupported;
    
    public JweEncryption(Cipher cipher, int authTagLen, JweHeaders headers, 
                        byte[] contentEncryptionKey, 
                        byte[] iv, boolean compressionSupported) {
        this.cipher = cipher;
        this.authTagLen = authTagLen;
        this.headers = headers;
        this.contentEncryptionKey = contentEncryptionKey;
        this.iv = iv;
        this.compressionSupported = compressionSupported;
    }
    public Cipher getCipher() {
        return cipher;
    }
    public JweHeaders getHeaders() {
        return headers;
    }
    public byte[] getContentEncryptionKey() {
        return contentEncryptionKey;
    }
    public byte[] getIv() {
        return iv;
    }
    public boolean isCompressionSupported() {
        return compressionSupported;
    }
    public int getAuthTagLen() {
        return authTagLen;
    }
    
}
