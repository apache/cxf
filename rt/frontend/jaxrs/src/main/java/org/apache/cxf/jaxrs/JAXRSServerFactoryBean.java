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
package org.apache.cxf.jaxrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.Invoker;


/**
 * Bean to help easily create Server endpoints for JAX-RS. Example:
 * <pre>
 * JAXRSServerFactoryBean sf = JAXRSServerFactoryBean();
 * sf.setResourceClasses(Book.class);
 * sf.setBindingId(JAXRSBindingFactory.JAXRS_BINDING_ID);
 * sf.setAddress("http://localhost:9080/");
 * Server myServer = sf.create();
 * </pre>
 * This will start a server for you and register it with the ServerManager.  Note 
 * you should explicitly close the {@link org.apache.cxf.endpoint.Server} created
 * when finished with it:
 * <pre>
 * myServer.close();
 * myServer.destroy(); // closes first if close() not previously called
 * </pre> 
 */
public class JAXRSServerFactoryBean extends AbstractJAXRSFactoryBean {
    
    protected Map<Class<?>, ResourceProvider> resourceProviders = new HashMap<Class<?>, ResourceProvider>();
    
    private Server server;
    private boolean start = true;
    private Map<Object, Object> languageMappings;
    private Map<Object, Object> extensionMappings;
    private ResourceComparator rc;
    private ApplicationInfo appProvider;
    private String documentLocation;
    
    public JAXRSServerFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }

    public JAXRSServerFactoryBean(JAXRSServiceFactoryBean sf) {
        super(sf);
    }
    
    /**
     * Saves the reference to the JAX-RS {@link Application}
     * @param app
     */
    public void setApplication(Application app) {
        setApplicationInfo(new ApplicationInfo(app, getBus()));
    }
    
    public void setApplicationInfo(ApplicationInfo provider) {
        appProvider = provider;
        Set<String> appNameBindings = AnnotationUtils.getNameBindings(provider.getProvider()
                                                                      .getClass().getAnnotations());
        for (ClassResourceInfo cri : getServiceFactory().getClassResourceInfo()) {
            Set<String> clsNameBindings = new LinkedHashSet<String>(appNameBindings);
            clsNameBindings.addAll(AnnotationUtils.getNameBindings(cri.getServiceClass().getAnnotations()));
            cri.setNameBindings(clsNameBindings);
        }
    }
    
    /**
     * Resource comparator which may be used to customize the way 
     * a root resource or resource method is selected 
     * @param rcomp comparator
     */
    public void setResourceComparator(ResourceComparator rcomp) {
        rc = rcomp;
    }
    
    /**
     * By default the subresources are resolved dynamically, mainly due to
     * the JAX-RS specification allowing Objects being returned from the subresource
     * locators. Setting this property to true enables the runtime to do the 
     * early resolution.
     * 
     * @param enableStatic enabling the static resolution if set to true
     */
    public void setStaticSubresourceResolution(boolean enableStatic) {
        serviceFactory.setEnableStaticResolution(enableStatic);
    }

    
    /**
     * Creates the JAX-RS Server instance
     * @return the server
     */
    public void init() {
        if (server == null) {
            create();
        }
    }
    
    /**
     * Creates the JAX-RS Server instance
     * @return the server
     */
    public Server create() {
        ClassLoaderHolder origLoader = null;
        try {
            Bus bus = getBus();
            ClassLoader loader = bus.getExtension(ClassLoader.class);
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            serviceFactory.setBus(bus);
            checkResources(true);
            if (serviceFactory.getService() == null) {
                serviceFactory.create();
                updateClassResourceProviders();
            }
            
            Endpoint ep = createEndpoint();

            getServiceFactory().sendEvent(FactoryBeanListener.Event.PRE_SERVER_CREATE,
                                          server);
            
            server = new ServerImpl(getBus(), 
                                    ep, 
                                    getDestinationFactory(), 
                                    getBindingFactory());

            Invoker invoker = serviceFactory.getInvoker(); 
            if (invoker == null) {
                ep.getService().setInvoker(createInvoker());
            } else {
                ep.getService().setInvoker(invoker);
            }
            
            ServerProviderFactory factory = setupFactory(ep);
            ep.put(Application.class.getName(), appProvider);
            factory.setRequestPreprocessor(
                new RequestPreprocessor(languageMappings, extensionMappings));
            ep.put(Bus.class.getName(), getBus());
            if (documentLocation != null) {
                ep.put(JAXRSUtils.DOC_LOCATION, documentLocation);
            }
            if (rc != null) {
                ep.put("org.apache.cxf.jaxrs.comparator", rc);
            }
            checkPrivateEndpoint(ep);
            
            factory.applyDynamicFeatures(getServiceFactory().getClassResourceInfo());
            applyBusFeatures(getBus());
            applyFeatures();

            getServiceFactory().sendEvent(FactoryBeanListener.Event.SERVER_CREATED,
                                          server, 
                                          null,
                                          null);
            
            
            if (start) {
                try {
                    server.start();
                } catch (RuntimeException re) {
                    server.destroy(); // prevent resource leak
                    throw re;
                }
            }
        } catch (EndpointException e) {
            throw new ServiceConstructionException(e);
        } catch (BusException e) {
            throw new ServiceConstructionException(e);
        } catch (IOException e) {
            throw new ServiceConstructionException(e);
        } catch (Exception e) {
            throw new ServiceConstructionException(e);
        } finally {
            if (origLoader != null) {
                origLoader.reset();
            }
        }

        return server;
    }

    public Server getServer() {
        return server;
    }
    
    protected ServerProviderFactory setupFactory(Endpoint ep) { 
        ServerProviderFactory factory = ServerProviderFactory.createInstance(getBus()); 
        setBeanInfo(factory);
        factory.setApplicationProvider(appProvider);
        super.setupFactory(factory, ep);
        return factory;
    }

    protected void setBeanInfo(ServerProviderFactory factory) {
        List<ClassResourceInfo> cris = serviceFactory.getClassResourceInfo();
        for (ClassResourceInfo cri : cris) {
            cri.initBeanParamInfo(factory);
        }
        
    }
    
    protected void applyBusFeatures(final Bus bus) {
        if (bus.getFeatures() != null) {
            for (Feature feature : bus.getFeatures()) {
                feature.initialize(server, bus);
            }
        }
    }
    
    protected void applyFeatures() {
        if (getFeatures() != null) {
            for (Feature feature : getFeatures()) {
                feature.initialize(server, getBus());
            }
        }
    }

    protected Invoker createInvoker() {
        return serviceFactory.createInvoker();
    }

    /**
     * Sets the language mappings, 
     * example, 'en' is the key and 'en-gb' is the value.
     * 
     * @param lMaps the language mappings
     */
    public void setLanguageMappings(Map<Object, Object> lMaps) {
        languageMappings = lMaps;
    }
    
    /**
     * Sets the extension mappings, 
     * example, 'xml' is the key and 'text/xml' is the value.
     * 
     * @param lMaps the extension mappings
     */
    public void setExtensionMappings(Map<Object, Object> extMaps) {
        extensionMappings = extMaps;
    }
    
    public List<Class<?>> getResourceClasses() {
        return serviceFactory.getResourceClasses();
    }
    
    /**
     * This method is used primarily by Spring handler processing
     * the jaxrs:server/@serviceClass attribute. It delegates to
     * setResourceClasses method accepting the array of Class parameters.
     * @param clazz the service/resource class
     */
    public void setServiceClass(Class<?> clazz) {
        serviceFactory.setResourceClasses(clazz);
    }

    /**
     * Sets one or more root resource classes 
     * @param classes the list of resource classes
     */
    public void setResourceClasses(List<Class<?>> classes) {
        serviceFactory.setResourceClasses(classes);
    }

    /**
     * Sets one or more root resource classes 
     * @param classes the array of resource classes
     */
    public void setResourceClasses(Class<?>... classes) {
        serviceFactory.setResourceClasses(classes);
    }
    
    /**
     * Sets the resource beans. If this is set then the JAX-RS runtime 
     * will not be responsible for the life-cycle of resource classes.
     * 
     * @param beans the array of resource instances
     */
    public void setServiceBeanObjects(Object... beans) {
        setServiceBeans(Arrays.asList(beans));
    }
    
    /**
     * Sets the single resource bean. If this is set then the JAX-RS runtime 
     * will not be responsible for the life-cycle of resource classes.
     * Please avoid setting the resource class of this bean explicitly,
     * the runtime will determine it itself.  
     * 
     * @param bean resource instance
     */
    public void setServiceBean(Object bean) {
        setServiceBeans(Arrays.asList(bean));
    }
    
    /**
     * Sets the resource beans. If this is set then the JAX-RS runtime 
     * will not be responsible for the life-cycle of resource classes.
     * 
     * @param beans the list of resource instances
     */
    public void setServiceBeans(List<Object> beans) {
        List<Object> newBeans = new ArrayList<Object>();
        addToBeans(newBeans, beans);
        serviceFactory.setResourceClassesFromBeans(newBeans);
    }    
    
    /**
     * Sets the provider managing the life-cycle of the given resource class
     * <pre>
     * Example:
     *  setResourceProvider(BookStoreInterface.class, new SingletonResourceProvider(new BookStore()));
     * </pre>
     * @param c resource class
     * @param rp resource provider
     */
    public void setResourceProvider(Class<?> c, ResourceProvider rp) {
        resourceProviders.put(c, rp);
    }
    
    /**
     * Sets the provider managing the life-cycle of the resource class
     * <pre>
     * Example:
     *  setResourceProvider(new SingletonResourceProvider(new BookStore()));
     * </pre>
     * @param rp resource provider
     */
    public void setResourceProvider(ResourceProvider rp) {
        setResourceProviders(CastUtils.cast(Collections.singletonList(rp), ResourceProvider.class));
    }
    
    /**
     * Sets the list of providers managing the life-cycle of the resource classes
     * 
     * @param rps resource providers
     */
    public void setResourceProviders(List<ResourceProvider> rps) {
        for (ResourceProvider rp : rps) {
            Class<?> c = rp.getResourceClass();
            setServiceClass(c);
            resourceProviders.put(c, rp);
        }
    }

    /**
     * Sets the custom Invoker which can be used to customize the way 
     * the default JAX-RS invoker calls on the service bean
     * @param invoker
     */
    public void setInvoker(Invoker invoker) {
        serviceFactory.setInvoker(invoker);
    }

    /**
     * Determines whether Services are automatically started during the create() call.  Default is true.
     * If false will need to call start() method on Server to activate it.
     * @param start Whether (true) or not (false) to start the Server during Server creation.
     */
    public void setStart(boolean start) {
        this.start = start;
    }

    protected void injectContexts() {
        Application application = appProvider == null ? null : appProvider.getProvider();
        for (ClassResourceInfo cri : serviceFactory.getClassResourceInfo()) {
            if (cri.isSingleton()) {
                InjectionUtils.injectContextProxiesAndApplication(cri, 
                                                    cri.getResourceProvider().getInstance(null),
                                                    application);
            }
        }
        if (application != null) {
            InjectionUtils.injectContextProxiesAndApplication(appProvider, 
                                                              application, null);
        }
    }
    
    protected void updateClassResourceProviders() {
        for (ClassResourceInfo cri : serviceFactory.getClassResourceInfo()) {
            if (cri.getResourceProvider() != null) {
                continue;
            }
            
            ResourceProvider rp = resourceProviders.get(cri.getResourceClass());
            if (rp != null) {
                cri.setResourceProvider(rp);
            } else {
                setDefaultResourceProvider(cri);  
            }
        }
        injectContexts();
    }

    protected void setDefaultResourceProvider(ClassResourceInfo cri) {
        cri.setResourceProvider(new PerRequestResourceProvider(cri.getResourceClass()));
    }
    
    /**
     * Set the reference to the document (WADL, etc) describing the endpoint
     * @param documentLocation document location
     */
    public void setDocLocation(String docLocation) {
        this.documentLocation = docLocation;
    }

    /**
     * Get the reference to the document (WADL, etc) describing the endpoint
     * @return document location
     */
    public String getDocLocation() {
        return documentLocation;
    }
}
