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

import java.util.Collections;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;


public class JwsJsonSignatureEntry {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsJsonSignatureEntry.class);
    private String jwsPayload;
    private String encodedProtectedHeader;
    private String encodedSignature;
    private JwsHeaders protectedHeader;
    private JwsHeaders unprotectedHeader;
    private JwsHeaders unionHeaders;
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
      
    public JwsJsonSignatureEntry(String jwsPayload,
                                 String encodedProtectedHeader,
                                 String encodedSignature,
                                 JwsHeaders unprotectedHeader) {
        if (encodedProtectedHeader == null && unprotectedHeader == null || encodedSignature == null) {
            LOG.warning("Invalid Signature entry");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        
        this.jwsPayload = jwsPayload;
        this.encodedProtectedHeader = encodedProtectedHeader;
        this.encodedSignature = encodedSignature;
        this.unprotectedHeader = unprotectedHeader;
        if (encodedProtectedHeader != null) {
            this.protectedHeader = new JwsHeaders(writer.fromJson(JoseUtils.decodeToString(encodedProtectedHeader)));
        }
        prepare();
    }
    private void prepare() {
        unionHeaders = new JwsHeaders();
        
        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.asMap());
        }
        if (unprotectedHeader != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(), 
                                     unprotectedHeader.asMap().keySet())) {
                LOG.warning("Protected and unprotected headers have duplicate values");
                throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
            }
            unionHeaders.asMap().putAll(unprotectedHeader.asMap());
        }
    }
    public String getJwsPayload() {
        return jwsPayload;
    }
    public String getDecodedJwsPayload() {
        if (protectedHeader == null || !JwsUtils.isPayloadUnencoded(protectedHeader)) {
            return JoseUtils.decodeToString(jwsPayload);
        } else {
            return jwsPayload;
        }
    }
    public byte[] getDecodedJwsPayloadBytes() {
        return StringUtils.toBytesUTF8(getDecodedJwsPayload());
    }
    public String getEncodedProtectedHeader() {
        return encodedProtectedHeader;
    }
    public JwsHeaders getProtectedHeader() {
        return protectedHeader;
    }
    public JwsHeaders getUnprotectedHeader() {
        return unprotectedHeader;
    }
    public JwsHeaders getUnionHeader() {
        return unionHeaders;
    }
    public String getEncodedSignature() {
        return encodedSignature;
    }
    public byte[] getDecodedSignature() {
        return JoseUtils.decode(getEncodedSignature());
    }
    public String getUnsignedSequence() {
        if (getEncodedProtectedHeader() != null) {
            return getEncodedProtectedHeader() + "." + getJwsPayload();
        } else {
            return "." + getJwsPayload();
        }
    }
    public String getKeyId() {
        return getUnionHeader().getKeyId();
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            if (validator.verify(getUnionHeader(),
                                 getUnsignedSequence(),
                                 getDecodedSignature())) {
                return true;
            }
        } catch (JwsException ex) {
            // ignore
        }
        LOG.warning("Invalid Signature Entry");
        return false;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public boolean validateCriticalHeaders() {
        if (this.getUnprotectedHeader().getHeader(JoseConstants.HEADER_CRITICAL) != null) {
            return false;
        }
        return JwsUtils.validateCriticalHeaders(getUnionHeader());
    }
    public String toJson() {
        return toJson(false);
    }
    public String toJson(boolean flattenedMode) {
        StringBuilder sb = new StringBuilder();
        if (!flattenedMode) {
            sb.append("{");
        }
        if (protectedHeader != null) {
            sb.append("\"protected\":\"" + Base64UrlUtility.encode(writer.toJson(protectedHeader)) + "\"");
        }
        if (unprotectedHeader != null) {
            if (protectedHeader != null) {
                sb.append(",");
            }
            sb.append("\"header\":" + writer.toJson(unprotectedHeader));
        }
        sb.append(",");
        sb.append("\"signature\":\"" + encodedSignature + "\"");
        if (!flattenedMode) {
            sb.append("}");
        }
        return sb.toString();
    }
}
