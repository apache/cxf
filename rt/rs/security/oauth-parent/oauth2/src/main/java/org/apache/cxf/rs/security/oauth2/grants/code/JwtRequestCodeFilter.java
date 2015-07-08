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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
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
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class JwtRequestCodeFilter implements AuthorizationCodeRequestFilter {
    private static final String REQUEST_PARAM = "request";
    private static final String REQUEST_URI_PARAM = "request_uri";
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private boolean verifyWithClientCertificates;
    private boolean verifyWithClientSecret;
    private boolean decryptWithClientSecret;
    private String issuer;
    private JsonMapObjectReaderWriter jsonHandler = new JsonMapObjectReaderWriter();
    @Override
    public MultivaluedMap<String, String> process(MultivaluedMap<String, String> params, 
                                                  UserSubject endUser,
                                                  Client client) {
        String requestToken = params.getFirst(REQUEST_PARAM);
        if (requestToken == null) {
            String requestUri = params.getFirst(REQUEST_URI_PARAM);
            if (requestUri != null && requestUri.startsWith(getPrefix(client))) {
                requestToken = WebClient.create(requestUri).get(String.class);
            }
        }
        if (requestToken != null) {
            JweDecryptionProvider theJweDecryptor = getInitializedDecryptionProvider(client);
            if (theJweDecryptor != null) {
                requestToken = theJweDecryptor.decrypt(requestToken).getContentText();
            }
            JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(client);
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
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Map) {
                    Map<String, Object> map = CastUtils.cast((Map<?, ?>)value);
                    value = jsonHandler.toJson(map);
                } else if (value instanceof List) {
                    List<Object> list = CastUtils.cast((List<?>)value);
                    value = jsonHandler.toJson(list);
                } 
                newParams.putSingle(key, value.toString());
            }
            return newParams;
        } else {
            return params;
        }
    }
    private String getPrefix(Client client) {
        //TODO: consider restricting to specific hosts
        return "https://";
    }
    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    public void setJweVerifier(JwsSignatureVerifier theJwsVerifier) {
        this.jwsVerifier = theJwsVerifier;
    }
    
    protected JweDecryptionProvider getInitializedDecryptionProvider(Client c) {
        if (jweDecryptor != null) {
            return jweDecryptor;    
        } 
        if (decryptWithClientSecret) {
            SecretKey key = CryptoUtils.decodeSecretKey(c.getClientSecret());
            return JweUtils.getDirectKeyJweDecryption(key, ContentAlgorithm.A128GCM);
        }
        return JweUtils.loadDecryptionProvider(false);
    }
    protected JwsSignatureVerifier getInitializedSigVerifier(Client c) {
        if (jwsVerifier != null) {
            return jwsVerifier;    
        } 
        if (verifyWithClientSecret) {
            byte[] hmac = CryptoUtils.decodeSequence(c.getClientSecret());
            return JwsUtils.getHmacSignatureVerifier(hmac, SignatureAlgorithm.HS256);
        } else if (verifyWithClientCertificates) {
            X509Certificate cert = 
                (X509Certificate)CryptoUtils.decodeCertificate(c.getApplicationCertificates().get(0));
            return JwsUtils.getPublicKeySignatureVerifier(cert, SignatureAlgorithm.RS256);
        } 
        return JwsUtils.loadSignatureVerifier(true);
    }
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    public void setVerifyWithClientCertificates(boolean verifyWithClientCertificates) {
        if (verifyWithClientSecret) {
            throw new SecurityException();          
        }
        this.verifyWithClientCertificates = verifyWithClientCertificates;
    }
    public void setVerifyWithClientSecret(boolean verifyWithClientSecret) {
        if (decryptWithClientSecret || verifyWithClientCertificates) {
            throw new SecurityException();          
        }
        this.verifyWithClientSecret = verifyWithClientSecret;
    }
    public void setDecryptWithClientSecret(boolean decryptWithClientSecret) {
        if (verifyWithClientSecret) {
            throw new SecurityException();          
        }
        this.decryptWithClientSecret = decryptWithClientSecret;
    }
}
