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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorMBR;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public abstract class ProviderFactory {
    public static final String SKIP_DEFAULT_JSON_PROVIDER_REGISTRATION = "skip.default.json.provider.registration";
    public static final String SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION = "skip.jakarta.json.providers.registration";

    public static final String DEFAULT_FILTER_NAME_BINDING = "org.apache.cxf.filter.binding";
    public static final String PROVIDER_SELECTION_PROPERTY_CHANGED = "provider.selection.property.changed";
    public static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";

    protected static final String SERVER_FACTORY_NAME = "org.apache.cxf.jaxrs.provider.ServerProviderFactory";
    protected static final String CLIENT_FACTORY_NAME = "org.apache.cxf.jaxrs.client.ClientProviderFactory";
    protected static final String IGNORE_TYPE_VARIABLES = "org.apache.cxf.jaxrs.providers.ignore.typevars";

    private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);

    private static final String JAXB_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.JAXBElementProvider";
    private static final String JSON_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.json.JSONProvider";
    private static final String BUS_PROVIDERS_ALL = "org.apache.cxf.jaxrs.bus.providers";
    private static final String PROVIDER_CACHE_ALLOWED = "org.apache.cxf.jaxrs.provider.cache.allowed";
    private static final String PROVIDER_CACHE_CHECK_ALL = "org.apache.cxf.jaxrs.provider.cache.checkAllCandidates";


    static class LazyProviderClass {
        // class to Lazily call the ClassLoaderUtil.loadClass, but do it once
        // and cache the result.  Then use the class to create instances as needed.
        // This avoids calling loadClass every time a factory is initialized as
        // calling loadClass is super expensive, particularly if the class
        // cannot be found and particularly in osgi where the search is very complex.
        // This would record that the class is not found and prevent future
        // searches.
        final String className;
        volatile boolean initialized;
        Class<?> cls;

        LazyProviderClass(String cn) {
            className = cn;
        }

        synchronized void loadClass() {
            if (!initialized) {
                try {
                    cls = ClassLoaderUtils.loadClass(className, ProviderFactory.class);
                } catch (final Throwable ex) {
                    LOG.fine(className + " not available, skipping");
                }
                initialized = true;
            }
        }

        public Object tryCreateInstance(Bus bus) {
            if (!initialized) {
                loadClass();
            }
            if (cls != null) {
                try {
                    for (Constructor<?> c : cls.getConstructors()) {
                        if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0] == Bus.class) {
                            return c.newInstance(bus);
                        }
                    }
                    return cls.getDeclaredConstructor().newInstance();
                } catch (Throwable ex) {
                    String message = "Problem with creating the provider " + className;
                    if (ex.getMessage() != null) {
                        message += ": " + ex.getMessage();
                    } else {
                        message += ", exception class : " + ex.getClass().getName();
                    }
                    LOG.fine(message);
                }
            }
            return null;
        }
    };

    private static final LazyProviderClass DATA_SOURCE_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.DataSourceProvider");
    private static final LazyProviderClass JAXB_PROVIDER_CLASS =
        new LazyProviderClass(JAXB_PROVIDER_NAME);
    private static final LazyProviderClass JAXB_ELEMENT_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.JAXBElementTypedProvider");
    private static final LazyProviderClass MULTIPART_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.MultipartProvider");
    private static final LazyProviderClass JSONB_PROVIDER_CLASS =
            new LazyProviderClass("org.apache.cxf.jaxrs.provider.jsrjsonb.JsrJsonbProvider");
    private static final LazyProviderClass JSONP_PROVIDER_CLASS = 
            new LazyProviderClass("org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider");

    protected Map<NameKey, ProviderInfo<ReaderInterceptor>> readerInterceptors =
        new NameKeyMap<>(true);
    protected Map<NameKey, ProviderInfo<WriterInterceptor>> writerInterceptors =
        new NameKeyMap<>(true);

    private List<ProviderInfo<MessageBodyReader<?>>> messageReaders =
        new ArrayList<>();
    private List<ProviderInfo<MessageBodyWriter<?>>> messageWriters =
        new ArrayList<>();
    private List<ProviderInfo<ContextResolver<?>>> contextResolvers =
        new ArrayList<>();
    private List<ProviderInfo<ContextProvider<?>>> contextProviders =
        new ArrayList<>();

    private List<ProviderInfo<ParamConverterProvider>> paramConverters =
        new ArrayList<>(1);
    private boolean paramConverterContextsAvailable;
    // List of injected providers
    private Collection<ProviderInfo<?>> injectedProviders =
        new HashSet<>();

    private Bus bus;

    private Comparator<?> providerComparator;

    private ProviderCache providerCache;


    protected ProviderFactory(Bus bus) {
        this.bus = bus;
        providerCache = initCache(bus);
    }

    public Bus getBus() {
        return bus;
    }
    protected static ProviderCache initCache(Bus theBus) {
        Object allowProp = theBus.getProperty(PROVIDER_CACHE_ALLOWED);
        boolean allowed = allowProp == null || PropertyUtils.isTrue(allowProp);
        if (!allowed) {
            return null;
        }
        boolean checkAll = PropertyUtils.isTrue(theBus.getProperty(PROVIDER_CACHE_CHECK_ALL));
        return new ProviderCache(checkAll);
    }
    protected static void initFactory(ProviderFactory factory) {
        // ensure to not load providers not available in a module environment if not needed
        factory.setProviders(false,
                             false,
                     new BinaryDataProvider<Object>(),
                     new SourceProvider<Object>(),
                     DATA_SOURCE_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     new FormEncodingProvider<Object>(),
                     new StringTextProvider(),
                     new PrimitiveTextProvider<Object>(),
                     JAXB_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     JAXB_ELEMENT_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     MULTIPART_PROVIDER_CLASS.tryCreateInstance(factory.getBus()));
        final Object skipJakartaJsonProviders = factory.getBus().getProperty(SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION);
        if (!PropertyUtils.isTrue(skipJakartaJsonProviders)) {
            factory.setProviders(false, false, JSONP_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                JSONB_PROVIDER_CLASS.tryCreateInstance(factory.getBus()));
        }
        final Object skipDefaultJsonProvider = factory.getBus().getProperty(SKIP_DEFAULT_JSON_PROVIDER_REGISTRATION);
        if (!PropertyUtils.isTrue(skipDefaultJsonProvider)) {
            factory.setProviders(false, false, createProvider(JSON_PROVIDER_NAME, factory.getBus()));
        }
    }

    protected static Object createProvider(String className, Bus bus) {

        try {
            Class<?> cls = ClassLoaderUtils.loadClass(className, ProviderFactory.class);
            for (Constructor<?> c : cls.getConstructors()) {
                if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0] == Bus.class) {
                    return c.newInstance(bus);
                }
            }
            return cls.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            String message = "Problem with creating the default provider " + className;
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            } else {
                message += ", exception class : " + ex.getClass().getName();
            }
            LOG.fine(message);
        }
        return null;
    }

    public abstract Configuration getConfiguration(Message message);

    public <T> ContextResolver<T> createContextResolver(Type contextType,
                                                        Message m) {
        boolean isRequestor = MessageUtils.isRequestor(m);
        Message requestMessage = isRequestor ? m.getExchange().getOutMessage()
                                             : m.getExchange().getInMessage();

        Message responseMessage = isRequestor ? m.getExchange().getInMessage()
                                              : m.getExchange().getOutMessage();
        final Object ctProperty;
        if (responseMessage != null) {
            ctProperty = responseMessage.get(Message.CONTENT_TYPE);
        } else {
            ctProperty = requestMessage.get(Message.CONTENT_TYPE);
        }
        MediaType mt = ctProperty != null ? JAXRSUtils.toMediaType(ctProperty.toString())
            : MediaType.WILDCARD_TYPE;
        return createContextResolver(contextType, m, mt);

    }

    @SuppressWarnings("unchecked")
    public <T> ContextResolver<T> createContextResolver(Type contextType,
                                                        Message m,
                                                        MediaType type) {
        Class<?> contextCls = InjectionUtils.getActualType(contextType);
        if (contextCls == null) {
            return null;
        }
        List<ContextResolver<T>> candidates = new LinkedList<>();
        for (ProviderInfo<ContextResolver<?>> cr : contextResolvers) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            List<MediaType> mTypes = JAXRSUtils.getProduceTypes(
                                 cr.getProvider().getClass().getAnnotation(Produces.class));
                            if (JAXRSUtils.doMimeTypesIntersect(mTypes, type)) {
                                injectContextValues(cr, m);
                                candidates.add((ContextResolver<T>)cr.getProvider());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            Collections.sort(candidates, new PriorityBasedClassComparator());
            return new ContextResolverProxy<T>(candidates);
        }

    }

    @SuppressWarnings("unchecked")
    public <T> ContextProvider<T> createContextProvider(Type contextType,
                                                        Message m) {
        Class<?> contextCls = InjectionUtils.getActualType(contextType);
        if (contextCls == null) {
            return null;
        }
        for (ProviderInfo<ContextProvider<?>> cr : contextProviders) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            return (ContextProvider<T>)cr.getProvider();
                        }
                    }
                }
            }
        }
        return null;
    }

    public <T> ParamConverter<T> createParameterHandler(Class<T> paramType,
                                                        Type genericType,
                                                        Annotation[] anns,
                                                        Message m) {

        anns = anns != null ? anns : new Annotation[]{};
        for (ProviderInfo<ParamConverterProvider> pi : paramConverters) {
            injectContextValues(pi, m);
            ParamConverter<T> converter = pi.getProvider().getConverter(paramType, genericType, anns);
            if (converter != null) {
                return converter;
            }
            pi.clearThreadLocalProxies();
        }
        return null;
    }

    protected <T> boolean handleMapper(ProviderInfo<T> em,
                                       Class<?> expectedType,
                                       Message m,
                                       Class<?> providerClass,
                                       boolean injectContext) {
        return handleMapper(em, expectedType, m, providerClass, null, injectContext);
    }

    protected <T> boolean handleMapper(ProviderInfo<T> em,
                                       Class<?> expectedType,
                                       Message m,
                                       Class<?> providerClass,
                                       Class<?> commonBaseClass,
                                       boolean injectContext) {

        Class<?> mapperClass = ClassHelper.getRealClass(bus, em.getProvider());
        Type[] types;
        if (m != null && MessageUtils.getContextualBoolean(m, IGNORE_TYPE_VARIABLES)) {
            types = new Type[]{mapperClass};
        } else {
            types = getGenericInterfaces(mapperClass, expectedType, commonBaseClass);
        }
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)t;
                Type[] args = pt.getActualTypeArguments();
                for (Type arg : args) {
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>) arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (Type bound : bounds) {
                            Class<?> cls = InjectionUtils.getRawType(bound);
                            if (cls != null && (cls == Object.class || cls.isAssignableFrom(expectedType))) {
                                isResolved = true;
                                break;
                            }
                        }
                        if (!isResolved) {
                            return false;
                        }
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        return true;
                    }
                    Class<?> actualClass = InjectionUtils.getRawType(arg);
                    if (actualClass == null) {
                        continue;
                    }
                    if (expectedType.isArray() && !actualClass.isArray()) {
                        expectedType = expectedType.getComponentType();
                    }
                    if (actualClass.isAssignableFrom(expectedType) || actualClass == Object.class) {
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        return true;
                    }
                }
            } else if (t instanceof Class && providerClass.isAssignableFrom((Class<?>)t)) {
                if (injectContext) {
                    injectContextValues(em, m);
                }
                return true;
            }
        }
        return false;
    }


    public <T> List<ReaderInterceptor> createMessageBodyReaderInterceptor(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m,
                                                            boolean checkMbrNow,
                                                            Set<String> names) {
        MessageBodyReader<T> mr = !checkMbrNow ? null : createMessageBodyReader(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        int size = readerInterceptors.size();
        if (mr != null || size > 0) {
            ReaderInterceptor mbrReader = new ReaderInterceptorMBR(mr, getResponseMessage(m));

            final List<ReaderInterceptor> interceptors;
            if (size > 0) {
                interceptors = new ArrayList<>(size + 1);
                List<ProviderInfo<ReaderInterceptor>> readers =
                    getBoundFilters(readerInterceptors, names);
                for (ProviderInfo<ReaderInterceptor> p : readers) {
                    injectContextValues(p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbrReader);
            } else {
                interceptors = Collections.singletonList(mbrReader);
            }

            return interceptors;
        }
        return null;
    }

    public <T> List<WriterInterceptor> createMessageBodyWriterInterceptor(Class<T> bodyType,
                                                                          Type parameterType,
                                                                          Annotation[] parameterAnnotations,
                                                                          MediaType mediaType,
                                                                          Message m,
                                                                          Set<String> names) {
        MessageBodyWriter<T> mw = createMessageBodyWriter(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        int size = writerInterceptors.size();
        if (mw != null || size > 0) {

            @SuppressWarnings({
                "unchecked", "rawtypes"
            })
            WriterInterceptor mbwWriter = new WriterInterceptorMBW((MessageBodyWriter)mw, m);

            final List<WriterInterceptor> interceptors;
            if (size > 0) {
                interceptors = new ArrayList<>(size + 1);
                List<ProviderInfo<WriterInterceptor>> writers =
                    getBoundFilters(writerInterceptors, names);
                for (ProviderInfo<WriterInterceptor> p : writers) {
                    injectContextValues(p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbwWriter);
            } else {
                interceptors = Collections.singletonList(mbwWriter);
            }

            return interceptors;
        }
        return null;
    }



    @SuppressWarnings("unchecked")
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Step1: check the cache

        if (providerCache != null) {
            for (ProviderInfo<MessageBodyReader<?>> ep : providerCache.getReaders(type, mediaType)) {
                if (isReadable(ep, type, genericType, annotations, mediaType, m)) {
                    return (MessageBodyReader<T>)ep.getProvider();
                }
            }
        }

        boolean checkAll = providerCache != null && providerCache.isCheckAllCandidates();
        List<ProviderInfo<MessageBodyReader<?>>> allCandidates =
            checkAll ? new LinkedList<ProviderInfo<MessageBodyReader<?>>>() : null;

        MessageBodyReader<T> selectedReader = null;
        for (ProviderInfo<MessageBodyReader<?>> ep : messageReaders) {
            if (matchesReaderMediaTypes(ep, mediaType)
                && handleMapper(ep, type, m, MessageBodyReader.class, false)) {
                // This writer matches Media Type and Class
                if (checkAll) {
                    allCandidates.add(ep);
                } else if (providerCache != null && providerCache.getReaders(type, mediaType).isEmpty()) {
                    providerCache.putReaders(type, mediaType, Collections.singletonList(ep));
                }
                if (selectedReader == null
                    && isReadable(ep, type, genericType, annotations, mediaType, m)) {
                    // This writer is a selected candidate
                    selectedReader = (MessageBodyReader<T>)ep.getProvider();
                    if (!checkAll) {
                        return selectedReader;
                    }
                }

            }
        }
        if (checkAll) {
            providerCache.putReaders(type, mediaType, allCandidates);
        }
        return selectedReader;
    }

    @SuppressWarnings("unchecked")
    public <T> MessageBodyWriter<T> createMessageBodyWriter(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {

        // Step1: check the cache.
        if (providerCache != null) {
            for (ProviderInfo<MessageBodyWriter<?>> ep : providerCache.getWriters(type, mediaType)) {
                if (isWriteable(ep, type, genericType, annotations, mediaType, m)) {
                    return (MessageBodyWriter<T>)ep.getProvider();
                }
            }
        }

        // Step2: check all the registered writers

        // The cache, if enabled, may have been configured to keep the top candidate only
        boolean checkAll = providerCache != null && providerCache.isCheckAllCandidates();
        List<ProviderInfo<MessageBodyWriter<?>>> allCandidates =
            checkAll ? new LinkedList<ProviderInfo<MessageBodyWriter<?>>>() : null;

        MessageBodyWriter<T> selectedWriter = null;
        for (ProviderInfo<MessageBodyWriter<?>> ep : messageWriters) {
            if (matchesWriterMediaTypes(ep, mediaType)
                && handleMapper(ep, type, m, MessageBodyWriter.class, false)) {
                // This writer matches Media Type and Class
                if (checkAll) {
                    allCandidates.add(ep);
                } else if (providerCache != null && providerCache.getWriters(type, mediaType).isEmpty()) {
                    providerCache.putWriters(type, mediaType, Collections.singletonList(ep));
                }
                if (selectedWriter == null
                    && isWriteable(ep, type, genericType, annotations, mediaType, m)) {
                    // This writer is a selected candidate
                    selectedWriter = (MessageBodyWriter<T>)ep.getProvider();
                    if (!checkAll) {
                        return selectedWriter;
                    }
                }

            }
        }
        if (checkAll) {
            providerCache.putWriters(type, mediaType, allCandidates);
        }
        return selectedWriter;

    }

    protected void setBusProviders() {
        List<Object> extensions = new LinkedList<>();
        addBusExtension(extensions,
                        MessageBodyReader.class,
                        MessageBodyWriter.class,
                        ExceptionMapper.class);
        if (!extensions.isEmpty()) {
            setProviders(true, true, extensions.toArray());
        }
    }


    private void addBusExtension(List<Object> extensions, Class<?>... extClasses) {
        for (Class<?> extClass : extClasses) {
            Object ext = bus.getProperty(extClass.getName());
            if (extClass.isInstance(ext)) {
                extensions.add(ext);
            }
        }
        Object allProp = bus.getProperty(BUS_PROVIDERS_ALL);
        if (allProp instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> all = (List<Object>)allProp;
            extensions.addAll(all);
        }
    }

    protected abstract void setProviders(boolean custom, boolean busGlobal, Object... providers);

    @SuppressWarnings("unchecked")
    protected void setCommonProviders(List<ProviderInfo<? extends Object>> theProviders, RuntimeType type) {
        List<ProviderInfo<ReaderInterceptor>> readInts =
            new LinkedList<>();
        List<ProviderInfo<WriterInterceptor>> writeInts =
            new LinkedList<>();
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(bus, provider.getProvider());

            // Check if provider is constrained to runtime type
            if (!constrainedTo(providerCls, type)) {
                continue;
            }

            if (filterContractSupported(provider, providerCls, MessageBodyReader.class)) {
                addProviderToList(messageReaders, provider);
            }

            if (filterContractSupported(provider, providerCls, MessageBodyWriter.class)) {
                addProviderToList(messageWriters, provider);
            }

            if (filterContractSupported(provider, providerCls, ContextResolver.class)) {
                addProviderToList(contextResolvers, provider);
            }

            if (ContextProvider.class.isAssignableFrom(providerCls)) {
                addProviderToList(contextProviders, provider);
            }

            if (filterContractSupported(provider, providerCls, ReaderInterceptor.class)) {
                readInts.add((ProviderInfo<ReaderInterceptor>)provider);
            }

            if (filterContractSupported(provider, providerCls, WriterInterceptor.class)) {
                writeInts.add((ProviderInfo<WriterInterceptor>)provider);
            }

            if (filterContractSupported(provider, providerCls, ParamConverterProvider.class)) {
                paramConverters.add((ProviderInfo<ParamConverterProvider>)provider);
            }
        }
        sortReaders();
        sortWriters();
        sortContextResolvers();
        sortParamConverterProviders();

        mapInterceptorFilters(readerInterceptors, readInts, ReaderInterceptor.class, true);
        mapInterceptorFilters(writerInterceptors, writeInts, WriterInterceptor.class, true);

        injectContextProxies(messageReaders, messageWriters, contextResolvers, paramConverters,
            readerInterceptors.values(), writerInterceptors.values());
        checkParamConverterContexts();
    }

    private void checkParamConverterContexts() {
        for (ProviderInfo<ParamConverterProvider> pi : paramConverters) {
            if (pi.contextsAvailable()) {
                paramConverterContextsAvailable = true;
            }
        }

    }
    public boolean isParamConverterContextsAvailable() {
        return paramConverterContextsAvailable;
    }



    protected void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
        }
    }

    protected void addProviderToList(List<?> list, ProviderInfo<?> provider) {
        List<ProviderInfo<?>> list2 = CastUtils.cast(list);
        for (ProviderInfo<?> pi : list2) {
            if (pi.getProvider() == provider.getProvider()) {
                return;
            }
        }
        list2.add(provider);
    }

    protected void injectContextProxies(Collection<?> ... providerLists) {
        for (Collection<?> list : providerLists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                injectContextProxiesIntoProvider(pi);
            }
        }
    }

    protected void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        injectContextProxiesIntoProvider(pi, null);
    }

    void injectContextProxiesIntoProvider(ProviderInfo<?> pi, Application app) {
        if (pi.contextsAvailable()) {
            InjectionUtils.injectContextProxiesAndApplication(pi, pi.getProvider(), app, this);
            injectedProviders.add(pi);
        }
    }

    /*
     * sorts the available providers according to the media types they declare
     * support for. Sorting of media types follows the general rule: x/y < * x < *,
     * i.e. a provider that explicitly lists a media types is sorted before a
     * provider that lists *. Quality parameter values are also used such that
     * x/y;q=1.0 < x/y;q=0.7.
     */
    private void sortReaders() {
        if (!customComparatorAvailable(MessageBodyReader.class)) {
            messageReaders.sort(new MessageBodyReaderComparator());
        } else {
            doCustomSort(messageReaders);
        }
    }
    private <T> void sortWriters() {
        if (!customComparatorAvailable(MessageBodyWriter.class)) {
            messageWriters.sort(new MessageBodyWriterComparator());
        } else {
            doCustomSort(messageWriters);
        }
    }
    
    private <T> void sortParamConverterProviders() {
        if (!customComparatorAvailable(ParamConverterProvider.class)) {
            paramConverters.sort(new ParamConverterProviderComparator(bus));
        } else {
            doCustomSort(paramConverters);
        }
    }

    private boolean customComparatorAvailable(Class<?> providerClass) {
        if (providerComparator != null) {
            Type type = ((ParameterizedType)providerComparator.getClass()
                .getGenericInterfaces()[0]).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)type;
                if (pt.getRawType() == ProviderInfo.class) {
                    Type type2 = pt.getActualTypeArguments()[0];
                    if (type2 == providerClass
                        || type2 instanceof WildcardType
                        || type2 instanceof ParameterizedType
                           && ((ParameterizedType)type2).getRawType() == providerClass) {
                        return true;
                    }
                }
            } else if (type == Object.class) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> void doCustomSort(List<?> listOfProviders) {
        Comparator<?> theProviderComparator = providerComparator;
        Type type = ((ParameterizedType)providerComparator.getClass()
            .getGenericInterfaces()[0]).getActualTypeArguments()[0];
        if (type == Object.class) {
            theProviderComparator =
                new ProviderInfoClassComparator((Comparator<Object>)theProviderComparator);
        }
        List<T> theProviders = (List<T>)listOfProviders;
        Comparator<? super T> theComparator = (Comparator<? super T>)theProviderComparator;
        theProviders.sort(theComparator);
    }

    private void sortContextResolvers() {
        contextResolvers.sort(new ContextResolverComparator());
    }





    private <T> boolean matchesReaderMediaTypes(ProviderInfo<MessageBodyReader<?>> pi,
                                                MediaType mediaType) {
        MessageBodyReader<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);

        return JAXRSUtils.doMimeTypesIntersect(Collections.singletonList(mediaType), supportedMediaTypes);
    }

    private boolean isReadable(ProviderInfo<MessageBodyReader<?>> pi,
                               Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType,
                               Message m) {
        MessageBodyReader<?> ep = pi.getProvider();
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
        }
        return ep.isReadable(type, genericType, annotations, mediaType);
    }

    private <T> boolean matchesWriterMediaTypes(ProviderInfo<MessageBodyWriter<?>> pi,
                                                MediaType mediaType) {
        MessageBodyWriter<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);

        return JAXRSUtils.doMimeTypesIntersect(Collections.singletonList(mediaType), supportedMediaTypes);
    }

    private boolean isWriteable(ProviderInfo<MessageBodyWriter<?>> pi,
                               Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType,
                               Message m) {
        MessageBodyWriter<?> ep = pi.getProvider();
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
        }
        return ep.isWriteable(type, genericType, annotations, mediaType);
    }

    List<ProviderInfo<MessageBodyReader<?>>> getMessageReaders() {
        return Collections.unmodifiableList(messageReaders);
    }

    List<ProviderInfo<MessageBodyWriter<?>>> getMessageWriters() {
        return Collections.unmodifiableList(messageWriters);
    }

    public List<ProviderInfo<ContextResolver<?>>> getContextResolvers() {
        return Collections.unmodifiableList(contextResolvers);
    }


    public void registerUserProvider(Object provider) {
        setUserProviders(Collections.singletonList(provider));
    }
    /**
     * Use for injection of entityProviders
     * @param userProviders the userProviders to set
     */
    public void setUserProviders(List<?> userProviders) {
        setProviders(true, false, userProviders.toArray());
    }

    private static final class MessageBodyReaderComparator
        implements Comparator<ProviderInfo<MessageBodyReader<?>>> {

        public int compare(ProviderInfo<MessageBodyReader<?>> p1,
                           ProviderInfo<MessageBodyReader<?>> p2) {
            MessageBodyReader<?> e1 = p1.getProvider();
            MessageBodyReader<?> e2 = p2.getProvider();

            List<MediaType> types1 = JAXRSUtils.getProviderConsumeTypes(e1);
            types1 = JAXRSUtils.sortMediaTypes(types1, null);
            List<MediaType> types2 = JAXRSUtils.getProviderConsumeTypes(e2);
            types2 = JAXRSUtils.sortMediaTypes(types2, null);

            int result = JAXRSUtils.compareSortedMediaTypes(types1, types2, null);
            if (result != 0) {
                return result;
            }
            result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
            result = compareCustomStatus(p1, p2);
            if (result != 0) {
                return result;
            }
            return comparePriorityStatus(p1.getProvider().getClass(), p2.getProvider().getClass());
        }
    }

    private static final class MessageBodyWriterComparator
        implements Comparator<ProviderInfo<MessageBodyWriter<?>>> {

        public int compare(ProviderInfo<MessageBodyWriter<?>> p1,
                           ProviderInfo<MessageBodyWriter<?>> p2) {
            MessageBodyWriter<?> e1 = p1.getProvider();
            MessageBodyWriter<?> e2 = p2.getProvider();

            int result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e1), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e2), JAXRSUtils.MEDIA_TYPE_QS_PARAM);

            result = JAXRSUtils.compareSortedMediaTypes(types1, types2, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            if (result != 0) {
                return result;
            }
            
            result = compareCustomStatus(p1, p2);
            if (result != 0) {
                return result;
            }

            return comparePriorityStatus(p1.getProvider().getClass(), p2.getProvider().getClass());
        }
    }

    private static final class ParamConverterProviderComparator
            implements Comparator<ProviderInfo<ParamConverterProvider>> {
        private final Bus bus;
        
        ParamConverterProviderComparator(Bus bus) {
            this.bus = bus;
        }
        
        @Override
        public int compare(ProviderInfo<ParamConverterProvider> p1, ProviderInfo<ParamConverterProvider> p2) {
            final int result = compareCustomStatus(p1, p2);
            if (result != 0) {
                return result;
            }

            final Class<?> cl1 = ClassHelper.getRealClass(bus, p1.getProvider());
            final Class<?> cl2 = ClassHelper.getRealClass(bus, p2.getProvider());

            return comparePriorityStatus(cl1, cl2);
        }
    }

    protected static int compareCustomStatus(ProviderInfo<?> p1, ProviderInfo<?> p2) {
        boolean custom1 = p1.isCustom();
        int result = Boolean.compare(p2.isCustom(), custom1);
        if (result == 0 && custom1) {
            result = Boolean.compare(p1.isBusGlobal(), p2.isBusGlobal());
        }
        return result;
    }

    static int comparePriorityStatus(Class<?> cl1, Class<?> cl2) {
        return Integer.compare(AnnotationUtils.getBindingPriority(cl1), AnnotationUtils.getBindingPriority(cl2));
    }

    private static final class ContextResolverComparator
        implements Comparator<ProviderInfo<ContextResolver<?>>> {

        public int compare(ProviderInfo<ContextResolver<?>> p1,
                           ProviderInfo<ContextResolver<?>> p2) {
            ContextResolver<?> e1 = p1.getProvider();
            ContextResolver<?> e2 = p2.getProvider();

            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProduceTypes(
                     e1.getClass().getAnnotation(Produces.class)), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProduceTypes(
                     e2.getClass().getAnnotation(Produces.class)), JAXRSUtils.MEDIA_TYPE_QS_PARAM);

            return JAXRSUtils.compareSortedMediaTypes(types1, types2, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
        }
    }

    public void clearThreadLocalProxies() {
        clearProxies(injectedProviders);
    }

    void clearProxies(Collection<?> ...lists) {
        for (Collection<?> list : lists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                pi.clearThreadLocalProxies();
            }
        }
    }

    public void clearProviders() {
        messageReaders.clear();
        messageWriters.clear();
        contextResolvers.clear();
        contextProviders.clear();
        readerInterceptors.clear();
        writerInterceptors.clear();
        paramConverters.clear();
    }

    public void setBus(Bus bus) {
        if (bus == null) {
            return;
        }
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            injectProviderProperty(r.getProvider(), "setBus", Bus.class, bus);
        }
    }

    private boolean injectProviderProperty(Object provider, String mName, Class<?> pClass,
                                        Object pValue) {
        try {
            Method m = provider.getClass().getMethod(mName, new Class[]{pClass});
            m.invoke(provider, new Object[]{pValue});
            return true;
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    public void setSchemaLocations(List<String> schemas) {
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            injectProviderProperty(r.getProvider(), "setSchemaLocations", List.class, schemas);
        }
    }

    protected static <T> List<ProviderInfo<T>> getBoundFilters(Map<NameKey, ProviderInfo<T>> boundFilters,
                                                                          Set<String> names) {
        if (boundFilters.isEmpty()) {
            return Collections.emptyList();
        }
        names = names == null ? Collections.<String>emptySet() : names;

        MultivaluedMap<ProviderInfo<T>, String> map =
            new MetadataMap<>();
        for (Map.Entry<NameKey, ProviderInfo<T>> entry : boundFilters.entrySet()) {
            String entryName = entry.getKey().getName();
            ProviderInfo<T> provider = entry.getValue();
            if (entryName.equals(DEFAULT_FILTER_NAME_BINDING)) {
                map.put(provider, Collections.<String>emptyList());
            } else {
                if (provider instanceof FilterProviderInfo) {
                    FilterProviderInfo<?> fpi = (FilterProviderInfo<?>)provider;
                    if (fpi.isDynamic() && !names.containsAll(fpi.getNameBindings())) {
                        continue;
                    }
                }
                map.add(provider, entryName);
            }
        }
        List<ProviderInfo<T>> list = new LinkedList<>();
        for (Map.Entry<ProviderInfo<T>, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            if (names.containsAll(values)) {
                ProviderInfo<T> provider = entry.getKey();
                list.add(provider);
            }
        }
        return list;
    }

    public void initProviders(List<ClassResourceInfo> cris) {
        Set<Object> set = getReadersWriters();
        for (Object o : set) {
            Object provider = ((ProviderInfo<?>)o).getProvider();
            if (provider instanceof AbstractConfigurableProvider) {
                ((AbstractConfigurableProvider)provider).init(cris);
            }
        }
    }

    Set<Object> getReadersWriters() {
        Set<Object> set = new HashSet<>();
        set.addAll(messageReaders);
        set.addAll(messageWriters);
        return set;
    }

    public static class ClassComparator implements
        Comparator<Object> {
        private Class<?> expectedCls;
        public ClassComparator() {
        }
        public ClassComparator(Class<?> expectedCls) {
            this.expectedCls = expectedCls;
        }

        public int compare(Object em1, Object em2) {
            return compareClasses(expectedCls, em1, em2);
        }
    }

    public static class ProviderInfoClassComparator implements Comparator<ProviderInfo<?>> {
        private Comparator<Object> comp;
        private boolean defaultComp;
        public ProviderInfoClassComparator(Class<?> expectedCls) {
            this.comp = new ClassComparator(expectedCls);
            this.defaultComp = true;
        }
        public ProviderInfoClassComparator(Comparator<Object> comp) {
            this.comp = comp;
        }
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            int result = comp.compare(p1.getProvider(), p2.getProvider());
            if (result == 0 && defaultComp) {
                result = compareCustomStatus(p1, p2);
            }
            return result;
        }
    }

    static class PriorityBasedClassComparator extends ClassComparator {
        PriorityBasedClassComparator() {
            super();
        }

        PriorityBasedClassComparator(Class<?> expectedCls) {
            super(expectedCls);
        }

        @Override
        public int compare(Object em1, Object em2) {
            int result = super.compare(em1, em2);
            if (result == 0) {
                result = comparePriorityStatus(em1.getClass(), em2.getClass());
            }
            return result;
        }
    }

    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();

        Message outM = m.getExchange().getOutMessage();
        boolean isClient = outM != null && MessageUtils.isRequestor(outM);
        String name = isClient ? CLIENT_FACTORY_NAME : SERVER_FACTORY_NAME;

        return (ProviderFactory)e.get(name);
    }
    protected static int compareClasses(Object o1, Object o2) {
        return compareClasses(null, o1, o2);
    }
    protected static int compareClasses(Class<?> expectedCls, Object o1, Object o2) {
        Class<?> cl1 = ClassHelper.getRealClass(o1);
        Class<?> cl2 = ClassHelper.getRealClass(o2);
        Type[] types1 = getGenericInterfaces(cl1, expectedCls);
        Type[] types2 = getGenericInterfaces(cl2, expectedCls);
        if (types1.length == 0 && types2.length == 0) {
            return 0;
        } else if (types1.length == 0 && types2.length > 0) {
            return 1;
        } else if (types1.length > 0 && types2.length == 0) {
            return -1;
        }

        Class<?> realClass1 = InjectionUtils.getActualType(types1[0]);
        Class<?> realClass2 = InjectionUtils.getActualType(types2[0]);
        if (realClass1 == realClass2) {
            return 0;
        }
        if (realClass1.isAssignableFrom(realClass2)) {
            // subclass should go first
            return 1;
        } else if (realClass2.isAssignableFrom(realClass1)) {
            // superclass should go last
            return -1;
        }
        
        // there is no relation between the types returned by the providers
        return 0;
    }

    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass) {
        return getGenericInterfaces(cls, expectedClass, Object.class);
    }
    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass,
                                               Class<?> commonBaseCls) {
        if (Object.class == cls) {
            return new Type[]{};
        }
        if (expectedClass != null) {
            Type genericSuperType = cls.getGenericSuperclass();
            if (genericSuperType instanceof ParameterizedType) {
                Class<?> actualType = InjectionUtils.getActualType(genericSuperType);
                if (actualType != null && actualType.isAssignableFrom(expectedClass)) {
                    return new Type[]{genericSuperType};
                } else if (commonBaseCls != null && commonBaseCls != Object.class
                           && commonBaseCls.isAssignableFrom(expectedClass)
                           && commonBaseCls.isAssignableFrom(actualType)
                           || expectedClass.isAssignableFrom(actualType)) {
                    return new Type[]{};
                }
            }
        }
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            return types;
        }
        return getGenericInterfaces(cls.getSuperclass(), expectedClass, commonBaseCls);
    }

    protected static class AbstractPriorityComparator {

        private boolean ascending;

        protected AbstractPriorityComparator(boolean ascending) {
            this.ascending = ascending;
        }

        protected int compare(Integer b1Value, Integer b2Value) {
            int result = b1Value.compareTo(b2Value);
            return ascending ? result : result * -1;
        }

    }

    protected static class BindingPriorityComparator extends AbstractPriorityComparator
        implements Comparator<ProviderInfo<?>> {
        private Class<?> providerCls;

        public BindingPriorityComparator(Class<?> providerCls, boolean ascending) {
            super(ascending);
            this.providerCls = providerCls;
        }

        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            return compare(getFilterPriority(p1, providerCls),
                           getFilterPriority(p2, providerCls));
        }

    }

    static class ContextResolverProxy<T> implements ContextResolver<T> {
        private List<ContextResolver<T>> candidates;
        ContextResolverProxy(List<ContextResolver<T>> candidates) {
            this.candidates = candidates;
        }
        public T getContext(Class<?> cls) {
            for (ContextResolver<T> resolver : candidates) {
                T context = resolver.getContext(cls);
                if (context != null) {
                    return context;
                }
            }
            return null;
        }

        public List<ContextResolver<T>> getResolvers() {
            return candidates;
        }
    }

    public static ProviderInfo<? extends Object> createProviderFromConstructor(Constructor<?> c,
                                                                 Map<Class<?>, Object> values,
                                                                 Bus theBus,
                                                                 boolean checkContexts,
                                                                 boolean custom) {


        Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> proxiesMap =
            CastUtils.cast((Map<?, ?>)theBus.getProperty(AbstractResourceInfo.CONSTRUCTOR_PROXY_MAP));
        Map<Class<?>, ThreadLocalProxy<?>> existingProxies = null;
        if (proxiesMap != null) {
            existingProxies = proxiesMap.get(c.getDeclaringClass());
        }
        Class<?>[] paramTypes = c.getParameterTypes();
        Object[] cArgs = ResourceUtils.createConstructorArguments(c, null, false, values);
        if (existingProxies != null && existingProxies.size() <= paramTypes.length) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (cArgs[i] instanceof ThreadLocalProxy) {
                    cArgs[i] = existingProxies.get(paramTypes[i]);
                }
            }
        }
        final Object instance;
        try {
            instance = c.newInstance(cArgs);
        } catch (Throwable ex) {
            throw new RuntimeException("Resource or provider class " + c.getDeclaringClass().getName()
                                       + " can not be instantiated", ex);
        }
        Map<Class<?>, ThreadLocalProxy<?>> proxies =
            new LinkedHashMap<>();
        for (int i = 0; i < paramTypes.length; i++) {
            if (cArgs[i] instanceof ThreadLocalProxy) {
                @SuppressWarnings("unchecked")
                ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>)cArgs[i];
                proxies.put(paramTypes[i], proxy);
            }
        }
        boolean isApplication = Application.class.isAssignableFrom(c.getDeclaringClass());
        if (isApplication) {
            return new ApplicationInfo((Application)instance, proxies, theBus);
        }
        return new ProviderInfo<Object>(instance, proxies, theBus, checkContexts, custom);
    }

    private Message getResponseMessage(Message message) {
        Message responseMessage = message.getExchange().getInMessage();
        if (responseMessage == null) {
            responseMessage = message.getExchange().getInFaultMessage();
        }

        return responseMessage;
    }

    protected static class NameKey {
        private String name;
        private Integer priority;
        private Class<?> providerCls;
        private ProviderInfo<?> providerInfo;

        public NameKey(String name,
                       int priority,
                       Class<?> providerCls) {

            this(name, priority, providerCls, null);
        }

        public NameKey(String name,
                       int priority,
                       Class<?> providerCls,
                       ProviderInfo<?> provider) {

            this.name = name;
            this.priority = priority;
            this.providerCls = providerCls;
            this.providerInfo = provider;
        }

        public String getName() {
            return name;
        }

        public Integer getPriority() {
            return priority;
        }

        public ProviderInfo<?> getProviderInfo() {
            return providerInfo;
        }

        public boolean equals(Object o) {
            if (!(o instanceof NameKey)) {
                return false;
            }
            NameKey other = (NameKey)o;
            return name.equals(other.name) && priority.equals(other.priority)
                && providerCls == other.providerCls;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public String toString() {
            return name + ":" + priority;
        }
    }

    protected static <T> void mapInterceptorFilters(Map<NameKey, ProviderInfo<T>> map,
                                                    List<ProviderInfo<T>> filters,
                                                    Class<?> providerCls,
                                                    boolean ascending) {

        for (ProviderInfo<T> p : filters) {
            Set<String> names = getFilterNameBindings(p);

            int priority = getFilterPriority(p, providerCls);

            for (String name : names) {
                map.put(new NameKey(name, priority, p.getClass(), p), p);
            }
        }

    }

    protected static Set<String> getFilterNameBindings(ProviderInfo<?> p) {
        if (p instanceof FilterProviderInfo) {
            return ((FilterProviderInfo<?>)p).getNameBindings();
        } else {
            return getFilterNameBindings(p.getBus(), p.getProvider());
        }

    }
    protected static Set<String> getFilterNameBindings(Bus bus, Object provider) {
        Set<String> names = AnnotationUtils.getInstanceNameBindings(bus, provider);
        if (names.isEmpty()) {
            names = Collections.singleton(DEFAULT_FILTER_NAME_BINDING);
        }
        return names;
    }

    protected static int getFilterPriority(ProviderInfo<?> p, Class<?> providerCls) {
        return p instanceof FilterProviderInfo ? ((FilterProviderInfo<?>)p).getPriority(providerCls)
            : AnnotationUtils.getBindingPriority(p.getProvider().getClass());
    }

    protected static class NameKeyComparator extends AbstractPriorityComparator
        implements Comparator<NameKey> {

        private final Comparator<ProviderInfo<?>> comparator;

        public NameKeyComparator(boolean ascending) {
            this(null, ascending);
        }

        public NameKeyComparator(
            Comparator<ProviderInfo<?>> comparator, boolean ascending) {

            super(ascending);
            this.comparator = comparator;
        }

        @Override
        public int compare(NameKey key1, NameKey key2) {
            int result = compare(key1.getPriority(), key2.getPriority());
            if (result != 0) {
                return result;
            }

            if (comparator != null) {
                result = comparator.compare(
                    key1.getProviderInfo(), key2.getProviderInfo());

                if (result != 0) {
                    return result;
                }
            }

            return compare(key1.hashCode(), key2.hashCode());
        }
    }

    protected static class NameKeyMap<T> extends TreeMap<NameKey, T> {
        private static final long serialVersionUID = -4352258671270502204L;

        public NameKeyMap(
            Comparator<ProviderInfo<?>> comparator, boolean ascending) {

            super(new NameKeyComparator(comparator, ascending));
        }

        public NameKeyMap(boolean ascending) {
            super(new NameKeyComparator(ascending));
        }
    }

    protected static boolean filterContractSupported(ProviderInfo<?> provider,
                                                     Class<?> providerCls,
                                                     Class<?> contract) {
        boolean result = false;
        if (contract.isAssignableFrom(providerCls)) {
            Set<Class<?>> actualContracts = null;
            if (provider instanceof FilterProviderInfo) {
                actualContracts = ((FilterProviderInfo<?>)provider).getSupportedContracts();
            }
            if (actualContracts != null) {
                result = actualContracts.contains(contract);
            } else {
                result = true;
            }
        }
        return result;
    }

    protected List<ProviderInfo<? extends Object>> prepareProviders(boolean custom,
                                                                    boolean busGlobal,
                                                                    Object[] providers,
                                                                    ProviderInfo<Application> application) {
        List<ProviderInfo<? extends Object>> theProviders =
            new ArrayList<>(providers.length);
        for (Object o : providers) {
            if (o == null) {
                continue;
            }
            Object provider = o;
            if (provider.getClass() == Class.class) {
                provider = ResourceUtils.createProviderInstance((Class<?>)provider);
            }
            if (provider instanceof Constructor) {
                Map<Class<?>, Object> values = CastUtils.cast(application == null ? null
                    : Collections.singletonMap(Application.class, application.getProvider()));
                theProviders.add(
                    createProviderFromConstructor((Constructor<?>)provider, values, getBus(), true, custom));
            } else if (provider instanceof ProviderInfo) {
                theProviders.add((ProviderInfo<?>)provider);
            } else {
                ProviderInfo<Object> theProvider = new ProviderInfo<>(provider, getBus(), custom);
                theProvider.setBusGlobal(busGlobal);
                theProviders.add(theProvider);
            }
        }
        return theProviders;
    }

    public MessageBodyWriter<?> getDefaultJaxbWriter() {
        for (ProviderInfo<MessageBodyWriter<?>> pi : this.messageWriters) {
            Class<?> cls = pi.getProvider().getClass();
            if (cls.getName().equals(JAXB_PROVIDER_NAME)) {
                return pi.getProvider();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setProviderComparator(Comparator<?> providerComparator) {
        this.providerComparator = providerComparator;

        sortReaders();
        sortWriters();
        sortParamConverterProviders();
        
        NameKeyMap<ProviderInfo<ReaderInterceptor>> sortedReaderInterceptors =
            new NameKeyMap<>(
                (Comparator<ProviderInfo<?>>) providerComparator, true);
        sortedReaderInterceptors.putAll(readerInterceptors);
        NameKeyMap<ProviderInfo<WriterInterceptor>> sortedWriterInterceptors =
            new NameKeyMap<>(
                (Comparator<ProviderInfo<?>>) providerComparator, true);
        sortedWriterInterceptors.putAll(writerInterceptors);

        readerInterceptors = sortedReaderInterceptors;
        writerInterceptors = sortedWriterInterceptors;
    }

    /**
     * Checks the presence of {@link ConstrainedTo} annotation and, if present, applicability to 
     * the runtime type.
     * @param providerCls provider class
     * @param type runtime type
     * @return "true" if provider could be used with runtime type, "false" otherwise
     */
    protected static boolean constrainedTo(Class<?> providerCls, RuntimeType type) {
        final ConstrainedTo constrained = AnnotationUtils.getClassAnnotation(providerCls, ConstrainedTo.class);
        return constrained == null || constrained.value() == type;
    }
}
