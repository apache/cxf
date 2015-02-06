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

import java.io.UnsupportedEncodingException;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;


public class JweCompactConsumer {
    private JweDecryptionInput jweDecryptionInput;
    public JweCompactConsumer(String jweContent) {
        if (jweContent.startsWith("\"") && jweContent.endsWith("\"")) {
            jweContent = jweContent.substring(1, jweContent.length() - 1);
        }
        String[] parts = jweContent.split("\\.");
        if (parts.length != 5) {
            throw new SecurityException("5 JWE parts are expected");
        }
        try {
            String headersJson = new String(Base64UrlUtility.decode(parts[0]));
            byte[] encryptedCEK = Base64UrlUtility.decode(parts[1]);
            byte[] initVector = Base64UrlUtility.decode(parts[2]);
            byte[] encryptedContent = Base64UrlUtility.decode(parts[3]);
            byte[] authTag = Base64UrlUtility.decode(parts[4]);
            JoseHeadersReaderWriter reader = new JoseHeadersReaderWriter();
            JoseHeaders joseHeaders = reader.fromJsonHeaders(headersJson);
            if (joseHeaders.getUpdateCount() != null) { 
                throw new SecurityException("Duplicate headers have been detected");
            }
            JweHeaders jweHeaders = new JweHeaders(joseHeaders);
            jweDecryptionInput = new JweDecryptionInput(encryptedCEK,
                                                        initVector, 
                                                        encryptedContent,
                                                        authTag,
                                                        null,
                                                        headersJson,
                                                        jweHeaders);
            
        } catch (Base64Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public String getDecodedJsonHeaders() {
        return jweDecryptionInput.getDecodedJsonHeaders();
    }
    
    public JweHeaders getJweHeaders() {
        return jweDecryptionInput.getJweHeaders();
    }
    
    public byte[] getEncryptedContentEncryptionKey() {
        return jweDecryptionInput.getEncryptedCEK();
    }
    
    public byte[] getContentDecryptionCipherInitVector() {
        return jweDecryptionInput.getInitVector();
    }
    
    public byte[] getContentEncryptionCipherAAD() {
        return JweHeaders.toCipherAdditionalAuthData(jweDecryptionInput.getDecodedJsonHeaders());
    }
    
    public byte[] getEncryptionAuthenticationTag() {
        return jweDecryptionInput.getAuthTag();
    }
    
    public byte[] getEncryptedContent() {
        return jweDecryptionInput.getEncryptedContent();
    }
    
    public byte[] getEncryptedContentWithAuthTag() {
        return getCipherWithAuthTag(getEncryptedContent(), getEncryptionAuthenticationTag());
    }
    public JweDecryptionInput getJweDecryptionInput() {
        return jweDecryptionInput;
    }
    public static byte[] getCipherWithAuthTag(byte[] cipher, byte[] authTag) {
        byte[] encryptedContentWithTag = new byte[cipher.length + authTag.length];
        System.arraycopy(cipher, 0, encryptedContentWithTag, 0, cipher.length);
        System.arraycopy(authTag, 0, encryptedContentWithTag, cipher.length, authTag.length);  
        return encryptedContentWithTag;
    }
    
    public byte[] getDecryptedContent(JweDecryptionProvider decryption) {
        // temp workaround
        return decryption.decrypt(jweDecryptionInput);
    }
    public String getDecryptedContentText(JweDecryptionProvider decryption) {
        try {
            return new String(getDecryptedContent(decryption), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
    public boolean validateCriticalHeaders() {
        return JweUtils.validateCriticalHeaders(getJweHeaders());
    }
}
