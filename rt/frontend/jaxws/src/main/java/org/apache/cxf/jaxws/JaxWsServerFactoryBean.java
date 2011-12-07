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
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Bean to help easily create Server endpoints for JAX-WS.
 * <pre>
 * JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
 * sf.setServiceClass(MyService.class);
 * sf.setAddress("http://acme.com/myService");
 * sf.create();
 * </pre>
 * This will start a server and register it with the ServerManager. 
 */
public class JaxWsServerFactoryBean extends ServerFactoryBean {
    protected boolean doInit;
    protected List<Handler> handlers = new ArrayList<Handler>();

    private boolean blockPostConstruct;
    private boolean blockInjection;
    
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
    public JaxWsServiceFactoryBean getJaxWsServiceFactory() {
        return (JaxWsServiceFactoryBean)getServiceFactory();
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
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?> ... cls) {
        Class<?> seiClass = ((JaxWsServiceFactoryBean)getServiceFactory())
            .getJaxWsImplementorInfo().getSEIClass();
        if (seiClass != null) {
            boolean found = false;
            for (Class<?> c : cls) {
                if (c.equals(seiClass)) {
                    found = true;
                }
            }
            if (!found) {
                Class<?> cls2[] = new Class<?>[cls.length + 1];
                System.arraycopy(cls, 0, cls2, 0, cls.length);
                cls2[cls.length] = seiClass;
                cls = cls2;
            }
        }
        
        AnnotationInterceptors provider = new AnnotationInterceptors(cls);
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
            if (this.getServiceFactory().isPopulateFromClass()) {
                //Provider, but no wsdl.  Synthetic ops
                for (BindingOperationInfo op : bindingInfo.getOperations()) {
                    op.setProperty("operation.is.synthetic", Boolean.TRUE);
                    op.getOperationInfo().setProperty("operation.is.synthetic", Boolean.TRUE);
                }
            }
        }

        return bindingInfo;
    }
    
    public Server create() {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            if (bus != null) {
                ClassLoader loader = bus.getExtension(ClassLoader.class);
                if (loader != null) {
                    Thread.currentThread().setContextClassLoader(loader);
                }
            }

            Server server = super.create();
            initializeResourcesAndHandlerChain();
            checkPrivateEndpoint(server.getEndpoint());
            
            return server;
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }
    
    private synchronized void initializeResourcesAndHandlerChain() {
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
        if (instance != null && !blockInjection) {
            ResourceManager resourceManager = getBus().getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = resourceManager.getResourceResolvers();
            resourceManager = new DefaultResourceManager(resolvers); 
            resourceManager.addResourceResolver(new WebServiceContextResourceResolver());
            ResourceInjector injector = new ResourceInjector(resourceManager);
            if (Proxy.isProxyClass(instance.getClass()) && getServiceClass() != null) {
                injector.inject(instance, getServiceClass());
                if (!blockPostConstruct) {
                    injector.construct(instance, getServiceClass());
                }
            } else {
                injector.inject(instance);
                if (!blockPostConstruct) {
                    injector.construct(instance);
                }
            }
        }
    }

    /**
     * @param blockPostConstruct @PostConstruct method will not be called 
     *  if this property is set to true - this may be necessary in cases
     *  when the @PostConstruct method needs to be called at a later stage,
     *  for example, when a higher level container does its own injection.  
     */
    public void setBlockPostConstruct(boolean blockPostConstruct) {
        this.blockPostConstruct = blockPostConstruct;
    }
    /**
     * No injection or PostConstruct will be called if this is set to true.
     * If the container has already handled the injection, this should 
     * be set to true.
     * @param b
     */
    public void setBlockInjection(boolean b) {
        this.blockInjection = b;
    }
      
}
