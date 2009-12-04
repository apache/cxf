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
import java.util.Collection;
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
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;


/**
 * Http-centric web client
 *
 */
public class WebClient extends AbstractClient {
    
    private MultivaluedMap<String, String> templates;
    
    protected WebClient(String baseAddress) {
        this(URI.create(baseAddress));
    }
    
    protected WebClient(URI baseAddress) {
        super(baseAddress, baseAddress);
    }
    
    /**
     * Creates WebClient
     * @param baseAddress baseAddress
     */
    public static WebClient create(String baseAddress) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        return bean.createWebClient();
    }
    
    /**
     * Creates WebClient
     * @param baseURI baseURI
     */
    public static WebClient create(URI baseURI) {
        return create(baseURI.toString());
    }
    
    /**
     * Creates WebClient
     * @param baseURI baseURI
     * @param providers list of providers
     */
    public static WebClient create(String baseAddress, List<?> providers) {
        return create(baseAddress, providers, null);        
    }
    
    /**
     * Creates a Spring-configuration aware WebClient
     * @param baseAddress baseAddress
     * @param providers list of providers
     * @param configLocation classpath location of Spring configuration resource, can be null  
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        return bean.createWebClient();
    }
    
    /**
     * Creates a Spring-configuration aware WebClient
     * @param baseAddress baseAddress
     * @param configLocation classpath location of Spring configuration resource, can be null  
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);
        
        return bean.createWebClient();
    }
    
    /**
     * Creates a Spring-configuration aware WebClient which will do basic authentication
     * @param baseAddress baseAddress
     * @param username username
     * @param password password
     * @param configLocation classpath location of Spring configuration resource, can be null  
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, String username, String password, 
                                         String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);
        
        bean.setUsername(username);
        bean.setPassword(password);
        
        return bean.createWebClient();
    }
    
    /**
     * Creates WebClient, baseURI will be set to Client currentURI
     * @param client existing client
     */
    public static WebClient fromClient(Client client) {
        return fromClient(client, false);
    }
    
    /**
     * Creates WebClient, baseURI will be set to Client currentURI
     * @param client existing client
     * @param inheritHeaders  if existing Client headers can be inherited by new proxy 
     *        and subresource proxies if any 
     */
    public static WebClient fromClient(Client client, boolean inheritHeaders) {
        WebClient webClient = create(client.getCurrentURI());
        if (inheritHeaders) {
            webClient.headers(client.getHeaders());
        }
        copyProperties(webClient, client);
        return webClient;
    }
    
    /**
     * Converts proxy to Client
     * @param proxy the proxy
     * @return proxy as a Client 
     */
    public static Client client(Object proxy) {
        if (proxy instanceof Client) {
            return (Client)proxy;
        }
        return null;
    }
    
    /**
     * Retieves ClientConfiguration
     * @param client proxy or http-centric Client
     * @return underlying ClientConfiguration instance 
     */
    public static ClientConfiguration getConfig(Object client) {
        if (client instanceof Client) {
            if (client instanceof WebClient) { 
                return ((AbstractClient)client).getConfiguration();
            } else if (client instanceof InvocationHandlerAware) {
                Object handler = ((InvocationHandlerAware)client).getInvocationHandler();
                return ((AbstractClient)handler).getConfiguration();
            }
        }
        throw new IllegalArgumentException("Not a valid Client");
    }
    
    /**
     * Does HTTP invocation
     * @param httpMethod HTTP method
     * @param body request body, can be null
     * @return JAXRS Response, entity may hold a string representaion of 
     *         error message if client or server error occured
     */
    public Response invoke(String httpMethod, Object body) {
        return doInvoke(httpMethod, body, InputStream.class, InputStream.class);
    }
    
    /**
     * Does HTTP POST invocation
     * @param body request body, can be null
     * @return JAXRS Response
     */
    public Response post(Object body) {
        return invoke("POST", body);
    }
    
    /**
     * Does HTTP PUT invocation
     * @param body request body, can be null
     * @return JAXRS Response
     */
    public Response put(Object body) {
        return invoke("PUT", body);
    }

    /**
     * Does HTTP GET invocation
     * @return JAXRS Response
     */
    public Response get() {
        return invoke("GET", null);
    }

    /**
     * Does HTTP HEAD invocation
     * @return JAXRS Response
     */
    public Response head() {
        return invoke("HEAD", null);
    }

    /**
     * Does HTTP OPTIONS invocation
     * @return JAXRS Response
     */
    public Response options() {
        return invoke("OPTIONS", null);
    }

    /**
     * Does HTTP DELETE invocation
     * @return JAXRS Response
     */
    public Response delete() {
        return invoke("DELETE", null);
    }

    /**
     * Posts form data
     * @param values form values
     * @return JAXRS Response
     */
    public Response form(Map<String, List<Object>> values) {
        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        return doInvoke("POST", values, InputStream.class, InputStream.class);
    }
    
    /**
     * Posts form data
     * @param form form values
     * @return JAXRS Response
     */
    public Response form(Form form) {
        type(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        return doInvoke("POST", form.getData(), InputStream.class, InputStream.class);
    }
    
    /**
     * Does HTTP invocation and returns types response object 
     * @param httpMethod HTTP method 
     * @param body request body, can be null
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers 
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T invoke(String httpMethod, Object body, Class<T> responseClass) {
        
        Response r = doInvoke(httpMethod, body, responseClass, responseClass);
        
        if (r.getStatus() >= 400 && responseClass != null) {
            throw new WebApplicationException(r);
        }
        
        return responseClass.cast(r.getEntity());
    }
    
    /**
     * Does HTTP invocation and returns a collection of typed objects 
     * @param httpMethod HTTP method 
     * @param body request body, can be null
     * @param memberClass expected type of collection member class
     * @return typed collection
     */
    public <T> Collection<? extends T> invokeAndGetCollection(String httpMethod, Object body, 
                                                    Class<T> memberClass) {
        
        Response r = doInvoke(httpMethod, body, Collection.class, memberClass);
        
        if (r.getStatus() >= 400) {
            throw new WebApplicationException(r);
        }
        
        return CastUtils.cast((Collection)r.getEntity(), memberClass);
    }
    
    /**
     * Does HTTP POST invocation and returns typed response object
     * @param body request body, can be null
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers 
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T post(Object body, Class<T> responseClass) {
        return invoke("POST", body, responseClass);
    }
    
    /**
     * Does HTTP POST invocation and returns a collection of typed objects 
     * @param body request body, can be null
     * @param memberClass expected type of collection member class
     * @return typed collection
     */
    public <T> Collection<? extends T> postAndGetCollection(Object body, Class<T> memberClass) {
        return invokeAndGetCollection("POST", body, memberClass);
    }
    
    /**
     * Does HTTP GET invocation and returns a collection of typed objects 
     * @param body request body, can be null
     * @param memberClass expected type of collection member class
     * @return typed collection
     */
    public <T> Collection<? extends T> getCollection(Class<T> memberClass) {
        return invokeAndGetCollection("GET", null, memberClass);
    }
    
    /**
     * Does HTTP GET invocation and returns typed response object
     * @param body request body, can be null
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers 
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T get(Class<T> responseClass) {
        return invoke("GET", null, responseClass);
    }
    
    /**
     * Updates the current URI path
     * @param path new relative path segment
     * @return updated WebClient
     */
    public WebClient path(Object path) {
        getCurrentBuilder().path(path.toString());
        
        return this;
    }
    
    /**
     * Updates the current URI path with path segment which may contain template variables
     * @param path new relative path segment
     * @param values template variable values
     * @return updated WebClient
     */
    public WebClient path(String path, Object... values) {
        URITemplate t = new URITemplate(path);
        List<String> vars = t.getVariables();
        if (vars.size() > 0 && vars.size() == values.length) {
            if (templates == null) {
                templates = new MetadataMap<String, String>();
            }
            for (int i = 0; i < values.length; i++) {
                templates.add(vars.get(i), values[i].toString());
            }
        }
        URI u = UriBuilder.fromUri(URI.create("http://tempuri")).path(path).buildFromEncoded(values);
        return path(u.getRawPath());
    }
    
    /**
     * Updates the current URI query parameters
     * @param name query name
     * @param values query values
     * @return updated WebClient
     */
    public WebClient query(String name, Object ...values) {
        if (!"".equals(name)) {
            getCurrentBuilder().queryParam(name, values);
        } else {
            addParametersToBuilder(getCurrentBuilder(), name, values[0], ParameterType.QUERY);
        }
        
        return this;
    }
    
    /**
     * Updates the current URI matrix parameters
     * @param name matrix name
     * @param values matrix values
     * @return updated WebClient
     */
    public WebClient matrix(String name, Object ...values) {
        if (!"".equals(name)) {
            getCurrentBuilder().matrixParam(name, values);
        } else {
            addParametersToBuilder(getCurrentBuilder(), name, values[0], ParameterType.MATRIX);
        }
        
        return this;
    }
    
    /**
     * Moves WebClient to a new baseURI or forwards to new currentURI  
     * @param newAddress new URI
     * @param forward if true then currentURI will be based on baseURI  
     * @return updated WebClient
     */
    public WebClient to(String newAddress, boolean forward) {
        clearTemplates();
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
    
    /**
     * Goes back
     * @param fast if true then goes back to baseURI otherwise to a previous path segment 
     * @return updated WebClient
     */
    public WebClient back(boolean fast) {
        clearTemplates();
        if (fast) {
            getCurrentBuilder().replacePath(getBaseURI().getPath());
        } else {
            URI uri = getCurrentURI();
            if (uri == getBaseURI()) {
                return this;
            }
            List<PathSegment> segments = JAXRSUtils.getPathSegments(uri.getPath(), false);
            getCurrentBuilder().replacePath(null);
            for (int i = 0; i < segments.size() - 1; i++) {
                getCurrentBuilder().path(HttpUtils.fromPathSegment(segments.get(i)));
            }
            
        }
        return this;
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
        clearTemplates();
        return (WebClient)super.reset();
    }
    
    protected Response doInvoke(String httpMethod, Object body, Class<?> responseClass, Type genericType) {
        
        MultivaluedMap<String, String> headers = getHeaders();
        if (body != null) {
            if (headers.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
                headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_TYPE.toString());
            }
        } else {
            headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.WILDCARD);
        }
        if (responseClass != null && headers.getFirst(HttpHeaders.ACCEPT) == null) {
            headers.putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_TYPE.toString());
        }
        resetResponse();
        try {
            return doChainedInvocation(httpMethod, headers, body, responseClass, genericType);
        } finally {
            clearTemplates();
        }
        
    }

    protected Response doChainedInvocation(String httpMethod, 
        MultivaluedMap<String, String> headers, Object body, Class<?> responseClass, Type genericType) {
        
        Message m = createMessage(httpMethod, headers, getCurrentURI());
        m.put(URITemplate.TEMPLATE_PARAMETERS, templates);
        if (body != null) {
            MessageContentsList contents = new MessageContentsList(body);
            m.setContent(List.class, contents);
            m.getInterceptorChain().add(new BodyWriter());
        } else if ("POST".equals(httpMethod)) {
            m.put("org.apache.cxf.post.empty", "true");
        }
        
        try {
            m.getInterceptorChain().doIntercept(m);
        } catch (Throwable ex) {
            // we'd like a user to get the whole Response anyway if needed
        }
        
        // TODO : this needs to be done in an inbound chain instead
        HttpURLConnection connect = (HttpURLConnection)m.get(HTTPConduit.KEY_HTTP_CONNECTION);
        return handleResponse(connect, m, responseClass, genericType);
    }
    
    protected Response handleResponse(HttpURLConnection conn, Message outMessage, 
                                      Class<?> responseClass, Type genericType) {
        try {
            ResponseBuilder rb = setResponseBuilder(conn, outMessage.getExchange()).clone();
            Response currentResponse = rb.clone().build();
            
            Object entity = readBody(currentResponse, conn, outMessage, responseClass, genericType,
                                     new Annotation[]{});
            rb.entity(entity);
            
            return rb.build();
        } catch (Throwable ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    protected HttpURLConnection getConnection(String methodName) {
        return createHttpConnection(getCurrentBuilder().clone().buildFromEncoded(), methodName);
    }
    
    private void clearTemplates() {
        if (templates != null) {
            templates.clear();
            templates = null;
        }
    }
    
    private class BodyWriter extends AbstractOutDatabindingInterceptor {

        public BodyWriter() {
            super(Phase.WRITE);
        }
        
        @SuppressWarnings("unchecked")
        public void handleMessage(Message outMessage) throws Fault {
            
            OutputStream os = outMessage.getContent(OutputStream.class);
            if (os == null) {
                return;
            }
            MessageContentsList objs = MessageContentsList.getContentsList(outMessage);
            if (objs == null || objs.size() == 0) {
                return;
            }
            MultivaluedMap<String, String> headers = 
                (MultivaluedMap)outMessage.get(Message.PROTOCOL_HEADERS);
            Object body = objs.get(0);
            try {
                writeBody(body, outMessage, body.getClass(), body.getClass(), new Annotation[]{}, 
                          headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        }
    }

    static void copyProperties(Client toClient, Client fromClient) {
        AbstractClient newClient = toAbstractClient(toClient);
        AbstractClient oldClient = toAbstractClient(fromClient);
        newClient.setConfiguration(oldClient.getConfiguration());
    }
    
    private static AbstractClient toAbstractClient(Client client) {
        if (client instanceof AbstractClient) {
            return (AbstractClient)client;
        } else {
            return (AbstractClient)((InvocationHandlerAware)client).getInvocationHandler();
        }
    }
    
    static JAXRSClientFactoryBean getBean(String baseAddress, String configLocation) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        
        if (configLocation != null) {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus(configLocation);
            bean.setBus(bus);
        }
        bean.setAddress(baseAddress);
        return bean;
    }
}
