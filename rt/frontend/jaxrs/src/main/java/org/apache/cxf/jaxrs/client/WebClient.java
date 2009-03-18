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
package org.apache.cxf.jaxrs.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.HTTPConduit;



public class WebClient extends AbstractClient implements MessageObserver {
    
    public WebClient(String baseAddress) {
        this(URI.create(baseAddress));
    }
    
    public WebClient(URI baseURI) {
        super(baseURI, baseURI);
    }
    
    public WebClient(Client client) {
        this(client, false);
    }
    
    public WebClient(Client client, boolean inheritHeaders) {
        super(client, inheritHeaders);
    }
    
    public Response invoke(String httpMethod, Object body) {
        return doInvoke(httpMethod, body, InputStream.class);
    }
    
    public Response post(Object o) {
        return invoke("POST", o);
    }
    
    public Response put(Object o) {
        return invoke("PUT", o);
    }

    public Response get() {
        return invoke("GET", null);
    }
    
    public Response head() {
        return invoke("HEAD", null);
    }
    
    public Response options() {
        return invoke("OPTIONS", null);
    }
    
    public Response delete() {
        return invoke("DELETE", null);
    }
    
    public Response form(Map<String, List<Object>> values) {
        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        return doInvoke("POST", values, InputStream.class);
    }
    
    public Response form(Form form) {
        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        return doInvoke("POST", form.getData(), InputStream.class);
    }
    
    public <T> T invoke(String httpMethod, Object body, Class<T> responseClass) {
        Response r = doInvoke(httpMethod, body, responseClass);
        
        if (r.getStatus() >= 400) {
            throw new WebApplicationException(r);
        }
        
        return responseClass.cast(r.getEntity());
    }
    
    public <T> T post(Object o, Class<T> responseClass) {
        return invoke("POST", o, responseClass);
    }
    
    public <T> T get(Class<T> responseClass) {
        return invoke("GET", null, responseClass);
    }
    
    public WebClient path(String path) {
        getCurrentBuilder().path(path);
        return this;
    }
    
    public WebClient query(String name, Object ...values) {
        for (Object o : values) {
            getCurrentBuilder().queryParam(name, o.toString());
        }
        return this;
    }
    
    public WebClient matrix(String name, Object ...values) {
        for (Object o : values) {
            getCurrentBuilder().matrixParam(name, o.toString());
        }
        return this;
    }
    
    public WebClient to(String newAddress, boolean forward) {
        if (forward) {
            if (!newAddress.startsWith(getBaseURI().toString())) {
                throw new IllegalArgumentException("Base address can not be preserved");
            }
            resetCurrentBuilder(URI.create(newAddress));
        } else {
            resetBaseAddress(URI.create(newAddress));
        }
        return this;
    }
    
    public WebClient back(boolean fast) {
        if (fast) {
            getCurrentBuilder().replacePath(getBaseURI().getPath());
        } else {
            URI uri = getCurrentURI();
            if (uri == getBaseURI()) {
                return this;
            }
            List<PathSegment> segments = JAXRSUtils.getPathSegments(uri.getPath(), false);
            getCurrentBuilder().replacePath(getBaseURI().getPath());
            for (int i = 0; i < segments.size() - 1; i++) {
                getCurrentBuilder().path(HttpUtils.fromPathSegment(segments.get(i)));
            }
            
        }
        return this;
    }
    
    public static WebClient createClient(String baseAddress) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        return bean.createWebClient();
    }
    
    
    @Override
    public WebClient type(MediaType ct) {
        return (WebClient)super.type(ct);
    }
    
    @Override
    public WebClient type(String type) {
        return (WebClient)super.type(type);
    }
    
    @Override
    public WebClient accept(MediaType... types) {
        return (WebClient)super.accept(types);
    }
    
    @Override
    public WebClient accept(String... types) {
        return (WebClient)super.accept(types);
    }
    
    @Override
    public WebClient language(String language) {
        return (WebClient)super.language(language);
    }
    
    @Override
    public WebClient acceptLanguage(String ...languages) {
        return (WebClient)super.acceptLanguage(languages);
    }
    
    @Override
    public WebClient encoding(String encoding) {
        return (WebClient)super.encoding(encoding);
    }
    
    @Override
    public WebClient acceptEncoding(String ...encodings) {
        return (WebClient)super.acceptEncoding(encodings);
    }
    
    @Override
    public WebClient match(EntityTag tag, boolean ifNot) {
        return (WebClient)super.match(tag, ifNot);
    }
    
    @Override
    public WebClient modified(Date date, boolean ifNot) {
        return (WebClient)super.modified(date, ifNot);
    }
    
    @Override
    public WebClient cookie(Cookie cookie) {
        return (WebClient)super.cookie(cookie);
    }
    
    @Override
    public WebClient header(String name, Object... values) {
        return (WebClient)super.header(name, values);
    }
    
    @Override
    public WebClient headers(MultivaluedMap<String, String> map) {
        return (WebClient)super.headers(map);
    }
    
    @Override
    public WebClient reset() {
        return (WebClient)super.reset();
    }
    
    private Response doInvoke(String httpMethod, Object body, Class<?> responseClass) {
        
        MultivaluedMap<String, String> headers = getHeaders();
        if (body != null && headers.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
            headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_TYPE.toString());
        }
        if (responseClass != null && headers.getFirst(HttpHeaders.ACCEPT) == null) {
            headers.putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_TYPE.toString());
        }
        if (conduitSelector == null) {
            return doDirectInvocation(httpMethod, headers, body, responseClass);
        } else {
            return doChainedInvocation(httpMethod, headers, body, responseClass);
        }
    }

    protected Response doDirectInvocation(String httpMethod, 
        MultivaluedMap<String, String> headers, Object body, Class<?> responseClass) {
        
        HttpURLConnection conn = getConnection(httpMethod);
        
        setAllHeaders(headers, conn);
        if (body != null) {
            try {
                writeBody(body, body.getClass(), body.getClass(), 
                      new Annotation[]{}, headers, conn.getOutputStream());
            } catch (IOException ex) {
                throw new WebApplicationException(ex);
            }
        }
        return handleResponse(conn, responseClass);
    }
    
    protected Response doChainedInvocation(String httpMethod, 
        MultivaluedMap<String, String> headers, Object body, Class<?> responseClass) {

        Message m = createMessage(httpMethod, headers, getCurrentURI().toString(), this);
        
        if (body != null) {
            MessageContentsList contents = new MessageContentsList(body);
            m.setContent(List.class, contents);
            m.getInterceptorChain().add(new BodyWriter());
        }
        
        try {
            m.getInterceptorChain().doIntercept(m);
        } catch (Throwable ex) {
            // we'd like a user to get the whole Response anyway if needed
        }
        
        // TODO : this needs to be done in an inbound chain instead
        HttpURLConnection connect = (HttpURLConnection)m.get(HTTPConduit.KEY_HTTP_CONNECTION);
        return handleResponse(connect, responseClass);
    }
    
    protected Response handleResponse(HttpURLConnection conn, Class<?> responseClass) {
        try {
            ResponseBuilder rb = setResponseBuilder(conn).clone();
            Response currentResponse = rb.clone().build();
            Object entity = readBody(currentResponse, conn, responseClass, responseClass,
                                     new Annotation[]{});
            rb.entity(entity);
            
            return rb.build();
        } catch (Throwable ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    protected HttpURLConnection getConnection(String methodName) {
        return createHttpConnection(getCurrentBuilder().clone().build(), methodName);
    }
    
    public static Client client(Object proxy) {
        return (Client)proxy;
    }
    
    private class BodyWriter extends AbstractOutDatabindingInterceptor {

        public BodyWriter() {
            super(Phase.WRITE);
        }
        
        @SuppressWarnings("unchecked")
        public void handleMessage(Message m) throws Fault {
            
            OutputStream os = m.getContent(OutputStream.class);
            if (os == null) {
                return;
            }
            MessageContentsList objs = MessageContentsList.getContentsList(m);
            if (objs == null || objs.size() == 0) {
                return;
            }
            MultivaluedMap<String, String> headers = (MultivaluedMap)m.get(Message.PROTOCOL_HEADERS);
            Object body = objs.get(0);
            try {
                writeBody(body, body.getClass(), body.getClass(), new Annotation[]{}, headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            
        }
        
    }

    public void onMessage(Message message) {
        // do nothing for now
    }
}
