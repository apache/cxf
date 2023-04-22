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
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwe.JweCompactBuilder;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionInput;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweOutputStream;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

@Priority(Priorities.JWE_WRITE_PRIORITY)
public class JweWriterInterceptor implements WriterInterceptor {
    protected static final Logger LOG = LogUtils.getL7dLogger(JweWriterInterceptor.class);
    private Set<String> protectedHttpHeaders;
    private boolean protectHttpHeaders;
    private JweEncryptionProvider encryptionProvider;
    private boolean contentTypeRequired = true;
    private boolean useJweOutputStream;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        if (ctx.getEntity() == null) {
            ctx.proceed();
            return;
        }
        OutputStream actualOs = ctx.getOutputStream();
        JweHeaders jweHeaders = new JweHeaders();
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider(jweHeaders);

        String ctString = null;
        MediaType contentMediaType = ctx.getMediaType();
        if (contentTypeRequired && contentMediaType != null) {
            if ("application".equals(contentMediaType.getType())) {
                ctString = contentMediaType.getSubtype();
            } else {
                ctString = JAXRSUtils.mediaTypeToString(contentMediaType);
            }
        }
        if (ctString != null) {
            jweHeaders.setContentType(ctString);
        }
        protectHttpHeadersIfNeeded(ctx, jweHeaders);
        if (useJweOutputStream) {
            JweEncryptionOutput encryption =
                theEncryptionProvider.getEncryptionOutput(new JweEncryptionInput(jweHeaders));
            JoseUtils.traceHeaders(encryption.getHeaders());
            try {
                JweCompactBuilder.startJweContent(actualOs,
                                                   encryption.getHeaders(),
                                                   encryption.getEncryptedContentEncryptionKey(),
                                                   encryption.getIv());
            } catch (IOException ex) {
                LOG.warning("JWE encryption error");
                throw new JweException(JweException.Error.CONTENT_ENCRYPTION_FAILURE, ex);
            }
            JweOutputStream jweOutputStream = new JweOutputStream(actualOs, encryption.getCipher(),
                                                         encryption.getAuthTagProducer());

            ctx.setOutputStream(
                encryption.isCompressionSupported() ? new DeflaterOutputStream(jweOutputStream) : jweOutputStream);
            ctx.proceed();
            setJoseMediaType(ctx);
            jweOutputStream.finalFlush();
        } else {
            CachedOutputStream cos = new CachedOutputStream();
            ctx.setOutputStream(cos);
            ctx.proceed();
            String jweContent = theEncryptionProvider.encrypt(cos.getBytes(), jweHeaders);
            JoseUtils.traceHeaders(jweHeaders);
            setJoseMediaType(ctx);
            IOUtils.copy(new ByteArrayInputStream(StringUtils.toBytesUTF8(jweContent)),
                         actualOs);
            actualOs.flush();
        }
    }

    private void setJoseMediaType(WriterInterceptorContext ctx) {
        MediaType joseMediaType = JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE);
        ctx.setMediaType(joseMediaType);
    }

    protected JweEncryptionProvider getInitializedEncryptionProvider(JweHeaders headers) {
        if (encryptionProvider != null) {
            return encryptionProvider;
        }
        return JweUtils.loadEncryptionProvider(headers, true);
    }

    public void setUseJweOutputStream(boolean useJweOutputStream) {
        this.useJweOutputStream = useJweOutputStream;
    }

    public void setEncryptionProvider(JweEncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }

    protected void protectHttpHeadersIfNeeded(WriterInterceptorContext ctx, JweHeaders jweHeaders) {
        if (protectHttpHeaders) {
            JoseJaxrsUtils.protectHttpHeaders(ctx.getHeaders(),
                                              jweHeaders,
                                              protectedHttpHeaders);
        }

    }

    public void setProtectHttpHeaders(boolean protectHttpHeaders) {
        this.protectHttpHeaders = protectHttpHeaders;
    }

    public void setProtectedHttpHeaders(Set<String> protectedHttpHeaders) {
        this.protectedHttpHeaders = protectedHttpHeaders;
    }
}
