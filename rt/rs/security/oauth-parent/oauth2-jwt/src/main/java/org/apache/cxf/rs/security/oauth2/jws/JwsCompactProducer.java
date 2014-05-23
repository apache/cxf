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
import org.apache.cxf.rs.security.oauth2.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public class JwsCompactProducer {
    private JwtTokenWriter writer = new JwtTokenReaderWriter();
    private JwtToken token;
    private String signature;
    private String plainRep;
    
    public JwsCompactProducer(JwtToken token) {
        this(token, null);
    }
    public JwsCompactProducer(JwtToken token, JwtTokenWriter w) {
        this.token = token;
        if (w != null) {
            this.writer = w;
        }
    }
    public JwsCompactProducer(JwtHeaders headers, JwtClaims claims) {
        this(headers, claims, null);
    }
    public JwsCompactProducer(JwtHeaders headers, JwtClaims claims, JwtTokenWriter w) {
        this(new JwtToken(headers, claims), w);
    }
    
    public String getUnsignedEncodedToken() {
        if (plainRep == null) {
            plainRep = Base64UrlUtility.encode(writer.headersToJson(token.getHeaders())) 
                + "." 
                + Base64UrlUtility.encode(writer.claimsToJson(token.getClaims()));
        }
        return plainRep;
    }
    
    public String getSignedEncodedToken() {
        boolean noSignature = StringUtils.isEmpty(signature);
        if (noSignature && !isPlainText()) {
            throw new IllegalStateException("Signature is not available");
        }
        return getUnsignedEncodedToken() + "." + (noSignature ? "" : signature);
    }
    public void signWith(JwsSignatureProvider signer) { 
        setSignatureOctets(signer.sign(token.getHeaders(), getUnsignedEncodedToken()));
    }
    
    public void setSignatureText(String sig) {
        setEncodedSignature(Base64UrlUtility.encode(sig));
    }
    public void setSignatureOctets(byte[] bytes) {
        setEncodedSignature(Base64UrlUtility.encode(bytes));
    }
    private void setEncodedSignature(String sig) {
        this.signature = sig;
    }
    private boolean isPlainText() {
        return JwtConstants.PLAIN_TEXT_ALGO.equals(token.getHeaders().getAlgorithm());
    }
}
