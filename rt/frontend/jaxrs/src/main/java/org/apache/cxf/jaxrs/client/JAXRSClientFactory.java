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

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

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
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls) {
        return create(URI.create(baseAddress), cls);
    }
    
    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls) {
        return create(baseURI, cls, false);
    }
    
    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param inheritHeaders if true then subresource proxies will inherit the headers
     *        set on parent proxies 
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls, boolean inheritHeaders) {
        
        return create(baseURI, cls, inheritHeaders, false);
    }
    
    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param direct if true then no bus and chains will be created
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls, boolean inheritHeaders, boolean direct) {
        
        if (!direct) {
            JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
            bean.setAddress(baseURI.toString());
            bean.setServiceClass(cls);
            bean.setInheritHeaders(inheritHeaders);
            return bean.create(cls);
        } else {
            boolean isRoot = AnnotationUtils.getClassAnnotation(cls, Path.class) != null;
            ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(cls, cls, isRoot, true);
            
            return cls.cast(ProxyHelper.getProxy(cls.getClassLoader(),
                            new Class[]{cls, Client.class}, 
                            new ClientProxyImpl(baseURI, baseURI, cri, inheritHeaders)));
        }
    }
    
    /**
     * Creates a proxy
     * @param baseAddress baseAddress
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param contentType JAXRS MediaType representing HTTP Content-Type header, can be null
     * @param acceptTypes JAXRS MediaTypes representing HTTP Accept header, can be null
     * @return typed proxy
     */
    public static <T> T create(String baseAddress, Class<T> cls, MediaType contentType, 
                               MediaType... acceptTypes) {
        T proxy = create(baseAddress, cls);
        WebClient.client(proxy).type(contentType).accept(acceptTypes);
        return proxy;
    }
    
    /**
     * Creates a proxy, baseURI will be set to Client currentURI
     *   
     * @param client Client instance
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T fromClient(Client client, Class<T> cls) {
        if (cls.isAssignableFrom(client.getClass())) {
            return cls.cast(client);
        }
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
        return fromClient(client, cls, inheritHeaders, false);
    }
    
    /**
     * Creates a proxy, baseURI will be set to Client currentURI
     * @param client Client instance
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @param inheritHeaders if true then existing Client headers will be inherited by new proxy 
     *        and subresource proxies if any 
     * @param direct if true then no bus and chains will be created       
     * @return typed proxy
     */
    public static <T> T fromClient(Client client, Class<T> cls, boolean inheritHeaders, boolean direct) {
        
        T proxy = create(client.getCurrentURI(), cls, inheritHeaders, direct);
        if (inheritHeaders) {
            WebClient.client(proxy).headers(client.getHeaders());
        }
        return proxy;
    }
    
    static <T> T create(Class<T> cls, InvocationHandler handler) {
        
        return cls.cast(ProxyHelper.getProxy(cls.getClassLoader(),
                        new Class[]{cls, Client.class}, handler));
    }
}
