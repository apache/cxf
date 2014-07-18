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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public class JwsCompactProducer {
    private JwtHeadersWriter writer = new JwtTokenReaderWriter();
    private JwtHeaders headers;
    private String plainJwsPayload;
    private String signature;
    private String plainRep;
    
    public JwsCompactProducer(String plainJwsPayload) {
        this(null, null, plainJwsPayload);
    }
    public JwsCompactProducer(JwtHeaders headers, String plainJwsPayload) {
        this(headers, null, plainJwsPayload);
    }
    public JwsCompactProducer(JwtHeaders headers, JwtHeadersWriter w, String plainJwsPayload) {
        this.headers = headers;
        if (w != null) {
            this.writer = w;
        }
        this.plainJwsPayload = plainJwsPayload;
    }
    public JwtHeaders getHeaders() {
        if (headers == null) {
            headers = new JwtHeaders();
        }
        return headers;
    }
    public String getUnsignedEncodedJws() {
        checkAlgorithm();
        if (plainRep == null) {
            plainRep = Base64UrlUtility.encode(writer.headersToJson(getHeaders())) 
                + "." 
                + Base64UrlUtility.encode(plainJwsPayload);
        }
        return plainRep;
    }
    
    public String getSignedEncodedJws() {
        checkAlgorithm();
        boolean noSignature = StringUtils.isEmpty(signature);
        if (noSignature && !isPlainText()) {
            throw new IllegalStateException("Signature is not available");
        }
        return getUnsignedEncodedJws() + "." + (noSignature ? "" : signature);
    }
    
    public String signWith(JwsSignatureProvider signer) { 
        JwsSignature worker = signer.createJwsSignature(getHeaders());
        try {
            byte[] bytes = getUnsignedEncodedJws().getBytes("UTF-8");
            worker.update(bytes, 0, bytes.length);
            signWith(worker.sign());
            return getSignedEncodedJws();
        } catch (Exception ex) {
            throw new SecurityException();
        }
    }
    
    public String signWith(String signatureText) {
        setEncodedSignature(Base64UrlUtility.encode(signatureText));
        return getSignedEncodedJws();
    }
    
    public String signWith(byte[] signatureOctets) {
        setEncodedSignature(Base64UrlUtility.encode(signatureOctets));
        return getSignedEncodedJws();
    }
    
    private void setEncodedSignature(String sig) {
        this.signature = sig;
    }
    private boolean isPlainText() {
        return JwtConstants.PLAIN_TEXT_ALGO.equals(getAlgorithm());
    }
    private String getAlgorithm() {
        return getHeaders().getAlgorithm();
    }
    private void checkAlgorithm() {
        if (getAlgorithm() == null) {
            throw new IllegalStateException("Algorithm header is not set");
        }
    }
}
