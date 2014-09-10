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
package org.apache.cxf.rs.security.oauth2.jwt.jaxrs;

import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.oauth2.jwk.JsonWebKey;
import org.apache.cxf.rs.security.oauth2.jwk.JwkUtils;
import org.apache.cxf.rs.security.oauth2.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProperties;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJwsReaderProvider {
    private static final String RSSEC_SIGNATURE_IN_PROPS = "rs.security.signature.in.properties";
    private static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    
    private JwsSignatureVerifier sigVerifier;
    private JwsSignatureProperties sigProperties;
    private String defaultMediaType;
    
    public void setSignatureVerifier(JwsSignatureVerifier signatureVerifier) {
        this.sigVerifier = signatureVerifier;
    }

    public void setSignatureProperties(JwsSignatureProperties signatureProperties) {
        this.sigProperties = signatureProperties;
    }
    
    public JwsSignatureProperties getSigProperties() {
        return sigProperties;
    }
    
    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (sigVerifier != null) {
            return sigVerifier;    
        } 
        
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_IN_PROPS, RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(propLoc, bus);
            JwsSignatureVerifier theVerifier = null;
            if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(CryptoUtils.RSSEC_KEY_STORE_TYPE))) {
                JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props);
                if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
                    theVerifier = new PublicKeyJwsSignatureVerifier(jwk.toRSAPublicKey());
                } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) 
                    && Algorithm.isHmacSign(jwk.getAlgorithm())) {
                    theVerifier = 
                        new HmacJwsSignatureProvider((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
                } else {
                    // TODO: support elliptic curve keys
                }
                
            } else {
                theVerifier = new PublicKeyJwsSignatureVerifier(
                                  (RSAPublicKey)CryptoUtils.loadPublicKey(m, props));
            }
            return theVerifier;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }
    
    
}
