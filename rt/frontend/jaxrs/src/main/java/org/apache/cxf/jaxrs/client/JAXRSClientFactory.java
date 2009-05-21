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

import org.apache.cxf.common.util.ProxyHelper;
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
     * @param baseAddress baseAddress
     * @param cls resource class, if not interface then a CGLIB proxy will be created
     * @param config classpath location of Spring configuration resource
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
     * @param config classpath location of Spring configuration resource
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
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param providers list of providers
     * @param config classpath location of Spring configuration resource
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = getBean(baseAddress, cls, configLocation);
        bean.setProviders(providers);
        return bean.create(cls);
    }
    
    /**
     * Creates a proxy which will do basic authentication
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param username username
     * @param password password
     * @param config classpath location of Spring configuration resource
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
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, String modelRef, 
                               List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        bean.setModelRef(modelRef);
        return bean.create(cls);
    }
    
    /**
     * Creates a proxy using user resource model
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param modelBeans model beans
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
     * @return typed proxy
     */
    public static <T> T createFromModel(String baseAddress, Class<T> cls, List<UserResource> modelBeans,
                               List<?> providers, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);
        bean.setProviders(providers);
        bean.setModelBeans(modelBeans);
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
        
        T proxy = create(client.getCurrentURI(), cls, inheritHeaders);
        if (inheritHeaders) {
            WebClient.client(proxy).headers(client.getHeaders());
        }
        WebClient.copyProperties(WebClient.client(proxy), client);
        return proxy;
    }
    
    static <T> T create(Class<T> cls, InvocationHandler handler) {
        
        return cls.cast(ProxyHelper.getProxy(cls.getClassLoader(), new Class[]{cls, Client.class}, handler));
    }
    
    private static JAXRSClientFactoryBean getBean(String baseAddress, Class<?> cls, String configLocation) {
        JAXRSClientFactoryBean bean = WebClient.getBean(baseAddress, configLocation);
        bean.setServiceClass(cls);
        return bean;
    }
    
    
}
