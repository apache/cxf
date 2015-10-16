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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseUtils;

public class JweJsonEncryptionEntry {
    private JweHeaders unprotectedHeader;
    private String encodedEncryptedKey;
    public JweJsonEncryptionEntry(String encodedEncryptedKey) {
        this(null, encodedEncryptedKey);
    }
    public JweJsonEncryptionEntry(JweHeaders unprotectedHeader, String encodedEncryptedKey) {
        this.unprotectedHeader = unprotectedHeader;
        this.encodedEncryptedKey = encodedEncryptedKey;
    }
    public JweHeaders getUnprotectedHeader() {
        return unprotectedHeader;
    }
    public String getEncodedEncryptedKey() {
        return encodedEncryptedKey;
    }
    public byte[] getEncryptedKey() {
        return encodedEncryptedKey == null ? null : JoseUtils.decode(encodedEncryptedKey);
    }
    public String toJson() {
        JsonMapObjectReaderWriter jsonWriter = new JsonMapObjectReaderWriter();
        Map<String, Object> recipientsEntry = new LinkedHashMap<String, Object>();
        if (unprotectedHeader != null) {
            recipientsEntry.put("header", this.unprotectedHeader);
        }
        if (encodedEncryptedKey != null) {
            recipientsEntry.put("encrypted_key", this.encodedEncryptedKey);
        }
        return jsonWriter.toJson(recipientsEntry);
    }
    public String toString() {
        return toJson();
    }
}
