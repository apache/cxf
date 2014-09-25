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

import java.io.UnsupportedEncodingException;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.jose.JoseHeadersReader;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwt.JwtTokenReader;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public class JwsCompactConsumer {
    private JoseHeadersReader reader = new JoseHeadersReaderWriter();
    private String encodedSequence;
    private String encodedSignature;
    private String headersJson;
    private String jwsPayload;
    private JwsSignatureProperties props;
    public JwsCompactConsumer(String encodedJws) {
        this(encodedJws, null, null);
    }
    public JwsCompactConsumer(String encodedJws, JwsSignatureProperties props) {
        this(encodedJws, props, null);
    }
    public JwsCompactConsumer(String encodedJws, JwtTokenReader r) {
        this(encodedJws, null, r);
    }
    public JwsCompactConsumer(String encodedJws, JwsSignatureProperties props, JoseHeadersReader r) {
        if (r != null) {
            this.reader = r;
        }
        this.props = props;
        String[] parts = encodedJws.split("\\.");
        if (parts.length != 3) {
            if (parts.length == 2 && encodedJws.endsWith(".")) {
                encodedSignature = "";
            } else {
                throw new OAuthServiceException("Invalid JWS Compact sequence");
            }
        } else {
            encodedSignature = parts[2];
        }
        headersJson = decodeToString(parts[0]);
        jwsPayload = decodeToString(parts[1]);
        encodedSequence = parts[0] + "." + parts[1];
        
    }
    public String getUnsignedEncodedPayload() {
        return encodedSequence;
    }
    public String getEncodedSignature() {
        return encodedSignature;
    }
    public String getDecodedJsonHeaders() {
        return headersJson;
    }
    public String getDecodedJwsPayload() {
        return jwsPayload;
    }
    public byte[] getDecodedJwsPayloadBytes() {
        try {
            return jwsPayload.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
    public byte[] getDecodedSignature() {
        return encodedSignature.isEmpty() ? new byte[]{} : decode(encodedSignature);
    }
    public JwsHeaders getJwsHeaders() {
        return new JwsHeaders(getReader().fromJsonHeaders(headersJson).asMap());
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        enforceJweSignatureProperties();
        if (!validator.verify(getJwsHeaders(), getUnsignedEncodedPayload(), getDecodedSignature())) {
            throw new SecurityException();
        }
        return true;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    private void enforceJweSignatureProperties() {
        if (props != null) {
            //TODO:
        }
    }
    private static String decodeToString(String encoded) {
        try {
            return new String(decode(encoded), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
        
    }
    protected JoseHeadersReader getReader() {
        return reader;
    }
    private static byte[] decode(String encoded) {
        try {
            return Base64UrlUtility.decode(encoded);
        } catch (Base64Exception ex) {
            throw new SecurityException(ex);
        }
    }
}
