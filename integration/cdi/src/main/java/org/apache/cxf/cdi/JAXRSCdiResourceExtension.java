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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
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
import org.apache.cxf.cdi.extension.JAXRSServerFactoryCustomizationExtension;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

/**
 * Apache CXF portable CDI extension to support initialization of JAX-RS resources.  
 */
public class JAXRSCdiResourceExtension implements Extension {
    private boolean hasBus;
    private Bus bus;

    private final Set< Bean< ? > > applicationBeans = new LinkedHashSet< Bean< ? > >();
    private final List< Bean< ? > > serviceBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? > > providerBeans = new ArrayList< Bean< ? > >();
    private final List< Bean< ? extends Feature > > featureBeans = new ArrayList< Bean< ? extends Feature > >();
    private final List< CreationalContext< ? > > disposableCreationalContexts = 
        new ArrayList< CreationalContext< ? > >();

    /**
     * Holder of the classified resource classes, converted to appropriate instance
     * representations.
     */
    private static class ClassifiedClasses {
        private List< Object > providers = new ArrayList<>();
        private List< Feature > features = new ArrayList<>();
        private List< CdiResourceProvider > resourceProviders = new ArrayList<>();
        
        public void addProviders(final Collection< Object > others) {
            this.providers.addAll(others);
        }
        
        public void addFeatures(final Collection< Feature > others) {
            this.features.addAll(others);
        }
        
        public void addResourceProvider(final CdiResourceProvider other) {
            this.resourceProviders.add(other);
        }
        
        public List< Object > getProviders() {
            return providers;
        }
        
        public List< Feature > getFeatures() {
            return features;
        }
        
        public List<CdiResourceProvider> getResourceProviders() {
            return resourceProviders;
        }
    }
    
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
            hasBus = true;
        }
    }

    public void load(@Observes final AfterDeploymentValidation event, final BeanManager beanManager) {
        // no need of creational context, it only works for app scoped instances anyway
        final Bean<?> busBean = beanManager.resolve(beanManager.getBeans(CdiBusBean.CXF));
        
        bus = (Bus)beanManager.getReference(
                busBean, 
                Bus.class,
                beanManager.createCreationalContext(busBean));

        for (final Bean< ? > application: applicationBeans) {
            final Application instance = (Application)beanManager.getReference(
                application,
                application.getBeanClass(),
                createCreationalContext(beanManager, application)
            );

            // If there is an application without any singletons and classes defined, we will
            // create a server factory bean with all services and providers discovered.
            if (instance.getSingletons().isEmpty() && instance.getClasses().isEmpty()) {
                final JAXRSServerFactoryBean factory = createFactoryInstance(instance,
                    loadServices(beanManager, Collections.<Class<?>>emptySet()),
                    loadProviders(beanManager, Collections.<Class<?>>emptySet()),
                    loadFeatures(beanManager, Collections.<Class<?>>emptySet()));
                customize(beanManager, factory);
                factory.init();
            } else {
                // If there is an application with any singletons or classes defined, we will
                // create a server factory bean with only application singletons and classes.
                final JAXRSServerFactoryBean factory = createFactoryInstance(instance, beanManager);
                customize(beanManager, factory);
                factory.init();
            }
        }
    }

    public void injectBus(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
        if (!hasBus) {
            final AnnotatedType< ExtensionManagerBus > busAnnotatedType =
                beanManager.createAnnotatedType(ExtensionManagerBus.class);

            final InjectionTarget<ExtensionManagerBus> busInjectionTarget =
                beanManager.createInjectionTarget(busAnnotatedType);
            event.addBean(new CdiBusBean(busInjectionTarget));
        }
        if (applicationBeans.isEmpty() && !serviceBeans.isEmpty()) {
            final DefaultApplicationBean applicationBean = new DefaultApplicationBean();
            applicationBeans.add(applicationBean);
            event.addBean(applicationBean);
        }
    }
    
    /**
     * Releases created CreationalContext instances 
     */
    public void release(@Observes final BeforeShutdown event) {
        for (final CreationalContext<?> disposableCreationalContext: disposableCreationalContexts) {
            disposableCreationalContext.release();
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

        final JAXRSServerFactoryBean instance = ResourceUtils.createApplication(application, false, false, bus);
        instance.setServiceBeans(new ArrayList<>(services));
        instance.setProviders(providers);
        instance.setProviders(loadExternalProviders());
        instance.setFeatures(features);

        return instance;
    }

    /**
     * Create the JAXRSServerFactoryBean from the objects declared by application itself.
     * @param application application instance
     * @return JAXRSServerFactoryBean instance
     */
    private JAXRSServerFactoryBean createFactoryInstance(final Application application, final BeanManager beanManager) {

        final JAXRSServerFactoryBean instance = ResourceUtils.createApplication(application, false, false, bus);
        final ClassifiedClasses classified = classes2singletons(application, beanManager);
        
        instance.setProviders(classified.getProviders());
        instance.getFeatures().addAll(classified.getFeatures());
        
        for (final CdiResourceProvider resourceProvider: classified.getResourceProviders()) {
            instance.setResourceProvider(resourceProvider.getResourceClass(), resourceProvider);
        }

        return instance;
    }

    /**
     * JAX-RS application has defined singletons as being classes of any providers, resources and features.
     * In the JAXRSServerFactoryBean, those should be split around several method calls depending on instance
     * type. At the moment, only the Feature is CXF-specific and should be replaced by JAX-RS Feature implementation.
     * @param application the application instance
     * @return classified instances of classes by instance types
     */
    private ClassifiedClasses classes2singletons(final Application application, final BeanManager beanManager) {
        final ClassifiedClasses classified = new ClassifiedClasses(); 

        // now loop through the classes
        Set<Class<?>> classes = application.getClasses();
        if (!classes.isEmpty()) {
            classified.addProviders(loadProviders(beanManager, classes));
            classified.addFeatures(loadFeatures(beanManager, classes));
            
            for (final Bean< ? > bean: serviceBeans) {
                if (classes.contains(bean.getBeanClass())) {
                    classified.addResourceProvider(new CdiResourceProvider(beanManager, bean));
                }
            }
        }
        
        return classified;
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
     * @param limitedClasses not null, if empty ignored.  the set of classes to consider as providers
     * @return the references for all discovered JAX-RS resources
     */
    private List< Object > loadProviders(final BeanManager beanManager, Collection<Class<?>> limitedClasses) {
        return loadBeans(beanManager, limitedClasses, providerBeans);
    }

    /**
     * Gets the references for all discovered JAX-RS providers
     * @param beanManager bean manager instance
     * @param limitedClasses not null, if empty ignored.  the set of classes to consider as providers
     * @return the references for all discovered JAX-RS providers
     */
    private List< Object > loadServices(final BeanManager beanManager, Collection<Class<?>> limitedClasses) {
        return loadBeans(beanManager, limitedClasses, serviceBeans);
    }

    /**
     * Gets references for all beans of a given type
     * @param beanManager bean manager instance
     * @param limitedClasses not null, if empty ignored.  the set of classes to consider as providers
     * @param beans the collection of beans to go through
     * @return the references for all discovered JAX-RS providers
     */
    private List< Object > loadBeans(final BeanManager beanManager, Collection<Class<?>> limitedClasses,
                                     Collection<Bean<?>> beans) {
        final List< Object > instances = new ArrayList<>();

        for (final Bean< ? > bean: beans) {
            if (limitedClasses.isEmpty() || limitedClasses.contains(bean.getBeanClass())) {
                instances.add(
                      beanManager.getReference(
                            bean,
                            bean.getBeanClass(),
                            createCreationalContext(beanManager, bean)
                      )
                );
            }
        }

        return instances;
    }

    /**
     * Gets the references for all discovered CXF-specific features
     * @param beanManager bean manager instance
     * @return the references for all discovered CXF-specific features
     */
    private List< Feature > loadFeatures(final BeanManager beanManager, Collection<Class<?>> limitedClasses) {
        final List< Feature > features = new ArrayList<>();
        
        for (final Bean< ? extends Feature > bean: featureBeans) {
            if (limitedClasses.isEmpty() || limitedClasses.contains(bean.getBeanClass())) {
                features.add(
                        (Feature) beanManager.getReference(
                                bean,
                                bean.getBeanClass(),
                                createCreationalContext(beanManager, bean)
                        )
                );
            }
        }
        
        return features;
    }
    
    /**
     * Look and apply the available JAXRSServerFactoryBean extensions to customize its
     * creation (f.e. add features, providers, assign transport, ...)
     * @param beanManager bean manager
     * @param bean JAX-RS server factory bean about to be created
     */
    private void customize(final BeanManager beanManager, final JAXRSServerFactoryBean bean) {
        final Collection<Bean<?>> extensionBeans = beanManager.getBeans(JAXRSServerFactoryCustomizationExtension.class);
        
        for (final Bean<?> extensionBean: extensionBeans) {
            final JAXRSServerFactoryCustomizationExtension extension =
                (JAXRSServerFactoryCustomizationExtension)beanManager.getReference(
                    extensionBean, 
                    extensionBean.getBeanClass(), 
                    beanManager.createCreationalContext(extensionBean) 
                );
            extension.customize(bean);
        }
    }
    
    /**
     * Creates and collects the CreationalContext instances for future releasing. 
     * @param beanManager bean manager instance
     * @param bean bean instance to create CreationalContext for
     * @return CreationalContext instance
     */
    private<T> CreationalContext< T > createCreationalContext(final BeanManager beanManager, Bean< T > bean) {
        final CreationalContext< T > creationalContext = beanManager.createCreationalContext(bean);
        if (!(bean instanceof DefaultApplicationBean)) {
            disposableCreationalContexts.add(creationalContext);
        }
        return creationalContext;
    }
}
