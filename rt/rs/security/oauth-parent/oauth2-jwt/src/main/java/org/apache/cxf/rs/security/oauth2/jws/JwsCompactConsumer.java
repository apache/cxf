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
package org.apache.cxf.rs.security.oauth2.jws;

import java.io.UnsupportedEncodingException;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.oauth2.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenJson;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReader;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public class JwsCompactConsumer {
    private JwtTokenReader reader = new JwtTokenReaderWriter();
    private String encodedSequence;
    private String encodedSignature;
    private String headersJson;
    private String claimsJson;
    private JwtToken token;
    public JwsCompactConsumer(String encodedJws) {
        this(encodedJws, null);
    }
    public JwsCompactConsumer(String encodedJws, JwtTokenReader r) {
        if (r != null) {
            this.reader = r;
        }
        
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
        claimsJson = decodeToString(parts[1]);
        
        encodedSequence = parts[0] + "." + parts[1];
        
    }
    public String getUnsignedEncodedToken() {
        return encodedSequence;
    }
    public String getEncodedSignature() {
        return encodedSignature;
    }
    public String getDecodedJsonHeaders() {
        return headersJson;
    }
    public String getDecodedJsonClaims() {
        return claimsJson;
    }
    public JwtTokenJson getDecodedJsonToken() {
        return new JwtTokenJson(getDecodedJsonHeaders(), getDecodedJsonClaims());
    }
    public byte[] getDecodedSignature() {
        return encodedSignature.isEmpty() ? new byte[]{} : decode(encodedSignature);
    }
    public JwtHeaders getJwtHeaders() {
        return getJwtToken().getHeaders();
    }
    public JwtClaims getJwtClaims() {
        return getJwtToken().getClaims();
    }
    public JwtToken getJwtToken() {
        if (token == null) {
            token = reader.fromJson(headersJson, claimsJson);
        }
        return token;
    }
    public void validateSignatureWith(JwsSignatureValidator validator) {
        validator.validate(getJwtHeaders(), getUnsignedEncodedToken(), getDecodedSignature());
    }
    private static String decodeToString(String encoded) {
        try {
            return new String(decode(encoded), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new OAuthServiceException(ex);
        }
        
    }
    
    private static byte[] decode(String encoded) {
        try {
            return Base64UrlUtility.decode(encoded);
        } catch (Base64Exception ex) {
            throw new OAuthServiceException(ex);
        }
    }
}
