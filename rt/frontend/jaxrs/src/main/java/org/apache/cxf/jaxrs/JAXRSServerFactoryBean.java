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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.BusException;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.Invoker;


/**
 * Bean to help easily create Server endpoints for JAX-RS. Example:
 * <pre>
 * JAXRSServerFactoryBean sf = JAXRSServerFactoryBean();
 * sf.setResourceClasses(Book.class);
 * sf.setBindingId(JAXRSBindingFactory.JAXRS_BINDING_ID);
 * sf.setAddress("http://localhost:9080/");
 * sf.create();
 * </pre>
 * This will start a server for you and register it with the ServerManager.
 */
public class JAXRSServerFactoryBean extends AbstractJAXRSFactoryBean {
    
    protected Map<Class, ResourceProvider> resourceProviders = new HashMap<Class, ResourceProvider>();
    
    private Server server;
    private boolean start = true;
    private Map<Object, Object> languageMappings;
    private Map<Object, Object> extensionMappings;
    private ResourceComparator rc;
    
    public JAXRSServerFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }

    public JAXRSServerFactoryBean(JAXRSServiceFactoryBean sf) {
        super(sf);
    }
    
    public void setResourceComparator(ResourceComparator rcomp) {
        rc = rcomp;
    }
    
    public void setStaticSubresourceResolution(boolean enableStatic) {
        serviceFactory.setEnableStaticResolution(enableStatic);
    }
    
    public Server create() {
        try {
            checkResources(true);
            if (serviceFactory.getService() == null) {
                serviceFactory.setServiceName(getServiceName());
                serviceFactory.create();
                updateClassResourceProviders();
            }
            
            Endpoint ep = createEndpoint();
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
            
            ProviderFactory factory = setupFactory(ep);
            factory.setRequestPreprocessor(
                new RequestPreprocessor(languageMappings, extensionMappings));
            if (rc != null) {
                ep.put("org.apache.cxf.jaxrs.comparator", rc);
            }
            checkPrivateEndpoint(ep);
            
            applyFeatures();
            
            if (start) {
                server.start();
            }
        } catch (EndpointException e) {
            throw new ServiceConstructionException(e);
        } catch (BusException e) {
            throw new ServiceConstructionException(e);
        } catch (IOException e) {
            throw new ServiceConstructionException(e);
        } catch (Exception e) {
            throw new ServiceConstructionException(e);
        }

        return server;
    }

    protected void applyFeatures() {
        if (getFeatures() != null) {
            for (AbstractFeature feature : getFeatures()) {
                feature.initialize(server, getBus());
            }
        }
    }

    protected Invoker createInvoker() {
        return new JAXRSInvoker();
    }

    public void setLanguageMappings(Map<Object, Object> lMaps) {
        languageMappings = lMaps;
    }
    
    public void setExtensionMappings(Map<Object, Object> extMaps) {
        extensionMappings = extMaps;
    }
    
    public List<Class<?>> getResourceClasses() {
        return serviceFactory.getResourceClasses();
    }
    
    public void setServiceClass(Class clazz) {
        serviceFactory.setResourceClasses(clazz);
    }

    public void setResourceClasses(List<Class> classes) {
        serviceFactory.setResourceClasses(classes);
    }

    public void setResourceClasses(Class... classes) {
        serviceFactory.setResourceClasses(classes);
    }
    
    /**
     * Set the backing service bean. If this is set, JAX-RS runtime will not be
     * responsible for the lifecycle of resource classes.
     * 
     * @return
     */
    public void setServiceBeans(Object... beans) {
        setServiceBeans(Arrays.asList(beans));
    }
    
    public void setServiceBeans(List<Object> beans) {
        serviceFactory.setResourceClassesFromBeans(beans);
    }
    
    public void setResourceProvider(Class c, ResourceProvider rp) {
        resourceProviders.put(c, rp);
    }
    
    public void setResourceProvider(ResourceProvider rp) {
        setResourceProviders(CastUtils.cast(Collections.singletonList(rp), ResourceProvider.class));
    }
    
    
    public void setResourceProviders(List<ResourceProvider> rps) {
        for (ResourceProvider rp : rps) {
            Class<?> c = rp.getResourceClass();
            setServiceClass(c);
            resourceProviders.put(c, rp);
        }
    }

    public void setInvoker(Invoker invoker) {
        serviceFactory.setInvoker(invoker);
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    private void injectContexts() {
        for (ClassResourceInfo cri : serviceFactory.getClassResourceInfo()) {
            if (cri.isSingleton()) {
                InjectionUtils.injectContextProxies(cri, 
                                                    cri.getResourceProvider().getInstance(null));
            }
        }
    }
    
    private void updateClassResourceProviders() {
        for (ClassResourceInfo cri : serviceFactory.getClassResourceInfo()) {
            if (cri.getResourceProvider() != null) {
                continue;
            }
            
            ResourceProvider rp = resourceProviders.get(cri.getResourceClass());
            if (rp != null) {
                cri.setResourceProvider(rp);
            } else {
                //default lifecycle is per-request
                rp = new PerRequestResourceProvider(cri.getResourceClass());
                cri.setResourceProvider(rp);  
            }
        }
        injectContexts();
    }
}
