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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsCompactConsumer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsCompactConsumer.class);
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
                LOG.warning("Compact JWS does not have 3 parts");
                throw new JwsException(JwsException.Error.INVALID_COMPACT_JWS);
            }
        } else {
            encodedSignature = parts[2];
        }
        String encodedJwsPayload = parts[1];
        if (encodedDetachedPayload != null) {
            if (!StringUtils.isEmpty(encodedJwsPayload)) {
                LOG.warning("Compact JWS includes a payload expected to be detached");
                throw new JwsException(JwsException.Error.INVALID_COMPACT_JWS);
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
    public JwsHeaders getJoseHeaders() {
        JoseHeaders joseHeaders = reader.fromJsonHeaders(headersJson);
        if (joseHeaders.getUpdateCount() != null) {
            LOG.warning("Duplicate headers have been detected");
            throw new JwsException(JwsException.Error.INVALID_COMPACT_JWS);
        }
        return new JwsHeaders(joseHeaders);
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            if (validator.verify(getJoseHeaders(), getUnsignedEncodedSequence(), getDecodedSignature())) {
                return true;
            }
        } catch (JwsException ex) {
            // ignore
        }
        LOG.warning("Invalid Signature");
        return false;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public boolean verifySignatureWith(JsonWebKey key, String algo) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key, algo));
    }
    public boolean verifySignatureWith(X509Certificate cert, String algo) {
        return verifySignatureWith(JwsUtils.getRSAKeySignatureVerifier(cert, algo));
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
