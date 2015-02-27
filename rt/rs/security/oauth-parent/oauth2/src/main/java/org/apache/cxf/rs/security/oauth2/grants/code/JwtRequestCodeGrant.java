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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.net.URI;

import javax.crypto.SecretKey;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;



/**
 * Base Authorization Code Grant representation, captures the code 
 * and the redirect URI this code has been returned to, visible to the client
 */
public class JwtRequestCodeGrant extends AuthorizationCodeGrant {
    private static final long serialVersionUID = -3738825769770411453L;
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    private String clientSecret;
    private boolean encryptWithClientSecret;
    private boolean signWithClientSecret;
    // can be a client id
    private String issuer;
    public JwtRequestCodeGrant() {
    }
    
    public JwtRequestCodeGrant(String issuer) {
        this.issuer = issuer;
    }
    
    public JwtRequestCodeGrant(String code, String issuer) {
        super(code);
        this.issuer = issuer;
    }
    
    public JwtRequestCodeGrant(String code, URI uri, String issuer) {
        super(code, uri);
        this.issuer = issuer;
    }
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    public void setEncryptionProvider(JweEncryptionProvider encProvider) {
        this.encryptionProvider = encProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider() {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        if (signWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(clientSecret);
            return JwsUtils.getHmacSignatureProvider(hmac, AlgorithmUtils.HMAC_SHA_256_ALGO);
        } else {
            return JwsUtils.loadSignatureProvider(true);
        }
    }
    public MultivaluedMap<String, String> toMap() {
        String request = getRequest();
        MultivaluedMap<String, String> newMap = new MetadataMap<String, String>();
        newMap.putSingle("request", request);
        return newMap;
        
    }
    public String getRequest() {
        MultivaluedMap<String, String> map = super.toMap();
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);
        for (String key : map.keySet()) {
            claims.setClaim(key, map.getFirst(key));
        }
        JwsJwtCompactProducer producer = new JwsJwtCompactProducer(claims);
        JwsSignatureProvider theSigProvider = getInitializedSigProvider();
        String request = producer.signWith(theSigProvider);
        
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider();
        if (theEncryptionProvider != null) {
            request = theEncryptionProvider.encrypt(StringUtils.toBytesUTF8(request), null);
        }
        return request;
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider() {
        if (encryptionProvider != null) {
            return encryptionProvider;    
        } 
        if (encryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(clientSecret);
            return JweUtils.getDirectKeyJweEncryption(key, AlgorithmUtils.A128GCM_ALGO);
        } else {
            return JweUtils.loadEncryptionProvider(false);
        }
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    public void setEncryptWithClientSecret(boolean encryptWithClientSecret) {
        if (signWithClientSecret) {
            throw new SecurityException();
        }
        this.encryptWithClientSecret = encryptWithClientSecret;
    }
    public void setSignWithClientSecret(boolean signWithClientSecret) {
        if (encryptWithClientSecret) {
            throw new SecurityException();
        }
        this.signWithClientSecret = signWithClientSecret;
    }
}
