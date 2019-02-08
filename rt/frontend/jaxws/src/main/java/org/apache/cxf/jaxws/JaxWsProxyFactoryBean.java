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
package org.apache.cxf.jaxws;

import java.io.Closeable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.context.WebServiceContextResourceResolver;
import org.apache.cxf.jaxws.handler.AnnotationHandlerChainBuilder;
import org.apache.cxf.jaxws.interceptors.HolderInInterceptor;
import org.apache.cxf.jaxws.interceptors.HolderOutInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassInInterceptor;
import org.apache.cxf.jaxws.interceptors.WrapperClassOutInterceptor;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

/**
 * Factory for creating JAX-WS proxies, This class provides access to the
 * internal properties used to set-up proxies. Using it provides more control
 * than the standard JAX-WS APIs.
 */
@NoJSR250Annotations
public class JaxWsProxyFactoryBean extends ClientProxyFactoryBean {
    @SuppressWarnings("rawtypes")
    List<Handler> handlers = new ArrayList<>();
    boolean loadHandlers = true;

    public JaxWsProxyFactoryBean() {
        super(new JaxWsClientFactoryBean());
    }
    public JaxWsProxyFactoryBean(ClientFactoryBean fact) {
        super(fact);
    }
    protected String getConfiguredName() {
        QName name = getEndpointName();
        if (name == null) {
            JaxWsServiceFactoryBean sfb = (JaxWsServiceFactoryBean)getClientFactoryBean().getServiceFactory();
            name = sfb.getJaxWsImplementorInfo().getEndpointName();
        }
        return name + ".jaxws-client.proxyFactory";
    }

    /**
     * Specifies a list of JAX-WS Handler implementations that are to be
     * used by the proxy.
     *
     * @param h a <code>List</code> of <code>Handler</code> objects
     */
    public void setHandlers(@SuppressWarnings("rawtypes") List<Handler> h) {
        handlers.clear();
        handlers.addAll(h);
    }
    /**
     * Returns the configured list of JAX-WS handlers for the proxy.
     *
     * @return a <code>List</code> of <code>Handler</code> objects
     */
    @SuppressWarnings("rawtypes")
    public List<Handler> getHandlers() {
        return handlers;
    }

    public void setLoadHandlers(boolean b) {
        loadHandlers = b;
    }
    public boolean isLoadHandlers() {
        return loadHandlers;
    }


    @Override
    protected ClientProxy clientClientProxy(Client c) {
        JaxWsClientProxy cp = new JaxWsClientProxy(c,
                                                   ((JaxWsEndpointImpl)c.getEndpoint()).getJaxwsBinding());
        cp.getRequestContext().putAll(this.getProperties());
        buildHandlerChain(cp);
        return cp;
    }

    @Override
    protected Class<?>[] getImplementingClasses() {
        Class<?> cls = getClientFactoryBean().getServiceClass();
        return new Class[] {cls, BindingProvider.class, Closeable.class, Client.class};
    }

    /**
     * Creates a JAX-WS proxy that can be used to make remote invocations.
     *
     * @return the proxy. You must cast the returned object to the approriate class
     * before making remote calls
     */
    @Override
    public synchronized Object create() {
        ClassLoaderHolder orig = null;
        try {
            if (getBus() != null) {
                ClassLoader loader = getBus().getExtension(ClassLoader.class);
                if (loader != null) {
                    orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
            }

            Object obj = super.create();
            Service service = getServiceFactory().getService();
            if (needWrapperClassInterceptor(service.getServiceInfos().get(0))) {
                List<Interceptor<? extends Message>> in = super.getInInterceptors();
                List<Interceptor<? extends Message>> out = super.getOutInterceptors();
                in.add(new WrapperClassInInterceptor());
                in.add(new HolderInInterceptor());
                out.add(new WrapperClassOutInterceptor());
                out.add(new HolderOutInterceptor());
            }
            return obj;
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
    }

    private boolean needWrapperClassInterceptor(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return false;
        }

        for (OperationInfo opInfo : serviceInfo.getInterface().getOperations()) {
            if (opInfo.isUnwrappedCapable()
                && opInfo.getProperty(ReflectionServiceFactoryBean.WRAPPERGEN_NEEDED) != null) {
                return true;

            }
        }
        return false;
    }

    private void buildHandlerChain(JaxWsClientProxy cp) {
        AnnotationHandlerChainBuilder builder = new AnnotationHandlerChainBuilder();
        JaxWsServiceFactoryBean sf = (JaxWsServiceFactoryBean)getServiceFactory();
        @SuppressWarnings("rawtypes")
        List<Handler> chain = new ArrayList<>(handlers);
        if (loadHandlers) {
            chain.addAll(builder.buildHandlerChainFromClass(sf.getServiceClass(),
                                                            sf.getEndpointInfo().getName(),
                                                            sf.getServiceQName(),
                                                            this.getBindingId()));
        }

        if (!chain.isEmpty()) {
            ResourceManager resourceManager = getBus().getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = resourceManager.getResourceResolvers();
            resourceManager = new DefaultResourceManager(resolvers);
            resourceManager.addResourceResolver(new WebServiceContextResourceResolver());
            ResourceInjector injector = new ResourceInjector(resourceManager);
            for (Handler<?> h : chain) {
                if (Proxy.isProxyClass(h.getClass()) && getServiceClass() != null) {
                    injector.inject(h, getServiceClass());
                    injector.construct(h, getServiceClass());
                } else {
                    injector.inject(h);
                    injector.construct(h);
                }
            }
        }

        cp.getBinding().setHandlerChain(chain);
    }

}