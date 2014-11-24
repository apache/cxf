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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsFactory;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class AbstractJwsWriterProvider {
    private static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    private static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    private static final String JWS_CONTEXT_PROPERTY = "org.apache.cxf.jws.context";
    private JwsSignatureProvider sigProvider;
    
    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected JwsSignatureProvider getInitializedSigProvider(JoseHeaders headers) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        Object factory = m.getContextualProperty(JwsFactory.class.getName());
        if (factory != null) {
            return ((JwsFactory)factory).getJwsSignatureProvider();
        }
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_OUT_PROPS, RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        JwsSignatureProvider theSigProvider = JwsUtils.loadSignatureProvider(propLoc, m); 
        headers.setAlgorithm(theSigProvider.getAlgorithm());
        return theSigProvider;
    }
    protected void setRequestContextProperty(Message m, JoseHeaders headers) {    
        String context = (String)m.getContextualProperty(JWS_CONTEXT_PROPERTY);
        if (context != null) {
            headers.setHeader(JWS_CONTEXT_PROPERTY, context);
        }
    }
    protected void writeJws(JwsCompactProducer p, JwsSignatureProvider theSigProvider, OutputStream os) 
        throws IOException {
        p.signWith(theSigProvider);
        byte[] bytes = StringUtils.toBytesUTF8(p.getSignedEncodedJws());
        IOUtils.copy(new ByteArrayInputStream(bytes), os);
    }
    
}
