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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.util.Base64UrlOutputStream;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonOutputStream;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

@Priority(Priorities.JWS_WRITE_PRIORITY)
public class JwsJsonWriterInterceptor extends AbstractJwsJsonWriterProvider implements WriterInterceptor {
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
    private boolean contentTypeRequired = true;
    private boolean useJwsOutputStream;
    private boolean encodePayload = true;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        if (ctx.getEntity() == null) {
            ctx.proceed();
            return;
        }
        List<JwsSignatureProvider> sigProviders = getInitializedSigProviders();
        OutputStream actualOs = ctx.getOutputStream();
        if (useJwsOutputStream) {
            List<String> protectedHeaders = new ArrayList<String>(sigProviders.size());
            List<JwsSignature> signatures = new ArrayList<JwsSignature>(sigProviders.size());
            for (JwsSignatureProvider signer : sigProviders) {
                JwsHeaders protectedHeader = prepareProtectedHeader(ctx, signer);
                String encoded = Base64UrlUtility.encode(writer.toJson(protectedHeader));
                protectedHeaders.add(encoded);
                JwsSignature signature = signer.createJwsSignature(protectedHeader);
                byte[] start = StringUtils.toBytesUTF8(encoded + ".");
                signature.update(start, 0, start.length);
                signatures.add(signature);
            }    
            ctx.setMediaType(JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE_JSON));
            actualOs.write(StringUtils.toBytesUTF8("{\"payload\":\""));
            JwsJsonOutputStream jwsStream = new JwsJsonOutputStream(actualOs, protectedHeaders, signatures);
            
            Base64UrlOutputStream base64Stream = null;
            if (encodePayload) {
                base64Stream = new Base64UrlOutputStream(jwsStream);
                ctx.setOutputStream(base64Stream);
            } else {
                ctx.setOutputStream(jwsStream);
            }
            ctx.proceed();
            if (encodePayload) {
                base64Stream.flush();
            }
            jwsStream.flush();
        } else {
            CachedOutputStream cos = new CachedOutputStream(); 
            ctx.setOutputStream(cos);
            ctx.proceed();
            JwsJsonProducer p = new JwsJsonProducer(new String(cos.getBytes(), "UTF-8"));
            for (JwsSignatureProvider signer : sigProviders) {
                JwsHeaders protectedHeader = prepareProtectedHeader(ctx, signer);
                p.signWith(signer, protectedHeader, null);    
            }
            ctx.setMediaType(JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE_JSON));
            writeJws(p, actualOs);
        }
        
    }
    
    private JwsHeaders prepareProtectedHeader(WriterInterceptorContext ctx, 
                                              JwsSignatureProvider signer) {
        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(signer.getAlgorithm());
        setContentTypeIfNeeded(headers, ctx);
        if (!encodePayload) {
            headers.setPayloadEncodingStatus(false);
        }
        return headers;
    }
    
    public void setContentTypeRequired(boolean contentTypeRequired) {
        this.contentTypeRequired = contentTypeRequired;
    }
    public void setUseJwsJsonOutputStream(boolean useJwsJsonOutputStream) {
        this.useJwsOutputStream = useJwsJsonOutputStream;
    }
    private void setContentTypeIfNeeded(JoseHeaders headers, WriterInterceptorContext ctx) {    
        if (contentTypeRequired) {
            MediaType mt = ctx.getMediaType();
            if (mt != null 
                && !JAXRSUtils.mediaTypeToString(mt).equals(JoseConstants.MEDIA_TYPE_JOSE_JSON)) {
                if ("application".equals(mt.getType())) {
                    headers.setContentType(mt.getSubtype());
                } else {
                    headers.setContentType(JAXRSUtils.mediaTypeToString(mt));
                }
            }
        }
    }

    public void setEncodePayload(boolean encodePayload) {
        this.encodePayload = encodePayload;
    }
    
}
