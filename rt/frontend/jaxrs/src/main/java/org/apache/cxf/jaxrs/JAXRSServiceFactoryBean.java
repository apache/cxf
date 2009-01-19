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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.invoker.Invoker;


/**
 * Build a JAX-RS service model from resource classes.
 */
public class JAXRSServiceFactoryBean extends AbstractServiceFactoryBean {
    
    protected List<ClassResourceInfo> classResourceInfos = 
        new ArrayList<ClassResourceInfo>();
    protected Map<Class, ResourceProvider> resourceProviders = new HashMap<Class, ResourceProvider>();
    
    private Invoker invoker;
    private Executor executor;
    private Map<String, Object> properties;
    private boolean enableStatic;
    
    public JAXRSServiceFactoryBean() {
    }

    
    public void setEnableStaticResolution(boolean staticResolution) {
        this.enableStatic = staticResolution;
    }
    
    @Override
    public Service create() {
        initializeServiceModel();

        initializeDefaultInterceptors();

        if (invoker != null) {
            getService().setInvoker(getInvoker());
        } else {
            getService().setInvoker(createInvoker());
        }

        if (getExecutor() != null) {
            getService().setExecutor(getExecutor());
        }
        if (getDataBinding() != null) {
            getService().setDataBinding(getDataBinding());
        }

        return getService();
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public List<Class> getResourceClasses() {
        List<Class> resourceClasses = new ArrayList<Class>();
        for (ClassResourceInfo cri : classResourceInfos) {
            resourceClasses.add(cri.getResourceClass());
        }
        return resourceClasses;
    }

    public List<ClassResourceInfo> getClassResourceInfo() {
        return Collections.unmodifiableList(classResourceInfos);
    }
    
    public void setResourceClasses(List<Class> classes) {
        for (Class resourceClass : classes) {
            ClassResourceInfo classResourceInfo = 
                ResourceUtils.createClassResourceInfo(resourceClass, resourceClass, true, enableStatic);
            if (classResourceInfo != null) {
                classResourceInfos.add(classResourceInfo);
            }
        }
        injectContexts();
    }

    public void setResourceClasses(Class... classes) {
        setResourceClasses(Arrays.asList(classes));
    }
    
    public void setResourceClassesFromBeans(List<Object> beans) {
        for (Object bean : beans) {
            
            Class<?> realClass = ClassHelper.getRealClass(bean);
            
            ClassResourceInfo classResourceInfo = 
                ResourceUtils.createClassResourceInfo(bean.getClass(), realClass, true, enableStatic);
            if (classResourceInfo != null) {
                classResourceInfos.add(classResourceInfo);
                
                classResourceInfo.setResourceProvider(
                                   new SingletonResourceProvider(bean));
            }
        }
    }
    
    private void injectContexts() {
        for (ClassResourceInfo cri : classResourceInfos) {
            if (cri.isSingleton()) {
                InjectionUtils.injectContextProxies(cri, 
                                                    cri.getResourceProvider().getInstance());
            }
        }
    }
    
    public void setResourceProvider(Class c, ResourceProvider rp) {
        resourceProviders.put(c, rp);
    }
    
    protected void initializeServiceModel() {
        
        updateClassResourceProviders();
        
        JAXRSServiceImpl service = new JAXRSServiceImpl(classResourceInfos);

        setService(service);

        if (properties != null) {
            service.putAll(properties);
        }
    }

    private void updateClassResourceProviders() {
        for (ClassResourceInfo cri : classResourceInfos) {
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
    
    
    
    
    protected Invoker createInvoker() {
        return new JAXRSInvoker();
    }

}
