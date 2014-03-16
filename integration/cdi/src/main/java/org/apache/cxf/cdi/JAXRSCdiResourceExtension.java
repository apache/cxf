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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessBean;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

/**
 * Apache CXF portable CDI extension to support initialization of JAX-RS / JAX-WS resources.  
 */

public class JAXRSCdiResourceExtension implements Extension {
    private final List< Bean< ? > > applicationBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? > > serviceBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? > > providerBeans = new ArrayList< Bean< ? > >();
    private final Map< Bean< ? >, Bean< JAXRSServerFactoryBean > > factoryBeans = 
        new HashMap< Bean< ? >, Bean< JAXRSServerFactoryBean > >();
    
    private final List< JAXRSServerFactoryBean > factories = new ArrayList< JAXRSServerFactoryBean >();
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
        
        for (final Map.Entry< Bean< ? >, Bean< JAXRSServerFactoryBean > > entry: factoryBeans.entrySet()) {
            final Application instance = (Application)beanManager.getReference(
                entry.getKey(), 
                entry.getKey().getBeanClass(), 
                beanManager.createCreationalContext(entry.getKey()) 
            );
            
            // Create the JAXRSServerFactoryBean for each application we have discovered
            factories.add(createFactoryInstance(instance, entry.getValue(), beanManager));
        }
    }
    
    public void injectFactories(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
        final AnnotatedType< JAXRSServerFactoryBean > factoryAnnotatedType = 
             beanManager.createAnnotatedType(JAXRSServerFactoryBean.class);
        
        final InjectionTarget<JAXRSServerFactoryBean> injectionTarget = 
             beanManager.createInjectionTarget(factoryAnnotatedType);
        
        for (final Bean< ? > applicationBean: applicationBeans) {
            final Bean< JAXRSServerFactoryBean > factoryBean =
                new JAXRSCdiServerFactoryBean(applicationBean, injectionTarget);   
            
            event.addBean(factoryBean);
            factoryBeans.put(applicationBean, factoryBean);
        }
    }
    
    public List< JAXRSServerFactoryBean > getFactories() {
        return Collections.< JAXRSServerFactoryBean > unmodifiableList(factories);
    }
    
    @SuppressWarnings("rawtypes")
    private JAXRSServerFactoryBean createFactoryInstance(final Application application, 
            final Bean< ? > factoryBean, final BeanManager beanManager) {
        
        final JAXRSServerFactoryBean instance = (JAXRSServerFactoryBean)beanManager.getReference(
            factoryBean, 
            factoryBean.getBeanClass(), 
            beanManager.createCreationalContext(factoryBean)
        );
                        
        ResourceUtils.initializeApplication(instance, application, false, false);          
        instance.setServiceBeans(new ArrayList< Object >(services));
        instance.setProviders(providers);
              
        final ServiceLoader< MessageBodyWriter > writers = ServiceLoader.load(MessageBodyWriter.class);
        for (final MessageBodyWriter< ? > writer: writers) {
            instance.setProvider(writer);
        }
        
        final ServiceLoader< MessageBodyReader > readers = ServiceLoader.load(MessageBodyReader.class);
        for (final MessageBodyReader< ? > reader: readers) {
            instance.setProvider(reader);
        }
        
        return instance; 
    }
}
