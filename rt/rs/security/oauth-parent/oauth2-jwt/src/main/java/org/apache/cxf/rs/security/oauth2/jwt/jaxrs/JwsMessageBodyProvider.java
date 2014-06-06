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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;

public class JwsMessageBodyProvider implements 
    MessageBodyWriter<JwtToken>, MessageBodyReader<JwtToken> {

    private JwsSignatureProvider sigProvider;
    private JwsSignatureVerifier sigVerifier;
    
    @Override
    public boolean isReadable(Class<?> cls, Type type, Annotation[] anns, MediaType mt) {
        return cls == JwtToken.class;
    }

    @Override
    public JwtToken readFrom(Class<JwtToken> cls, Type t, Annotation[] anns, MediaType mt,
                             MultivaluedMap<String, String> headers, InputStream is) throws IOException,
        WebApplicationException {
        JwsCompactConsumer p = new JwsCompactConsumer(IOUtils.readStringFromStream(is));
        p.verifySignatureWith(sigVerifier);
        return p.getJwtToken();
    }

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
        JwsCompactProducer p = new JwsCompactProducer(token);
        p.signWith(sigProvider);
        IOUtils.copy(new ByteArrayInputStream(p.getSignedEncodedToken().getBytes("UTF-8")), os);
    }

    
    public void setSigProvider(JwsSignatureProvider sigProvider) {
        this.sigProvider = sigProvider;
    }

    
    public void setSigVerifier(JwsSignatureVerifier sigVerifier) {
        this.sigVerifier = sigVerifier;
    }

}
