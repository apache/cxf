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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;

@Priority(Priorities.JWE_WRITE_PRIORITY)
public class JweJsonWriterInterceptor extends AbstractJweJsonWriterProvider implements WriterInterceptor {
    private boolean contentTypeRequired = true;
    private boolean useJweOutputStream;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        if (ctx.getEntity() == null) {
            ctx.proceed();
            return;
        }
        OutputStream actualOs = ctx.getOutputStream();
        List<JweEncryptionProvider> providers = getInitializedEncryptionProviders();
        
        String ctString = null;
        MediaType contentMediaType = ctx.getMediaType();
        if (contentTypeRequired && contentMediaType != null) {
            if ("application".equals(contentMediaType.getType())) {
                ctString = contentMediaType.getSubtype();
            } else {
                ctString = JAXRSUtils.mediaTypeToString(contentMediaType);
            }
        }
        JweHeaders protectedHeaders = new JweHeaders(ContentAlgorithm.A128GCM);
        if (ctString != null) {
            protectedHeaders.setContentType(ctString);
        }
        List<KeyAlgorithm> keyAlgos = new ArrayList<KeyAlgorithm>();
        for (JweEncryptionProvider p : providers) {
            if (!keyAlgos.contains(p.getKeyAlgorithm())) {
                keyAlgos.add(p.getKeyAlgorithm());    
            }
        }
        List<JweHeaders> perRecipientUnprotectedHeaders = null;
        if (keyAlgos.size() == 1) {
            // Can be optionally set in shared unprotected headers 
            // or per-recipient headers
            protectedHeaders.setKeyEncryptionAlgorithm(keyAlgos.get(0));
        } else {
            perRecipientUnprotectedHeaders = new ArrayList<JweHeaders>();
            for (KeyAlgorithm keyAlgo : keyAlgos) {
                JweHeaders headers = new JweHeaders();
                headers.setKeyEncryptionAlgorithm(keyAlgo);
                perRecipientUnprotectedHeaders.add(headers);
            }
        }
        if (useJweOutputStream) {
            //TODO
        } else {
            CachedOutputStream cos = new CachedOutputStream(); 
            ctx.setOutputStream(cos);
            ctx.proceed();
            
            
            
            JweJsonProducer producer = new JweJsonProducer(protectedHeaders, cos.getBytes());
            String jweContent = producer.encryptWith(providers, perRecipientUnprotectedHeaders);
            
            setJoseMediaType(ctx);
            IOUtils.copy(new ByteArrayInputStream(StringUtils.toBytesUTF8(jweContent)), 
                         actualOs);
            actualOs.flush();
        }
    }
    
    private void setJoseMediaType(WriterInterceptorContext ctx) {
        MediaType joseMediaType = JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE_JSON);
        ctx.setMediaType(joseMediaType);
    }
    
    public void setUseJweOutputStream(boolean useJweOutputStream) {
        this.useJweOutputStream = useJweOutputStream;
    }

    
    
}
