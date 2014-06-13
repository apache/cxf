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
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.oauth2.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProperties;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jws.PrivateKeyJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.rs.security.oauth2.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;

public class JwsMessageBodyProvider implements 
    MessageBodyWriter<JwtToken>, MessageBodyReader<JwtToken> {
    private static final String RSSEC_SIGNATURE_PROPS = "rs-security.signature.properties";
    private static final String RSSEC_KEY_PSWD_PROVIDER = "org.apache.rs.security.crypto.private.provider";
    
    private JwsSignatureProperties sigProperties;
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
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier();
        if (theSigVerifier == null) {
            throw new SecurityException();
        }
        JwsCompactConsumer p = new JwsCompactConsumer(IOUtils.readStringFromStream(is), 
                                                      sigProperties);
        p.verifySignatureWith(theSigVerifier);
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
        
        JwsSignatureProvider theSigProvider = getInitializedSigProvider();
        if (theSigProvider == null) {
            throw new SecurityException();
        }
        JwsCompactProducer p = new JwsCompactProducer(token);
        p.signWith(theSigProvider);
        IOUtils.copy(new ByteArrayInputStream(p.getSignedEncodedToken().getBytes("UTF-8")), os);
    }

    
    public void setSigProvider(JwsSignatureProvider sigProvider) {
        this.sigProvider = sigProvider;
    }

    
    public void setSigVerifier(JwsSignatureVerifier sigVerifier) {
        this.sigVerifier = sigVerifier;
    }

    protected JwsSignatureProvider getInitializedSigProvider() {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            return null;
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            return null;
        }
        
        PrivateKeyPasswordProvider cb = (PrivateKeyPasswordProvider)m.getContextualProperty(RSSEC_KEY_PSWD_PROVIDER);
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PrivateKey pk = CryptoUtils.loadPrivateKey(propLoc, bus, cb);
        return new PrivateKeyJwsSignatureProvider(pk);
    }
    
    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (sigVerifier != null) {
            return sigVerifier;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            return null;
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_SIGNATURE_PROPS);
        if (propLoc == null) {
            return null;
        }
        
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PublicKey pk = CryptoUtils.loadPublicKey(propLoc, bus);
        return new PublicKeyJwsSignatureVerifier(pk);
    }

    public void setSigProperties(JwsSignatureProperties sigProperties) {
        this.sigProperties = sigProperties;
    }
}
