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



import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.AnnotationInterceptors;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.context.WebServiceContextResourceResolver;
import org.apache.cxf.jaxws.handler.AnnotationHandlerChainBuilder;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.SingletonFactory;
import org.apache.cxf.service.model.BindingInfo;

/**
 * Bean to help easily create Server endpoints for JAX-WS. Example:
 * <pre>
 * JaxWsServerFactoryBean sf = JaxWsServerFactoryBean();
 * sf.setServiceClass(MyService.class);
 * sf.setAddress("http://acme.com/myService");
 * sf.create();
 * </pre>
 * This will start a server for you and register it with the ServerManager. 
 */
public class JaxWsServerFactoryBean extends ServerFactoryBean {
    protected boolean doInit;
    protected List<Handler> handlers = new ArrayList<Handler>();
    
    public JaxWsServerFactoryBean() {
        this(new JaxWsServiceFactoryBean());
    }
    public JaxWsServerFactoryBean(JaxWsServiceFactoryBean serviceFactory) {
        super(serviceFactory);
        
        JaxWsSoapBindingConfiguration defConfig 
            = new JaxWsSoapBindingConfiguration(serviceFactory);
        
        setBindingConfig(defConfig);
        doInit = true;
    }
    
    public void setHandlers(List<Handler> h) {
        handlers.clear();
        handlers.addAll(h);
    }
    public void addHandlers(List<Handler> h) {
        handlers.addAll(h);
    }
    public List<Handler> getHandlers() {
        return handlers;
    }

    /**
     * Add annotated Interceptors and Features to the Endpoint
     * @param ep
     */
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?> cls) {
        Class<?> seiClass = ((JaxWsServiceFactoryBean)getServiceFactory())
            .getJaxWsImplementorInfo().getSEIClass();
        AnnotationInterceptors provider;
        if (seiClass != null) {
            provider = new AnnotationInterceptors(cls, seiClass);
        } else {
            provider = new AnnotationInterceptors(cls);
        }
        initializeAnnotationInterceptors(provider, ep);
    }      
    
    @Override
    protected Invoker createInvoker() {
        if (getServiceBean() == null) {
            return new JAXWSMethodInvoker(new SingletonFactory(getServiceClass()));
        }
        return new JAXWSMethodInvoker(getServiceBean());
    }

    @Override
    protected BindingInfo createBindingInfo() {
        JaxWsServiceFactoryBean sf = (JaxWsServiceFactoryBean)getServiceFactory(); 
        
        JaxWsImplementorInfo implInfo = sf.getJaxWsImplementorInfo();
        String jaxBid = implInfo.getBindingType();
        String binding = getBindingId();
        if (binding == null) {
            binding = jaxBid;
            setBindingId(binding);
        }
        
        if (binding.equals(SOAPBinding.SOAP11HTTP_BINDING) 
            || binding.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)) {
            binding = "http://schemas.xmlsoap.org/wsdl/soap/";
            setBindingId(binding);
            if (getBindingConfig() == null) {
                setBindingConfig(new JaxWsSoapBindingConfiguration(sf));
            }            
        } else if (binding.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING)) {
            binding = SOAPBinding.SOAP12HTTP_BINDING;
            setBindingId(binding);
            if (getBindingConfig() == null) {
                setBindingConfig(new JaxWsSoapBindingConfiguration(sf));
            }
        }
        
        if (getBindingConfig() instanceof JaxWsSoapBindingConfiguration) {
            JaxWsSoapBindingConfiguration conf = (JaxWsSoapBindingConfiguration)getBindingConfig();
            
            if (jaxBid.equals(SOAPBinding.SOAP12HTTP_BINDING)) {
                conf.setVersion(Soap12.getInstance());
            }
            
            if (jaxBid.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING)) {
                conf.setVersion(Soap12.getInstance());
                conf.setMtomEnabled(true);
            }
            if (jaxBid.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)) {
                conf.setMtomEnabled(true);
            }

            if (transportId != null) {
                conf.setTransportURI(transportId);
            }
            conf.setJaxWsServiceFactoryBean(sf);
            
        }
        
        BindingInfo bindingInfo = super.createBindingInfo();        

        if (implInfo.isWebServiceProvider()) {
            bindingInfo.getService().setProperty("soap.force.doclit.bare", Boolean.TRUE);
        }

        return bindingInfo;
    }
    
    public Server create() {
        Server server = super.create();
        init();
        return server;
    }
    
    private synchronized void init() {
        if (doInit) {
            try {
                injectResources(getServiceBean());
                buildHandlerChain();
            } catch (Exception ex) {
                if (ex instanceof WebServiceException) { 
                    throw (WebServiceException)ex; 
                }
                throw new WebServiceException("Creation of Endpoint failed", ex);
            }
        }
        doInit = false;
    }
    
    
    /**
     * Obtain handler chain from annotations.
     *
     */
    private void buildHandlerChain() {
        AnnotationHandlerChainBuilder builder = new AnnotationHandlerChainBuilder();
        JaxWsServiceFactoryBean sf = (JaxWsServiceFactoryBean)getServiceFactory(); 
        List<Handler> chain = new ArrayList<Handler>(handlers);
        
        chain.addAll(builder.buildHandlerChainFromClass(getServiceBeanClass(), sf.getEndpointInfo()
            .getName(), sf.getServiceQName(), this.getBindingId()));
        for (Handler h : chain) {
            injectResources(h);
        }
        ((JaxWsEndpointImpl)getServer().getEndpoint()).getJaxwsBinding().setHandlerChain(chain);
    }
    
    /**
     * inject resources into servant.  The resources are injected
     * according to @Resource annotations.  See JSR 250 for more
     * information.
     */
    /**
     * @param instance
     */
    protected void injectResources(Object instance) {
        if (instance != null) {
            ResourceManager resourceManager = getBus().getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = resourceManager.getResourceResolvers();
            resourceManager = new DefaultResourceManager(resolvers); 
            resourceManager.addResourceResolver(new WebServiceContextResourceResolver());
            ResourceInjector injector = new ResourceInjector(resourceManager);
            if (Proxy.isProxyClass(instance.getClass()) && getServiceClass() != null) {
                injector.inject(instance, getServiceClass());
                injector.construct(instance, getServiceClass());
            } else {
                injector.inject(instance);
                injector.construct(instance);
            }
        }
    }  
}
