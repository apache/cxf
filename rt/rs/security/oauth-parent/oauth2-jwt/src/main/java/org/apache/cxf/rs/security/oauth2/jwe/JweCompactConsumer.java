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

import java.io.UnsupportedEncodingException;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersReader;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;


public class JweCompactConsumer {
    private String headersJson;
    private byte[] encryptedCEK;
    private byte[] initVector;
    private byte[] encryptedContent;
    private byte[] authTag;
    private JweHeaders jweHeaders;
    public JweCompactConsumer(String jweContent) {
        this(jweContent, new JwtTokenReaderWriter());
    }
    public JweCompactConsumer(String jweContent, JwtHeadersReader reader) {
        String[] parts = jweContent.split("\\.");
        if (parts.length != 5) {
            throw new SecurityException("5 JWE parts are expected");
        }
        try {
            headersJson = new String(Base64UrlUtility.decode(parts[0]));
            encryptedCEK = Base64UrlUtility.decode(parts[1]);
            initVector = Base64UrlUtility.decode(parts[2]);
            
            encryptedContent = Base64UrlUtility.decode(parts[3]);
            authTag = Base64UrlUtility.decode(parts[4]);
            jweHeaders = new JweHeaders(reader.fromJsonHeaders(headersJson).asMap());
        } catch (Base64Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public void enforceJweCryptoProperties(JweCryptoProperties props) {
        if (props != null) { 
            //TODO
        }
    }
    
    public String getDecodedJsonHeaders() {
        return headersJson;
    }
    
    public JweHeaders getJweHeaders() {
        return jweHeaders;
    }
    
    public byte[] getEncryptedContentEncryptionKey() {
        return encryptedCEK;
    }
    
    public byte[] getContentDecryptionCipherInitVector() {
        return initVector;
    }
    
    public byte[] getContentEncryptionCipherAAD() {
        return JweHeaders.toCipherAdditionalAuthData(headersJson);
    }
    
    public byte[] getEncryptionAuthenticationTag() {
        return authTag;
    }
    
    public byte[] getEncryptedContent() {
        return encryptedContent;
    }
    
    public byte[] getEncryptedContentWithAuthTag() {
        return getEncryptedContentWithAuthTag(encryptedContent, authTag);
    }
    
    public static byte[] getEncryptedContentWithAuthTag(byte[] cipher, byte[] authTag) {
        byte[] encryptedContentWithTag = new byte[cipher.length + authTag.length];
        System.arraycopy(cipher, 0, encryptedContentWithTag, 0, cipher.length);
        System.arraycopy(authTag, 0, encryptedContentWithTag, cipher.length, authTag.length);  
        return encryptedContentWithTag;
    }
    
    public byte[] getDecryptedContent(JweDecryptionProvider decryption) {
        return decryption.decrypt(this);
    }
    public String getDecryptedContentText(JweDecryptionProvider decryption) {
        try {
            return new String(getDecryptedContent(decryption), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
}
