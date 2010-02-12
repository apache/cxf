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
import java.util.Date;
import java.util.HashMap;
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
import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
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
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.ClientOnlyHTTPTransportFactory;

/**
 * Common proxy and http-centric client implementation
 *
 */
public class AbstractClient implements Client {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractClient.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractClient.class);
    private static final String REQUEST_CONTEXT = "RequestContext";
    private static final String RESPONSE_CONTEXT = "ResponseContext";
    
    protected ClientConfiguration cfg = new ClientConfiguration();
    private ClientState state;
    
    protected AbstractClient(URI baseURI) {
        this.state = new LocalClientState(baseURI);
    }
    
    protected AbstractClient(ClientState initialState) {
        this.state = initialState;
    }
    
    /**
     * {@inheritDoc}
     */
    public Client header(String name, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        if (HttpHeaders.CONTENT_TYPE.equals(name)) {
            if (values.length > 1) {
                throw new IllegalArgumentException("Content-Type can have a single value only");
            }
            type(values[0].toString());
        } else {
            for (Object o : values) {
                possiblyAddHeader(name, o.toString());
            }
        }
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public Client headers(MultivaluedMap<String, String> map) {
        state.getRequestHeaders().putAll(map);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public Client accept(MediaType... types) {
        for (MediaType mt : types) {
            possiblyAddHeader(HttpHeaders.ACCEPT, mt.toString());
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
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client accept(String... types) {
        for (String type : types) {
            possiblyAddHeader(HttpHeaders.ACCEPT, type);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client cookie(Cookie cookie) {
        possiblyAddHeader(HttpHeaders.COOKIE, cookie.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client modified(Date date, boolean ifNot) {
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();
        String hName = ifNot ? HttpHeaders.IF_UNMODIFIED_SINCE : HttpHeaders.IF_MODIFIED_SINCE;
        state.getRequestHeaders().putSingle(hName, dateFormat.format(date));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client language(String language) {
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client match(EntityTag tag, boolean ifNot) {
        String hName = ifNot ? HttpHeaders.IF_NONE_MATCH : HttpHeaders.IF_MATCH; 
        state.getRequestHeaders().putSingle(hName, tag.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client acceptLanguage(String... languages) {
        for (String s : languages) {
            possiblyAddHeader(HttpHeaders.ACCEPT_LANGUAGE, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client acceptEncoding(String... encs) {
        for (String s : encs) {
            possiblyAddHeader(HttpHeaders.ACCEPT_ENCODING, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Client encoding(String enc) {
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, enc);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(state.getRequestHeaders());
        return map;
    }
    
    /**
     * {@inheritDoc}
     */
    public URI getBaseURI() {
        return state.getBaseURI();
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
        if (state.getResponseBuilder() == null) {
            return null;
        }
        return state.getResponseBuilder().build();
    }
    
    /**
     * {@inheritDoc}
     */
    public Client reset() {
        state.reset();
        return this;
    }

    private void possiblyAddHeader(String name, String value) {
        if (!isDuplicate(name, value)) {
            state.getRequestHeaders().add(name, value);
        }
    }
    
    private boolean isDuplicate(String name, String value) {
        List<String> values = state.getRequestHeaders().get(name);
        return values != null && values.contains(value) ? true : false;
    }
    
    protected ClientState getState() {
        return state;
    }
    
    protected UriBuilder getCurrentBuilder() {
        return state.getCurrentBuilder();
    }

    protected void resetResponse() {
        state.setResponseBuilder(null);
    }
    
    protected void resetBaseAddress(URI uri) {
        state.setBaseURI(uri);
        resetCurrentBuilder(uri);
    }
    
    protected void resetCurrentBuilder(URI uri) {
        state.setCurrentBuilder(new UriBuilderImpl(uri));
    }
    
    protected ResponseBuilder setResponseBuilder(HttpURLConnection conn, Exchange exchange) throws Throwable {
        Message inMessage = exchange.getInMessage();
        if (conn == null) {
            throw new WebApplicationException(); 
        }
        Integer responseCode = (Integer)exchange.get(Message.RESPONSE_CODE);
        if (responseCode == null) {
            //Invocation was never made to server, something stopped the outbound 
            //interceptor chain, we dont have a response code.
            //Do not call conn.getResponseCode() as that will
            //result in a call to the server when we have already decided not to.
            //Throw an exception if we have one
            Exception ex = exchange.getOutMessage().getContent(Exception.class);
            if (ex != null) {
                throw ex; 
            } else {
                throw new RuntimeException("Unknown client side exception");
            }
        } 
        int status = responseCode.intValue();
        ResponseBuilder currentResponseBuilder = Response.status(status);
        
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (null == entry.getKey()) {
                continue;
            }
            if (HttpUtils.isDateRelatedHeader(entry.getKey())) {
                currentResponseBuilder.header(entry.getKey(), entry.getValue());
            } else if (entry.getValue().size() > 0) {
                String[] values = entry.getValue().get(0).split(",");
                for (String s : values) {
                    String theValue = s.trim();
                    if (theValue.length() > 0) {
                        currentResponseBuilder.header(entry.getKey(), theValue);
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
                currentResponseBuilder.entity(errorStream);
            } catch (Exception ex) {
                // nothing we can do really
            }
        } else {
            try {
                InputStream stream = mStream == null ? conn.getInputStream() : mStream;
                currentResponseBuilder.entity(stream);
            } catch (Exception ex) {
                // it may that the successful response has no response body
            }
        }
        ResponseBuilder rb = currentResponseBuilder.clone();
        state.setResponseBuilder(currentResponseBuilder);
        return rb;
    }

    @SuppressWarnings("unchecked")
    protected void writeBody(Object o, Message outMessage, Class<?> cls, Type type, Annotation[] anns, 
        MultivaluedMap<String, String> headers, OutputStream os) {
        
        if (o == null) {
            return;
        }
        
        MediaType contentType = MediaType.valueOf(headers.getFirst("Content-Type")); 
        
        MessageBodyWriter mbw = ProviderFactory.getInstance(outMessage).createMessageBodyWriter(
            cls, type, anns, contentType, outMessage);
        if (mbw == null) {
            mbw = ProviderFactory.getInstance().createMessageBodyWriter(
                      cls, type, anns, contentType, outMessage);
        }
        if (mbw != null) {
            try {
                mbw.writeTo(o, cls, type, anns, contentType, headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new WebApplicationException(ex);
            }
             
        } else {
            reportNoMessageHandler("NO_MSG_WRITER", cls, contentType);
        }
                                                                                 
    }
    
    @SuppressWarnings("unchecked")
    protected Object readBody(Response r, HttpURLConnection conn, Message outMessage, Class<?> cls, 
                              Type type, Annotation[] anns) {

        InputStream inputStream = (InputStream)r.getEntity();
        if (inputStream == null) {
            return cls == Response.class ? r : null;
        }
        try {
            int status = conn.getResponseCode();
            if (status < 200 || status == 204 || status > 300) {
                Object length = r.getMetadata().getFirst(HttpHeaders.CONTENT_LENGTH);
                if (length == null || Integer.parseInt(length.toString()) == 0
                    || status >= 400) {
                    return cls == Response.class ? r : cls == InputStream.class ? inputStream : null;
                }
            }
        } catch (IOException ex) {
            // won't happen at this stage
        }
        
        MediaType contentType = getResponseContentType(r);
        
        MessageBodyReader mbr = ProviderFactory.getInstance(outMessage).createMessageBodyReader(
            cls, type, anns, contentType, outMessage);
        if (mbr == null) {
            ProviderFactory.getInstance().createMessageBodyReader(
                cls, type, anns, contentType, outMessage);
        }
        if (mbr != null) {
            try {
                return mbr.readFrom(cls, type, anns, contentType, 
                       new MetadataMap<String, Object>(r.getMetadata(), true, true), inputStream);
            } catch (Exception ex) {
                throw new WebApplicationException(ex);
            }
             
        } else if (cls == Response.class) {
            return r;
        } else {
            reportNoMessageHandler("NO_MSG_READER", cls, contentType);
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
    
    protected static void reportNoMessageHandler(String name, Class<?> cls, MediaType ct) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(name, 
                                                   BUNDLE,
                                                   cls,
                                                   ct.toString());
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
        try {
            cfg.getConduitSelector().prepare(message);
        } catch (Fault ex) {
            LOG.fine("Failure to prepare a message from conduit selector");
            if (ex.getCause() instanceof BusException) {
                String code = ((BusException)ex.getCause()).getCode();
                if ("NO_CONDUIT_INITIATOR".equals(code)) {
                    ConduitInitiatorManager cim = cfg.getBus().getExtension(ConduitInitiatorManager.class);
                    ClientOnlyHTTPTransportFactory factory = new ClientOnlyHTTPTransportFactory();
                    factory.setBus(cfg.getBus());                   
                    cim.registerConduitInitiator(
                        cfg.getConduitSelector().getEndpoint().getEndpointInfo().getTransportId(), factory);
                    cfg.getConduitSelector().prepare(message);
                } else {
                    throw ex;
                }
            }
        }
        message.getExchange().put(ConduitSelector.class, cfg.getConduitSelector());
    }
    
    protected static PhaseInterceptorChain setupOutInterceptorChain(ClientConfiguration cfg) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor> i1 = cfg.getBus().getOutInterceptors();
        List<Interceptor> i2 = cfg.getOutInterceptors();
        List<Interceptor> i3 = cfg.getConduitSelector().getEndpoint().getOutInterceptors();
        return new PhaseChainCache().get(pm.getOutPhases(), i1, i2, i3);
    }
    
    protected static PhaseInterceptorChain setupInInterceptorChain(ClientConfiguration cfg) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor> i1 = cfg.getBus().getInInterceptors();
        List<Interceptor> i2 = cfg.getInInterceptors();
        List<Interceptor> i3 = cfg.getConduitSelector().getEndpoint().getInInterceptors();
        
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
        exchange.put(MessageObserver.class, new ClientMessageObserver(cfg));
        exchange.put(Endpoint.class, cfg.getConduitSelector().getEndpoint());
        exchange.setOneWay("true".equals(headers.getFirst(Message.ONE_WAY_REQUEST)));
        m.setExchange(exchange);
        
        PhaseInterceptorChain chain = setupOutInterceptorChain(cfg);
        m.setInterceptorChain(chain);
        
        // context
        if (cfg.getRequestContext().size() > 0 || cfg.getResponseContext().size() > 0) {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(REQUEST_CONTEXT, cfg.getRequestContext());
            context.put(RESPONSE_CONTEXT, cfg.getResponseContext());
            m.put(Message.INVOCATION_CONTEXT, context);
            m.putAll(cfg.getRequestContext());
            exchange.putAll(cfg.getRequestContext());
        }
        
        //setup conduit selector
        prepareConduitSelector(m);
        exchange.put(Service.class, cfg.getConduitSelector().getEndpoint().getService());
        
        return m;
    }

    protected void setEmptyRequestProperty(Message outMessage, String httpMethod) {
        if ("POST".equals(httpMethod)) {
            outMessage.put("org.apache.cxf.post.empty", true);
        }
    }
    
    protected void setPlainOperationNameProperty(Message outMessage, String name) {
        outMessage.getExchange().put("org.apache.cxf.resource.operation.name", name);
    }
    
}
