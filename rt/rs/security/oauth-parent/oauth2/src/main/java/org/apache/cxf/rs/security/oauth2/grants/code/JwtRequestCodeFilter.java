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

import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AuthorizationCodeRequestFilter;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class JwtRequestCodeFilter implements AuthorizationCodeRequestFilter {
    private static final String REQUEST_PARAM = "request";
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuer;
    @Override
    public MultivaluedMap<String, String> process(MultivaluedMap<String, String> params, 
                                                  UserSubject endUser,
                                                  Client client) {
        String requestToken = params.getFirst(REQUEST_PARAM);
        if (requestToken != null) {
            JweDecryptionProvider theJweDecryptor = getInitializedDecryptionProvider();
            if (theJweDecryptor != null) {
                requestToken = theJweDecryptor.decrypt(requestToken).getContentText();
            }
            JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier();
            JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(requestToken);
            if (!consumer.verifySignatureWith(theSigVerifier)) {
                throw new SecurityException("Invalid Signature");
            }
            JwtClaims claims = consumer.getJwtClaims();
            String iss = issuer != null ? issuer : client.getClientId();  
            if (!iss.equals(claims.getIssuer())
                || claims.getClaim(OAuthConstants.CLIENT_ID) != null 
                && claims.getStringProperty(OAuthConstants.CLIENT_ID).equals(client.getClientId())) {
                throw new SecurityException();
            }
            MultivaluedMap<String, String> newParams = new MetadataMap<String, String>();
            Map<String, Object> claimsMap = claims.asMap();
            for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
                newParams.putSingle(entry.getKey(), entry.getValue().toString());
            }
            return newParams;
        } else {
            return params;
        }
    }
    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    public void setJweVerifier(JwsSignatureVerifier theJwsVerifier) {
        this.jwsVerifier = theJwsVerifier;
    }
    
    protected JweDecryptionProvider getInitializedDecryptionProvider() {
        if (jweDecryptor != null) {
            return jweDecryptor;    
        } 
        return JweUtils.loadDecryptionProvider(false);
    }
    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (jwsVerifier != null) {
            return jwsVerifier;    
        } 
        return JwsUtils.loadSignatureVerifier(true);
    }
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
