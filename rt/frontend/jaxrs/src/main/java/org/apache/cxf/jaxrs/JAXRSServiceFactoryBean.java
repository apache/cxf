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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.ws.rs.Path;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
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
    
    private Invoker invoker;
    private Executor executor;
    private Map<String, Object> properties;
    private boolean enableStatic;
    
    public JAXRSServiceFactoryBean() {
    }

    
    public void setEnableStaticResolution(boolean staticResolution) {
        this.enableStatic = staticResolution;
    }
    
    public boolean resourcesAvailable() {
        return !classResourceInfos.isEmpty();
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
    
    public void setResourceClass(Class cls) {
        classResourceInfos.clear();
        boolean isRoot = AnnotationUtils.getClassAnnotation(cls, Path.class) != null;
        createResourceInfo(cls, isRoot);
    }
    
    public void setResourceClasses(List<Class> classes) {
        for (Class resourceClass : classes) {
            createResourceInfo(resourceClass, true);
        }
    }
    
    protected ClassResourceInfo createResourceInfo(Class cls, boolean isRoot) {
        ClassResourceInfo classResourceInfo = 
            ResourceUtils.createClassResourceInfo(cls, cls, isRoot, enableStatic);
        if (classResourceInfo != null) {
            classResourceInfos.add(classResourceInfo);
        }
        return classResourceInfo;
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
    
    protected void initializeServiceModel() {
        
        JAXRSServiceImpl service = new JAXRSServiceImpl(classResourceInfos);

        setService(service);

        if (properties != null) {
            service.putAll(properties);
        }
    }

    protected Invoker createInvoker() {
        return new JAXRSInvoker();
    }

    public void setService(Service service) {
        super.setService(service);
    }
}
