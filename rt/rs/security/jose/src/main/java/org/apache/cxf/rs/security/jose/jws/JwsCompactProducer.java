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

import java.security.interfaces.RSAPrivateKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsCompactProducer {
    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    private JoseHeaders headers;
    private String plainJwsPayload;
    private String signature;
    public JwsCompactProducer(String plainJwsPayload) {
        this(null, null, plainJwsPayload);
    }
    public JwsCompactProducer(JoseHeaders headers, String plainJwsPayload) {
        this(headers, null, plainJwsPayload);
    }
    protected JwsCompactProducer(JoseHeaders headers, JoseHeadersReaderWriter w, String plainJwsPayload) {
        this.headers = headers;
        if (w != null) {
            this.writer = w;
        }
        this.plainJwsPayload = plainJwsPayload;
    }
    public JoseHeaders getJoseHeaders() {
        if (headers == null) {
            headers = new JoseHeaders();
        }
        return headers;
    }
    public String getUnsignedEncodedJws() {
        return getUnsignedEncodedJws(false);
    }
    private String getUnsignedEncodedJws(boolean detached) {
        checkAlgorithm();
        return Base64UrlUtility.encode(writer.headersToJson(getJoseHeaders())) 
               + "." 
               + (detached ? "" : Base64UrlUtility.encode(plainJwsPayload));
    }
    public String getEncodedSignature() {
        return signature;
    }
    public String getSignedEncodedJws() {
        return getSignedEncodedJws(false);
    }
    public String getSignedEncodedJws(boolean detached) {
        checkAlgorithm();
        boolean noSignature = StringUtils.isEmpty(signature);
        if (noSignature && !isPlainText()) {
            throw new IllegalStateException("Signature is not available");
        }
        return getUnsignedEncodedJws(detached) + "." + (noSignature ? "" : signature);
    }
    public String signWith(JsonWebKey jwk) {
        return signWith(JwsUtils.getSignatureProvider(jwk, headers.getAlgorithm()));
    }
    
    public String signWith(RSAPrivateKey key) {
        return signWith(JwsUtils.getRSAKeySignatureProvider(key, headers.getAlgorithm()));
    }
    public String signWith(byte[] key) {
        return signWith(JwsUtils.getHmacSignatureProvider(key, headers.getAlgorithm()));
    }
    
    public String signWith(JwsSignatureProvider signer) {
        byte[] bytes = StringUtils.toBytesUTF8(getUnsignedEncodedJws());
        byte[] sig = signer.sign(getJoseHeaders(), bytes);
        return setSignatureBytes(sig);
    }
    
    public String setSignatureText(String signatureText) {
        setEncodedSignature(Base64UrlUtility.encode(signatureText));
        return getSignedEncodedJws();
    }
    
    public String setSignatureBytes(byte[] signatureOctets) {
        setEncodedSignature(Base64UrlUtility.encode(signatureOctets));
        return getSignedEncodedJws();
    }
    
    private void setEncodedSignature(String sig) {
        this.signature = sig;
    }
    private boolean isPlainText() {
        return AlgorithmUtils.PLAIN_TEXT_ALGO.equals(getAlgorithm());
    }
    private String getAlgorithm() {
        return getJoseHeaders().getAlgorithm();
    }
    private void checkAlgorithm() {
        if (getAlgorithm() == null) {
            throw new IllegalStateException("Algorithm header is not set");
        }
    }
}
