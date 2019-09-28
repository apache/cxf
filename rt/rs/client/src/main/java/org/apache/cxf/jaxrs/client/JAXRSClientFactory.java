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

import java.lang.reflect.InvocationHandler;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.model.UserResource;

/**
 * Factory for creating proxy clients.
 *
 */
public final class JAXRSClientFactory {

    private JAXRSClientFactory() {

    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls) {
        return create(URI.create(baseAddress), cls);
    }

    /**
     * Creates a proxy using a custom class loader
     * @param baseAddress baseAddress
     * @param loader class loader
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, ClassLoader loader) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, null);
        bean.setClassLoader(loader);
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls) {
        return create(baseURI, cls, false);
    }

    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @param inheritHeaders if true then existing proxy headers will be inherited by
     *        subresource proxies if any
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls, boolean inheritHeaders) {
        JAXRSClientFactoryBean bean = getBean(baseURI.toString(), cls, null);
        bean.setInheritHeaders(inheritHeaders);
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddres
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @param properties additional properties
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, Map<String, Object> properties) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, null);
        bean.setProperties(properties);
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     *        This class is expected to have a root JAXRS Path annotation containing
     *        template variables, for ex, "/path/{id1}/{id2}"
     * @param configLocation classpath location of the configuration resource
     * @param varValues values to replace root Path template variables
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, String configLocation,
                               Object... varValues) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        return bean.create(cls, varValues);
    }


    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers) {
        return create(baseAddress, cls, providers, null);
    }

    /**
     * Creates a thread safe proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param threadSafe if true then a thread-safe proxy will be created
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, boolean threadSafe) {
        return create(baseAddress, cls, providers, Collections.emptyMap(), threadSafe);
    }
    /**
     * Creates a thread safe proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param threadSafe if true then a thread-safe proxy will be created
     * @param properties additional properties
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, 
            Map<String, Object> properties, boolean threadSafe) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, null);
        bean.setProviders(providers);
        bean.setProperties(properties);
        if (threadSafe) {
            bean.setInitialState(new ThreadLocalClientState(baseAddress, properties));
        }
        return bean.create(cls);
    }

    /**
     * Creates a thread safe proxy and allows to specify time to keep state.
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param timeToKeepState how long to keep this state
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, long timeToKeepState) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, null);
        bean.setProviders(providers);
        bean.setInitialState(new ThreadLocalClientState(baseAddress, timeToKeepState));
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        bean.setProviders(providers);
        return bean.create(cls);
    }

    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param features the features which will be applied to the client
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers,
                               List<Feature> features,
                               String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        bean.setProviders(providers);
        bean.setFeatures(features);
        return bean.create(cls);
    }

    /**
     * Creates a proxy which will do basic authentication
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param username username
     * @param password password
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, String username,
                               String password, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        bean.setUsername(username);
        bean.setPassword(password);
        return bean.create(cls);
    }

    /**
     * Creates a proxy which will do basic authentication
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param username username
     * @param password password
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers,
                               String username, String password, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        bean.setUsername(username);
        bean.setPassword(password);
        bean.setProviders(providers);
        return bean.create(cls);
    }

    /**
     * Creates a proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelRef model location
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, String modelRef,
                                        String configLocation) {
        return createFromModel(baseAddress, cls, modelRef, Collections.emptyList(), configLocation);
    }

    /**
     * Creates a proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelRef model location
     * @param providers list of providers
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, String modelRef,
                               List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        bean.setModelRef(modelRef);
        bean.setServiceClass(cls);
        return bean.create(cls);
    }

    /**
     * Creates a thread safe proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelRef model location
     * @param providers list of providers
     * @param threadSafe if true then thread-safe proxy will be created
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, String modelRef,
                                        List<?> providers, boolean threadSafe) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, null);
        bean.setProviders(providers);
        bean.setModelRef(modelRef);
        bean.setServiceClass(cls);
        if (threadSafe) {
            bean.setInitialState(new ThreadLocalClientState(baseAddress));
        }
        return bean.create(cls);
    }

    /**
     * Creates a thread safe proxy using user resource model and allows to
     * specify time to keep state.
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelRef model location
     * @param providers list of providers
     * @param timeToKeepState how long to keep this state
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, String modelRef,
                                        List<?> providers, long timeToKeepState) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, null);
        bean.setProviders(providers);
        bean.setModelRef(modelRef);
        bean.setServiceClass(cls);
        bean.setInitialState(new ThreadLocalClientState(baseAddress, timeToKeepState));
        return bean.create(cls);
    }

    /**
     * Creates a proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelBeans model beans
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, List<UserResource> modelBeans,
                               String configLocation) {
        return createFromModel(baseAddress, cls, modelBeans, Collections.emptyList(), configLocation);
    }

    /**
     * Creates a proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelBeans model beans
     * @param providers list of providers
     * @param configLocation classpath location of the configuration resource
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, List<UserResource> modelBeans,
                               List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);

        bean.setProviders(providers);
        bean.setModelBeans(modelBeans);
        bean.setServiceClass(cls);
        return bean.create(cls);
    }

    /**
     * Creates a proxy, baseURI will be set to Client currentURI
     *
     * @param client Client instance
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T fromClient(Client client, Class<T> cls) {
        return fromClient(client, cls, false);
    }

    /**
     * Creates a proxy, baseURI will be set to Client currentURI
     * @param client Client instance
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param inheritHeaders if true then existing Client headers will be inherited by new proxy
     *        and subresource proxies if any
     * @return typed proxy
     */
    public static <T> T fromClient(Client client, Class<T> cls, boolean inheritHeaders) {
        JAXRSClientFactoryBean bean = getBean(client.getCurrentURI().toString(), cls, null);
        bean.setInheritHeaders(inheritHeaders);

        ClientState clientState = WebClient.getClientState(client);

        T proxy = null;
        if (clientState == null) {
            proxy = bean.create(cls);
            if (inheritHeaders) {
                WebClient.client(proxy).headers(client.getHeaders());
            }
        } else {
            MultivaluedMap<String, String> headers = inheritHeaders ? client.getHeaders() : null;
            bean.setInitialState(clientState.newState(client.getCurrentURI(), headers, null, bean.getProperties()));
            proxy = bean.create(cls);
        }
        WebClient.copyProperties(WebClient.client(proxy), client);
        return proxy;
    }

    static <T> T createProxy(Class<T> cls, ClassLoader loader, InvocationHandler handler) {

        return cls.cast(ProxyHelper.getProxy(loader == null ? cls.getClassLoader() : loader,
                                             new Class[]{Client.class, InvocationHandlerAware.class, cls},
                                             handler));
    }

    private static JAXRSClientFactoryBean getBean(String baseAddress, Class<?> cls, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);
        bean.setServiceClass(cls);
        return bean;
    }


}
