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
    private byte[] content;
    private boolean contentEncryptionRequired = true;
    public JweEncryptionInput() {

    }
    public JweEncryptionInput(JweHeaders jweHeaders) {
        this(jweHeaders, null);
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] content) {
        this(jweHeaders, content, null);
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] content,
                              byte[] aad) {
        this(jweHeaders, content, aad, null, null);

    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] content,
                              byte[] cek,
                              byte[] iv) {
        this(jweHeaders, content, null, cek, iv);
    }
    public JweEncryptionInput(JweHeaders jweHeaders,
                              byte[] content,
                              byte[] aad,
                              byte[] cek,
                              byte[] iv) {
        this.jweHeaders = jweHeaders;
        this.content = content;
        this.cek = cek;
        this.iv = iv;
        this.aad = aad;
    }
    public JweHeaders getJweHeaders() {
        return jweHeaders;
    }
    public void setJweHeaders(JweHeaders jweHeaders) {
        this.jweHeaders = jweHeaders;
    }
    public byte[] getCek() {
        return cek;
    }
    public void setCek(byte[] cek) {
        this.cek = cek;
    }
    public byte[] getIv() {
        return iv;
    }
    public void setIv(byte[] iv) {
        this.iv = iv;
    }
    public byte[] getAad() {
        return aad;
    }
    public void setAad(byte[] aad) {
        this.aad = aad;
    }
    public byte[] getContent() {
        return content;
    }
    public void setContent(byte[] content) {
        this.content = content;
    }
    public boolean isContentEncryptionRequired() {
        return contentEncryptionRequired;
    }
    public void setContentEncryptionRequired(boolean required) {
        this.contentEncryptionRequired = required;
    }
}
