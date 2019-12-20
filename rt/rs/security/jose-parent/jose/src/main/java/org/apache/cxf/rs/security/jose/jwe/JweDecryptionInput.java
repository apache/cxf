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


public class JweDecryptionInput {
    private final String headersJson;
    private final byte[] encryptedCEK;
    private final byte[] initVector;
    private final byte[] encryptedContent;
    private final byte[] authTag;
    private final byte[] aad;
    private final JweHeaders jweHeaders;

    public JweDecryptionInput(byte[] encryptedCEK,
                              byte[] initVector,
                              byte[] encryptedContent,
                              byte[] authTag,
                              byte[] aad,
                              String headersJson,
                              JweHeaders jweHeaders) {
        this.encryptedCEK = encryptedCEK;
        this.initVector = initVector;
        this.encryptedContent = encryptedContent;
        this.aad = aad;
        this.authTag = authTag;
        this.headersJson = headersJson;
        this.jweHeaders = jweHeaders;
    }

    public byte[] getEncryptedCEK() {
        return encryptedCEK;
    }
    public byte[] getInitVector() {
        return initVector;
    }
    public byte[] getEncryptedContent() {
        return encryptedContent;
    }
    public byte[] getAuthTag() {
        return authTag;
    }
    public byte[] getAad() {
        return aad;
    }
    public String getDecodedJsonHeaders() {
        return headersJson;
    }
    public JweHeaders getJweHeaders() {
        return jweHeaders;
    }
}
