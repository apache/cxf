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

public class JweEncryptionInput {
    private JweHeaders jweHeaders;
    private byte[] cek;
    private byte[] iv;
    private byte[] aad;
    public JweEncryptionInput(JweHeaders jweHeaders) {
        this.jweHeaders = jweHeaders;
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] cek,
                              byte[] iv) {
        this(jweHeaders, cek, iv, null);
        
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] aad) {
        this(jweHeaders, null, null, aad);
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] cek,
                              byte[] iv,
                              byte[] aad) {
        this(jweHeaders);
        this.cek = cek;
        this.iv = iv;
        this.aad = aad;
    }
    public JweHeaders getJweHeaders() {
        return jweHeaders;
    }
    public byte[] getCek() {
        return cek;
    }
    public byte[] getIv() {
        return iv;
    }
    public byte[] getAad() {
        return aad;
    }
}
