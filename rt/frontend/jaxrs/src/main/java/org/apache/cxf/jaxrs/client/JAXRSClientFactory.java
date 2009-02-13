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
        return create(baseURI, baseURI, cls, true, false);
    }
    
    /**
     * Creates a proxy
     * @param baseURI baseURI
     * @param cls proxy class, if not interface then a CGLIB proxy will be created
     * @return typed proxy
     */
    public static <T> T create(URI baseURI, Class<T> cls, boolean inheritHeaders) {
        return create(baseURI, baseURI, cls, true, inheritHeaders);
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
     * @param inheritHeaders if existing Client headers can be inherited by new proxy 
     *        and subresource proxies if any 
     * @return typed proxy
     */
    public static <T> T fromClient(Client client, Class<T> cls, boolean inheritHeaders) {
        if (client.getClass().isAssignableFrom(cls)) {
            return cls.cast(client);
        }
        T proxy = create(client.getCurrentURI(), client.getCurrentURI(), cls, 
                         AnnotationUtils.getClassAnnotation(cls, Path.class) != null, inheritHeaders);
        if (inheritHeaders) {
            WebClient.client(proxy).headers(client.getHeaders());
        }
        return proxy;
    }
    
    static <T> T create(URI baseURI, URI currentURI, Class<T> cls, boolean root, boolean inheritHeaders) {
        ClassResourceInfo classResourceInfo = ResourceUtils.createClassResourceInfo(cls, cls, root, true);
        
        return cls.cast(ProxyHelper.getProxy(cls.getClassLoader(),
                        new Class[]{cls, Client.class},
                        new ClientProxyImpl(baseURI, currentURI, classResourceInfo, inheritHeaders)));
    }
}
