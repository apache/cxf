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

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

/**
 * Apache CXF portable CDI extension to support initialization of JAX-RS resources.  
 */
public class JAXRSCdiResourceExtension implements Extension {    
    private Bean< ? > busBean;
    private Bus bus;
    
    private final List< Bean< ? > > applicationBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? > > serviceBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? > > providerBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? extends Feature > > featureBeans = new ArrayList< Bean< ? extends Feature > >();
        
    @SuppressWarnings("unchecked")
    public <T> void collect(@Observes final ProcessBean< T > event) {
        if (event.getAnnotated().isAnnotationPresent(ApplicationPath.class)) {
            applicationBeans.add(event.getBean());
        } else if (event.getAnnotated().isAnnotationPresent(Path.class)) {
            serviceBeans.add(event.getBean());
        } else if (event.getAnnotated().isAnnotationPresent(Provider.class)) {
            providerBeans.add(event.getBean());
        } else if (event.getBean().getTypes().contains(Feature.class)) {
            featureBeans.add((Bean< ? extends Feature >)event.getBean());
        } else if (CdiBusBean.CXF.equals(event.getBean().getName()) 
                && Bus.class.isAssignableFrom(event.getBean().getBeanClass())) {
            busBean = event.getBean();
        }
    }
    
    public void load(@Observes final AfterDeploymentValidation event, final BeanManager beanManager) {
        bus = (Bus)beanManager.getReference(
            busBean, 
            busBean.getBeanClass(), 
            beanManager.createCreationalContext(busBean)
        );
        
        for (final Bean< ? > application: applicationBeans) {
            final Application instance = (Application)beanManager.getReference(
                application, 
                application.getBeanClass(), 
                beanManager.createCreationalContext(application) 
            );
            
            // If there is an application without any singletons and classes defined, we will
            // create a server factory bean with all services and providers discovered. 
            if (instance.getSingletons().isEmpty() && instance.getClasses().isEmpty()) {
                final JAXRSServerFactoryBean factory = createFactoryInstance(instance,
                    loadServices(beanManager), loadProviders(beanManager), loadFeatures(beanManager));
                factory.init();   
            } else {
                // If there is an application with any singletons or classes defined, we will
                // create a server factory bean with only application singletons and classes.
                final JAXRSServerFactoryBean factory = createFactoryInstance(instance);
                factory.init();  
            }
        }
    }

    public void injectBus(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
        if (busBean == null) {
            final AnnotatedType< ExtensionManagerBus > busAnnotatedType = 
                beanManager.createAnnotatedType(ExtensionManagerBus.class);
               
            final InjectionTarget<ExtensionManagerBus> busInjectionTarget = 
                beanManager.createInjectionTarget(busAnnotatedType);
               
            busBean = new CdiBusBean(busInjectionTarget);
            event.addBean(busBean);
        } 
    }
    
    /**
     * Create the JAXRSServerFactoryBean from the application and all discovered service and provider instances.
     * @param application application instance
     * @param services all discovered services
     * @param providers all discovered providers
     * @return JAXRSServerFactoryBean instance
     */
    private JAXRSServerFactoryBean createFactoryInstance(final Application application, final List< ? > services,
            final List< ? > providers, final List< ? extends Feature > features) {
        
        final JAXRSServerFactoryBean instance = ResourceUtils.createApplication(application, false, false);          
        instance.setServiceBeans(new ArrayList< Object >(services));
        instance.setProviders(providers);
        instance.setProviders(loadExternalProviders());
        instance.setFeatures(features);
        instance.setBus(bus);                  
        
        return instance; 
    }
    
    /**
     * Create the JAXRSServerFactoryBean from the objects declared by application itself.
     * @param application application instance
     * @return JAXRSServerFactoryBean instance
     */
    private JAXRSServerFactoryBean createFactoryInstance(final Application application) {
        
        final JAXRSServerFactoryBean instance = ResourceUtils.createApplication(application, false, false);
        instance.setBus(bus);

        return instance; 
    }
    
    /**
     * Load external providers from service loader
     * @return loaded external providers
     */
    @SuppressWarnings("rawtypes")
    private List< Object > loadExternalProviders() {
        final List< Object > providers = new ArrayList< Object >();
        
        final ServiceLoader< MessageBodyWriter > writers = ServiceLoader.load(MessageBodyWriter.class);
        for (final MessageBodyWriter< ? > writer: writers) {
            providers.add(writer);
        }
        
        final ServiceLoader< MessageBodyReader > readers = ServiceLoader.load(MessageBodyReader.class);
        for (final MessageBodyReader< ? > reader: readers) {
            providers.add(reader);
        }
        
        return providers;
    }
    
    /**
     * Gets the references for all discovered JAX-RS resources 
     * @param beanManager bean manager instance
     * @return the references for all discovered JAX-RS resources 
     */
    private List< Object > loadProviders(final BeanManager beanManager) {        
        final List< Object > providers = new ArrayList< Object >();
       
        for (final Bean< ? > bean: providerBeans) {
            providers.add(
                beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean)
                ) 
            );    
        }
        
        return providers;
    }

    /**
     * Gets the references for all discovered JAX-RS providers 
     * @param beanManager bean manager instance
     * @return the references for all discovered JAX-RS providers 
     */
    private List< Object > loadServices(final BeanManager beanManager) {
        final List< Object > services = new ArrayList< Object >();
        
        for (final Bean< ? > bean: serviceBeans) {
            services.add(
                beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean) 
                )
            );    
        }
        
        return services;
    }
    
    /**
     * Gets the references for all discovered CXF-specific features
     * @param beanManager bean manager instance
     * @return the references for all discovered CXF-specific features
     */
    private List< Feature > loadFeatures(final BeanManager beanManager) {
        final List< Feature > services = new ArrayList<>();
        
        for (final Bean< ? extends Feature > bean: featureBeans) {
            services.add(
                (Feature)beanManager.getReference(
                    bean, 
                    bean.getBeanClass(), 
                    beanManager.createCreationalContext(bean) 
                )
            );    
        }
        
        return services;
    }
}
