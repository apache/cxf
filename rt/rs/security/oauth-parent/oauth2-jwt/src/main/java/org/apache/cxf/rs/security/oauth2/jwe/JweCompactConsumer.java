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
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;


public class JweCompactConsumer {
    private String headersJson;
    private byte[] encryptedCEK;
    private byte[] initVector;
    private byte[] encryptedContentWithTag;
    private byte[] authTag;
    private JweHeaders jweHeaders;
    public JweCompactConsumer(String jweContent) {
        String[] parts = jweContent.split("\\.");
        if (parts.length != 5) {
            throw new SecurityException("5 JWE parts are expected");
        }
        try {
            headersJson = new String(Base64UrlUtility.decode(parts[0]));
            encryptedCEK = Base64UrlUtility.decode(parts[1]);
            initVector = Base64UrlUtility.decode(parts[2]);
            
            byte[] cipherText = Base64UrlUtility.decode(parts[3]);
            authTag = Base64UrlUtility.decode(parts[4]);
            encryptedContentWithTag = new byte[cipherText.length + authTag.length];
            System.arraycopy(cipherText, 0, encryptedContentWithTag, 0, cipherText.length);
            System.arraycopy(authTag, 0, encryptedContentWithTag, cipherText.length, authTag.length);
            jweHeaders = new JweHeaders(new JwtTokenReaderWriter().fromJsonHeaders(headersJson).asMap());
        } catch (Base64Exception ex) {
            throw new SecurityException(ex);
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
    
    public byte[] getEncryptedContentWithAuthTag() {
        return encryptedContentWithTag;
    }
    
    public byte[] getDecryptedContent(ContentEncryptionProvider provider) {
        byte[] cek = provider.getContentEncryptionKey(getJweHeaders(), getEncryptedContentEncryptionKey());
        KeyProperties keyProperties = new KeyProperties(
            Algorithm.toJavaName(getJweHeaders().getContentEncryptionAlgorithm()));
        keyProperties.setAdditionalData(getContentEncryptionCipherAAD());
        
        AlgorithmParameterSpec spec = provider.getContentEncryptionCipherSpec(getJweHeaders(),
                                                         getEncryptionAuthenticationTag().length * 8,
                                                         getContentDecryptionCipherInitVector());
        keyProperties.setAlgoSpec(spec);
        boolean compressionSupported = 
            JwtConstants.DEFLATE_ZIP_ALGORITHM.equals(getJweHeaders().getZipAlgorithm());
        keyProperties.setCompressionSupported(compressionSupported);
        Key secretKey = CryptoUtils.createSecretKeySpec(cek, keyProperties.getKeyAlgo());
        return CryptoUtils.decryptBytes(getEncryptedContentWithAuthTag(), secretKey, keyProperties);
    }
    public String getDecryptedContentText(ContentEncryptionProvider provider) {
        try {
            return new String(getDecryptedContent(provider), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
}
