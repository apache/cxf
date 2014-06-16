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

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;

@Priority(Priorities.JWS_WRITE_PRIORITY)
public class JwsWriterInterceptor extends AbstractJwsWriterProvider implements WriterInterceptor {
    private boolean contentTypeRequired = true;
    @Override
    public void aroundWriteTo(WriterInterceptorContext ctx) throws IOException, WebApplicationException {
        OutputStream actualOs = ctx.getOutputStream();
        CachedOutputStream cos = new CachedOutputStream(); 
        ctx.setOutputStream(cos);
        ctx.proceed();
        
        JwtHeaders headers = new JwtHeaders();
        if (contentTypeRequired) {
            MediaType mt = ctx.getMediaType();
            if (mt != null) {
                headers.setContentType(JAXRSUtils.mediaTypeToString(mt));
            }
        }
        JwsCompactProducer p = new JwsCompactProducer(headers, new String(cos.getBytes(), "UTF-8"));
        writeJws(p, actualOs);
    }
    public void setContentTypeRequired(boolean contentTypeRequired) {
        this.contentTypeRequired = contentTypeRequired;
    }
        
}
