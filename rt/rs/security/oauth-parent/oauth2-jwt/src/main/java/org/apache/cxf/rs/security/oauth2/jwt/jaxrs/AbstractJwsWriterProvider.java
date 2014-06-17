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
import java.util.Properties;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJwsWriterProvider {
    private static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    private static final String JSON_WEB_SIGNATURE_ALGO_PROP = "rs.security.jws.content.signature.algorithm";
    
    private JwsSignatureProvider sigProvider;
    
    public void setSigProvider(JwsSignatureProvider sigProvider) {
        this.sigProvider = sigProvider;
    }

    
    protected JwsSignatureProvider getInitializedSigProvider() {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = (String)m.getContextualProperty(RSSEC_SIGNATURE_OUT_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        try {
            Properties props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
            PrivateKey pk = CryptoUtils.loadPrivateKey(m, props, CryptoUtils.RSSEC_SIG_KEY_PSWD_PROVIDER);
            PrivateKeyJwsSignatureProvider provider = new PrivateKeyJwsSignatureProvider(pk);
            provider.setDefaultJwtAlgorithm(props.getProperty(JSON_WEB_SIGNATURE_ALGO_PROP));
            return provider;
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    protected void writeJws(JwsCompactProducer p, OutputStream os) throws IOException {
        JwsSignatureProvider theSigProvider = getInitializedSigProvider();
        p.signWith(theSigProvider);
        IOUtils.copy(new ByteArrayInputStream(p.getSignedEncodedJws().getBytes("UTF-8")), os);
    }
}
