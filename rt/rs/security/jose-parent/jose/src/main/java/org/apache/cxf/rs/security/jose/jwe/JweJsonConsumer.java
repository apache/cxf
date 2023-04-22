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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public class JweJsonConsumer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JweJsonConsumer.class);
    private String protectedHeaderJson;
    private JweHeaders protectedHeaderJwe;
    private JweHeaders sharedUnprotectedHeader;
    private List<JweJsonEncryptionEntry> recipients = new LinkedList<>();
    private Map<JweJsonEncryptionEntry, JweHeaders> recipientsMap =
        new LinkedHashMap<>();
    private byte[] aad;
    private byte[] iv;
    private byte[] cipherBytes;
    private byte[] authTag;

    private JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();

    public JweJsonConsumer(String payload) {
        prepare(payload);
    }

    public JweDecryptionOutput decryptWith(JweDecryptionProvider jwe) {
        return decryptWith(jwe, (Map<String, Object>)null);
    }
    public JweDecryptionOutput decryptWith(JweDecryptionProvider jwe,
                                           Map<String, Object> recipientProps) {
        JweJsonEncryptionEntry entry = getJweDecryptionEntry(jwe, recipientProps);
        return decryptWith(jwe, entry);
    }
    public JweDecryptionOutput decryptWith(JweDecryptionProvider jwe, JweJsonEncryptionEntry entry) {
        JweDecryptionInput jweDecryptionInput = getJweDecryptionInput(entry);
        byte[] content = jwe.decrypt(jweDecryptionInput);
        return new JweDecryptionOutput(jweDecryptionInput.getJweHeaders(), content);
    }

    private JweDecryptionInput getJweDecryptionInput(JweJsonEncryptionEntry entry) {
        if (entry == null) {
            LOG.warning("JWE JSON Entry is not available");
            throw new JweException(JweException.Error.INVALID_JSON_JWE);
        }
        JweHeaders unionHeaders = recipientsMap.get(entry);
        if (unionHeaders == null) {
            LOG.warning("JWE JSON Entry union headers are not available");
            throw new JweException(JweException.Error.INVALID_JSON_JWE);
        }
        return new JweDecryptionInput(entry.getEncryptedKey(),
                                      iv,
                                      cipherBytes,
                                      authTag,
                                      aad,
                                      protectedHeaderJson,
                                      unionHeaders);
    }

    public JweJsonEncryptionEntry getJweDecryptionEntry(JweDecryptionProvider jwe) {
        return getJweDecryptionEntry(jwe, null);
    }

    public JweJsonEncryptionEntry getJweDecryptionEntry(JweDecryptionProvider jwe,
                                                        Map<String, Object> recipientProps) {
        for (Map.Entry<JweJsonEncryptionEntry, JweHeaders> entry : recipientsMap.entrySet()) {
            KeyAlgorithm keyAlgo = entry.getValue().getKeyEncryptionAlgorithm();
            if (keyAlgo != null && keyAlgo.equals(jwe.getKeyAlgorithm())
                || keyAlgo == null
                    && (jwe.getKeyAlgorithm() == null || KeyAlgorithm.DIRECT.equals(jwe.getKeyAlgorithm()))) {
                if (recipientProps != null
                    && !entry.getValue().asMap().entrySet().containsAll(recipientProps.entrySet())) {
                    continue;
                }
                return entry.getKey();
            }
        }
        return null;
    }

    private void prepare(String payload) {
        Map<String, Object> jsonObjectMap = reader.fromJson(payload);
        String encodedProtectedHeader = (String)jsonObjectMap.get("protected");
        if (encodedProtectedHeader != null) {
            protectedHeaderJson = JoseUtils.decodeToString(encodedProtectedHeader);
            protectedHeaderJwe =
                new JweHeaders(reader.fromJson(protectedHeaderJson));
        }
        Map<String, Object> unprotectedHeader = CastUtils.cast((Map<?, ?>)jsonObjectMap.get("unprotected"));
        sharedUnprotectedHeader = unprotectedHeader == null ? null : new JweHeaders(unprotectedHeader);
        List<Map<String, Object>> encryptionArray = CastUtils.cast((List<?>)jsonObjectMap.get("recipients"));
        if (encryptionArray != null) {
            if (jsonObjectMap.containsKey("encryption_key")) {
                LOG.warning("JWE JSON encryption_key is missing");
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            for (Map<String, Object> encryptionEntry : encryptionArray) {
                this.recipients.add(getEncryptionObject(encryptionEntry));
            }
        } else {
            this.recipients.add(getEncryptionObject(jsonObjectMap));
        }
        aad = getDecodedBytes(jsonObjectMap, "aad");
        cipherBytes = getDecodedBytes(jsonObjectMap, "ciphertext");
        iv = getDecodedBytes(jsonObjectMap, "iv");
        authTag = getDecodedBytes(jsonObjectMap, "tag");
    }
    protected final JweJsonEncryptionEntry getEncryptionObject(Map<String, Object> encryptionEntry) {
        Map<String, Object> header = CastUtils.cast((Map<?, ?>)encryptionEntry.get("header"));
        JweHeaders recipientUnprotected = header == null ? null : new JweHeaders(header);
        String encodedKey = (String)encryptionEntry.get("encrypted_key");
        JweJsonEncryptionEntry entry = new JweJsonEncryptionEntry(recipientUnprotected, encodedKey);

        JweHeaders unionHeaders = new JweHeaders();
        if (protectedHeaderJwe != null) {
            unionHeaders.asMap().putAll(protectedHeaderJwe.asMap());
            unionHeaders.setProtectedHeaders(protectedHeaderJwe);
        }
        if (sharedUnprotectedHeader != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(),
                                      sharedUnprotectedHeader.asMap().keySet())) {
                LOG.warning("Protected and unprotected headers have duplicate values");
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            unionHeaders.asMap().putAll(sharedUnprotectedHeader.asMap());
        }
        if (recipientUnprotected != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(),
                                      recipientUnprotected.asMap().keySet())) {
                LOG.warning("Union and recipient unprotected headers have duplicate values");
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            unionHeaders.asMap().putAll(recipientUnprotected.asMap());
        }

        recipientsMap.put(entry, unionHeaders);
        return entry;

    }
    protected byte[] getDecodedBytes(Map<String, Object> map, String name) {
        String value = (String)map.get(name);
        if (value != null) {
            return JoseUtils.decode(value);
        }
        return null;
    }

    public JweHeaders getProtectedHeader() {
        return protectedHeaderJwe;
    }

    public JweHeaders getSharedUnprotectedHeader() {
        return sharedUnprotectedHeader;
    }

    public byte[] getAad() {
        return aad;
    }
    public String getAadText() {
        if (aad == null) {
            return null;
        }
        return new String(aad, StandardCharsets.UTF_8);
    }

    public byte[] getIvBytes() {
        return iv;
    }
    public String getIvText() {
        if (iv == null) {
            return null;
        }
        return new String(iv, StandardCharsets.UTF_8);
    }

    public byte[] getCipherBytes() {
        return cipherBytes;
    }
    public String getCipherText() {
        if (cipherBytes == null) {
            return null;
        }
        return new String(cipherBytes, StandardCharsets.UTF_8);
    }

    public byte[] getAuthTagBytes() {
        return authTag;
    }
    public String getAuthTagText() {
        if (authTag == null) {
            return null;
        }
        return new String(authTag, StandardCharsets.UTF_8);
    }

    public List<JweJsonEncryptionEntry> getRecipients() {
        return recipients;
    }

    public Map<JweJsonEncryptionEntry, JweHeaders> getRecipientsMap() {
        return recipientsMap;
    }
}
