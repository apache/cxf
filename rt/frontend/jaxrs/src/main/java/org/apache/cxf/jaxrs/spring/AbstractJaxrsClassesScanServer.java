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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
public abstract class AbstractJaxrsClassesScanServer extends AbstractSpringConfigurationFactory {
    @Value("${cxf.jaxrs.classes-scan-packages}")
    private String basePackages;
    
    protected AbstractJaxrsClassesScanServer() {
        
    }
    protected void setJaxrsResources(JAXRSServerFactoryBean factory) {
        try {
            final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                ClasspathScanner.findClasses(basePackages, Provider.class, Path.class);
                                      
            List<Object> jaxrsServices = createBeansFromDiscoveredClasses(classes.get(Path.class));
            List<Object> jaxrsProviders = createBeansFromDiscoveredClasses(classes.get(Provider.class));
            
            factory.setServiceBeans(jaxrsServices);
            factory.setProviders(jaxrsProviders);
        } catch (Exception ex) {
            throw new ServiceConstructionException(ex);
        }
        
    }
        
    protected List<Object> createBeansFromDiscoveredClasses(Collection<Class<?>> classes) {
        AutowireCapableBeanFactory beanFactory = super.applicationContext.getAutowireCapableBeanFactory();
        final List< Object > providers = new ArrayList< Object >();
        for (final Class< ? > clazz: classes) {
            Object bean = null;
            try {
                bean = beanFactory.createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            } catch (Exception ex) {
                bean = beanFactory.createBean(clazz);
            }
            providers.add(bean);
        }
        return providers;
    }
}
