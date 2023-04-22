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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.cxf.common.util.Base64UrlOutputStream;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsOutputStream;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

@Priority(Priorities.JWS_WRITE_PRIORITY)
public class JwsWriterInterceptor extends AbstractJwsWriterProvider implements WriterInterceptor {
    private static final Set<String> DEFAULT_PROTECTED_HTTP_HEADERS = 
        new HashSet<>(Arrays.asList(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
    private Set<String> protectedHttpHeaders = DEFAULT_PROTECTED_HTTP_HEADERS;
    private boolean protectHttpHeaders;
    
    private boolean contentTypeRequired = true;
    private boolean useJwsOutputStream;
    private boolean encodePayload = true;
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
    
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        if (ctx.getEntity() == null) {
            ctx.proceed();
            return;
        }
        JwsHeaders headers = new JwsHeaders();
        JwsSignatureProvider sigProvider = getInitializedSigProvider(headers);
        setContentTypeIfNeeded(headers, ctx);
        if (!encodePayload) {
            headers.setPayloadEncodingStatus(false);
        }
        protectHttpHeadersIfNeeded(ctx, headers);
        OutputStream actualOs = ctx.getOutputStream();
        if (useJwsOutputStream) {
            JwsSignature jwsSignature = sigProvider.createJwsSignature(headers);
            JoseUtils.traceHeaders(headers);
            JwsOutputStream jwsStream = new JwsOutputStream(actualOs, jwsSignature, true);
            byte[] headerBytes = StringUtils.toBytesUTF8(writer.toJson(headers));
            Base64UrlUtility.encodeAndStream(headerBytes, 0, headerBytes.length, jwsStream);
            jwsStream.write(new byte[]{'.'});

            Base64UrlOutputStream base64Stream = null;
            if (encodePayload) {
                base64Stream = new Base64UrlOutputStream(jwsStream);
                ctx.setOutputStream(base64Stream);
            } else {
                ctx.setOutputStream(jwsStream);
            }
            ctx.proceed();
            setJoseMediaType(ctx);
            if (base64Stream != null) {
                base64Stream.flush();
            }
            jwsStream.flush();
        } else {
            CachedOutputStream cos = new CachedOutputStream();
            ctx.setOutputStream(cos);
            ctx.proceed();
            JwsCompactProducer p = new JwsCompactProducer(headers, new String(cos.getBytes(), StandardCharsets.UTF_8));
            setJoseMediaType(ctx);
            writeJws(p, sigProvider, actualOs);
        }
    }

    public void setContentTypeRequired(boolean contentTypeRequired) {
        this.contentTypeRequired = contentTypeRequired;
    }

    public void setUseJwsOutputStream(boolean useJwsOutputStream) {
        this.useJwsOutputStream = useJwsOutputStream;
    }
    private void setContentTypeIfNeeded(JoseHeaders headers, WriterInterceptorContext ctx) {
        if (contentTypeRequired) {
            MediaType mt = ctx.getMediaType();
            if (mt != null
                && !JAXRSUtils.mediaTypeToString(mt).equals(JoseConstants.MEDIA_TYPE_JOSE)) {
                if ("application".equals(mt.getType())) {
                    headers.setContentType(mt.getSubtype());
                } else {
                    headers.setContentType(JAXRSUtils.mediaTypeToString(mt));
                }
            }
        }
    }

    private void setJoseMediaType(WriterInterceptorContext ctx) {
        MediaType joseMediaType = JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE);
        ctx.setMediaType(joseMediaType);
    }
    public void setEncodePayload(boolean encodePayload) {
        this.encodePayload = encodePayload;
    }
    protected void protectHttpHeadersIfNeeded(WriterInterceptorContext ctx, JwsHeaders jwsHeaders) {
        if (protectHttpHeaders) {
            JoseJaxrsUtils.protectHttpHeaders(ctx.getHeaders(), 
                                              jwsHeaders, 
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
