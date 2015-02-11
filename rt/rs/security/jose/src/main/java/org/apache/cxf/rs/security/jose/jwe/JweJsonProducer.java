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
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;

public class JweJsonProducer {
    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    private JweHeaders protectedHeader;
    private JweHeaders unprotectedHeader;
    private byte[] content;
    private byte[] aad;
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content) {
        this.protectedHeader = protectedHeader;
        this.content = content;    
    }
    public JweJsonProducer(JweHeaders protectedHeader, byte[] content, byte[] aad) {
        this(protectedHeader, content);
        this.aad = aad;
    }
    public JweJsonProducer(JweHeaders protectedHeader, JweHeaders unprotectedHeader, 
                           byte[] content, byte[] aad) {
        this(protectedHeader, content, aad);
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
        //TODO: determine the actual cek and iv length based on the algo
        byte[] cek = CryptoUtils.generateSecureRandomBytes(32);
        byte[] iv = CryptoUtils.generateSecureRandomBytes(16);
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
        if (protectedHeader != null) {
            jweJsonMap.put("protected", 
                        Base64UrlUtility.encode(writer.toJson(protectedHeader)));
        }
        if (unprotectedHeader != null) {
            jweJsonMap.put("unprotected", unprotectedHeader);
        }
        byte[] cipherText = null;
        byte[] authTag = null;
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
            
            JweEncryptionInput input = new JweEncryptionInput(jsonHeaders,
                                                              cek,
                                                              iv,
                                                              aad);
                
            JweEncryptionState state = encryptor.createJweEncryptionState(input);
            try {
                byte[] currentCipherOutput = state.getCipher().doFinal(content);
                byte[] currentCipherText = null;
                byte[] currentAuthTag = null;
                if (state.getAuthTagProducer() != null) {
                    currentCipherText = currentCipherOutput;
                    state.getAuthTagProducer().update(content, 0, content.length);
                    currentAuthTag = state.getAuthTagProducer().getTag();
                } else {
                    final int authTagLengthBits = 128;
                    final int cipherTextLen = currentCipherOutput.length - authTagLengthBits / 8;
                    currentCipherText = Arrays.copyOf(currentCipherOutput, cipherTextLen);
                    currentAuthTag = Arrays.copyOfRange(currentCipherOutput, cipherTextLen, 
                                                        cipherTextLen + authTagLengthBits / 8);
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
                }
                
                byte[] encryptedCek = state.getContentEncryptionKey(); 
                if (encryptedCek == null && encryptor.getKeyAlgorithm() != null) {
                    // can be null only if it is the direct key encryption
                    throw new SecurityException();
                }
                String encodedCek = encryptedCek == null ? null : Base64UrlUtility.encode(encryptedCek);    
                entries.add(new JweJsonEncryptionEntry(perRecipientUnprotected, encodedCek));
            } catch (Exception ex) {
                throw new SecurityException(ex);
            }
        }
        jweJsonMap.put("recipients", entries);
        if (aad != null) {
            jweJsonMap.put("aad", Base64UrlUtility.encode(aad));
        }
        jweJsonMap.put("iv", Base64UrlUtility.encode(iv));
        jweJsonMap.put("ciphertext", Base64UrlUtility.encode(cipherText));
        jweJsonMap.put("tag", Base64UrlUtility.encode(authTag));
        return writer.toJson(jweJsonMap);
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
