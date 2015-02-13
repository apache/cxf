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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;

public class JweJsonProducer {
    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    private JweHeaders protectedHeader;
    private JweHeaders unprotectedHeader;
    private byte[] content;
    private byte[] aad;
    private boolean canBeFlat;
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content) {
        this(protectedHeader, content, false);    
    }
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content, boolean canBeFlat) {
        this(protectedHeader, content, null, canBeFlat);
    }
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content, byte[] aad, boolean canBeFlat) {
        this.protectedHeader = protectedHeader;
        this.content = content;
        this.aad = aad;
        this.canBeFlat = canBeFlat;
    }
    public JweJsonProducer(JweHeaders protectedHeader, 
                           JweHeaders unprotectedHeader, 
                           byte[] content, 
                           byte[] aad,
                           boolean canBeFlat) {
        this(protectedHeader, content, aad, canBeFlat);
        this.unprotectedHeader = unprotectedHeader;
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
                throw new SecurityException("Protected and unprotected headers have duplicate values");
            }
            unionHeaders.asMap().putAll(unprotectedHeader.asMap());
        }
        
        List<JweJsonEncryptionEntry> entries = new ArrayList<JweJsonEncryptionEntry>(encryptors.size());
        Map<String, Object> jweJsonMap = new LinkedHashMap<String, Object>();
        byte[] cipherText = null;
        byte[] authTag = null;
        byte[] iv = null;
        for (int i = 0; i < encryptors.size(); i++) {
            JweEncryptionProvider encryptor = encryptors.get(i);
            JweHeaders perRecipientUnprotected = 
                recipientUnprotected == null ? null : recipientUnprotected.get(i);
            JweHeaders jsonHeaders = null;
            if (recipientUnprotected != null) {
                if (!Collections.disjoint(unionHeaders.asMap().keySet(), 
                                          perRecipientUnprotected.asMap().keySet())) {
                    throw new SecurityException("Protected and unprotected headers have duplicate values");
                }
                jsonHeaders = new JweHeaders(unionHeaders.asMap());
                jsonHeaders.asMap().putAll(unprotectedHeader.asMap());
            } else {  
                jsonHeaders = unionHeaders;
            }
            jsonHeaders.setProtectedHeaders(protectedHeader);
            
            JweEncryptionInput input = createEncryptionInput(jsonHeaders);
                
            JweEncryptionOutput state = encryptor.getEncryptionOutput(input);
            
            byte[] currentCipherText = state.getEncryptedContent();
            byte[] currentAuthTag = state.getAuthTag();
            byte[] currentIv = state.getIv();
            if (cipherText == null) {
                cipherText = currentCipherText;
            } else if (!Arrays.equals(cipherText, currentCipherText)) {
                throw new SecurityException();
            }
            if (authTag == null) {
                authTag = currentAuthTag;
            } else if (!Arrays.equals(authTag, currentAuthTag)) {
                throw new SecurityException();
            }
            if (iv == null) {
                iv = currentIv;
            } else if (!Arrays.equals(iv, currentIv)) {
                throw new SecurityException();
            }
            
            
            byte[] encryptedCek = state.getContentEncryptionKey(); 
            if (encryptedCek == null && encryptor.getKeyAlgorithm() != null) {
                // can be null only if it is the direct key encryption
                throw new SecurityException();
            }
            String encodedCek = encryptedCek == null ? null : Base64UrlUtility.encode(encryptedCek);    
            entries.add(new JweJsonEncryptionEntry(perRecipientUnprotected, encodedCek));
            
        }
        if (protectedHeader != null) {
            jweJsonMap.put("protected", 
                        Base64UrlUtility.encode(writer.toJson(protectedHeader)));
        }
        if (unprotectedHeader != null) {
            jweJsonMap.put("unprotected", unprotectedHeader);
        }
        if (entries.size() == 1 && canBeFlat) {
            JweHeaders unprotectedEntryHeader = entries.get(0).getUnprotectedHeader();
            if (unprotectedEntryHeader != null) {
                jweJsonMap.put("header", unprotectedEntryHeader);
            }
            jweJsonMap.put("encrypted_key", entries.get(0).getEncodedEncryptedKey());
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
        Set<String> set = new HashSet<String>();
        for (JweEncryptionProvider encryptor : encryptors) {
            set.add(encryptor.getContentAlgorithm());
        }
        if (set.size() != 1) {
            throw new SecurityException("Invalid content encryption algorithm");
        }
        return set.iterator().next();
    }
    
}
