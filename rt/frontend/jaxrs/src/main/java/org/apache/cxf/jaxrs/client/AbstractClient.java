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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
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
import org.apache.cxf.transport.MessageObserver;

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
        return state.getResponseBuilder().clone().build();
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
    
    protected MultivaluedMap<String, String> getTemplateParametersMap(URITemplate template, 
                                                                      List<Object> values) {
        if (values != null && values.size() != 0) { 
            List<String> vars = template.getVariables();
            MultivaluedMap<String, String> templatesMap =  new MetadataMap<String, String>(vars.size());
            for (int i = 0; i < vars.size(); i++) {
                if (i < values.size()) {
                    templatesMap.add(vars.get(i), values.get(i).toString());
                }
            }
            return templatesMap;
        } 
        return null;
    }
    
    protected ResponseBuilder setResponseBuilder(HttpURLConnection conn, Exchange exchange) throws Throwable {
        Message inMessage = exchange.getInMessage();
        if (conn == null) {
            // unlikely to occur
            throw new ClientWebApplicationException("HTTP Connection is null"); 
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
            if (entry.getValue().size() > 0) {
                if (HttpUtils.isDateRelatedHeader(entry.getKey())) {
                    currentResponseBuilder.header(entry.getKey(), entry.getValue().get(0));
                    continue;                    
                }
                for (String val : entry.getValue()) {
                    String[] values;
                    if (val == null || val.length() == 0) {
                        values = new String[]{""};
                    } else if (val.charAt(0) == '"' && val.charAt(val.length() - 1) == '"') {
                        // if the value starts with a quote and ends with a quote, we do a best
                        // effort attempt to determine what the individual values are.
                        values = parseQuotedHeaderValue(val);
                    } else {
                        boolean splitPossible = !(HttpHeaders.SET_COOKIE.equalsIgnoreCase(entry.getKey())
                            && val.toUpperCase().contains(HttpHeaders.EXPIRES.toUpperCase()));
                        values = splitPossible ? val.split(",") : new String[]{val};
                    }
                    for (String s : values) {
                        String theValue = s.trim();
                        if (theValue.length() > 0) {
                            currentResponseBuilder.header(entry.getKey(), theValue);
                        }
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
        if (mbw != null) {
            try {
                mbw.writeTo(o, cls, type, anns, contentType, headers, os);
                if (os != null) {
                    os.flush();
                }
            } catch (Exception ex) {
                reportMessageHandlerProblem("MSG_WRITER_PROBLEM", cls, contentType, ex, null);
            }
        } else {
            reportMessageHandlerProblem("NO_MSG_WRITER", cls, contentType, null, null);
        }
                                                                                 
    }
    
    @SuppressWarnings("unchecked")
    protected Object readBody(Response r, Message inMessage, Class<?> cls, 
                              Type type, Annotation[] anns) {

        InputStream inputStream = (InputStream)r.getEntity();
        if (inputStream == null) {
            return cls == Response.class ? r : null;
        }
        
        int status = r.getStatus();
        if (status < 200 || status == 204 || status > 300) {
            Object length = r.getMetadata().getFirst(HttpHeaders.CONTENT_LENGTH);
            if (length == null || Integer.parseInt(length.toString()) == 0
                || status >= 400) {
                return cls == Response.class ? r : status >= 400 ? inputStream : null;
            }
        }
        
        MediaType contentType = getResponseContentType(r);
        
        MessageBodyReader mbr = ProviderFactory.getInstance(inMessage).createMessageBodyReader(
            cls, type, anns, contentType, inMessage);
        if (mbr != null) {
            try {
                return mbr.readFrom(cls, type, anns, contentType, 
                       new MetadataMap<String, Object>(r.getMetadata(), true, true), inputStream);
            } catch (Exception ex) {
                reportMessageHandlerProblem("MSG_READER_PROBLEM", cls, contentType, ex, r);
            }
        } else if (cls == Response.class) {
            return r;
        } else {
            reportMessageHandlerProblem("NO_MSG_READER", cls, contentType, null, null);
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
            
            if (InjectionUtils.isSupportedCollectionOrArray(pValue.getClass())) {
                Collection<?> c = pValue.getClass().isArray() 
                    ? Arrays.asList((Object[]) pValue) : (Collection) pValue;
                for (Iterator<?> it = c.iterator(); it.hasNext();) {
                    addToBuilder(ub, paramName, it.next(), pt);
                }
            } else { 
                addToBuilder(ub, paramName, pValue, pt); 
            }
                
                    
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
    
    protected static void reportMessageHandlerProblem(String name, Class<?> cls, MediaType ct, 
                                                      Throwable cause, Response response) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(name, 
                                                   BUNDLE,
                                                   cls,
                                                   ct.toString());
        LOG.severe(errorMsg.toString());
        throw new ClientWebApplicationException(errorMsg.toString(), cause, response);
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
            throw new ClientWebApplicationException("REMOTE_CONNECTION_PROBLEM", ex, null);
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

    protected String[] parseQuotedHeaderValue(String originalValue) {
        // this algorithm isn't perfect; see CXF-3518 for further discussion.
        List<String> results = new ArrayList<String>();
        char[] chars = originalValue.toCharArray();

        int lastIndex = chars.length - 1;

        boolean quote = false;
        StringBuilder sb = new StringBuilder();

        for (int pos = 0; pos <= lastIndex; pos++) {
            char c = chars[pos];
            if (pos == lastIndex) {
                sb.append(c);
                results.add(sb.toString());
            } else {
                switch(c) {
                case '\"':
                    sb.append(c);
                    quote = !quote;
                    break;
                case '\\':
                    if (quote) {
                        pos++;
                        if (pos <= lastIndex) {
                            c = chars[pos];
                            sb.append(c);
                        }
                        if (pos == lastIndex) {
                            results.add(sb.toString());
                        }
                    } else {
                        sb.append(c);
                    }
                    break;
                case ',':
                    if (quote) {
                        sb.append(c);
                    } else {
                        results.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    break;
                default:
                    sb.append(c);
                }
            }
        }
        return results.toArray(new String[results.size()]);
    }

    protected ClientConfiguration getConfiguration() {
        return cfg;
    }
    
    protected void setConfiguration(ClientConfiguration config) {
        cfg = config;
    }
    
    protected void prepareConduitSelector(Message message) {
        try {
            cfg.prepareConduitSelector(message);
        } catch (Fault ex) {
            LOG.warning("Failure to prepare a message from conduit selector");
        }
        message.getExchange().put(ConduitSelector.class, cfg.getConduitSelector());
    }
    
    protected static PhaseInterceptorChain setupOutInterceptorChain(ClientConfiguration cfg) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor<? extends Message>> i1 = cfg.getBus().getOutInterceptors();
        List<Interceptor<? extends Message>> i2 = cfg.getOutInterceptors();
        List<Interceptor<? extends Message>> i3 = cfg.getConduitSelector().getEndpoint().getOutInterceptors();
        return new PhaseChainCache().get(pm.getOutPhases(), i1, i2, i3);
    }
    
    protected static PhaseInterceptorChain setupInInterceptorChain(ClientConfiguration cfg) { 
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor<? extends Message>> i1 = cfg.getBus().getInInterceptors();
        List<Interceptor<? extends Message>> i2 = cfg.getInInterceptors();
        List<Interceptor<? extends Message>> i3 = cfg.getConduitSelector().getEndpoint().getInInterceptors();
        
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
        // no need for the underlying conduit to throw the IO exceptions in case of
        // client requests returning error HTTP code, it can be overridden if really needed 
        exchange.put("org.apache.cxf.http.no_io_exceptions", true);
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
            exchange.putAll(cfg.getResponseContext());
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
