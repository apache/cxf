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
package org.apache.cxf.jaxrs.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.ResourceInfoImpl;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.BeanParamInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public final class ServerProviderFactory extends ProviderFactory {
    private static final String SHARED_SERVER_FACTORY = "jaxrs.shared.server.factory";
    private static final Set<Class<?>> SERVER_FILTER_INTERCEPTOR_CLASSES = 
        new HashSet<Class<?>>(Arrays.<Class<?>>asList(ContainerRequestFilter.class,
                                                      ContainerResponseFilter.class,
                                                      ReaderInterceptor.class,
                                                      WriterInterceptor.class));
    
    private static final String WADL_PROVIDER_NAME = "org.apache.cxf.jaxrs.model.wadl.WadlGenerator";
    private List<ProviderInfo<ExceptionMapper<?>>> exceptionMappers = 
        new ArrayList<ProviderInfo<ExceptionMapper<?>>>(1);
    
    private List<ProviderInfo<ContainerRequestFilter>> preMatchContainerRequestFilters = 
        new ArrayList<ProviderInfo<ContainerRequestFilter>>(1);
    private Map<NameKey, ProviderInfo<ContainerRequestFilter>> postMatchContainerRequestFilters = 
        new NameKeyMap<ProviderInfo<ContainerRequestFilter>>(true);
    private Map<NameKey, ProviderInfo<ContainerResponseFilter>> containerResponseFilters = 
        new NameKeyMap<ProviderInfo<ContainerResponseFilter>>(false);
    private RequestPreprocessor requestPreprocessor;
    private ApplicationInfo application;
    private Set<DynamicFeature> dynamicFeatures = new LinkedHashSet<DynamicFeature>();
    
    private Map<Class<?>, BeanParamInfo> beanParams = new HashMap<Class<?>, BeanParamInfo>();
    private ProviderInfo<ContainerRequestFilter> wadlGenerator;
        
    private ServerProviderFactory(ProviderFactory baseFactory, Bus bus) {
        super(baseFactory, bus);
        if (baseFactory == null) {
            wadlGenerator = createWadlGenerator(bus);
        }
    }
    
    private static ProviderInfo<ContainerRequestFilter> createWadlGenerator(Bus bus) {
        Object provider = createProvider(WADL_PROVIDER_NAME);
        if (provider == null) {
            return null;
        } else {
            return new ProviderInfo<ContainerRequestFilter>((ContainerRequestFilter)provider, bus);
        }
    }
    
    public static ServerProviderFactory getInstance() {
        return createInstance(null);
    }
    
    public static ServerProviderFactory createInstance(Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        ServerProviderFactory baseFactory = initBaseFactory(bus);
        ServerProviderFactory factory = new ServerProviderFactory(baseFactory, bus);
        factory.setBusProviders();
        return factory;
    }
    
    public static ServerProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();
        return (ServerProviderFactory)e.get(SERVER_FACTORY_NAME);
    }
    
    private static synchronized ServerProviderFactory initBaseFactory(Bus bus) {
        ServerProviderFactory factory = (ServerProviderFactory)bus.getProperty(SHARED_SERVER_FACTORY);
        if (factory != null) {
            return factory;
        }
        factory = new ServerProviderFactory(null, bus);
        ProviderFactory.initBaseFactory(factory);
        factory.setProviders(new WebApplicationExceptionMapper());
        
        bus.setProperty(SHARED_SERVER_FACTORY, factory);


        return factory;
    }
    
    public List<ProviderInfo<ContainerRequestFilter>> getPreMatchContainerRequestFilters() {
        return getContainerRequestFilters(preMatchContainerRequestFilters, true);
    }
    
    public List<ProviderInfo<ContainerRequestFilter>> getPostMatchContainerRequestFilters(Set<String> names) {
        return getBoundFilters(postMatchContainerRequestFilters, names);
        
    }
    
    private List<ProviderInfo<ContainerRequestFilter>> getContainerRequestFilters(
        List<ProviderInfo<ContainerRequestFilter>> filters, boolean syncNeeded) {
        ProviderInfo<ContainerRequestFilter> generator = wadlGenerator != null ? wadlGenerator 
            : ((ServerProviderFactory)getBaseFactory()).wadlGenerator;
        if (generator == null) { 
            return filters;
        }
        if (filters.size() == 0) {
            return Collections.singletonList(generator);
        } else if (!syncNeeded) {
            filters.add(0, generator);
            return filters;
        } else {
            synchronized (filters) {
                if (filters.get(0) != generator) {
                    filters.add(0, generator);
                }
            }
            return filters;
        }
    }
    
    public List<ProviderInfo<ContainerResponseFilter>> getContainerResponseFilters(Set<String> names) {
        return getBoundFilters(containerResponseFilters, names);
    }
    
    public void addBeanParamInfo(BeanParamInfo bpi) {
        beanParams.put(bpi.getResourceClass(), bpi);
    }
    
    public BeanParamInfo getBeanParamInfo(Class<?> beanClass) {
        return beanParams.get(beanClass);
    }
   
    public <T extends Throwable> ExceptionMapper<T> createExceptionMapper(Class<?> exceptionType,
                                                                          Message m) {
        ExceptionMapper<T> mapper = doCreateExceptionMapper(exceptionType, m);
        if (mapper != null || isBaseFactory()) {
            return mapper;
        }
        
        return ((ServerProviderFactory)getBaseFactory()).createExceptionMapper(exceptionType, m);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Throwable> ExceptionMapper<T> doCreateExceptionMapper(
        Class<?> exceptionType, Message m) {
        
        List<ExceptionMapper<?>> candidates = new LinkedList<ExceptionMapper<?>>();
        for (ProviderInfo<ExceptionMapper<?>> em : exceptionMappers) {
            handleMapper(candidates, em, exceptionType, m, ExceptionMapper.class, true);
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator(exceptionType));
        return (ExceptionMapper<T>) candidates.get(0);
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    protected void setProviders(Object... providers) {
        List<ProviderInfo<ContainerRequestFilter>> postMatchRequestFilters = 
            new LinkedList<ProviderInfo<ContainerRequestFilter>>();
        List<ProviderInfo<ContainerResponseFilter>> postMatchResponseFilters = 
            new LinkedList<ProviderInfo<ContainerResponseFilter>>();
        
        List<ProviderInfo<? extends Object>> theProviders = 
            prepareProviders((Object[])providers, application);
        super.setCommonProviders(theProviders);
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());
            
            if (filterContractSupported(provider, providerCls, ContainerRequestFilter.class)) {
                addContainerRequestFilter(postMatchRequestFilters, 
                                          (ProviderInfo<ContainerRequestFilter>)provider);
            }
            
            if (filterContractSupported(provider, providerCls, ContainerResponseFilter.class)) {
                postMatchResponseFilters.add((ProviderInfo<ContainerResponseFilter>)provider); 
            }
            
            if (DynamicFeature.class.isAssignableFrom(providerCls)) {
                //TODO: review the possibility of DynamicFeatures needing to have Contexts injected
                Object feature = provider.getProvider();
                dynamicFeatures.add((DynamicFeature)feature);
            }
            
            
            if (ExceptionMapper.class.isAssignableFrom(providerCls)) {
                addProviderToList(exceptionMappers, provider); 
            }
            
        }
        
        Collections.sort(preMatchContainerRequestFilters, 
            new BindingPriorityComparator(ContainerRequestFilter.class, true));
        mapInterceptorFilters(postMatchContainerRequestFilters, postMatchRequestFilters,
                              ContainerRequestFilter.class, true);
        mapInterceptorFilters(containerResponseFilters, postMatchResponseFilters,
                              ContainerResponseFilter.class, false);
        
        injectContextProxies(exceptionMappers,
            postMatchContainerRequestFilters.values(), preMatchContainerRequestFilters,
            containerResponseFilters.values());
    }
    
    @Override
    protected void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        injectContextProxiesIntoProvider(pi, application == null ? null : application.getProvider());
    }
    
    @Override
    protected void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
            if (application != null && application.contextsAvailable()) {
                InjectionUtils.injectContexts(application.getProvider(), application, m);
            }
        }
    }
    
    private void addContainerRequestFilter(
        List<ProviderInfo<ContainerRequestFilter>> postMatchFilters,
        ProviderInfo<ContainerRequestFilter> p) {
        ContainerRequestFilter filter = p.getProvider();
        if (isWadlGenerator(filter.getClass())) {
            wadlGenerator = p; 
        } else {
            if (isPrematching(filter.getClass())) {
                addProviderToList(preMatchContainerRequestFilters, p);
            } else {
                postMatchFilters.add(p);
            }
        }
        
    }
    
    private static boolean isWadlGenerator(Class<?> filterCls) {
        if (filterCls == null || filterCls == Object.class) {
            return false;
        }
        if (WADL_PROVIDER_NAME.equals(filterCls.getName())) {
            return true;
        } else {
            return isWadlGenerator(filterCls.getSuperclass());
        }
    }
    
    public RequestPreprocessor getRequestPreprocessor() {
        return requestPreprocessor;
    }
    
    public void setApplicationProvider(ApplicationInfo app) {
        application = app;
    }
    
    public ApplicationInfo getApplicationProvider() {
        return application;
    }
    
    public void setRequestPreprocessor(RequestPreprocessor rp) {
        this.requestPreprocessor = rp;
    }
    
    public void clearExceptionMapperProxies() {
        clearProxies(exceptionMappers);
    }
    
    @Override
    public void clearProviders() {
        super.clearProviders();
        exceptionMappers.clear();
        preMatchContainerRequestFilters.clear();
        postMatchContainerRequestFilters.clear();
        containerResponseFilters.clear();
    }
    
    @Override
    public void clearThreadLocalProxies() {
        if (application != null) {
            application.clearThreadLocalProxies();
        }
        super.clearThreadLocalProxies();
    }
    
    public void applyDynamicFeatures(List<ClassResourceInfo> list) {
        if (dynamicFeatures.size() > 0) {
            for (ClassResourceInfo cri : list) {
                doApplyDynamicFeatures(cri);
            }
        }
    }
    
    public Configuration getConfiguration(Message m) {
        return new ServerConfigurationImpl();
    }
    
    private void doApplyDynamicFeatures(ClassResourceInfo cri) {
        Set<OperationResourceInfo> oris = cri.getMethodDispatcher().getOperationResourceInfos();
        for (OperationResourceInfo ori : oris) {
            for (DynamicFeature feature : dynamicFeatures) {
                FeatureContext featureContext = new MethodFeatureContextImpl(ori);
                feature.configure(new ResourceInfoImpl(ori), featureContext);
            }
        }
        Collection<ClassResourceInfo> subs = cri.getSubResources();
        for (ClassResourceInfo sub : subs) {
            if (sub != cri) {
                doApplyDynamicFeatures(sub);    
            }
        }
    }
    
    protected static boolean isPrematching(Class<?> filterCls) {
        return AnnotationUtils.getClassAnnotation(filterCls, PreMatching.class) != null;
    }
    
    
    
    private class MethodFeatureContextImpl implements FeatureContext {
        private MethodFeatureContextConfigurable configImpl;    
        private OperationResourceInfo ori;
        private String nameBinding;
        
        public MethodFeatureContextImpl(OperationResourceInfo ori) {
            this.ori = ori;
            configImpl = new MethodFeatureContextConfigurable(this);
            if (application != null) {
                Map<String, Object> appProps = application.getProvider().getProperties();
                for (Map.Entry<String, Object> entry : appProps.entrySet()) {
                    configImpl.property(entry.getKey(), entry.getValue());
                }
            }
            nameBinding = DEFAULT_FILTER_NAME_BINDING 
                + ori.getClassResourceInfo().getServiceClass().getName()
                + "."
                + ori.getMethodToInvoke().toString();
        }
        

        @Override
        public Configuration getConfiguration() {
            return configImpl.getConfiguration();
        }
        
        @Override
        public FeatureContext property(String name, Object value) {
            return configImpl.property(name, value);
        }

        @Override
        public FeatureContext register(Class<?> cls) {
            return configImpl.register(cls);
        }

        @Override
        public FeatureContext register(Object object) {
            return configImpl.register(object);
        }

        @Override
        public FeatureContext register(Class<?> cls, int index) {
            return configImpl.register(cls, index);
        }

        @Override
        public FeatureContext register(Class<?> cls, Class<?>... contracts) {
            return configImpl.register(cls, contracts);
        }

        @Override
        public FeatureContext register(Class<?> cls, Map<Class<?>, Integer> map) {
            return configImpl.register(cls, map);
        }

        @Override
        public FeatureContext register(Object object, int index) {
            return configImpl.register(object, index);
        }

        @Override
        public FeatureContext register(Object object, Class<?>... contracts) {
            return configImpl.register(object, contracts);
        }

        @Override
        public FeatureContext register(Object object, Map<Class<?>, Integer> map) {
            return configImpl.register(object, map);
        }
        
        FeatureContext doRegister(Object provider, Map<Class<?>, Integer> contracts) {
        
            Map<Class<?>, Integer> actualContracts = new HashMap<Class<?>, Integer>();
            
            for (Class<?> contract : contracts.keySet()) {
                if (SERVER_FILTER_INTERCEPTOR_CLASSES.contains(contract)
                    && contract.isAssignableFrom(provider.getClass())) {
                    actualContracts.put(contract, contracts.get(contract));
                }
            }
            if (!actualContracts.isEmpty()) {
                registerUserProvider(new FilterProviderInfo<Object>(provider, 
                    getBus(),
                    nameBinding,
                    true,
                    actualContracts));
                ori.addNameBindings(Collections.singletonList(nameBinding));
            }
            return this;
        }
        
    }
    
    private static class MethodFeatureContextConfigurable extends ConfigurableImpl<FeatureContext> {
        protected MethodFeatureContextConfigurable(MethodFeatureContextImpl mc) {
            super(mc, RuntimeType.SERVER, SERVER_FILTER_INTERCEPTOR_CLASSES.toArray(new Class<?>[]{}));
        }
        @Override
        public FeatureContext register(Object provider, Map<Class<?>, Integer> contracts) {
            super.register(provider, contracts);
            return ((MethodFeatureContextImpl)super.getConfigurable())
                .doRegister(provider, contracts);
        }
        
    }
    
    public static void clearThreadLocalProxies(Message message) {
        clearThreadLocalProxies(ServerProviderFactory.getInstance(message), message);
    }
    public static void clearThreadLocalProxies(ServerProviderFactory factory, Message message) {
        factory.clearThreadLocalProxies();
        ClassResourceInfo cri =
            (ClassResourceInfo)message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
        if (cri != null) {
            cri.clearThreadLocalProxies();
        }    
    }
    public static void releaseRequestState(Message message) {
        releaseRequestState(ServerProviderFactory.getInstance(message), message);
    }
    public static void releaseRequestState(ServerProviderFactory factory, Message message) {
        Object rootInstance = message.getExchange().remove(JAXRSUtils.ROOT_INSTANCE);
        if (rootInstance != null) {
            Object rootProvider = message.getExchange().remove(JAXRSUtils.ROOT_PROVIDER);
            if (rootProvider != null) {
                try {
                    ((ResourceProvider)rootProvider).releaseInstance(message, rootInstance);
                } catch (Throwable tex) {
                    // ignore
                }
            }
        }
        
        clearThreadLocalProxies(factory, message);
    }
    
    
    private class ServerConfigurationImpl implements Configuration {
        public ServerConfigurationImpl() {
            
        }
        
        @Override
        public Set<Class<?>> getClasses() {
            return application != null ? application.getProvider().getClasses() 
                : Collections.<Class<?>>emptySet();
        }

        @Override
        public Set<Object> getInstances() {
            return application != null ? application.getProvider().getSingletons() 
                : Collections.emptySet();
        }

        @Override
        public boolean isEnabled(Feature f) {
            return dynamicFeatures.contains(f);
        }

        @Override
        public boolean isEnabled(Class<? extends Feature> featureCls) {
            for (DynamicFeature f : dynamicFeatures) {
                if (featureCls.isAssignableFrom(f.getClass())) { 
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isRegistered(Object o) {
            return isRegistered(preMatchContainerRequestFilters, o)
                || isRegistered(postMatchContainerRequestFilters.values(), o)
                || isRegistered(containerResponseFilters.values(), o)
                || isRegistered(readerInterceptors.values(), o)
                || isRegistered(writerInterceptors.values(), o);
        }

        @Override
        public boolean isRegistered(Class<?> cls) {
            return isRegistered(preMatchContainerRequestFilters, cls)
                || isRegistered(postMatchContainerRequestFilters.values(), cls)
                || isRegistered(containerResponseFilters.values(), cls)
                || isRegistered(readerInterceptors.values(), cls)
                || isRegistered(writerInterceptors.values(), cls);
        }

        @Override
        public Map<Class<?>, Integer> getContracts(Class<?> cls) {
            Map<Class<?>, Integer> map = new HashMap<Class<?>, Integer>();
            if (isRegistered(cls)) {
                if (ContainerRequestFilter.class.isAssignableFrom(cls)) {
                    boolean isPreMatch = cls.getAnnotation(PreMatching.class) != null;
                    map.put(ContainerRequestFilter.class, 
                            getPriority(isPreMatch ? preMatchContainerRequestFilters
                                : postMatchContainerRequestFilters.values(), cls, ContainerRequestFilter.class));    
                }
                if (ContainerResponseFilter.class.isAssignableFrom(cls)) {
                    map.put(ContainerResponseFilter.class, 
                            getPriority(containerResponseFilters.values(), cls, ContainerResponseFilter.class));    
                }
                if (WriterInterceptor.class.isAssignableFrom(cls)) {
                    map.put(WriterInterceptor.class, 
                            getPriority(writerInterceptors.values(), cls, WriterInterceptor.class));    
                }
                if (ReaderInterceptor.class.isAssignableFrom(cls)) {
                    map.put(ReaderInterceptor.class, 
                            getPriority(readerInterceptors.values(), cls, ReaderInterceptor.class));    
                }
            }
            return map;
        }
        
        @Override
        public Map<String, Object> getProperties() {
            return application != null ? application.getProperties() 
                : Collections.<String, Object>emptyMap();
        }

        @Override
        public Object getProperty(String name) {
            return getProperties().get(name);
        }

        @Override
        public Collection<String> getPropertyNames() {
            return getProperties().keySet();
        }

        @Override
        public RuntimeType getRuntimeType() {
            return RuntimeType.SERVER;
        }
        
        private boolean isRegistered(Collection<?> list, Object o) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : list2) {
                if (pi.getProvider() == o) {
                    return true;
                }
            }
            return false;
        }
        private boolean isRegistered(Collection<?> list, Class<?> cls) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : list2) {
                if (cls.isAssignableFrom(pi.getProvider().getClass())) {
                    return true;
                }
            }
            return false;
        }
        private Integer getPriority(Collection<?> list, Class<?> cls, Class<?> filterClass) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : list2) {
                if (pi instanceof FilterProviderInfo && pi.getProvider().getClass().isAssignableFrom(cls)) {
                    return ((FilterProviderInfo<?>)pi).getPriority(filterClass);
                }
            }
            return Priorities.USER;
        }
    }
    
}
