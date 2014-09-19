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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJwsWriterProvider {
    private static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    private static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    private static final String JSON_WEB_SIGNATURE_ALGO_PROP = "rs.security.jws.content.signature.algorithm";
    
    private JwsSignatureProvider sigProvider;
    
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider(JwtHeaders headers) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_OUT_PROPS, RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        try {
            Properties props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
            JwsSignatureProvider theSigProvider = null; 
            String rsaSignatureAlgo = null;
            if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(CryptoUtils.RSSEC_KEY_STORE_TYPE))) {
                JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_SIGN);
                rsaSignatureAlgo = getSignatureAlgo(props, jwk.getAlgorithm());
                theSigProvider = JwsUtils.getSignatureProvider(jwk, rsaSignatureAlgo);
            } else {
                rsaSignatureAlgo = getSignatureAlgo(props, null);
                RSAPrivateKey pk = (RSAPrivateKey)CryptoUtils.loadPrivateKey(m, props, 
                                                              CryptoUtils.RSSEC_SIG_KEY_PSWD_PROVIDER);
                theSigProvider = new PrivateKeyJwsSignatureProvider(pk, rsaSignatureAlgo);
            }
            if (theSigProvider == null) {
                throw new SecurityException();
            }
            headers.setAlgorithm(rsaSignatureAlgo);
            return theSigProvider;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    protected void writeJws(JwsCompactProducer p, JwsSignatureProvider theSigProvider, OutputStream os) 
        throws IOException {
        p.signWith(theSigProvider);
        IOUtils.copy(new ByteArrayInputStream(p.getSignedEncodedJws().getBytes("UTF-8")), os);
    }
    private String getSignatureAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_SIGNATURE_ALGO_PROP) : algo;
    }
}
