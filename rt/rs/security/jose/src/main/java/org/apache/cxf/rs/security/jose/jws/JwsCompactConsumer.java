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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.provider.json.JsonMapObject;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsCompactConsumer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsCompactConsumer.class);
    private JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
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
    protected JwsCompactConsumer(String encodedJws, String encodedDetachedPayload, JsonMapObjectReaderWriter r) {
        if (r != null) {
            this.reader = r;
        }
        String[] parts = JoseUtils.getCompactParts(encodedJws);
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
    public JwsHeaders getJwsHeaders() {
        JsonMapObject joseHeaders = reader.fromJsonToJsonObject(headersJson);
        if (joseHeaders.getUpdateCount() != null) {
            LOG.warning("Duplicate headers have been detected");
            throw new JwsException(JwsException.Error.INVALID_COMPACT_JWS);
        }
        return new JwsHeaders(joseHeaders.asMap());
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            if (validator.verify(getJwsHeaders(), getUnsignedEncodedSequence(), getDecodedSignature())) {
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
    public boolean verifySignatureWith(JsonWebKey key, SignatureAlgorithm algo) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key, algo));
    }
    public boolean verifySignatureWith(X509Certificate cert, SignatureAlgorithm algo) {
        return verifySignatureWith(JwsUtils.getPublicKeySignatureVerifier(cert, algo));
    }
    public boolean verifySignatureWith(PublicKey key, SignatureAlgorithm algo) {
        return verifySignatureWith(JwsUtils.getPublicKeySignatureVerifier(key, algo));
    }
    public boolean verifySignatureWith(byte[] key, SignatureAlgorithm algo) {
        return verifySignatureWith(JwsUtils.getHmacSignatureVerifier(key, algo));
    }
    public boolean validateCriticalHeaders() {
        return JwsUtils.validateCriticalHeaders(getJwsHeaders());
    }
    protected JsonMapObjectReaderWriter getReader() {
        return reader;
    }
    
}
