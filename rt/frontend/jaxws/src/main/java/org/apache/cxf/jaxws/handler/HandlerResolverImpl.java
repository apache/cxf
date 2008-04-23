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

package org.apache.cxf.jaxws.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

public class HandlerResolverImpl implements HandlerResolver {
    private final Map<PortInfo, List<Handler>> handlerMap = new HashMap<PortInfo, List<Handler>>();
    
    //private QName service;   
    private Class<?> annotationClass;
    private Bus bus;

    public HandlerResolverImpl(Bus bus, QName serviceName, Class<?> clazz) {
        //this.service = pService;
        this.bus = bus;
        this.annotationClass = clazz;
    }

    public HandlerResolverImpl() {
        this(null, null, null);
    }

    public List<Handler> getHandlerChain(PortInfo portInfo) {

        List<Handler> handlerChain = handlerMap.get(portInfo);
        if (handlerChain == null) {
            handlerChain = createHandlerChain(portInfo);
            handlerMap.put(portInfo, handlerChain);
        }
        return handlerChain;
    }

    private List<Handler> createHandlerChain(PortInfo portInfo) {
        List<Handler> chain = null;

        if (null == chain) {
            chain = new ArrayList<Handler>();
        }
        if (annotationClass != null) {
            chain.addAll(getHandlersFromAnnotation(annotationClass, portInfo));         
        }
        
        for (Handler h : chain) {
            configHandler(h);
        }       
        
        return chain;
    }

    /**
     * Obtain handler chain from annotations.
     * 
     * @param obj A endpoint implementation class or a SEI, or a generated
     *            service class.
     */
    private List<Handler> getHandlersFromAnnotation(Class<?> clazz, PortInfo portInfo) {
        AnnotationHandlerChainBuilder builder = new AnnotationHandlerChainBuilder();

        List<Handler> chain = builder.buildHandlerChainFromClass(clazz, 
            portInfo != null ? portInfo.getPortName() : null, 
            portInfo != null ? portInfo.getServiceName() : null,
            portInfo != null ? portInfo.getBindingID() : null);
        return chain;
    }
    
    /**
     * JAX-WS section 9.3.1: The runtime MUST then carry out any injections
     * requested by the handler, typically via the javax .annotation.Resource
     * annotation. After all the injections have been carried out, including in
     * the case where no injections were requested, the runtime MUST invoke the
     * method carrying a javax.annotation .PostConstruct annotation, if present.
     */
    private void configHandler(Handler handler) {
        if (handler != null) {
            ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = resourceManager.getResourceResolvers();
            resourceManager = new DefaultResourceManager(resolvers);
//            resourceManager.addResourceResolver(new WebContextEntriesResourceResolver());
            ResourceInjector injector = new ResourceInjector(resourceManager);
            injector.inject(handler);
            injector.construct(handler);
        }

    }
}
