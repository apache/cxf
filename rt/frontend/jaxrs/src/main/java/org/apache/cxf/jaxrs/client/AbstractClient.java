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
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.MessageObserver;

public class AbstractClient implements Client {

    protected static final MediaType WILDCARD = MediaType.valueOf("*/*");
    protected List<Interceptor> inInterceptors = new ModCountCopyOnWriteArrayList<Interceptor>();
    protected List<Interceptor> outInterceptors = new ModCountCopyOnWriteArrayList<Interceptor>();
    protected ConduitSelector conduitSelector;
    protected Bus bus;
    
    private MultivaluedMap<String, String> requestHeaders = new MetadataMap<String, String>();
    private ResponseBuilder responseBuilder;
    
    private URI baseURI;
    private UriBuilder currentBuilder;

    protected AbstractClient(URI baseURI, URI currentURI) {
        this.baseURI = baseURI;
        this.currentBuilder = new UriBuilderImpl(currentURI);
    }
    
    protected AbstractClient(Client client, boolean inheritHeaders) {
        this.baseURI = client.getCurrentURI();
        this.currentBuilder = new UriBuilderImpl(client.getCurrentURI());
        if (inheritHeaders) {
            this.requestHeaders = client.getHeaders();
        }
    }
    
    public Client header(String name, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        if (HttpHeaders.CONTENT_TYPE.equals(name) && values.length > 1) {
            throw new WebApplicationException();
        }
        for (Object o : values) {
            requestHeaders.add(name, o.toString());
        }
        return this;
    }

    public Client headers(MultivaluedMap<String, String> map) {
        requestHeaders.putAll(map);
        return this;
    }
    
    public Client accept(MediaType... types) {
        for (MediaType mt : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, mt.toString());
        }
        return this;
    }

    public Client type(MediaType ct) {
        return type(ct.toString());
    }
    
    public Client type(String type) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    public Client accept(String... types) {
        for (String type : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, type);
        }
        return this;
    }

    public Client cookie(Cookie cookie) {
        requestHeaders.add(HttpHeaders.COOKIE, cookie.toString());
        return this;
    }

    public Client modified(Date date, boolean ifNot) {
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();
        String hName = ifNot ? HttpHeaders.IF_UNMODIFIED_SINCE : HttpHeaders.IF_MODIFIED_SINCE;
        requestHeaders.putSingle(hName, dateFormat.format(date));
        return this;
    }

    public Client language(String language) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    public Client match(EntityTag tag, boolean ifNot) {
        String hName = ifNot ? HttpHeaders.IF_NONE_MATCH : HttpHeaders.IF_MATCH; 
        requestHeaders.putSingle(hName, tag.toString());
        return this;
    }

    public Client acceptLanguage(String... languages) {
        for (String s : languages) {
            requestHeaders.add(HttpHeaders.ACCEPT_LANGUAGE, s);
        }
        return this;
    }

    public Client acceptEncoding(String... encs) {
        for (String s : encs) {
            requestHeaders.add(HttpHeaders.ACCEPT_ENCODING, s);
        }
        return this;
    }

    public Client encoding(String enc) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_ENCODING, enc);
        return this;
    }
    
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(requestHeaders);
        return map;
    }
    
    public URI getBaseURI() {
        return baseURI;
    }

    public URI getCurrentURI() {
        return getCurrentBuilder().clone().build();
    }
    
    public Response getResponse() {
        if (responseBuilder == null) {
            throw new IllegalStateException();
        }
        Response r = responseBuilder.build();
        responseBuilder = null;
        return r;
    }
    
    public Client reset() {
        requestHeaders.clear();
        resetResponse();
        return this;
    }
    
    protected List<MediaType> getAccept() {
        List<String> headers = requestHeaders.get(HttpHeaders.ACCEPT);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        List<MediaType> types = new ArrayList<MediaType>();
        for (String s : headers) {
            types.add(MediaType.valueOf(s));
        }
        return types;
    }

    
    protected MediaType getType() {
        String type = requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        return type == null ? null : MediaType.valueOf(type);
    }

    protected UriBuilder getCurrentBuilder() {
        return currentBuilder;
    }


    protected void resetResponse() {
        responseBuilder = null;
    }
    
    protected void resetBaseAddress(URI uri) {
        baseURI = uri;
        resetCurrentBuilder(uri);
    }
    
    protected void resetCurrentBuilder(URI uri) {
        currentBuilder = new UriBuilderImpl(uri);
    }
    
    protected ResponseBuilder setResponseBuilder(HttpURLConnection conn) throws Throwable {
        
        if (conn == null) {
            throw new WebApplicationException(); 
        }
        
        int status = conn.getResponseCode();
        responseBuilder = Response.status(status);
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            for (String s : entry.getValue()) {
                responseBuilder.header(entry.getKey(), s);
            }
        }
        if (status >= 400) {
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    responseBuilder.entity(IOUtils.readStringFromStream(errorStream));
                }
            } catch (Exception ex) {
                // nothing we can do really
            }
        }
        return responseBuilder;
    }

    @SuppressWarnings("unchecked")
    protected static void writeBody(Object o, Class<?> cls, Type type, Annotation[] anns, 
        MultivaluedMap<String, String> headers, OutputStream os) {
        
        if (o == null) {
            return;
        }
        
        MediaType contentType = MediaType.valueOf(headers.getFirst("Content-Type")); 
        
        MessageBodyWriter mbr = ProviderFactory.getInstance().createMessageBodyWriter(
            cls, type, anns, contentType, new MessageImpl());
        if (mbr != null) {
            try {
                mbr.writeTo(o, cls, type, anns, contentType, headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else {
            throw new WebApplicationException();
        }
                                                                                 
    }
    
    @SuppressWarnings("unchecked")
    protected static Object readBody(Response r, HttpURLConnection conn, Class<?> cls, Type type, 
                                     Annotation[] anns) {

        try {
            int status = conn.getResponseCode();
            if (status < 200 || status == 204 || status > 300) {
                return null;
            }
        } catch (IOException ex) {
            // won't happen at this stage
        }
        
        MediaType contentType = getResponseContentType(r);
        
        MessageBodyReader mbr = ProviderFactory.getInstance().createMessageBodyReader(
            cls, type, anns, contentType, new MessageImpl());
        if (mbr != null) {
            try {
                return mbr.readFrom(cls, type, anns, contentType, r.getMetadata(), conn.getInputStream());
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else {
            throw new WebApplicationException();
        }
                                                                                 
    }
    
    private static MediaType getResponseContentType(Response r) {
        MultivaluedMap<String, Object> map = r.getMetadata();
        if (map.containsKey(HttpHeaders.CONTENT_TYPE)) {
            return MediaType.valueOf(map.getFirst(HttpHeaders.CONTENT_TYPE).toString());
        }
        return WILDCARD;
    }
    
    protected static HttpURLConnection createHttpConnection(URI uri, String methodName) {
        try {
            URL url = uri.toURL();
            HttpURLConnection connect = (HttpURLConnection)url.openConnection();
            connect.setDoOutput(true);
            connect.setRequestMethod(methodName);
            return connect;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    protected static void setAllHeaders(MultivaluedMap<String, String> headers, HttpURLConnection conn) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            StringBuilder b = new StringBuilder();    
            for (int i = 0; i < entry.getValue().size(); i++) {
                String value = entry.getValue().get(i);
                b.append(value);
                if (i + 1 < entry.getValue().size()) {
                    b.append(',');
                }
            }
            conn.setRequestProperty(entry.getKey(), b.toString());
        }
    }
    
    protected void setConduitSelector(ConduitSelector cs) {
        this.conduitSelector = cs;
    }
    
    protected void setBus(Bus bus) {
        this.bus = bus;
    }
    
    protected void prepareConduitSelector(Message message) {
        conduitSelector.prepare(message);
        message.getExchange().put(ConduitSelector.class, conduitSelector);
    }
    
    protected PhaseInterceptorChain setupOutInterceptorChain(Endpoint endpoint) { 
        PhaseManager pm = bus.getExtension(PhaseManager.class);
        List<Interceptor> i1 = bus.getOutInterceptors();
        List<Interceptor> i2 = outInterceptors;
        List<Interceptor> i3 = endpoint.getOutInterceptors();
        return new PhaseChainCache().get(pm.getOutPhases(), i1, i2, i3);
    }
    
    protected PhaseInterceptorChain setupInInterceptorChain(Endpoint endpoint) { 
        PhaseManager pm = bus.getExtension(PhaseManager.class);
        List<Interceptor> i1 = bus.getInInterceptors();
        List<Interceptor> i2 = inInterceptors;
        List<Interceptor> i3 = endpoint.getInInterceptors();
        return new PhaseChainCache().get(pm.getInPhases(), i1, i2, i3);
    }
    
    protected Message createMessage(String httpMethod, 
                                    MultivaluedMap<String, String> headers,
                                    String address) {
        Message m = conduitSelector.getEndpoint().getBinding().createMessage();
        m.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        m.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        
        m.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put(Message.ENDPOINT_ADDRESS, address);
        m.put(Message.CONTENT_TYPE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
        
        
        Exchange exchange = new ExchangeImpl();
        exchange.setSynchronous(true);
        exchange.setOutMessage(m);
        exchange.put(Bus.class, bus);
        exchange.put(MessageObserver.class, new ClientMessageObserver());
        exchange.setOneWay(false);
        m.setExchange(exchange);
        
        PhaseInterceptorChain chain = setupOutInterceptorChain(conduitSelector.getEndpoint());
        m.setInterceptorChain(chain);
        
        //setup conduit selector
        prepareConduitSelector(m);
        
        return m;
    }

    protected void setInInterceptors(List<Interceptor> interceptors) {
        inInterceptors = interceptors;
    }

    protected void setOutInterceptors(List<Interceptor> interceptors) {
        outInterceptors = interceptors;
    }
    
    private class ClientMessageObserver implements MessageObserver {

        public void onMessage(Message m) {
            
            Message message = conduitSelector.getEndpoint().getBinding().createMessage(m);
            message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
            message.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
            PhaseInterceptorChain chain = setupInInterceptorChain(conduitSelector.getEndpoint());
            message.setInterceptorChain(chain);
            Bus origBus = BusFactory.getThreadDefaultBus(false);
            BusFactory.setThreadDefaultBus(bus);

            // execute chain
            try {
                chain.doIntercept(message);
            } finally {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
        
    }
}
