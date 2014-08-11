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
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;

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
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.oauth2.jwe.JweCompactProducer;
import org.apache.cxf.rs.security.oauth2.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.oauth2.jwe.JweEncryptionState;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.JweOutputStream;
import org.apache.cxf.rs.security.oauth2.jwe.RSAOaepKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweEncryption;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

@Priority(Priorities.JWE_WRITE_PRIORITY)
public class JweWriterInterceptor implements WriterInterceptor {
    private static final String JSON_ENCRYPTION_OUT_PROPS = "rs.security.encryption.out.properties";
    private static final String JSON_ENCRYPTION_PROPS = "rs.security.encryption.properties";
    private static final String JSON_WEB_ENCRYPTION_CEK_ALGO_PROP = "rs.security.jwe.content.encryption.algorithm";
    private static final String JSON_WEB_ENCRYPTION_KEY_ALGO_PROP = "rs.security.jwe.key.encryption.algorithm";
    private static final String JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP = "rs.security.jwe.zip.algorithm";
    private JweEncryptionProvider encryptionProvider;
    private boolean contentTypeRequired = true;
    private boolean useJweOutputStream;
    private JwtHeadersWriter writer = new JwtTokenReaderWriter();
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        OutputStream actualOs = ctx.getOutputStream();
        
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider();
        
        String ctString = null;
        if (contentTypeRequired) {
            MediaType mt = ctx.getMediaType();
            if (mt != null) {
                ctString = JAXRSUtils.mediaTypeToString(mt);
            }
        }
        
        
        if (useJweOutputStream) {
            JweEncryptionState encryption = theEncryptionProvider.createJweEncryptionState(ctString);
            try {
                JweCompactProducer.startJweContent(actualOs,
                                                   encryption.getHeaders(), 
                                                   writer, 
                                                   encryption.getContentEncryptionKey(), 
                                                   encryption.getIv());
            } catch (IOException ex) {
                throw new SecurityException(ex);
            }
            OutputStream jweStream = new JweOutputStream(actualOs, encryption.getCipher(), 
                                                         encryption.getAuthTagProducer());
            if (encryption.isCompressionSupported()) {
                jweStream = new DeflaterOutputStream(jweStream);
            }
            
            ctx.setOutputStream(jweStream);
            ctx.proceed();
            jweStream.flush();
        } else {
            CachedOutputStream cos = new CachedOutputStream(); 
            ctx.setOutputStream(cos);
            ctx.proceed();
            String jweContent = theEncryptionProvider.encrypt(cos.getBytes(), ctString);
            IOUtils.copy(new ByteArrayInputStream(jweContent.getBytes("UTF-8")), actualOs);
            actualOs.flush();
        }
    }
    
    protected JweEncryptionProvider getInitializedEncryptionProvider() {
        if (encryptionProvider != null) {
            return encryptionProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, JSON_ENCRYPTION_OUT_PROPS, JSON_ENCRYPTION_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(propLoc, bus);
            PublicKey pk = CryptoUtils.loadPublicKey(m, props);
            if (!(pk instanceof RSAPublicKey)) {
                throw new SecurityException();
            }
            JweHeaders headers = new JweHeaders(props.getProperty(JSON_WEB_ENCRYPTION_KEY_ALGO_PROP),
                                                props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP));
            String compression = props.getProperty(JSON_WEB_ENCRYPTION_ZIP_ALGO_PROP);
            if (compression != null) {
                headers.setZipAlgorithm(compression);
            }
            
            return new WrappedKeyJweEncryption(headers, new RSAOaepKeyEncryptionAlgorithm((RSAPublicKey)pk));
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public void setUseJweOutputStream(boolean useJweOutputStream) {
        this.useJweOutputStream = useJweOutputStream;
    }

    public void setWriter(JwtHeadersWriter writer) {
        this.writer = writer;
    }

    public void setEncryptionProvider(JweEncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }
    
}
