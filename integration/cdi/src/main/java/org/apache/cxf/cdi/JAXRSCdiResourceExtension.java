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
package org.apache.cxf.cdi;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

/**
 * Apache CXF portable CDI extension to support initialization of JAX-RS / JAX-WS resources.  
 */
public class JAXRSCdiResourceExtension implements Extension {
    private final List< Bean< ? > > applicationBeans = new ArrayList<Bean<?>>();
    private final List< Bean< ? > > serviceBeans = new ArrayList<Bean<?>>();
    private final List< Bean< ? > > providerBeans = new ArrayList<Bean<?>>();
    
    private final List< Application > applications = new ArrayList< Application >();
    private final List< Object > services = new ArrayList< Object >();
    private final List< Object > providers = new ArrayList< Object >();
    
    public <T> void collect(@Observes final ProcessBean< T > event) {
        if (event.getAnnotated().isAnnotationPresent(ApplicationPath.class)) {
            applicationBeans.add(event.getBean());
        } else if (event.getAnnotated().isAnnotationPresent(Path.class)) {
            serviceBeans.add(event.getBean());
        } else if (event.getAnnotated().isAnnotationPresent(Provider.class)) {
            providerBeans.add(event.getBean());
        }
    }
    
    public void load(@Observes final AfterDeploymentValidation event, final BeanManager beanManager) {
        for (final Bean< ? > bean: applicationBeans) {
            applications.add(
                (Application)beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean) 
                ) 
            );    
        }
        
        for (final Bean< ? > bean: serviceBeans) {
            services.add(
                beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean) 
                )
            );    
        }
        
        for (final Bean< ? > bean: providerBeans) {
            providers.add(
                beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean)
                ) 
            );    
        }
    }
    
    public List<Application> getApplications() {
        return applications;
    }
    
    public List< Object > getServices() {
        return services;
    }
    
    public List< Object > getProviders() {
        return providers;
    }
}
