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
import java.security.PublicKey;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jwe.JweEncryptor;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweEncryptor;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

@Priority(Priorities.JWE_WRITE_PRIORITY)
public class JweWriterInterceptor implements WriterInterceptor {
    private static final String RSSEC_ENCRYPTION_PROPS = "rs-security.encryption.properties";
    private JweEncryptor encryptor;

    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        OutputStream actualOs = ctx.getOutputStream();
        CachedOutputStream cos = new CachedOutputStream(); 
        ctx.setOutputStream(cos);
        ctx.proceed();
        
        JweEncryptor theEncryptor = getInitializedEncryptor();
        String jweContent = theEncryptor.encrypt(cos.getBytes());
        IOUtils.copy(new ByteArrayInputStream(jweContent.getBytes("UTF-8")), actualOs);
        actualOs.flush();
    }
    
    protected JweEncryptor getInitializedEncryptor() {
        if (encryptor != null) {
            return encryptor;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            throw new SecurityException();
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_ENCRYPTION_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PublicKey pk = CryptoUtils.loadPublicKey(propLoc, bus);
        return new WrappedKeyJweEncryptor(new JweHeaders(Algorithm.RSA_OAEP.getJwtName(),
                                                         Algorithm.A256GCM.getJwtName()), 
                                          pk);
    }
    
}
