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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jwt.JwtHeaders;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;

public class JwsJwtMessageBodyWriter  extends AbstractJwsWriterProvider 
    implements MessageBodyWriter<JwtToken> {
    
    @Override
    public long getSize(JwtToken token, Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return cls == JwtToken.class;
    }

    @Override
    public void writeTo(JwtToken token, Class<?> cls, Type type, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os) throws IOException,
        WebApplicationException {
        JwsJwtCompactProducer p = new JwsJwtCompactProducer(token);
        JwtHeaders jwtHeaders = new JwtHeaders();
        JwsSignatureProvider sigProvider = getInitializedSigProvider(jwtHeaders);
        jwtHeaders.setContentType(JoseConstants.TYPE_JWT);
        writeJws(p, sigProvider, os);
    }
}
