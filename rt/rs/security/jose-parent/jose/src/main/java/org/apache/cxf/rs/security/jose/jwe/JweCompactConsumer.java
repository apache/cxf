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
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseUtils;


public class JweCompactConsumer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JweCompactConsumer.class);
    private final JweDecryptionInput jweDecryptionInput;

    public JweCompactConsumer(String jweContent) {
        String[] parts = JoseUtils.getCompactParts(jweContent);
        if (parts.length != 5) {
            LOG.warning("5 JWE parts are expected");
            throw new JweException(JweException.Error.INVALID_COMPACT_JWE);
        }
        try {
            String headersJson = new String(Base64UrlUtility.decode(parts[0]));
            byte[] encryptedCEK = Base64UrlUtility.decode(parts[1]);
            byte[] initVector = Base64UrlUtility.decode(parts[2]);
            byte[] encryptedContent = Base64UrlUtility.decode(parts[3]);
            byte[] authTag = Base64UrlUtility.decode(parts[4]);
            JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
            JsonMapObject joseHeaders = reader.fromJsonToJsonObject(headersJson);
            if (joseHeaders.getUpdateCount() != null) {
                LOG.warning("Duplicate headers have been detected");
                throw new JweException(JweException.Error.INVALID_COMPACT_JWE);
            }
            JweHeaders jweHeaders = new JweHeaders(joseHeaders.asMap());
            jweDecryptionInput = new JweDecryptionInput(encryptedCEK,
                                                        initVector,
                                                        encryptedContent,
                                                        authTag,
                                                        null,
                                                        headersJson,
                                                        jweHeaders);

        } catch (Base64Exception ex) {
            LOG.warning("Incorrect Base64 URL encoding");
            throw new JweException(JweException.Error.INVALID_COMPACT_JWE);
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
        return new String(getDecryptedContent(decryption), StandardCharsets.UTF_8);
    }
    public boolean validateCriticalHeaders() {
        return JweUtils.validateCriticalHeaders(getJweHeaders());
    }
}
