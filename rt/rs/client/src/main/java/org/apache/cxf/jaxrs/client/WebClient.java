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

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.spec.ClientImpl.WebTargetImpl;
import org.apache.cxf.jaxrs.client.spec.InvocationBuilderImpl;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ParameterizedCollectionType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;


/**
 * Http-centric web client
 *
 */
public class WebClient extends AbstractClient {
    private static final String REQUEST_CLASS = "request.class";
    private static final String REQUEST_TYPE = "request.type";
    private static final String REQUEST_ANNS = "request.annotations";
    private static final String RESPONSE_CLASS = "response.class";
    private static final String RESPONSE_TYPE = "response.type";
    private static final String WEB_CLIENT_OPERATION_REPORTING = "enable.webclient.operation.reporting";
    private BodyWriter bodyWriter = new BodyWriter();
    protected WebClient(String baseAddress) {
        this(convertStringToURI(baseAddress), Collections.emptyMap());
    }
    
    protected WebClient(String baseAddress, Map<String, Object> properties) {
        this(convertStringToURI(baseAddress), properties);
    }

    protected WebClient(URI baseURI) {
        this(baseURI, Collections.emptyMap());
    }

    protected WebClient(URI baseURI, Map<String, Object> properties) {
        this(new LocalClientState(baseURI, properties));
    }

    protected WebClient(ClientState state) {
        super(state);
        cfg.getInInterceptors().add(new ClientAsyncResponseInterceptor());
    }




    /**
     * Creates WebClient
     * @param baseAddress baseAddress
     */
    public static WebClient create(String baseAddress) {
        return create(baseAddress, Collections.emptyMap());
    }

    /**
     * Creates WebClient
     * @param baseAddress baseAddress
     */
    public static WebClient create(String baseAddress, Map<String, Object> properties) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseAddress);
        bean.setProperties(properties);
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
     */
    public static WebClient create(String baseURI, boolean threadSafe) {
        return create(baseURI, Collections.emptyList(), threadSafe);
    }

    /**
     * Creates WebClient
     * @param baseAddress baseURI
     * @param providers list of providers
     */
    public static WebClient create(String baseAddress, List<?> providers) {
        return create(baseAddress, providers, null);
    }

    /**
     * Creates WebClient
     * @param baseAddress baseURI
     * @param providers list of providers
     * @param threadSafe if true ThreadLocalClientState is used
     */
    public static WebClient create(String baseAddress, List<?> providers, boolean threadSafe) {
        return create(baseAddress, providers, Collections.emptyMap(), threadSafe);
    }
    
    /**
     * Creates WebClient
     * @param baseAddress baseURI
     * @param providers list of providers
     * @param threadSafe if true ThreadLocalClientState is used
     * @param properties additional properties
     */
    public static WebClient create(String baseAddress, List<?> providers, 
            Map<String, Object> properties, boolean threadSafe) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, null);
        bean.setProviders(providers);
        bean.setProperties(properties);
        if (threadSafe) {
            bean.setInitialState(new ThreadLocalClientState(baseAddress, properties));
        }
        return bean.createWebClient();
    }

    /**
     * Creates a thread safe WebClient
     * @param baseAddress baseURI
     * @param providers list of providers
     * @param timeToKeepState time to keep this thread safe state.
     */
    public static WebClient create(String baseAddress, List<?> providers, long timeToKeepState) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, null);
        bean.setProviders(providers);
        bean.setInitialState(new ThreadLocalClientState(baseAddress, timeToKeepState));
        return bean.createWebClient();
    }

    /**
     * Creates WebClient
     * @param baseAddress baseAddress
     * @param providers list of providers
     * @param configLocation classpath location of the configuration resource, can be null
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        return bean.createWebClient();
    }

    /**
     * Creates WebClient with a list of custom features
     * @param baseAddress baseAddress
     * @param providers list of providers
     * @param features the features which will be applied to the client
     * @param configLocation classpath location of the configuration resource, can be null
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress,
                                   List<?> providers,
                                   List<? extends Feature> features,
                                   String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        bean.setFeatures(features);
        return bean.createWebClient();
    }

    /**
     * Creates WebClient
     * @param baseAddress baseAddress
     * @param configLocation classpath location of the configuration resource, can be null
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);

        return bean.createWebClient();
    }

    /**
     * Creates WebClient which will do basic authentication
     * @param baseAddress baseAddress
     * @param username username
     * @param password password
     * @param configLocation classpath location of the configuration resource, can be null
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
     * Creates WebClient which will do basic authentication
     * @param baseAddress baseAddress
     * @param providers list of providers
     * @param username username
     * @param password password
     * @param configLocation classpath location of the configuration resource, can be null
     * @return WebClient instance
     */
    public static WebClient create(String baseAddress, List<?> providers,
                                   String username, String password, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, configLocation);

        bean.setUsername(username);
        bean.setPassword(password);
        bean.setProviders(providers);
        return bean.createWebClient();
    }

    /**
     * Creates WebClient, baseURI will be set to Client currentURI
     * @param object existing client object
     */
    public static WebClient fromClientObject(Object object) {
        Client client = client(object);
        return client == null ? null : fromClient(client, false);
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
     * @param inheritHeaders  if existing Client headers can be inherited by new client
     */
    public static WebClient fromClient(Client client, boolean inheritHeaders) {

        final WebClient webClient;

        ClientState clientState = getClientState(client);
        if (clientState == null) {
            webClient = create(client.getCurrentURI());
            if (inheritHeaders) {
                webClient.headers(client.getHeaders());
            }
        } else {
            MultivaluedMap<String, String> headers = inheritHeaders ? client.getHeaders() : null;
            webClient = new WebClient(clientState.newState(client.getCurrentURI(), headers, null));
        }
        copyProperties(webClient, client);
        return webClient;
    }

    /**
     * Converts object to Client
     * @param object the object
     * @return Client object converted to Client
     */
    public static Client client(Object object) {
        if (object instanceof Client) {
            return (Client)object;
        }
        return null;
    }

    /**
     * Retrieves ClientConfiguration
     * @param client proxy or http-centric Client
     * @return underlying ClientConfiguration instance
     */
    public static ClientConfiguration getConfig(Object client) {
        if (client instanceof WebTargetImpl) {
            client = ((WebTargetImpl)client).getWebClient();
        } else if (client instanceof InvocationBuilderImpl) {
            client = ((InvocationBuilderImpl)client).getWebClient();
        }

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
        return doInvoke(httpMethod, body, null, Response.class, Response.class);
    }

    /**
     * Does HTTP POST invocation
     * @param body request body, can be null
     * @return JAXRS Response
     */
    public Response post(Object body) {
        return invoke(HttpMethod.POST, body);
    }

    /**
     * Does HTTP PUT invocation
     * @param body request body, can be null
     * @return JAXRS Response
     */
    public Response put(Object body) {
        return invoke(HttpMethod.PUT, body);
    }

    /**
     * Does HTTP GET invocation
     * @return JAXRS Response
     */
    public Response get() {
        return invoke(HttpMethod.GET, null);
    }

    /**
     * Does HTTP HEAD invocation
     * @return JAXRS Response
     */
    public Response head() {
        return invoke(HttpMethod.HEAD, null);
    }

    /**
     * Does HTTP OPTIONS invocation
     * @return JAXRS Response
     */
    public Response options() {
        return invoke(HttpMethod.OPTIONS, null);
    }

    /**
     * Does HTTP DELETE invocation
     * @return JAXRS Response
     */
    public Response delete() {
        return invoke(HttpMethod.DELETE, null);
    }

    /**
     * Posts form data
     * @param values form values
     * @return JAXRS Response
     */
    public Response form(Map<String, List<Object>> values) {
        type(MediaType.APPLICATION_FORM_URLENCODED);
        return doInvoke(HttpMethod.POST, values, null, Response.class, Response.class);
    }

    /**
     * Posts form data
     * @param form form values
     * @return JAXRS Response
     */
    public Response form(Form form) {
        type(MediaType.APPLICATION_FORM_URLENCODED);
        return doInvoke(HttpMethod.POST, form.asMap(), null, Response.class, Response.class);
    }

    /**
     * Does HTTP invocation and returns types response object
     * @param httpMethod HTTP method
     * @param body request body, can be null
     * @param responseType generic response type
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T invoke(String httpMethod, Object body, GenericType<T> responseType) {
        @SuppressWarnings("unchecked")
        Class<T> responseClass = (Class<T>)responseType.getRawType();
        Response r = doInvoke(httpMethod, body, null, responseClass, responseType.getType());
        return castResponse(r, responseClass);
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
        Response r = doInvoke(httpMethod, body, null, responseClass, responseClass);
        return castResponse(r, responseClass);
    }

    /**
     * Does HTTP invocation and returns types response object
     * @param httpMethod HTTP method
     * @param body request body, can be null
     * @param requestClass request body class
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T invoke(String httpMethod, Object body, Class<?> requestClass, Class<T> responseClass) {
        Response r = doInvoke(httpMethod, body, requestClass, null, responseClass, responseClass);
        return castResponse(r, responseClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T castResponse(Response r, Class<T> responseClass) {
        return (T)(responseClass == Response.class ? r : r.getEntity());
    }
    /**
     * Does HTTP POST invocation and returns typed response object
     * @param body request body, can be null
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T post(Object body, Class<T> responseClass) {
        return invoke(HttpMethod.POST, body, responseClass);
    }

    /**
     * Does HTTP POST invocation and returns typed response object
     * @param body request body, can be null
     * @param responseType generic response type
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T post(Object body, GenericType<T> responseType) {
        return invoke(HttpMethod.POST, body, responseType);
    }

    /**
     * Does HTTP Async POST invocation and returns Future.
     * Shortcut for async().post(Entity, InvocationCallback)
     * @param callback invocation callback
     * @return the future
     */
    public <T> Future<T> post(Object body, InvocationCallback<T> callback) {
        return doInvokeAsyncCallback(HttpMethod.POST, body, body.getClass(), null, callback);
    }

    /**
     * Does HTTP PUT invocation and returns typed response object
     * @param body request body, can be null
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T put(Object body, Class<T> responseClass) {
        return invoke(HttpMethod.PUT, body, responseClass);
    }


    /**
     * Does HTTP PUT invocation and returns typed response object
     * @param body request body, can be null
     * @param responseType generic response type
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T put(Object body, GenericType<T> responseType) {
        return invoke(HttpMethod.PUT, body, responseType);
    }

    /**
     * Does HTTP Async PUT invocation and returns Future.
     * Shortcut for async().put(Entity, InvocationCallback)
     * @param callback invocation callback
     * @return the future
     */
    public <T> Future<T> put(Object body, InvocationCallback<T> callback) {
        return doInvokeAsyncCallback(HttpMethod.PUT, body, body.getClass(), null, callback);
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
        Response r = doInvoke(httpMethod, body, null,
                              Collection.class, new ParameterizedCollectionType(memberClass));
        return CastUtils.cast((Collection<?>)r.getEntity(), memberClass);
    }

    /**
     * Posts a collection of typed objects
     * @param collection request body
     * @param memberClass type of collection member class
     * @return JAX-RS Response
     */
    public <T> Response postCollection(Object collection, Class<T> memberClass) {
        return doInvoke(HttpMethod.POST, collection, new ParameterizedCollectionType(memberClass),
                        Response.class, Response.class);
    }

    /**
     * Posts a collection of typed objects
     * @param collection request body
     * @param memberClass type of collection member class
     * @param responseClass expected type of response object
     * @return JAX-RS Response
     */
    public <T1, T2> T2 postCollection(Object collection, Class<T1> memberClass,
                                            Class<T2> responseClass) {
        Response r = doInvoke(HttpMethod.POST, collection, new ParameterizedCollectionType(memberClass),
                              responseClass, responseClass);
        return responseClass.cast(responseClass == Response.class ? r : r.getEntity());
    }

    /**
     * Posts collection of typed objects and returns a collection of typed objects
     * @param collection request body
     * @param memberClass type of collection member class
     * @param responseClass expected type of response object
     * @return JAX-RS Response
     */
    public <T1, T2> Collection<? extends T2> postAndGetCollection(Object collection,
                                                                  Class<T1> memberClass,
                                                                  Class<T2> responseClass) {
        Response r = doInvoke(HttpMethod.POST, collection, new ParameterizedCollectionType(memberClass),
                              Collection.class, new ParameterizedCollectionType(responseClass));
        return CastUtils.cast((Collection<?>)r.getEntity(), responseClass);
    }

    /**
     * Posts the object and returns a collection of typed objects
     * @param body request body
     * @param responseClass expected type of response object
     * @return JAX-RS Response
     */
    public <T> Collection<? extends T> postObjectGetCollection(Object body,
                                                                  Class<T> responseClass) {
        Response r = doInvoke(HttpMethod.POST, body, null, Collection.class,
                              new ParameterizedCollectionType(responseClass));
        return CastUtils.cast((Collection<?>)r.getEntity(), responseClass);
    }

    /**
     * Posts request body and returns a collection of typed objects
     * @param body request body, can be null
     * @param memberClass expected type of collection member class
     * @return typed collection
     */
    public <T> Collection<? extends T> postAndGetCollection(Object body, Class<T> memberClass) {
        return invokeAndGetCollection(HttpMethod.POST, body, memberClass);
    }

    /**
     * Does HTTP GET invocation and returns a collection of typed objects
     * @param memberClass expected type of collection member class
     * @return typed collection
     */
    public <T> Collection<? extends T> getCollection(Class<T> memberClass) {
        return invokeAndGetCollection(HttpMethod.GET, null, memberClass);
    }

    /**
     * Does HTTP GET invocation and returns typed response object
     * @param responseClass expected type of response object
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T get(Class<T> responseClass) {
        return invoke(HttpMethod.GET, null, responseClass);
    }


    /**
     * Does HTTP GET invocation and returns typed response object
     * @param responseType generic response type
     * @return typed object, can be null. Response status code and headers
     *         can be obtained too, see Client.getResponse()
     */
    public <T> T get(GenericType<T> responseType) {
        return invoke(HttpMethod.GET, null, responseType);
    }

    /**
     * Does HTTP Async GET invocation and returns Future.
     * Shortcut for async().get(InvocationCallback)
     * @param callback invocation callback
     * @return the future
     */
    public <T> Future<T> get(InvocationCallback<T> callback) {
        return doInvokeAsyncCallback(HttpMethod.GET, null, null, null, callback);
    }

    /**
     * Updates the current URI path
     * @param path new relative path segment
     * @return updated WebClient
     */
    public WebClient path(Object path) {
        getCurrentBuilder().path(convertParamValue(path, null));

        return this;
    }

    /**
     * Updates the current URI path with path segment which may contain template variables
     * @param path new relative path segment
     * @param values template variable values
     * @return updated WebClient
     */
    public WebClient path(String path, Object... values) {
        URI u = new UriBuilderImpl().uri(URI.create("http://tempuri")).path(path).buildFromEncoded(values);
        getState().setTemplates(getTemplateParametersMap(new URITemplate(path), Arrays.asList(values)));
        return path(u.getRawPath());
    }

    @Override
    public WebClient query(String name, Object ...values) {
        return (WebClient)super.query(name, values);
    }

    /**
     * Updates the current URI matrix parameters
     * @param name matrix name
     * @param values matrix values
     * @return updated WebClient
     */
    public WebClient matrix(String name, Object ...values) {
        addMatrixQueryParamsToBuilder(getCurrentBuilder(), name, ParameterType.MATRIX, null, values);
        return this;
    }

    /**
     * Updates the current URI fragment
     * @param name fragment name
     * @return updated WebClient
     */
    public WebClient fragment(String name) {
        getCurrentBuilder().fragment(name);
        return this;
    }

    /**
     * Moves WebClient to a new baseURI or forwards to new currentURI
     * @param newAddress new URI
     * @param forward if true then currentURI will be based on baseURI
     * @return updated WebClient
     */
    public WebClient to(String newAddress, boolean forward) {
        getState().setTemplates(null);
        if (forward) {
            if (!newAddress.startsWith("/")
                && !newAddress.startsWith(getBaseURI().toString())) {
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
        getState().setTemplates(null);
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

    /**
     * Replaces the current path with the new value.
     * @param path new path value. If it starts from "/" then all the current
     * path starting from the base URI will be replaced, otherwise only the
     * last path segment will be replaced. Providing a null value is equivalent
     * to calling back(true)
     * @return updated WebClient
     */
    public WebClient replacePath(String path) {
        if (path == null) {
            return back(true);
        }
        back(path.startsWith("/") ? true : false);
        return path(path);
    }

    /**
     * Resets the current query
     * @return updated WebClient
     */
    public WebClient resetQuery() {
        return replaceQuery(null);
    }

    /**
     * Replaces the current query with the new value.
     * @param queryString the new value, providing a null is
     *        equivalent to calling resetQuery().
     * @return updated WebClient
     */
    public WebClient replaceQuery(String queryString) {
        getCurrentBuilder().replaceQuery(queryString);
        return this;
    }

    /**
     * Replaces the header value with the new values.
     * @param headerName headerValues
     * @param value new values, null is equivalent to removing the header
     * @return updated WebClient
     */
    public WebClient replaceHeader(String headerName, Object value) {
        MultivaluedMap<String, String> headers = getState().getRequestHeaders();
        headers.remove(headerName);
        if (value != null) {
            super.header(headerName, value);
        }
        return this;
    }

    /**
     * Replaces the current query with the new value.
     * @param queryParam query param name
     * @param value the new value, providing a null is
     *        equivalent to calling resetQuery().
     * @return updated WebClient
     */
    public WebClient replaceQueryParam(String queryParam, Object... value) {
        getCurrentBuilder().replaceQueryParam(queryParam, value);
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
    public WebClient authorization(Object auth) {
        return (WebClient)super.authorization(auth);
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
        //clearTemplates();
        return (WebClient)super.reset();
    }

    protected Response doInvoke(String httpMethod,
                                Object body,
                                Type inGenericType,
                                Class<?> responseClass,
                                Type outGenericType) {
        return doInvoke(httpMethod, body, body == null ? null : body.getClass(), inGenericType,
            responseClass, outGenericType);
    }



    protected Response doInvoke(String httpMethod,
                                Object body,
                                Class<?> requestClass,
                                Type inGenericType,
                                Class<?> responseClass,
                                Type outGenericType) {
        Annotation[] inAnns = null;
        if (body instanceof Entity) {
            Entity<?> entity = (Entity<?>)body;
            setEntityHeaders(entity);
            body = entity.getEntity();
            requestClass = body.getClass();
            inGenericType = body.getClass();
            inAnns = entity.getAnnotations();
        }
        if (body instanceof GenericEntity) {
            GenericEntity<?> genericEntity = (GenericEntity<?>)body;
            body = genericEntity.getEntity();
            requestClass = genericEntity.getRawType();
            inGenericType = genericEntity.getType();
        }
        MultivaluedMap<String, String> headers = prepareHeaders(responseClass, body);
        resetResponse();
        final Response r;
        try {
            r = doChainedInvocation(httpMethod, headers, body, requestClass, inGenericType,
                                             inAnns, responseClass, outGenericType, null, null);
        } finally {
            resetResponseStateImmediatelyIfNeeded();
        }

        int status = r.getStatus();
        if (status != 304 && status >= 300 && responseClass != Response.class) {
            throw convertToWebApplicationException(r);
        }
        return r;
    }

    protected <T> Future<T> doInvokeAsyncCallback(String httpMethod,
                                                  Object body,
                                                  Class<?> requestClass,
                                                  Type inType,
                                                  InvocationCallback<T> callback) {

        Type outType = getCallbackType(callback);
        Class<?> respClass = getCallbackClass(outType);
        return doInvokeAsync(httpMethod, body, requestClass, inType, respClass, outType, callback);
    }

    protected <T> Future<T> doInvokeAsync(String httpMethod,
                                          Object body,
                                          Class<?> requestClass,
                                          Type inType,
                                          Class<?> respClass,
                                          Type outType,
                                          InvocationCallback<T> callback) {
        JaxrsClientCallback<T> cb = new JaxrsClientCallback<>(callback, respClass, outType);
        prepareAsyncClient(httpMethod, body, requestClass, inType, respClass, outType, cb);
        return cb.createFuture();
    }

    protected void prepareAsyncClient(String httpMethod,
                                   Object body,
                                   Class<?> requestClass,
                                   Type inType,
                                   Class<?> respClass,
                                   Type outType,
                                   JaxrsClientCallback<?> cb) {
        Annotation[] inAnns = null;
        if (body instanceof Entity) {
            Entity<?> entity = (Entity<?>)body;
            setEntityHeaders(entity);
            body = entity.getEntity();
            requestClass = body.getClass();
            inType = body.getClass();
            inAnns = entity.getAnnotations();
        }
        if (body instanceof GenericEntity) {
            GenericEntity<?> genericEntity = (GenericEntity<?>)body;
            body = genericEntity.getEntity();
            requestClass = genericEntity.getRawType();
            inType = genericEntity.getType();
        }

        MultivaluedMap<String, String> headers = prepareHeaders(respClass, body);
        resetResponse();

        Message m = finalizeMessage(httpMethod, headers, body, requestClass, inType,
                                  inAnns, respClass, outType, null, null);

        m.getExchange().setSynchronous(false);
        setAsyncMessageObserverIfNeeded(m.getExchange());
        m.getExchange().put(JaxrsClientCallback.class, cb);

        doRunInterceptorChain(m);
    }


    private MultivaluedMap<String, String> prepareHeaders(Class<?> responseClass, Object body) {
        MultivaluedMap<String, String> headers = getHeaders();
        if (headers.getFirst(HttpHeaders.CONTENT_TYPE) == null && body instanceof Form) {
            headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        }

        if (responseClass != null && responseClass != Response.class
            && headers.getFirst(HttpHeaders.ACCEPT) == null) {
            headers.putSingle(HttpHeaders.ACCEPT, MediaType.WILDCARD);
        }
        return headers;
    }

    class ClientAsyncResponseInterceptor extends AbstractClientAsyncResponseInterceptor {
        @Override
        protected void doHandleAsyncResponse(Message message, Response r, JaxrsClientCallback<?> cb) {
            if (r == null) {
                try {
                    r = handleResponse(message.getExchange().getOutMessage(),
                                       cb.getResponseClass(),
                                       cb.getOutGenericType());
                } catch (Throwable t) {
                    cb.handleException(message, t);
                    return;
                } finally {
                    completeExchange(message.getExchange(), false);
                }
            }
            if (cb.getResponseClass() == null || Response.class.equals(cb.getResponseClass())) {
                cb.handleResponse(message, new Object[] {r});
            } else if (r.getStatus() >= 300) {
                cb.handleException(message, convertToWebApplicationException(r));
            } else {
                cb.handleResponse(message, new Object[] {r.getEntity()});
                closeAsyncResponseIfPossible(r, message, cb);
            }
        }
    }



    //TODO: retry invocation will not work in case of async request failures for the moment
    @Override
    protected Object retryInvoke(URI newRequestURI,
                                 MultivaluedMap<String, String> headers,
                                 Object body,
                                 Exchange exchange,
                                 Map<String, Object> invContext) throws Throwable {

        Map<String, Object> reqContext = CastUtils.cast((Map<?, ?>)invContext.get(REQUEST_CONTEXT));
        String httpMethod = (String)reqContext.get(Message.HTTP_REQUEST_METHOD);
        Class<?> requestClass = (Class<?>)reqContext.get(REQUEST_CLASS);
        Type inType = (Type)reqContext.get(REQUEST_TYPE);
        Annotation[] inAnns = (Annotation[])reqContext.get(REQUEST_ANNS);
        Class<?> respClass = (Class<?>)reqContext.get(RESPONSE_CLASS);
        Type outType = (Type)reqContext.get(RESPONSE_TYPE);
        return doChainedInvocation(httpMethod, headers, body, requestClass, inType,
                                   inAnns, respClass, outType, exchange, invContext);
    }
    //CHECKSTYLE:OFF
    protected Response doChainedInvocation(String httpMethod, //NOPMD
                                           MultivaluedMap<String, String> headers,
                                           Object body,
                                           Class<?> requestClass,
                                           Type inType,
                                           Annotation[] inAnns,
                                           Class<?> respClass,
                                           Type outType,
                                           Exchange exchange,
                                           Map<String, Object> invContext) {
    //CHECKSTYLE:ON
        Bus configuredBus = getConfiguration().getBus();
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(configuredBus);
        ClassLoaderHolder origLoader = null;
        try {
            ClassLoader loader = configuredBus.getExtension(ClassLoader.class);
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            Message m = finalizeMessage(httpMethod, headers, body, requestClass, inType,
                                        inAnns, respClass, outType, exchange, invContext);
            doRunInterceptorChain(m);
            return doResponse(m, respClass, outType);
        } finally {
            if (origLoader != null) {
                origLoader.reset();
            }
            if (origBus != configuredBus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
    }

    //CHECKSTYLE:OFF
    private Message finalizeMessage(String httpMethod, //NOPMD
                                   MultivaluedMap<String, String> headers,
                                   Object body,
                                   Class<?> requestClass,
                                   Type inGenericType,
                                   Annotation[] inAnns,
                                   Class<?> responseClass,
                                   Type outGenericType,
                                   Exchange exchange,
                                   Map<String, Object> invContext) {
   //CHECKSTYLE:ON
        URI uri = getCurrentURI();
        Message m = createMessage(body, httpMethod, headers, uri, exchange,
                invContext, false);
        setSupportOnewayResponseProperty(m);
        if (inAnns != null) {
            m.put(Annotation.class.getName(), inAnns);
        }
        Map<String, Object> reqContext = getRequestContext(m);
        reqContext.put(Message.HTTP_REQUEST_METHOD, httpMethod);
        reqContext.put(REQUEST_CLASS, requestClass);
        reqContext.put(REQUEST_TYPE, inGenericType);
        reqContext.put(REQUEST_ANNS, inAnns);
        reqContext.put(RESPONSE_CLASS, responseClass);
        reqContext.put(RESPONSE_TYPE, outGenericType);

        if (body != null) {
            m.put(Type.class, inGenericType);
        }
        m.getInterceptorChain().add(bodyWriter);

        setWebClientOperationProperty(m, httpMethod);

        return m;
    }

    private void setWebClientOperationProperty(Message m, String httpMethod) {
        Object prop = m.getContextualProperty(WEB_CLIENT_OPERATION_REPORTING);
        // Enable the operation reporting by default
        if (prop == null || PropertyUtils.isTrue(prop)) {
            UriBuilder absPathUri = super.getCurrentBuilder().clone();
            absPathUri.replaceQuery(null);
            setPlainOperationNameProperty(m, httpMethod + ":" + absPathUri.build().toString());
        }

    }

    protected Response doResponse(Message m,
                                  Class<?> responseClass,
                                  Type outGenericType) {
        try {
            Object[] results = preProcessResult(m);
            if (results != null && results.length == 1) {
                return (Response)results[0];
            }
        } catch (WebApplicationException | ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }

        try {
            return handleResponse(m, responseClass, outGenericType);
        } finally {
            completeExchange(m.getExchange(), false);
        }
    }

    protected Response handleResponse(Message outMessage, Class<?> responseClass, Type genericType) {
        try {
            ResponseBuilder rb = setResponseBuilder(outMessage, outMessage.getExchange());
            Response currentResponse = rb.clone().build();
            ((ResponseImpl)currentResponse).setOutMessage(outMessage);

            Object entity = readBody(currentResponse, outMessage, responseClass, genericType,
                                     new Annotation[]{});

            if (entity == null) {
                int status = currentResponse.getStatus();
                if (status >= 400) {
                    entity = currentResponse.getEntity();
                }
            }
            rb = JAXRSUtils.fromResponse(currentResponse, false);

            rb.entity(entity instanceof Response
                      ? ((Response)entity).getEntity() : entity);

            Response r = rb.build();
            getState().setResponse(r);
            ((ResponseImpl)r).setOutMessage(outMessage);
            return r;
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ProcessingException(ex);
        } finally {
            ClientProviderFactory.getInstance(outMessage).clearThreadLocalProxies();
        }
    }


    private class BodyWriter extends AbstractBodyWriter {

        protected void doWriteBody(Message outMessage,
                                   Object body,
                                   Type bodyType,
                                   Annotation[] customAnns,
                                   OutputStream os) throws Fault {

            Map<String, Object> requestContext = WebClient.this.getRequestContext(outMessage);
            Class<?> requestClass = null;
            Type requestType = null;
            if (requestContext != null) {
                requestClass = (Class<?>)requestContext.get(REQUEST_CLASS);
                requestType = (Type)requestContext.get(REQUEST_TYPE);
            }
            if (bodyType != null) {
                requestType = bodyType;
            }

            Annotation[] anns = customAnns != null ? customAnns : new Annotation[]{};
            boolean isAssignable = requestClass != null && requestClass.isAssignableFrom(body.getClass());
            try {
                writeBody(body, outMessage,
                          requestClass == null || !isAssignable ? body.getClass() : requestClass,
                          requestType == null || !isAssignable ? body.getClass() : requestType,
                          anns, os);
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

    private static AbstractClient toAbstractClient(Object client) {

        if (client instanceof AbstractClient) {
            return (AbstractClient)client;
        } else if (client instanceof InvocationHandlerAware) {
            return (AbstractClient)((InvocationHandlerAware)client).getInvocationHandler();
        } else {
            Object realObject = ClassHelper.getRealObject(client);
            if (realObject instanceof AbstractClient) {
                return (AbstractClient)realObject;
            }
        }
        return null;
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

    static ClientState getClientState(Client client) {
        AbstractClient newClient = toAbstractClient(client);
        if (newClient == null) {
            return null;
        }
        return newClient.getState();
    }

    static URI convertStringToURI(String baseAddress) {
        try {
            return URI.create(baseAddress);
        } catch (RuntimeException ex) {
            // no need to check "https" scheme or indeed ':'
            // as the relative address will not work as the base address
            if (baseAddress.startsWith(HTTP_SCHEME)) {
                return new UriBuilderImpl().uriAsTemplate(baseAddress).build();
            }
            throw ex;
        }
    }

    // Link to JAX-RS 2.0 AsyncInvoker
    public AsyncInvoker async() {
        return new AsyncInvokerImpl(this);
    }

    // Link to JAX-RS 2.0 SyncInvoker
    public SyncInvoker sync() {
        return new SyncInvokerImpl(this);
    }

    // Link to JAX-RS 2.1 CompletionStageRxInvoker
    public CompletionStageRxInvoker rx() {
        return rx(lookUpExecutorService());
    }

    public CompletionStageRxInvoker rx(ExecutorService ex) {
        return new CompletionStageRxInvokerImpl(this, ex);
    }
    // Link to JAX-RS 2.1 RxInvoker extensions
    @SuppressWarnings("rawtypes")
    public <T extends RxInvoker> T rx(Class<T> rxCls) {
        return rx(rxCls, (ExecutorService)null);
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public <T extends RxInvoker> T rx(Class<T> rxCls, ExecutorService executorService) {
        if (CompletionStageRxInvoker.class.isAssignableFrom(rxCls)) {
            return (T)rx(executorService);
        }
        ClientProviderFactory pf =
            ClientProviderFactory.getInstance(WebClient.getConfig(this).getEndpoint());
        RxInvokerProvider rxProvider = pf.getRxInvokerProvider();
        if (rxProvider != null && rxProvider.isProviderFor(rxCls)) {
            return (T)rxProvider.getRxInvoker(sync(), executorService);
        }
        throw new IllegalStateException("Provider for " + rxCls.getName() + " is not available");
    }

    private void setEntityHeaders(Entity<?> entity) {
        type(entity.getMediaType());
        if (entity.getLanguage() != null) {
            language(entity.getLanguage().toString());
        }
        if (entity.getEncoding() != null) {
            encoding(entity.getEncoding());
        }
    }

    private ExecutorService lookUpExecutorService() {
        try {
            javax.naming.InitialContext ic = new javax.naming.InitialContext();
            Object execService = ic.lookup("java:comp/DefaultManagedExecutorService");
            if (execService != null) {
                return (ExecutorService)execService;
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }
}
