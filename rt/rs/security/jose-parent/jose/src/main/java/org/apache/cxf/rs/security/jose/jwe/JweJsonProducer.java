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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public class JweJsonProducer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JweJsonProducer.class);
    private final JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
    private final JweHeaders protectedHeader;
    private final JweHeaders unprotectedHeader;
    private final byte[] content;
    private final byte[] aad;
    private final boolean canBeFlat;

    public JweJsonProducer(JweHeaders protectedHeader, byte[] content) {
        this(protectedHeader, content, false);
    }
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content, boolean canBeFlat) {
        this(protectedHeader, content, null, canBeFlat);
    }
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content, byte[] aad, boolean canBeFlat) {
        this(protectedHeader, null, content, aad, canBeFlat);
    }
    public JweJsonProducer(JweHeaders protectedHeader,
                           JweHeaders unprotectedHeader,
                           byte[] content,
                           byte[] aad,
                           boolean canBeFlat) {
        this.protectedHeader = protectedHeader;
        this.content = content;
        this.aad = aad;
        this.canBeFlat = canBeFlat;
        this.unprotectedHeader = unprotectedHeader;
    }
    public JweJsonProducer(JweHeaders protectedHeader,
                           JweHeaders unprotectedHeader,
                           byte[] content) {
        this(protectedHeader, unprotectedHeader, content, null, false);
    }
    public String encryptWith(JweEncryptionProvider encryptor) {
        return encryptWith(Collections.singletonList(encryptor), null);
    }
    public String encryptWith(JweEncryptionProvider encryptor, JweHeaders recipientUnprotected) {
        return encryptWith(Collections.singletonList(encryptor),
                           Collections.singletonList(recipientUnprotected));
    }
    public String encryptWith(List<JweEncryptionProvider> encryptors) {
        return encryptWith(encryptors, null);
    }
    public String encryptWith(List<JweEncryptionProvider> encryptors,
                              List<JweHeaders> recipientUnprotected) {
        checkAndGetContentAlgorithm(encryptors);
        if (recipientUnprotected != null
            && recipientUnprotected.size() != encryptors.size()) {
            throw new IllegalArgumentException();
        }
        JweHeaders unionHeaders = new JweHeaders();
        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.asMap());
        }
        if (unprotectedHeader != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(),
                                     unprotectedHeader.asMap().keySet())) {
                LOG.warning("Protected and unprotected headers have duplicate values");
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            checkCriticalHeaders(unprotectedHeader);
            unionHeaders.asMap().putAll(unprotectedHeader.asMap());
        }

        List<JweJsonEncryptionEntry> entries = new ArrayList<>(encryptors.size());
        Map<String, Object> jweJsonMap = new LinkedHashMap<>();
        byte[] cipherText = null;
        byte[] authTag = null;
        byte[] iv = null;
        for (int i = 0; i < encryptors.size(); i++) {
            JweEncryptionProvider encryptor = encryptors.get(i);
            JweHeaders perRecipientUnprotected =
                recipientUnprotected == null ? null : recipientUnprotected.get(i);
            final JweHeaders jsonHeaders;
            if (perRecipientUnprotected != null && !perRecipientUnprotected.asMap().isEmpty()) {
                checkCriticalHeaders(perRecipientUnprotected);
                if (!Collections.disjoint(unionHeaders.asMap().keySet(),
                                          perRecipientUnprotected.asMap().keySet())) {
                    LOG.warning("union and recipient unprotected headers have duplicate values");
                    throw new JweException(JweException.Error.INVALID_JSON_JWE);
                }
                jsonHeaders = new JweHeaders(new LinkedHashMap<String, Object>(unionHeaders.asMap()));
                jsonHeaders.asMap().putAll(perRecipientUnprotected.asMap());
            } else {
                jsonHeaders = unionHeaders;
            }
            jsonHeaders.setProtectedHeaders(protectedHeader);

            JweEncryptionInput input = createEncryptionInput(jsonHeaders);
            if (i > 0) {
                input.setContent(null);
                input.setContentEncryptionRequired(false);
            }
            JweEncryptionOutput state = encryptor.getEncryptionOutput(input);

            if (state.getHeaders() != null && state.getHeaders().asMap().size() != jsonHeaders.asMap().size()) {
                // New headers were generated during encryption for recipient
                Map<String, Object> newHeaders = new LinkedHashMap<>();
                state.getHeaders().asMap().forEach((name, value) -> {
                    if (!unionHeaders.containsHeader(name)) {
                        // store recipient header
                        newHeaders.put(name, value);
                    }
                });
                Map<String, Object> perRecipientUnprotectedHeaders = (perRecipientUnprotected != null)
                    ? new LinkedHashMap<>(perRecipientUnprotected.asMap())
                        : new LinkedHashMap<>();
                perRecipientUnprotectedHeaders.putAll(newHeaders);
                perRecipientUnprotected = new JweHeaders(perRecipientUnprotectedHeaders);
            }
            byte[] currentCipherText = state.getEncryptedContent();
            byte[] currentAuthTag = state.getAuthTag();
            byte[] currentIv = state.getIv();
            if (cipherText == null) {
                cipherText = currentCipherText;
            }
            if (authTag == null) {
                authTag = currentAuthTag;
            }
            if (iv == null) {
                iv = currentIv;
            }

            byte[] encryptedCek = state.getEncryptedContentEncryptionKey();
            if (encryptedCek.length == 0
                && encryptor.getKeyAlgorithm() != null
                && !KeyAlgorithm.isDirect(encryptor.getKeyAlgorithm())) {
                LOG.warning("Unexpected key encryption algorithm");
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            String encodedCek = encryptedCek.length == 0 ? null : Base64UrlUtility.encode(encryptedCek);
            entries.add(new JweJsonEncryptionEntry(perRecipientUnprotected, encodedCek));

        }
        if (protectedHeader != null && !protectedHeader.asMap().isEmpty()) {
            jweJsonMap.put("protected",
                        Base64UrlUtility.encode(writer.toJson(protectedHeader)));
        }
        if (unprotectedHeader != null && !unprotectedHeader.asMap().isEmpty()) {
            jweJsonMap.put("unprotected", unprotectedHeader);
        }
        if (entries.size() == 1 && canBeFlat) {
            JweHeaders unprotectedEntryHeader = entries.get(0).getUnprotectedHeader();
            if (unprotectedEntryHeader != null && !unprotectedEntryHeader.asMap().isEmpty()) {
                jweJsonMap.put("header", unprotectedEntryHeader);
            }
            String encryptedKey = entries.get(0).getEncodedEncryptedKey();
            if (encryptedKey != null) {
                jweJsonMap.put("encrypted_key", encryptedKey);
            }
        } else {
            jweJsonMap.put("recipients", entries);
        }
        if (aad != null) {
            jweJsonMap.put("aad", Base64UrlUtility.encode(aad));
        }
        jweJsonMap.put("iv", Base64UrlUtility.encode(iv));
        jweJsonMap.put("ciphertext", Base64UrlUtility.encode(cipherText));
        jweJsonMap.put("tag", Base64UrlUtility.encode(authTag));
        return writer.toJson(jweJsonMap);
    }
    protected JweEncryptionInput createEncryptionInput(JweHeaders jsonHeaders) {
        return new JweEncryptionInput(jsonHeaders, content, aad);
    }
    private String checkAndGetContentAlgorithm(List<JweEncryptionProvider> encryptors) {
        Set<String> set = new HashSet<>();
        for (JweEncryptionProvider encryptor : encryptors) {
            set.add(encryptor.getContentAlgorithm().getJwaName());
        }
        if (set.size() != 1) {
            LOG.warning("Invalid content encryption algorithm");
            throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
        }
        return set.iterator().next();
    }
    private static void checkCriticalHeaders(JweHeaders unprotected) {
        if (unprotected.asMap().containsKey(JoseConstants.HEADER_CRITICAL)) {
            LOG.warning("Unprotected headers contain critical headers");
            throw new JweException(JweException.Error.INVALID_JSON_JWE);
        }
    }
}
