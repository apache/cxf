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

import java.security.PrivateKey;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsCompactProducer {
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
    private JwsHeaders headers;
    private String plainJwsPayload;
    private String signature;
    private boolean detached;
    public JwsCompactProducer(String plainJwsPayload) {
        this(plainJwsPayload, false);
    }
    public JwsCompactProducer(String plainJwsPayload, boolean detached) {
        this(null, null, plainJwsPayload, detached);
    }
    public JwsCompactProducer(JwsHeaders headers, String plainJwsPayload) {
        this(headers, plainJwsPayload, false);
    }
    public JwsCompactProducer(JwsHeaders headers, String plainJwsPayload, boolean detached) {
        this(headers, null, plainJwsPayload, detached);
    }
    protected JwsCompactProducer(JwsHeaders headers, JsonMapObjectReaderWriter w, String plainJwsPayload) {
        this(headers, w, plainJwsPayload, false);
    }    
    protected JwsCompactProducer(JwsHeaders headers, JsonMapObjectReaderWriter w, String plainJwsPayload, 
                                 boolean detached) {    
        this.headers = headers;
        if (w != null) {
            this.writer = w;
        }
        this.plainJwsPayload = plainJwsPayload;
        this.detached = detached;
    }
    public JwsHeaders getJwsHeaders() {
        if (headers == null) {
            headers = new JwsHeaders();
        }
        return headers;
    }
    public String getUnsignedEncodedJws() {
        checkAlgorithm();
        return Base64UrlUtility.encode(writer.toJson(getJwsHeaders())) 
               + "." 
               + (detached ? "" : Base64UrlUtility.encode(plainJwsPayload));
    }
    private String getSigningInput() {
        checkAlgorithm();
        boolean unencoded = JwsUtils.isPayloadUnencoded(getJwsHeaders());
        if (unencoded && !detached) {
            throw new JwsException(JwsException.Error.INVALID_COMPACT_JWS);
        }
        return Base64UrlUtility.encode(writer.toJson(getJwsHeaders())) 
               + "." 
               + (unencoded ? plainJwsPayload : Base64UrlUtility.encode(plainJwsPayload));
    }
    public String getEncodedSignature() {
        return signature;
    }
    public String getSignedEncodedJws() {
        checkAlgorithm();
        boolean noSignature = StringUtils.isEmpty(signature);
        if (noSignature && !isPlainText()) {
            throw new IllegalStateException("Signature is not available");
        }
        return getUnsignedEncodedJws() + "." + (noSignature ? "" : signature);
    }
    public String signWith(JsonWebKey jwk) {
        return signWith(JwsUtils.getSignatureProvider(jwk, 
                        headers.getSignatureAlgorithm()));
    }
    
    public String signWith(PrivateKey key) {
        return signWith(JwsUtils.getPrivateKeySignatureProvider(key, 
                                   headers.getSignatureAlgorithm()));
    }
    public String signWith(byte[] key) {
        return signWith(JwsUtils.getHmacSignatureProvider(key, 
                   headers.getSignatureAlgorithm()));
    }
    
    public String signWith(JwsSignatureProvider signer) {
        byte[] bytes = StringUtils.toBytesUTF8(getSigningInput());
        byte[] sig = signer.sign(getJwsHeaders(), bytes);
        return setSignatureBytes(sig);
    }
    
    public String setSignatureText(String signatureText) {
        setEncodedSignature(Base64UrlUtility.encode(signatureText));
        return getSignedEncodedJws();
    }
    
    public boolean isPlainText() {
        return SignatureAlgorithm.NONE == getAlgorithm();
    }
    
    public String setSignatureBytes(byte[] signatureOctets) {
        setEncodedSignature(Base64UrlUtility.encode(signatureOctets));
        return getSignedEncodedJws();
    }
    
    private void setEncodedSignature(String sig) {
        this.signature = sig;
    }
    private SignatureAlgorithm getAlgorithm() {
        return getJwsHeaders().getSignatureAlgorithm();
    }
    private void checkAlgorithm() {
        if (getAlgorithm() == null) {
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
        }
    }
}
