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
import org.apache.cxf.endpoint.Retryable;
import org.apache.cxf.helpers.CastUtils;
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
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.HTTPConduit;

/**
 * Common proxy and http-centric client implementation
 *
 */
public abstract class AbstractClient implements Client, Retryable {
    protected static final String REQUEST_CONTEXT = "RequestContext";
    protected static final String RESPONSE_CONTEXT = "ResponseContext";
    protected static final String KEEP_CONDUIT_ALIVE = "KeepConduitAlive";
    
    private static final String PROXY_PROPERTY = "jaxrs.proxy";
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractClient.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractClient.class);
    
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
    
    protected ResponseBuilder setResponseBuilder(Message outMessage, Exchange exchange) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)outMessage.get(HTTPConduit.KEY_HTTP_CONNECTION);
        
        if (conn == null) {
            throw new ClientWebApplicationException("HTTP Connection is null"); 
        }
        checkClientException(exchange.getOutMessage(), exchange.getOutMessage().getContent(Exception.class));
        
        int status = (Integer)exchange.get(Message.RESPONSE_CODE);
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
        
        Message inMessage = exchange.getInMessage();
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
    protected void writeBody(Object o, Message outMessage, Class<?> cls, Type type, 
                            Annotation[] anns, 
                            MultivaluedMap<String, String> headers, 
                            OutputStream os) {
        
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
        
        MediaType contentType = getResponseContentType(inMessage, r);
        
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
    
    protected void completeExchange(Object response, Exchange exchange) {
        // higher level conduits such as FailoverTargetSelector need to
        // clear the request state but a fair number of response objects 
        // depend on InputStream being still open thus lower-level conduits
        // operating on InputStream don't have to close streams pro-actively
        exchange.put(KEEP_CONDUIT_ALIVE, true);    
        getConfiguration().getConduitSelector().complete(exchange);
    }
    
    protected Object[] preProcessResult(Message message) throws Exception {
        
        Exchange exchange = message.getExchange(); 
      
        Exception ex = null;
        // Check to see if there is a Fault from the outgoing chain if it's an out Message
        if (!message.get(Message.INBOUND_MESSAGE).equals(Boolean.TRUE)) {
            ex = message.getContent(Exception.class);
        }
        if (ex != null) {
            getConfiguration().getConduitSelector().complete(exchange);
            checkClientException(message, message.getContent(Exception.class));
        }
        checkClientException(message, message.getExchange().get(Exception.class));
        
        List result = message.getExchange().get(List.class);
        return result != null ? result.toArray() : null;
    }
    
    protected void checkClientException(Message message, Exception ex) throws Exception {
        if (message.getExchange().get(Message.RESPONSE_CODE) == null) {
            if (ex instanceof ClientWebApplicationException) {
                throw ex;
            } else if (ex != null) {
                throw new ClientWebApplicationException(ex);
            } else {
                throw new ClientWebApplicationException();
            }
        }
    }
    
    protected URI calculateNewRequestURI(Map<String, Object> reqContext) {
        URI newBaseURI = URI.create(reqContext.get(Message.ENDPOINT_ADDRESS).toString());
        URI requestURI = URI.create(reqContext.get(Message.REQUEST_URI).toString());
        return calculateNewRequestURI(newBaseURI, requestURI,
                MessageUtils.isTrue(reqContext.get(PROXY_PROPERTY)));
    }
    
    private URI calculateNewRequestURI(URI newBaseURI, URI requestURI, boolean proxy) {
        String baseURIPath = newBaseURI.getRawPath();
        String reqURIPath = requestURI.getRawPath();
        
        UriBuilder builder = UriBuilder.fromUri(newBaseURI);
        String basePath = reqURIPath.startsWith(baseURIPath) ? baseURIPath : getBaseURI().getRawPath(); 
        builder.path(reqURIPath.equals(basePath) ? "" : reqURIPath.substring(basePath.length()));
        URI newRequestURI = builder.replaceQuery(requestURI.getRawQuery()).build();
        
        resetBaseAddress(newBaseURI);
        URI current = proxy ? newBaseURI : newRequestURI; 
        resetCurrentBuilder(current);
        
        return newRequestURI;
    }
    
    @SuppressWarnings("unchecked")
    public Object[] invoke(BindingOperationInfo oi, Object[] params, Map<String, Object> context,
                           Exchange exchange) throws Exception {
        
        try {
            Object body = params.length == 0 ? null : params[0];
            Map<String, Object> reqContext = CastUtils.cast((Map)context.get(REQUEST_CONTEXT));
            MultivaluedMap<String, String> headers = 
                (MultivaluedMap<String, String>)reqContext.get(Message.PROTOCOL_HEADERS);
                        
            URI newRequestURI = calculateNewRequestURI(reqContext);
            // TODO: if failover conduit selector fails to find a failover target
            // then it will revert to the previous endpoint; that is not very likely
            // but possible - thus ideally we need to resert base and current URI only
            // if we get the same ConduitInitiatior endpoint instance before and after
            // retryInvoke.
            Object response = retryInvoke(newRequestURI, headers, body, exchange, context);
            exchange.put(List.class, getContentsList(response));
            return new Object[]{response};
        } catch (Throwable t) {
            Exception ex = t instanceof Exception ? (Exception)t : new Exception(t);
            exchange.put(Exception.class, ex);
            return null;
        }
    }
    
    protected abstract Object retryInvoke(URI newRequestURI, 
                                 MultivaluedMap<String, String> headers,
                                 Object body,
                                 Exchange exchange, 
                                 Map<String, Object> invContext) throws Throwable;
    
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
    
    private static MediaType getResponseContentType(Message m, Response r) {
        MultivaluedMap<String, Object> map = r.getMetadata();
        if (map.containsKey(HttpHeaders.CONTENT_TYPE)) {
            try {
                return MediaType.valueOf(map.getFirst(HttpHeaders.CONTENT_TYPE).toString());
            } catch (IllegalArgumentException ex) {
                String error = ex.getMessage();
                if (error != null && error.contains("separator") && m.containsKey(Message.CONTENT_TYPE)) { 
                    return MediaType.valueOf((String)m.get(Message.CONTENT_TYPE));
                } else {
                    throw ex;
                }
            }
        }
        return MediaType.WILDCARD_TYPE;
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
    
    // Note that some conduit selectors may update Message.ENDPOINT_ADDRESS
    // after the conduit selector has been prepared but before the actual 
    // invocation thus it is also important to have baseURI and currentURI 
    // synched up with the latest endpoint address, after a successful proxy 
    // or web client invocation has returned
    protected void prepareConduitSelector(Message message, URI currentURI, boolean proxy) {
        try {
            cfg.prepareConduitSelector(message);
            
        } catch (Fault ex) {
            LOG.warning("Failure to prepare a message from conduit selector");
        }
        message.getExchange().put(ConduitSelector.class, cfg.getConduitSelector());
        message.getExchange().put(Service.class, cfg.getConduitSelector().getEndpoint().getService());
        
        String address = (String)message.get(Message.ENDPOINT_ADDRESS);
        // custom conduits may override the initial/current address
        if (!address.equals(currentURI.toString())) {
            URI baseAddress = URI.create(address);
            currentURI = calculateNewRequestURI(baseAddress, currentURI, proxy);
            message.put(Message.ENDPOINT_ADDRESS, currentURI.toString());
            message.put(Message.REQUEST_URI, currentURI.toString());
        }
        message.put(Message.BASE_PATH, getBaseURI().toString());
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
    
    protected Message createMessage(Object body,
                                    String httpMethod, 
                                    MultivaluedMap<String, String> headers,
                                    URI currentURI,
                                    Exchange exchange,
                                    Map<String, Object> invocationContext,
                                    boolean proxy) {
        Message m = cfg.getConduitSelector().getEndpoint().getBinding().createMessage();
        m.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        m.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        
        m.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put(Message.ENDPOINT_ADDRESS, currentURI.toString());
        m.put(Message.REQUEST_URI, currentURI.toString());
        
        m.put(Message.CONTENT_TYPE, headers.getFirst(HttpHeaders.CONTENT_TYPE));
        
        m.setContent(List.class, getContentsList(body));
        if (body == null) {
            setEmptyRequestProperty(m, httpMethod);
        }
        
        m.put(URITemplate.TEMPLATE_PARAMETERS, getState().getTemplates());
        
        PhaseInterceptorChain chain = setupOutInterceptorChain(cfg);
        m.setInterceptorChain(chain);
        
        exchange = createExchange(m, exchange);
        exchange.setOneWay("true".equals(headers.getFirst(Message.ONE_WAY_REQUEST)));
        exchange.put(Retryable.class, this);
        
        // context
        setContexts(m, exchange, invocationContext, proxy);
        
        //setup conduit selector
        prepareConduitSelector(m, currentURI, proxy);
        
        return m;
    }
    
    protected Map<String, Object> getRequestContext(Message outMessage) {
        Map<String, Object> invContext = CastUtils.cast((Map)outMessage.get(Message.INVOCATION_CONTEXT));
        return CastUtils.cast((Map)invContext.get(REQUEST_CONTEXT));
    }
    
    protected List getContentsList(Object body) {
        return body == null ? new MessageContentsList() : new MessageContentsList(body);
    }
    
    protected Exchange createExchange(Message m, Exchange exchange) {
        if (exchange == null) {
            exchange = new ExchangeImpl();
        }
        exchange.setSynchronous(true);
        exchange.setOutMessage(m);
        exchange.put(Bus.class, cfg.getBus());
        exchange.put(MessageObserver.class, new ClientMessageObserver(cfg));
        exchange.put(Endpoint.class, cfg.getConduitSelector().getEndpoint());
        exchange.put("org.apache.cxf.http.no_io_exceptions", true);
        m.setExchange(exchange);
        return exchange;
    }
    
    protected void setContexts(Message message, Exchange exchange, 
                               Map<String, Object> context, boolean proxy) {
        Map<String, Object> reqContext = null;
        Map<String, Object> resContext = null;
        if (context == null) {
            context = new HashMap<String, Object>();
        }
        reqContext = CastUtils.cast((Map)context.get(REQUEST_CONTEXT));
        resContext = CastUtils.cast((Map)context.get(RESPONSE_CONTEXT));
        if (reqContext == null) { 
            reqContext = new HashMap<String, Object>(cfg.getRequestContext());
            context.put(REQUEST_CONTEXT, reqContext);
        }
        reqContext.put(Message.PROTOCOL_HEADERS, message.get(Message.PROTOCOL_HEADERS));
        reqContext.put(Message.REQUEST_URI, message.get(Message.REQUEST_URI));
        reqContext.put(Message.ENDPOINT_ADDRESS, message.get(Message.ENDPOINT_ADDRESS));
        reqContext.put(PROXY_PROPERTY, proxy);
        
        if (resContext == null) {
            resContext = new HashMap<String, Object>();
            context.put(RESPONSE_CONTEXT, resContext);
        }
        
        message.put(Message.INVOCATION_CONTEXT, context);
        message.putAll(reqContext);
        exchange.putAll(reqContext);
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
