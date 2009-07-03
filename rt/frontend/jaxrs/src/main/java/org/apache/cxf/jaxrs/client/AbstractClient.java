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
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.MessageObserver;

/**
 * Common proxy and http-centric client implementation
 *
 */
public class AbstractClient implements Client {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractClient.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractClient.class);

    protected ClientConfiguration cfg = new ClientConfiguration();
    
    private MultivaluedMap<String, String> requestHeaders = new MetadataMap<String, String>();
    private ResponseBuilder responseBuilder;
    
    private URI baseURI;
    private UriBuilder currentBuilder;

    protected AbstractClient(URI baseURI, URI currentURI) {
        this.baseURI = baseURI;
        this.currentBuilder = new UriBuilderImpl(currentURI);
    }
    
    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Client headers(MultivaluedMap<String, String> map) {
        requestHeaders.putAll(map);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public Client accept(MediaType... types) {
        for (MediaType mt : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, mt.toString());
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client type(MediaType ct) {
        return type(ct.toString());
    }
    
    /**
     * {@inheritDoc}
     */
    public Client type(String type) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client accept(String... types) {
        for (String type : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, type);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client cookie(Cookie cookie) {
        requestHeaders.add(HttpHeaders.COOKIE, cookie.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client modified(Date date, boolean ifNot) {
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();
        String hName = ifNot ? HttpHeaders.IF_UNMODIFIED_SINCE : HttpHeaders.IF_MODIFIED_SINCE;
        requestHeaders.putSingle(hName, dateFormat.format(date));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client language(String language) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client match(EntityTag tag, boolean ifNot) {
        String hName = ifNot ? HttpHeaders.IF_NONE_MATCH : HttpHeaders.IF_MATCH; 
        requestHeaders.putSingle(hName, tag.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client acceptLanguage(String... languages) {
        for (String s : languages) {
            requestHeaders.add(HttpHeaders.ACCEPT_LANGUAGE, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client acceptEncoding(String... encs) {
        for (String s : encs) {
            requestHeaders.add(HttpHeaders.ACCEPT_ENCODING, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client encoding(String enc) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_ENCODING, enc);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(requestHeaders);
        return map;
    }
    
    /**
     * {@inheritDoc}
     */
    public URI getBaseURI() {
        return baseURI;
    }
    
    /**
     * {@inheritDoc}
     */
    public URI getCurrentURI() {
        return getCurrentBuilder().clone().buildFromEncoded();
    }

    /**
     * {@inheritDoc}
     */
    public Response getResponse() {
        if (responseBuilder == null) {
            return null;
        }
        return responseBuilder.build();
    }
    
    /**
     * {@inheritDoc}
     */
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
    
    protected ResponseBuilder setResponseBuilder(HttpURLConnection conn, Message inMessage) throws Throwable {
        
        if (conn == null) {
            throw new WebApplicationException(); 
        }
        
        int status = conn.getResponseCode();
        responseBuilder = Response.status(status);
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (null == entry.getKey()) {
                continue;
            }
            if (HttpUtils.isDateRelatedHeader(entry.getKey())) {
                responseBuilder.header(entry.getKey(), entry.getValue());
            } else if (entry.getValue().size() > 0) {
                String[] values = entry.getValue().get(0).split(",");
                for (String s : values) {
                    String theValue = s.trim();
                    if (theValue.length() > 0) {
                        responseBuilder.header(entry.getKey(), theValue);
                    }
                }
            }
        }
        InputStream mStream = null;
        if (inMessage != null) {
            mStream = inMessage.getContent(InputStream.class);
        }
        if (status >= 400) {
            try {
                InputStream errorStream = mStream == null ? conn.getErrorStream() : mStream;
                if (errorStream != null) {
                    responseBuilder.entity(IOUtils.readStringFromStream(errorStream));
                }
            } catch (Exception ex) {
                // nothing we can do really
            }
        } else {
            try {
                InputStream stream = mStream == null ? conn.getInputStream() : mStream;
                responseBuilder.entity(stream);
            } catch (Exception ex) {
                // it may that the successful response has no response body
            }
        }
        return responseBuilder;
    }

    @SuppressWarnings("unchecked")
    protected void writeBody(Object o, Message m, Class<?> cls, Type type, Annotation[] anns, 
        MultivaluedMap<String, String> headers, OutputStream os) {
        
        if (o == null) {
            return;
        }
        
        MediaType contentType = MediaType.valueOf(headers.getFirst("Content-Type")); 
        
        MessageBodyWriter mbw = ProviderFactory.getInstance(m).createMessageBodyWriter(
            cls, type, anns, contentType, m);
        if (mbw == null) {
            mbw = ProviderFactory.getInstance().createMessageBodyWriter(
                      cls, type, anns, contentType, m);
        }
        if (mbw != null) {
            try {
                mbw.writeTo(o, cls, type, anns, contentType, headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else {
            reportNoMessageHandler("NO_MSG_WRITER", cls);
        }
                                                                                 
    }
    
    @SuppressWarnings("unchecked")
    protected Object readBody(Response r, HttpURLConnection conn, Message inMessage, Class<?> cls, 
                              Type type, Annotation[] anns) {

        InputStream inputStream = (InputStream)r.getEntity();
        if (inputStream == null) {
            return cls == Response.class ? cls : null;
        }
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status == 204 || status > 300) {
                Object length = r.getMetadata().getFirst(HttpHeaders.CONTENT_LENGTH);
                if (length == null || Integer.parseInt(length.toString()) == 0) {
                    return cls == Response.class ? cls : null;
                }
            }
        } catch (IOException ex) {
            // won't happen at this stage
        }
        
        MediaType contentType = getResponseContentType(r);
        
        MessageBodyReader mbr = ProviderFactory.getInstance(inMessage).createMessageBodyReader(
            cls, type, anns, contentType, inMessage);
        if (mbr == null) {
            ProviderFactory.getInstance().createMessageBodyReader(
                cls, type, anns, contentType, inMessage);
        }
        if (mbr != null) {
            try {
                return mbr.readFrom(cls, type, anns, contentType, 
                       new MetadataMap<String, Object>(r.getMetadata(), true, true), inputStream);
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else if (cls == Response.class) {
            return r;
        } else {
            reportNoMessageHandler("NO_MSG_READER", cls);
        }
        return null;                                                
    }
    
    // TODO : shall we just do the reflective invocation here ?
    protected static void addParametersToBuilder(UriBuilder ub, String paramName, Object pValue,
                                                 ParameterType pt) {
        if (pt != ParameterType.MATRIX && pt != ParameterType.QUERY) {
            throw new IllegalArgumentException("This method currently deal "
                                               + "with matrix and query parameters only");
        }
        if (!"".equals(paramName)) {
            addToBuilder(ub, paramName, pValue, pt);    
        } else {
            MultivaluedMap<String, Object> values = 
                InjectionUtils.extractValuesFromBean(pValue, "");
            for (Map.Entry<String, List<Object>> entry : values.entrySet()) {
                for (Object v : entry.getValue()) {
                    addToBuilder(ub, entry.getKey(), v, pt);
                }
            }
        }
    }

    private static void addToBuilder(UriBuilder ub, String paramName, Object pValue,
                                     ParameterType pt) {
        if (pt == ParameterType.MATRIX) {
            ub.matrixParam(paramName, pValue.toString());
        } else {
            ub.queryParam(paramName, pValue.toString());
        }
    }
    
    protected static void reportNoMessageHandler(String name, Class<?> cls) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(name, 
                                                   BUNDLE,
                                                   cls);
        LOG.severe(errorMsg.toString());
        throw new WebApplicationException(415);
    }
    
    private static MediaType getResponseContentType(Response r) {
        MultivaluedMap<String, Object> map = r.getMetadata();
        if (map.containsKey(HttpHeaders.CONTENT_TYPE)) {
            return MediaType.valueOf(map.getFirst(HttpHeaders.CONTENT_TYPE).toString());
        }
        return MediaType.WILDCARD_TYPE;
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
    
    protected ClientConfiguration getConfiguration() {
        return cfg;
    }
    
    protected void setConfiguration(ClientConfiguration config) {
        cfg = config;
    }
    
    protected void prepareConduitSelector(Message message) {
        cfg.getConduitSelector().prepare(message);
        message.getExchange().put(ConduitSelector.class, cfg.getConduitSelector());
    }
    
    protected PhaseInterceptorChain setupOutInterceptorChain(Endpoint endpoint) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor> i1 = cfg.getBus().getOutInterceptors();
        List<Interceptor> i2 = cfg.getOutInterceptors();
        List<Interceptor> i3 = endpoint.getOutInterceptors();
        return new PhaseChainCache().get(pm.getOutPhases(), i1, i2, i3);
    }
    
    protected PhaseInterceptorChain setupInInterceptorChain(Endpoint endpoint) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor> i1 = cfg.getBus().getInInterceptors();
        List<Interceptor> i2 = cfg.getInInterceptors();
        List<Interceptor> i3 = endpoint.getInInterceptors();
        return new PhaseChainCache().get(pm.getInPhases(), i1, i2, i3);
    }
    
    protected Message createSimpleMessage() {
        Message m = new MessageImpl();
        m.put(Message.PROTOCOL_HEADERS, getHeaders());
        return m;
    }
    
    protected Message createMessage(String httpMethod, 
                                    MultivaluedMap<String, String> headers,
                                    URI currentURI) {
        Message m = cfg.getConduitSelector().getEndpoint().getBinding().createMessage();
        m.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        m.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        
        m.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put(Message.ENDPOINT_ADDRESS, currentURI.toString());
        m.put(Message.REQUEST_URI, currentURI.toString());
        
        m.put(Message.CONTENT_TYPE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
        
        
        Exchange exchange = new ExchangeImpl();
        exchange.setSynchronous(true);
        exchange.setOutMessage(m);
        exchange.put(Bus.class, cfg.getBus());
        exchange.put(MessageObserver.class, new ClientMessageObserver());
        exchange.put(Endpoint.class, cfg.getConduitSelector().getEndpoint());
        exchange.setOneWay(false);
        m.setExchange(exchange);
        
        PhaseInterceptorChain chain = setupOutInterceptorChain(cfg.getConduitSelector().getEndpoint());
        m.setInterceptorChain(chain);
        
        //setup conduit selector
        prepareConduitSelector(m);
        
        return m;
    }

    private class ClientMessageObserver implements MessageObserver {

        public void onMessage(Message m) {
            
            Message message = cfg.getConduitSelector().getEndpoint().getBinding().createMessage(m);
            message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);
            message.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
            PhaseInterceptorChain chain = setupInInterceptorChain(cfg.getConduitSelector().getEndpoint());
            message.setInterceptorChain(chain);
            message.getExchange().setInMessage(message);
            Bus origBus = BusFactory.getThreadDefaultBus(false);
            BusFactory.setThreadDefaultBus(cfg.getBus());

            // execute chain
            try {
                chain.doIntercept(message);
            } finally {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
        
    }

    


}
