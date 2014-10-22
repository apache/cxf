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
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProtectedHeader;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

@Priority(Priorities.JWS_WRITE_PRIORITY)
public class JwsJsonWriterInterceptor extends AbstractJwsJsonWriterProvider implements WriterInterceptor {
    private boolean contentTypeRequired = true;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        
        List<JwsSignatureProvider> sigProviders = getInitializedSigProviders();
        OutputStream actualOs = ctx.getOutputStream();
        CachedOutputStream cos = new CachedOutputStream(); 
        ctx.setOutputStream(cos);
        ctx.proceed();
        JwsJsonProducer p = new JwsJsonProducer(new String(cos.getBytes(), "UTF-8"));
        for (JwsSignatureProvider signer : sigProviders) {
            JoseHeaders headers = new JoseHeaders();
            headers.setAlgorithm(signer.getAlgorithm());
            setContentTypeIfNeeded(headers, ctx);
            //TODO: support setting public JWK kid property as the unprotected header;
            //      the property would have to be associated with the individual signer
            p.signWith(signer, new JwsJsonProtectedHeader(headers), null);    
        }
        ctx.setMediaType(JAXRSUtils.toMediaType(JoseConstants.MEDIA_TYPE_JOSE_JSON));
        writeJws(p, actualOs);
    }
    
    public void setContentTypeRequired(boolean contentTypeRequired) {
        this.contentTypeRequired = contentTypeRequired;
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
}
