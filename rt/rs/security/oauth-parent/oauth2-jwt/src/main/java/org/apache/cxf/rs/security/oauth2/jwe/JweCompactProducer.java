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

import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;


public class JweCompactProducer {
    private String encodedHeaders;
    private String encodedContentEncryptionKey;
    private String encodedInitVector;
    private String encodedEncryptedContent;
    private String encodedAuthTag;
    public JweCompactProducer(JweHeaders headers,
                       byte[] encryptedContentEncryptionKey,
                       byte[] cipherInitVector,
                       byte[] encryptedContentNoTag,
                       byte[] authenticationTag) {    
        this(headers, null, encryptedContentEncryptionKey, 
             cipherInitVector, encryptedContentNoTag, authenticationTag);
    }
    
    public JweCompactProducer(JweHeaders headers,
                       JwtHeadersWriter writer,
                       byte[] encryptedContentEncryptionKey,
                       byte[] cipherInitVector,
                       byte[] encryptedContentNoTag,
                       byte[] authenticationTag) {    
        this.encodedEncryptedContent = Base64UrlUtility.encode(encryptedContentNoTag);
        this.encodedAuthTag = Base64UrlUtility.encode(authenticationTag);
        finalizeInit(headers, writer, encryptedContentEncryptionKey, cipherInitVector);
    }
    
    public JweCompactProducer(JweHeaders headers,
                       byte[] encryptedContentEncryptionKey,
                       byte[] cipherInitVector,
                       byte[] encryptedContentWithTag,
                       int authTagLengthBits) {    
        this(headers, null, encryptedContentEncryptionKey, 
             cipherInitVector, encryptedContentWithTag, authTagLengthBits);
    }
    public JweCompactProducer(JweHeaders headers,
                       JwtHeadersWriter writer,
                       byte[] encryptedContentEncryptionKey,
                       byte[] cipherInitVector,
                       byte[] encryptedContentWithTag,
                       int authTagLengthBits) {    
        this.encodedEncryptedContent = Base64UrlUtility.encodeChunk(
            encryptedContentWithTag, 
            0, 
            encryptedContentWithTag.length - authTagLengthBits / 8);
        this.encodedAuthTag = Base64UrlUtility.encodeChunk(
            encryptedContentWithTag, 
            encryptedContentWithTag.length - authTagLengthBits / 8, 
            encryptedContentWithTag.length);
        finalizeInit(headers, writer, encryptedContentEncryptionKey, cipherInitVector);
    }
    
    private void finalizeInit(JweHeaders headers,
                              JwtHeadersWriter writer, 
                              byte[] encryptedContentEncryptionKey,
                              byte[] cipherInitVector) {
        writer = writer == null ? new JwtTokenReaderWriter() : writer;
        this.encodedHeaders = Base64UrlUtility.encode(writer.headersToJson(headers));
        this.encodedContentEncryptionKey = Base64UrlUtility.encode(encryptedContentEncryptionKey);
        this.encodedInitVector = Base64UrlUtility.encode(cipherInitVector);
    }
    
    public String getJweContent() {
        StringBuilder sb = new StringBuilder();
        return sb.append(encodedHeaders)
                 .append('.')
                 .append(encodedContentEncryptionKey)
                 .append('.')
                 .append(encodedInitVector)
                 .append('.')
                 .append(encodedEncryptedContent)
                 .append('.')
                 .append(encodedAuthTag)
                 .toString();
    }
}
