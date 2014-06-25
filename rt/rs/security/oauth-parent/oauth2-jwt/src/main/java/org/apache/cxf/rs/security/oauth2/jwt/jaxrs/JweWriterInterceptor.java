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
import java.util.Properties;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jwe.JweEncryptor;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.JweOutputStream;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweEncryptor;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

@Priority(Priorities.JWE_WRITE_PRIORITY)
public class JweWriterInterceptor implements WriterInterceptor {
    private static final String JSON_WEB_ENCRYPTION_OUT_PROPS = "rs.security.encryption.out.properties";
    private static final String JSON_WEB_ENCRYPTION_CEK_ALGO_PROP = "rs.security.jwe.content.encryption.algorithm";
    private static final String JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP = "rs.security.jwe.zip.algorithm";
    private JweEncryptor encryptor;
    private boolean contentTypeRequired = true;
    private boolean useJweOutputStream;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        OutputStream actualOs = ctx.getOutputStream();
        
        JweEncryptor theEncryptor = getInitializedEncryptor();
        
        String ctString = null;
        if (contentTypeRequired) {
            MediaType mt = ctx.getMediaType();
            if (mt != null) {
                ctString = JAXRSUtils.mediaTypeToString(mt);
            }
        }
        
        
        if (useJweOutputStream) {
            JweOutputStream jweStream = theEncryptor.createJweStream(actualOs, ctString);
            ctx.setOutputStream(jweStream);
            ctx.proceed();
            jweStream.flush();
        } else {
            CachedOutputStream cos = new CachedOutputStream(); 
            ctx.setOutputStream(cos);
            ctx.proceed();
            String jweContent = theEncryptor.encrypt(cos.getBytes(), ctString);
            IOUtils.copy(new ByteArrayInputStream(jweContent.getBytes("UTF-8")), actualOs);
            actualOs.flush();
        }
    }
    
    protected JweEncryptor getInitializedEncryptor() {
        if (encryptor != null) {
            return encryptor;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = (String)m.getContextualProperty(JSON_WEB_ENCRYPTION_OUT_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(propLoc, bus);
            PublicKey pk = CryptoUtils.loadPublicKey(m, props);
            JweHeaders headers = new JweHeaders(Algorithm.RSA_OAEP.getJwtName(),
                                                props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP));
            String compression = props.getProperty(JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP);
            if (compression != null) {
                headers.setZipAlgorithm(compression);
            }
            
            return new WrappedKeyJweEncryptor(headers, pk);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public void setUseJweOutputStream(boolean useJweOutputStream) {
        this.useJweOutputStream = useJweOutputStream;
    }
    
}
