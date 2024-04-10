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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessProducerField;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.cdi.event.DisposableCreationalContext;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.ContextClassProvider;
import org.apache.cxf.jaxrs.ext.JAXRSServerFactoryCustomizationExtension;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.provider.ServerConfigurableFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSServerFactoryCustomizationUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.cxf.cdi.AbstractCXFBean.DEFAULT;

/**
 * Apache CXF portable CDI extension to support initialization of JAX-RS resources.
 */
public class JAXRSCdiResourceExtension implements Extension {
    private boolean hasBus;
    private Bus bus;

    private Set< Bean< ? > > applicationBeans = new LinkedHashSet< Bean< ? > >();
    private Set< Bean< ? > > serviceBeans = new HashSet< Bean< ? > >();
    private Set< Bean< ? > > providerBeans = new HashSet< Bean< ? > >();
    private Set< Bean< ? extends Feature > > featureBeans = new HashSet< Bean< ? extends Feature > >();
    private Set< Type > contextTypes = new LinkedHashSet<>();

    private final List< CreationalContext< ? > > disposableCreationalContexts =
        new ArrayList<>();
    private final List< Lifecycle > disposableLifecycles =
        new ArrayList<>();

    private final Collection< String > existingStandardClasses = new HashSet<>();

    /**
     * Holder of the classified resource classes, converted to appropriate instance
     * representations.
     */
    private static final class ClassifiedClasses {
        private List< Object > providers = new ArrayList<>();
        private List< Feature > features = new ArrayList<>();
        private List<ResourceProvider> resourceProviders = new ArrayList<>();

        public void addProviders(final Collection< Object > others) {
            this.providers.addAll(others);
        }

        public void addFeatures(final Collection< Feature > others) {
            this.features.addAll(others);
        }

        public void addResourceProvider(final ResourceProvider other) {
            this.resourceProviders.add(other);
        }

        public List< Object > getProviders() {
            return providers;
        }

        public List< Feature > getFeatures() {
            return features;
        }

        public List<ResourceProvider> getResourceProviders() {
            return resourceProviders;
        }
    }

    // observing JAXRSCdiResourceExtension a "container" can customize that value to prevent some instances
    // to be added with the default qualifier
    public Collection<String> getExistingStandardClasses() {
        return existingStandardClasses;
    }

    /**
     * Fires itsels, allows other extensions to modify this one.
     * Typical example can be to modify existingStandardClasses to prevent CXF
     * to own some beans it shouldn't create with default classifier.
     *
     * @param beforeBeanDiscovery the corresponding cdi event.
     * @param beanManager the cdi bean manager.
     */
    void onStartup(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        final ClassLoader loader = ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(ClassLoader::getSystemClassLoader);
        boolean webHandled = false;
        try { // OWB
            loader.loadClass("org.apache.webbeans.web.lifecycle.WebContainerLifecycle");
            webHandled = true;
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            // ok to keep them all
        }
        if (!webHandled) {
            try { // Weld
                loader.loadClass("org.jboss.weld.module.web.WeldWebModule");
                webHandled = true;
            } catch (final NoClassDefFoundError | ClassNotFoundException e) {
                // ok to keep them all
            }
        }
        if (webHandled) {
            existingStandardClasses.addAll(asList(
                "jakarta.servlet.http.HttpServletRequest",
                "jakarta.servlet.ServletContext"));
        }
        beanManager.getEvent().fire(this);
    }

    /**
     * For any {@link AnnotatedType} that includes a {@link Context} injection point, this method replaces
     * the field with the following code:
     * <pre>
     *     @Inject @ContextResolved T field;
     * </pre>
     * For any usage of T that is a valid context object in JAX-RS.
     *
     * It also has a side effect of capturing the context object type, in case no
     * {@link org.apache.cxf.jaxrs.ext.ContextClassProvider} was registered for the type.
     *
     * @param processAnnotatedType the annotated type being investigated
     * @param <X> the generic type of that processAnnotatedType
     */
    public <X> void convertContextsToCdi(@Observes @WithAnnotations({Context.class})
                                             ProcessAnnotatedType<X> processAnnotatedType) {
        AnnotatedType<X> annotatedType = processAnnotatedType.getAnnotatedType();
        DelegateContextAnnotatedType<X> type = new DelegateContextAnnotatedType<>(annotatedType);
        contextTypes.addAll(type.getContextFieldTypes());
        processAnnotatedType.setAnnotatedType(type);
    }

    @SuppressWarnings("unchecked")
    public <T> void collect(@Observes final ProcessBean< T > event, final BeanManager beanManager) {
        final Annotated annotated = event.getAnnotated();
        if (isAnnotationPresent(beanManager, annotated, ApplicationPath.class)) {
            applicationBeans.add(event.getBean());
        } else if (isAnnotationPresent(beanManager, annotated, Path.class)) {
            serviceBeans.add(event.getBean());
        } else if (isAnnotationPresent(beanManager, annotated, Provider.class)) {
            providerBeans.add(event.getBean());
        } else if (event.getBean().getTypes().contains(jakarta.ws.rs.core.Feature.class)) {
            providerBeans.add(event.getBean());
        } else if (event.getBean().getTypes().contains(Feature.class)) {
            featureBeans.add((Bean< ? extends Feature >)event.getBean());
        } else if (CdiBusBean.CXF.equals(event.getBean().getName())
                && Bus.class.isAssignableFrom(event.getBean().getBeanClass())) {
            hasBus = true;
        } else if (event.getBean().getQualifiers().contains(DEFAULT)) {
            event.getBean().getTypes().stream()
                .filter(e -> Object.class != e && InjectionUtils.STANDARD_CONTEXT_CLASSES.contains(e.getTypeName()))
                .findFirst()
                .ifPresent(type -> existingStandardClasses.add(type.getTypeName()));
        }
    }
    
    public <T, X> void collect(@Observes final ProcessProducerField< T, X > event) {
        final Type baseType = event.getAnnotatedProducerField().getBaseType();
        processProducer(event, baseType);
    }

    public <T, X> void collect(@Observes final ProcessProducerMethod< T, X > event) {
        final Type baseType = event.getAnnotatedProducerMethod().getBaseType();
        processProducer(event, baseType);
    }

    public void load(@Observes final AfterDeploymentValidation event, final BeanManager beanManager) {
        // no need of creational context, it only works for app scoped instances anyway
        final Bean<?> busBean = beanManager.resolve(beanManager.getBeans(CdiBusBean.CXF));

        bus = (Bus)beanManager.getReference(
                busBean,
                Bus.class,
                beanManager.createCreationalContext(busBean));
        
        // Adding the extension for dynamic providers registration and instantiation
        if (bus.getExtension(ServerConfigurableFactory.class) == null) {
            bus.setExtension(new CdiServerConfigurableFactory(beanManager), ServerConfigurableFactory.class);
        }

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

        cleanStartupData();
    }

    public void injectBus(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
        if (!hasBus) {
            final AnnotatedType< ExtensionManagerBus > busAnnotatedType =
                beanManager.createAnnotatedType(ExtensionManagerBus.class);

            event.addBean(new CdiBusBean(beanManager.getInjectionTargetFactory(busAnnotatedType)));
        }

        if (applicationBeans.isEmpty() && !serviceBeans.isEmpty()) {
            final DefaultApplicationBean applicationBean = new DefaultApplicationBean();
            applicationBeans.add(applicationBean);
            event.addBean(applicationBean);
        } else {
            // otherwise will be ambiguous since we scanned it with default qualifier already
            existingStandardClasses.add(Application.class.getName());
        }

        // always add the standard context classes
        InjectionUtils.STANDARD_CONTEXT_CLASSES.stream()
                .map(this::toClass)
                .filter(Objects::nonNull)
                .forEach(contextTypes::add);
        // add custom contexts
        contextTypes.addAll(getCustomContextClasses());
        // register all of the context types
        contextTypes.forEach(
            t -> event.addBean(new ContextProducerBean(t, !existingStandardClasses.contains(t.getTypeName()))));
    }

    private void cleanStartupData() { // enable gc
        Stream.of(serviceBeans, providerBeans, featureBeans, applicationBeans, contextTypes).forEach(Collection::clear);
        serviceBeans = null;
        providerBeans = null;
        featureBeans = null;
        applicationBeans = null;
        contextTypes = null;
    }

    /**
     * Registers created CreationalContext instances for disposal
     */
    public void registerCreationalContextForDisposal(@Observes final DisposableCreationalContext event) {
        synchronized (disposableCreationalContexts) {
            disposableCreationalContexts.add(event.getContext());
        }
    }
    
    /**
     * Releases created CreationalContext instances
     */
    public void release(@Observes final BeforeShutdown event) {
        synchronized (disposableCreationalContexts) {
            for (final CreationalContext<?> disposableCreationalContext: disposableCreationalContexts) {
                disposableCreationalContext.release();
            }
            disposableCreationalContexts.clear();
        }

        disposableLifecycles.forEach(Lifecycle::destroy);
        disposableLifecycles.clear();
    }

    private Class<?> toClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            return null;
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

        final JAXRSServerFactoryBean instance = 
            ResourceUtils.createApplication(application, false, false, false, bus);
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
        final JAXRSServerFactoryBean instance =
            ResourceUtils.createApplication(application, false, false, false, bus);
        final ClassifiedClasses classified = classes2singletons(application, beanManager);

        instance.setProviders(classified.getProviders());
        instance.getFeatures().addAll(classified.getFeatures());

        for (final ResourceProvider resourceProvider: classified.getResourceProviders()) {
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
                    // normal scoped beans will return us a proxy in getInstance so it is singletons for us,
                    // @Singleton is indeed a singleton
                    // @Dependent should be a request scoped instance but for backward compat we kept it a singleton
                    //
                    // other scopes are considered request scoped (for jaxrs)
                    // and are created per request (getInstance/releaseInstance)
                    final ResourceProvider resourceProvider;
                    if (isCxfSingleton(beanManager, bean)) {
                        final Lifecycle lifecycle = new Lifecycle(beanManager, bean);
                        resourceProvider = new SingletonResourceProvider(lifecycle, bean.getBeanClass());

                        // if not a singleton we manage it per request
                        // if @Singleton the container handles it
                        // so we only need this case here
                        if (Dependent.class == bean.getScope()) {
                            disposableLifecycles.add(lifecycle);
                        }
                    } else {
                        resourceProvider = new PerRequestResourceProvider(
                        () -> new Lifecycle(beanManager, bean), bean.getBeanClass());
                    }
                    classified.addResourceProvider(resourceProvider);
                }
            }
        }

        return classified;
    }

    boolean isCxfSingleton(final BeanManager beanManager, final Bean<?> bean) {
        return beanManager.isNormalScope(bean.getScope()) || isConsideredSingleton(bean.getScope());
    }

    // warn: several impls use @Dependent == request so we should probably add a flag
    private boolean isConsideredSingleton(final Class<?> scope) {
        return Singleton.class == scope || Dependent.class == scope;
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
                            Object.class,
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
        // Use set to account for singletons and application scoped beans
        final Set< Feature > features = new LinkedHashSet<>();

        for (final Bean< ? extends Feature > bean: featureBeans) {
            if (limitedClasses.isEmpty() || limitedClasses.contains(bean.getBeanClass())) {
                features.add(
                        (Feature) beanManager.getReference(
                                bean,
                                Feature.class,
                                createCreationalContext(beanManager, bean)
                        )
                );
            }
        }

        return new ArrayList<>(features);
    }

    /**
     * Look and apply the available JAXRSServerFactoryBean extensions to customize its
     * creation (f.e. add features, providers, assign transport, ...)
     * @param beanManager bean manager
     * @param bean JAX-RS server factory bean about to be created
     */
    private void customize(final BeanManager beanManager, final JAXRSServerFactoryBean bean) {
        JAXRSServerFactoryCustomizationUtils.customize(bean);
        final Collection<Bean<?>> extensionBeans = beanManager.getBeans(JAXRSServerFactoryCustomizationExtension.class);

        for (final Bean<?> extensionBean: extensionBeans) {
            final JAXRSServerFactoryCustomizationExtension extension =
                (JAXRSServerFactoryCustomizationExtension)beanManager.getReference(
                    extensionBean,
                    JAXRSServerFactoryCustomizationExtension.class,
                    createCreationalContext(beanManager, extensionBean)
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
            synchronized (disposableCreationalContexts) {
                disposableCreationalContexts.add(creationalContext);
            }
        }
        
        return creationalContext;
    }

    /**
     * Extracts relevant beans from producers.
     * @param event process bean event
     * @param baseType base type of the producer
     */
    private <T> void processProducer(final ProcessBean<T> event, final Type baseType) {
        if (baseType instanceof Class<?>) {
            final Class<?> clazz = (Class<?>)baseType;
            if (clazz.isAnnotationPresent(Path.class)) {
                serviceBeans.add(event.getBean());
            } else if (clazz.isAnnotationPresent(Provider.class)) {
                providerBeans.add(event.getBean());
            } else if (clazz.isAnnotationPresent(ApplicationPath.class)) {
                applicationBeans.add(event.getBean());
            } 
        }
    }

    public static Set<Class<?>> getCustomContextClasses() {
        ServiceLoader<ContextClassProvider> classProviders = ServiceLoader.load(ContextClassProvider.class);
        Set<Class<?>> customContextClasses = new LinkedHashSet<>();
        for (ContextClassProvider classProvider : classProviders) {
            customContextClasses.add(classProvider.getContextClass());
        }
        return Collections.unmodifiableSet(customContextClasses);
    }
    
    @SuppressWarnings("unchecked")
    private static boolean isAnnotationPresent(final BeanManager beanManager, final Annotated annotated, 
            final Class<? extends Annotation> annotationType) {

        if (annotated.isAnnotationPresent(annotationType)) {
            return true;
        }
        
        final Stream<AnnotatedType<?>> annotatedTypes = annotated
            .getTypeClosure()
            .stream()
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(cls -> (AnnotatedType<?>)beanManager.createAnnotatedType(cls));
        
        return annotatedTypes.anyMatch(at -> at.isAnnotationPresent(annotationType));
    }
}
