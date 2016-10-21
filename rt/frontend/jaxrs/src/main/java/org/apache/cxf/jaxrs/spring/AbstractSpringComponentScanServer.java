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
package org.apache.cxf.jaxrs.spring;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, 
                                           value = {Path.class, 
                                                    Provider.class,
                                                    org.apache.cxf.annotations.Provider.class})
)
public abstract class AbstractSpringComponentScanServer extends AbstractSpringConfigurationFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSpringComponentScanServer.class);
    @Value("${cxf.jaxrs.classes-scan-packages:}")
    private String classesScanPackages;
    
    private List<ResourceProvider> resourceProviders = new LinkedList<ResourceProvider>();
    private List<Object> jaxrsProviders = new LinkedList<Object>();
    private List<Feature> cxfFeatures = new LinkedList<Feature>();
    private List<Interceptor<? extends Message>> cxfInInterceptors = new LinkedList<Interceptor<?>>();
    private List<Interceptor<? extends Message>> cxfOutInterceptors = new LinkedList<Interceptor<?>>();
    private Class<? extends Annotation> serviceAnnotation;
    
    protected AbstractSpringComponentScanServer() {
        
    }
    protected AbstractSpringComponentScanServer(Class<? extends Annotation> serviceAnnotation) {
        this.serviceAnnotation = serviceAnnotation;
    }
    protected void setJaxrsResources(JAXRSServerFactoryBean factory) {
        boolean checkJaxrsRoots = checkJaxrsRoots();
        boolean checkJaxrsProviders = checkJaxrsProviders();
        boolean checkCxfProviders = checkCxfProviders();
        
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (checkJaxrsRoots && isAnnotationAvailable(beanName, Path.class)
                && matchesServiceAnnotation(beanName)) {
                SpringResourceFactory resourceFactory = new SpringResourceFactory(beanName);
                resourceFactory.setApplicationContext(applicationContext);
                resourceProviders.add(resourceFactory);
            } else if (checkJaxrsProviders && isAnnotationAvailable(beanName, Provider.class)
                && matchesServiceAnnotation(beanName)) {
                jaxrsProviders.add(getProviderBean(beanName));
            } else if (checkCxfProviders && isAnnotationAvailable(beanName, 
                org.apache.cxf.annotations.Provider.class) && matchesServiceAnnotation(beanName)) {
                addCxfProvider(getProviderBean(beanName));
            }
        }

        if (!StringUtils.isEmpty(classesScanPackages)) {
            try {
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                    ClasspathScanner.findClasses(classesScanPackages, Provider.class, 
                                                 org.apache.cxf.annotations.Provider.class);
                                          
                jaxrsProviders.addAll(JAXRSServerFactoryBeanDefinitionParser
                    .createBeansFromDiscoveredClasses(super.applicationContext, classes.get(Provider.class), null));
                warnIfDuplicatesAvailable(jaxrsProviders);
                List<Object> cxfProviders = JAXRSServerFactoryBeanDefinitionParser
                    .createBeansFromDiscoveredClasses(super.applicationContext, 
                                                      classes.get(org.apache.cxf.annotations.Provider.class), 
                                                      null);
                for (Object cxfProvider : cxfProviders) {
                    addCxfProvider(cxfProvider);
                }
                warnIfDuplicatesAvailable(cxfFeatures);
            } catch (Exception ex) {
                throw new ServiceConstructionException(ex);
            }
        }
        
        factory.setResourceProviders(getResourceProviders());
        factory.setProviders(getJaxrsProviders());
        factory.setFeatures(getFeatures());
        factory.setInInterceptors(getInInterceptors());
        factory.setOutInterceptors(getOutInterceptors());
        
    }
    
    private static void warnIfDuplicatesAvailable(List<? extends Object> providers) {
        Set<String> classNames = new HashSet<String>();
        for (Object o : providers) {
            if (!classNames.add(o.getClass().getName())) {
                LOG.warning("Duplicate Provider " + o.getClass().getName() + " has been detected");
            }
        }
        
    }
    private Object getProviderBean(String beanName) {
        return applicationContext.getBean(beanName);
    }
    protected void addCxfProvider(Object bean) {
        org.apache.cxf.annotations.Provider ann = 
            bean.getClass().getAnnotation(org.apache.cxf.annotations.Provider.class);
        if (ann.scope() == Scope.Client) {
            return;
        }
        if (ann.value() == org.apache.cxf.annotations.Provider.Type.Feature) {
            cxfFeatures.add((Feature)bean);
        } else if (ann.value() == org.apache.cxf.annotations.Provider.Type.InInterceptor) {
            cxfInInterceptors.add((Interceptor<?>)bean);
        } else if (ann.value() == org.apache.cxf.annotations.Provider.Type.OutInterceptor) {
            cxfOutInterceptors.add((Interceptor<?>)bean);
        }
        
    }
    protected boolean matchesServiceAnnotation(String beanName) {
        return serviceAnnotation == null || isAnnotationAvailable(beanName, serviceAnnotation);
    }
    protected <A extends Annotation> boolean isAnnotationAvailable(String beanName, Class<A> annClass) {
        return applicationContext.findAnnotationOnBean(beanName, annClass) != null;
    }
    
    protected boolean checkCxfProviders() {
        return true;    
    }
    
    protected boolean checkJaxrsProviders() {
        return true;    
    }
    
    protected boolean checkJaxrsRoots() {
        return true;    
    }

    protected List<ResourceProvider> getResourceProviders() {
        return resourceProviders;
    }
    
    protected List<Object> getJaxrsProviders() {
        return jaxrsProviders;
    }
    @Override
    public List<Feature> getFeatures() {
        return cxfFeatures;
    }
    @Override
    public List<Interceptor<? extends Message>> getInInterceptors() {
        return cxfInInterceptors;
    }
    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return cxfOutInterceptors;
    }
    
}
