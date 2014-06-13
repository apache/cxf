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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PrivateKey;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;

public class AbstractJwsWriterProvider {
    private static final String RSSEC_SIGNATURE_PROPS = "rs-security.signature.properties";
    private static final String RSSEC_KEY_PSWD_PROVIDER = "org.apache.rs.security.crypto.private.provider";
    
    private JwsSignatureProvider sigProvider;
    
    public void setSigProvider(JwsSignatureProvider sigProvider) {
        this.sigProvider = sigProvider;
    }

    
    protected JwsSignatureProvider getInitializedSigProvider() {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            throw new SecurityException();
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        
        PrivateKeyPasswordProvider cb = (PrivateKeyPasswordProvider)m.getContextualProperty(RSSEC_KEY_PSWD_PROVIDER);
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PrivateKey pk = CryptoUtils.loadPrivateKey(propLoc, bus, cb);
        return new PrivateKeyJwsSignatureProvider(pk);
    }
    
    public void writeJws(JwsCompactProducer p, OutputStream os) throws IOException {
        JwsSignatureProvider theSigProvider = getInitializedSigProvider();
        p.signWith(theSigProvider);
        IOUtils.copy(new ByteArrayInputStream(p.getSignedEncodedJws().getBytes("UTF-8")), os);
    }
}
