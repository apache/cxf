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
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                                           value = {ApplicationPath.class,
                                                    Path.class,
                                                    Provider.class,
                                                    org.apache.cxf.annotations.Provider.class})
)
public abstract class AbstractSpringComponentScanServer extends AbstractSpringConfigurationFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSpringComponentScanServer.class);
    @Value("${cxf.jaxrs.classes-scan-packages:}")
    private String classesScanPackages;
    @Value("${cxf.jaxrs.component-scan-packages:}")
    private String componentScanPackages;
    @Value("${cxf.jaxrs.component-scan-beans:}")
    private String componentScanBeans;

    private List<ResourceProvider> resourceProviders = new LinkedList<>();
    private List<Object> jaxrsProviders = new LinkedList<>();
    private List<Feature> cxfFeatures = new LinkedList<>();
    private Class<? extends Annotation> serviceAnnotation;

    protected AbstractSpringComponentScanServer() {

    }
    protected AbstractSpringComponentScanServer(Class<? extends Annotation> serviceAnnotation) {
        this.serviceAnnotation = serviceAnnotation;
    }
    @Override
    protected Server createJaxRsServer() {
            
        JAXRSServerFactoryBean factoryBean = null;
        
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(ApplicationPath.class);

        if (beanNames.length > 0) {
            Set<String> componentScanPackagesSet = parseSetProperty(componentScanPackages);
            Set<String> componentScanBeansSet = parseSetProperty(componentScanBeans);
            
            for (String beanName : beanNames) {
                if (isComponentMatched(beanName, componentScanPackagesSet, componentScanBeansSet)) {
                    Application app = applicationContext.getBean(beanName, Application.class);
                    factoryBean = createFactoryBeanFromApplication(app);
                    for (String cxfBeanName : applicationContext.getBeanNamesForAnnotation(
                                                  org.apache.cxf.annotations.Provider.class)) {
                        if (isComponentMatched(cxfBeanName, componentScanPackagesSet, componentScanBeansSet)) {
                            addCxfProvider(getProviderBean(cxfBeanName));
                        }
                    }
                    break;
                }
            }
        }
            
        if (!StringUtils.isEmpty(classesScanPackages)) {
            try {
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > appClasses =
                    ClasspathScanner.findClasses(classesScanPackages, ApplicationPath.class);
                
                List<Application> apps = CastUtils.cast(JAXRSServerFactoryBeanDefinitionParser
                    .createBeansFromDiscoveredClasses(super.applicationContext, 
                                                      appClasses.get(ApplicationPath.class), null));
                if (!apps.isEmpty()) {
                    factoryBean = createFactoryBeanFromApplication(apps.get(0));
                    final Map< Class< ? extends Annotation >, Collection< Class< ? > > > cxfClasses =
                        ClasspathScanner.findClasses(classesScanPackages, org.apache.cxf.annotations.Provider.class);
                    addCxfProvidersFromClasses(cxfClasses.get(org.apache.cxf.annotations.Provider.class));
                }
                
            } catch (Exception ex) {
                throw new ServiceConstructionException(ex);
            }
        }
            
        if (factoryBean != null) {
            setFactoryCxfProviders(factoryBean);
            return factoryBean.create();
        }
        
        return super.createJaxRsServer();
    }
    
    protected void setJaxrsResources(JAXRSServerFactoryBean factory) {
        Set<String> componentScanPackagesSet = parseSetProperty(componentScanPackages);
        Set<String> componentScanBeansSet = parseSetProperty(componentScanBeans);
            
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (isValidComponent(beanName, Path.class, componentScanPackagesSet, componentScanBeansSet)) {
                SpringResourceFactory resourceFactory = new SpringResourceFactory(beanName);
                resourceFactory.setApplicationContext(applicationContext);
                resourceProviders.add(resourceFactory);
            } else if (isValidComponent(beanName, Provider.class, componentScanPackagesSet, componentScanBeansSet)) {
                jaxrsProviders.add(getProviderBean(beanName));
            } else if (isValidComponent(beanName, org.apache.cxf.annotations.Provider.class, 
                                    componentScanPackagesSet, componentScanBeansSet)) {
                addCxfProvider(getProviderBean(beanName));
            }
        }

        if (!StringUtils.isEmpty(classesScanPackages)) {
            try {
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                    ClasspathScanner.findClasses(classesScanPackages, Provider.class,
                                                 org.apache.cxf.annotations.Provider.class);

                jaxrsProviders.addAll(JAXRSServerFactoryBeanDefinitionParser
                    .createBeansFromDiscoveredClasses(applicationContext, classes.get(Provider.class), null));
                warnIfDuplicatesAvailable(jaxrsProviders);
                addCxfProvidersFromClasses(classes.get(org.apache.cxf.annotations.Provider.class));
            } catch (Exception ex) {
                throw new ServiceConstructionException(ex);
            }
        }

        factory.setResourceProviders(getResourceProviders());
        factory.setProviders(getJaxrsProviders());
        setFactoryCxfProviders(factory);

    }

    protected void setFactoryCxfProviders(JAXRSServerFactoryBean factory) {
        factory.setFeatures(getFeatures());
        factory.setInInterceptors(getInInterceptors());
        factory.setOutInterceptors(getOutInterceptors());
        factory.setOutFaultInterceptors(getOutFaultInterceptors());
    }
    
    protected void addCxfProvidersFromClasses(Collection<Class<?>> classes) {
        List<Object> cxfProviders = JAXRSServerFactoryBeanDefinitionParser
            .createBeansFromDiscoveredClasses(applicationContext, classes, null);
        for (Object cxfProvider : cxfProviders) {
            addCxfProvider(cxfProvider);
        }
        warnIfDuplicatesAvailable(cxfFeatures);    
    }
    protected boolean isValidComponent(String beanName, 
                                      Class<? extends Annotation> ann,
                                      Set<String> componentScanPackagesSet,
                                      Set<String> componentScanBeansSet) {
        return isAnnotationAvailable(beanName, ann)
            && nonProxyClass(beanName)
            && isComponentMatched(beanName, componentScanPackagesSet, componentScanBeansSet);
    }
    protected boolean isComponentMatched(String beanName, 
                                         Set<String> componentScanPackagesSet,
                                         Set<String> componentScanBeansSet) {
        return matchesServiceAnnotation(beanName)
            && matchesComponentPackage(beanName, componentScanPackagesSet)
            && matchesComponentName(beanName, componentScanBeansSet);
    }
    protected boolean nonProxyClass(String beanName) {
        // JAX-RS runtime needs to be able to access the real component class to introspect it for
        // JAX-RS annotations; the following check ensures that the valid proxified components
        // are accepted while the client proxies are ignored.
        Class<?> type = ClassHelper.getRealClassFromClass(applicationContext.getType(beanName));
        if (Proxy.isProxyClass(type) && applicationContext.isSingleton(beanName)) {
            type = ClassHelper.getRealClass(applicationContext.getBean(beanName));
        }
        if (Proxy.isProxyClass(type)) {
            LOG.fine("Can not determine the real class of the component '" + beanName + "'");
            return false;
        }
        return true;
    }
    protected boolean matchesComponentName(String beanName, Set<String> componentScanBeansSet) {
        return componentScanBeansSet == null || componentScanBeansSet.contains(beanName);
    }
    protected boolean matchesComponentPackage(String beanName, Set<String> componentScanPackagesSet) {
        return componentScanPackagesSet == null 
            || !applicationContext.isSingleton(beanName)
            || componentScanPackagesSet.contains(
                PackageUtils.getPackageName(applicationContext.getBean(beanName).getClass()));
        
    }
    private static void warnIfDuplicatesAvailable(List<? extends Object> providers) {
        Set<String> classNames = new HashSet<>();
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
            super.getInInterceptors().add((Interceptor<?>)bean);
        } else if (ann.value() == org.apache.cxf.annotations.Provider.Type.OutInterceptor) {
            super.getOutInterceptors().add((Interceptor<?>)bean);
        } else if (ann.value() == org.apache.cxf.annotations.Provider.Type.OutFaultInterceptor) {
            super.getOutFaultInterceptors().add((Interceptor<?>)bean);
        }
    }
    
    protected boolean matchesServiceAnnotation(String beanName) {
        return serviceAnnotation == null || isAnnotationAvailable(beanName, serviceAnnotation);
    }
    protected <A extends Annotation> boolean isAnnotationAvailable(String beanName, Class<A> annClass) {
        return applicationContext.findAnnotationOnBean(beanName, annClass) != null;
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
    
    protected JAXRSServerFactoryBean createFactoryBeanFromApplication(Application app) {
        return ResourceUtils.createApplication(app, false, true, false, getBus());
    }
    protected static Set<String> parseSetProperty(String componentScanProp) {
        return !StringUtils.isEmpty(componentScanProp) 
            ? ClasspathScanner.parsePackages(componentScanProp) : null;
    }
}
