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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.ws.rs.Path;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.invoker.Invoker;


/**
 * Build a JAX-RS service model from resource classes.
 */
public class JAXRSServiceFactoryBean extends AbstractServiceFactoryBean {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSServiceFactoryBean.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSServiceFactoryBean.class);

    protected List<ClassResourceInfo> classResourceInfos = 
        new ArrayList<ClassResourceInfo>();
    protected Map<Class, ResourceProvider> resourceProviders = new HashMap<Class, ResourceProvider>();
    
    private Invoker invoker;
    private Executor executor;
    private Map<String, Object> properties;

    public JAXRSServiceFactoryBean() {
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
                createClassResourceInfo(resourceClass, resourceClass, true);
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
            
            ClassResourceInfo classResourceInfo = 
                createClassResourceInfo(bean.getClass(), 
                                        ClassHelper.getRealClass(bean),
                                            true);
            classResourceInfos.add(classResourceInfo);
            classResourceInfo.setResourceProvider(
                               new SingletonResourceProvider(bean));
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
    
    protected ClassResourceInfo createClassResourceInfo(
        final Class<?> rClass, final Class<?> sClass, boolean root) {
        ClassResourceInfo cri  = new ClassResourceInfo(rClass, sClass, root);

        if (root) {
            URITemplate t = URITemplate.createTemplate(cri, cri.getPath());
            cri.setURITemplate(t);
        }
        
        MethodDispatcher md = createOperation(cri);
        cri.setMethodDispatcher(md);
        return checkMethodDispatcher(cri) ? cri : null;
    }

    protected MethodDispatcher createOperation(ClassResourceInfo cri) {
        MethodDispatcher md = new MethodDispatcher();
        for (Method m : cri.getServiceClass().getMethods()) {
            
            Method annotatedMethod = AnnotationUtils.getAnnotatedMethod(m);
            
            String httpMethod = AnnotationUtils.getHttpMethodValue(annotatedMethod);
            Path path = (Path)AnnotationUtils.getMethodAnnotation(annotatedMethod, Path.class);
            
            if (httpMethod != null || path != null) {
                md.bind(createOperationInfo(m, annotatedMethod, cri, path, httpMethod), m);
                if (httpMethod == null) {
                    // subresource locator
                    Class subResourceClass = m.getReturnType();
                    ClassResourceInfo subCri = createClassResourceInfo(
                         subResourceClass, subResourceClass, false);
                    if (checkMethodDispatcher(subCri)) {
                        cri.addSubClassResourceInfo(subCri);
                    }
                }
            }
        }
        
        return md;
    }
    
    private OperationResourceInfo createOperationInfo(Method m, Method annotatedMethod, 
                                                      ClassResourceInfo cri, Path path, String httpMethod) {
        OperationResourceInfo ori = new OperationResourceInfo(m, cri);
        URITemplate t = 
            URITemplate.createTemplate(cri, path);
        ori.setURITemplate(t);
        ori.setHttpMethod(httpMethod);
        ori.setAnnotatedMethod(annotatedMethod);
        return ori;
    }
    
       
    private boolean checkMethodDispatcher(ClassResourceInfo cr) {
        if (cr.getMethodDispatcher().getOperationResourceInfos().isEmpty()) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("NO_RESOURCE_OP_EXC", 
                                                               BUNDLE, 
                                                               cr.getClass().getName()).toString());
            return false;
        }
        return true;
    }
    
    
    protected Invoker createInvoker() {
        return new JAXRSInvoker();
    }

}
