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

import java.security.PublicKey;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProperties;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJwsReaderProvider {
    private static final String RSSEC_SIGNATURE_PROPS = "rs-security.signature.properties";
    
    private JwsSignatureVerifier sigVerifier;
    private JwsSignatureProperties sigProperties;
    
    public void setSigVerifier(JwsSignatureVerifier sigVerifier) {
        this.sigVerifier = sigVerifier;
    }

    public void setSigProperties(JwsSignatureProperties sigProperties) {
        this.sigProperties = sigProperties;
    }
    
    public JwsSignatureProperties getSigProperties() {
        return sigProperties;
    }
    
    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (sigVerifier != null) {
            return sigVerifier;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            return null;
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            return null;
        }
        
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PublicKey pk = CryptoUtils.loadPublicKey(propLoc, bus);
        return new PublicKeyJwsSignatureVerifier(pk);
    }
    
    
}
