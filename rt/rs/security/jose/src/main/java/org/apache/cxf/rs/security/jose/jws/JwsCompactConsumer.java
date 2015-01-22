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
package org.apache.cxf.rs.security.jose.jws;

import java.security.interfaces.RSAPublicKey;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsCompactConsumer {
    private JoseHeadersReaderWriter reader = new JoseHeadersReaderWriter();
    private String encodedSequence;
    private String encodedSignature;
    private String headersJson;
    private String decodedJwsPayload;
    public JwsCompactConsumer(String encodedJws) {
        this(encodedJws, null, null);
    }
    public JwsCompactConsumer(String encodedJws, String encodedDetachedPayload) {
        this(encodedJws, encodedDetachedPayload, null);
    }
    protected JwsCompactConsumer(String encodedJws, String encodedDetachedPayload, JoseHeadersReaderWriter r) {
        if (r != null) {
            this.reader = r;
        }
        if (encodedJws.startsWith("\"") && encodedJws.endsWith("\"")) {
            encodedJws = encodedJws.substring(1, encodedJws.length() - 1);
        }
        String[] parts = encodedJws.split("\\.");
        if (parts.length != 3) {
            if (parts.length == 2 && encodedJws.endsWith(".")) {
                encodedSignature = "";
            } else {
                throw new SecurityException("Invalid JWS Compact sequence");
            }
        } else {
            encodedSignature = parts[2];
        }
        String encodedJwsPayload = parts[1];
        if (encodedDetachedPayload != null) {
            if (!StringUtils.isEmpty(encodedJwsPayload)) {
                throw new SecurityException("Invalid JWS Compact sequence");
            }
            encodedJwsPayload = encodedDetachedPayload;
        }
        encodedSequence = parts[0] + "." + encodedJwsPayload;
        headersJson = JoseUtils.decodeToString(parts[0]);
        decodedJwsPayload = JoseUtils.decodeToString(encodedJwsPayload);
    }
    public String getUnsignedEncodedSequence() {
        return encodedSequence;
    }
    public String getEncodedSignature() {
        return encodedSignature;
    }
    public String getDecodedJsonHeaders() {
        return headersJson;
    }
    public String getDecodedJwsPayload() {
        return decodedJwsPayload;
    }
    public byte[] getDecodedJwsPayloadBytes() {
        return StringUtils.toBytesUTF8(decodedJwsPayload);
    }
    public byte[] getDecodedSignature() {
        return encodedSignature.isEmpty() ? new byte[]{} : JoseUtils.decode(encodedSignature);
    }
    public JoseHeaders getJoseHeaders() {
        JoseHeaders joseHeaders = reader.fromJsonHeaders(headersJson);
        if (joseHeaders.getUpdateCount() != null) { 
            throw new SecurityException("Duplicate headers have been detected");
        }
        return joseHeaders;
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            if (validator.verify(getJoseHeaders(), getUnsignedEncodedSequence(), getDecodedSignature())) {
                return true;
            }
        } catch (SecurityException ex) {
            // ignore
        }
        return false;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public boolean verifySignatureWith(JsonWebKey key, String algo) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key, algo));
    }
    public boolean verifySignatureWith(RSAPublicKey key, String algo) {
        return verifySignatureWith(JwsUtils.getRSAKeySignatureVerifier(key, algo));
    }
    public boolean verifySignatureWith(byte[] key, String algo) {
        return verifySignatureWith(JwsUtils.getHmacSignatureVerifier(key, algo));
    }
    public boolean validateCriticalHeaders() {
        return JwsUtils.validateCriticalHeaders(getJoseHeaders());
    }
    protected JoseHeadersReaderWriter getReader() {
        return reader;
    }
    
}
