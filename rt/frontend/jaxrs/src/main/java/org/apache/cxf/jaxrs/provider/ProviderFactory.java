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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

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
    
    protected Map<NameKey, ProviderInfo<ReaderInterceptor>> readerInterceptors = 
        new NameKeyMap<ProviderInfo<ReaderInterceptor>>(true);
    protected Map<NameKey, ProviderInfo<WriterInterceptor>> writerInterceptors = 
        new NameKeyMap<ProviderInfo<WriterInterceptor>>(true);
    
    private List<ProviderInfo<MessageBodyReader<?>>> messageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader<?>>>();
    private List<ProviderInfo<MessageBodyWriter<?>>> messageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter<?>>>();
    private List<ProviderInfo<ContextResolver<?>>> contextResolvers = 
        new ArrayList<ProviderInfo<ContextResolver<?>>>(1);
    private List<ProviderInfo<ContextProvider<?>>> contextProviders = 
        new ArrayList<ProviderInfo<ContextProvider<?>>>(1);
    
    private Set<ParamConverterProvider> newParamConverters;
    
    // List of injected providers
    private Collection<ProviderInfo<?>> injectedProviders = 
        new LinkedList<ProviderInfo<?>>();
    
    private Bus bus;
    
    private Comparator<?> providerComparator;
    
    protected ProviderFactory(Bus bus) {
        this.bus = bus;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    protected static void initFactory(ProviderFactory factory) {
        factory.setProviders(false,
                             false,
                     new BinaryDataProvider<Object>(),
                     new SourceProvider<Object>(),
                     new DataSourceProvider<Object>(),
                     new FormEncodingProvider<Object>(),
                     new StringTextProvider(),
                     new PrimitiveTextProvider<Object>(),
                     createProvider(JAXB_PROVIDER_NAME),
                     new MultipartProvider());
        Object prop = factory.getBus().getProperty("skip.default.json.provider.registration");
        if (!PropertyUtils.isTrue(prop)) {
            factory.setProviders(false, false, createProvider(JSON_PROVIDER_NAME));
        }
            
    }
    
    protected static Object createProvider(String className) {
        
        try {
            return ClassLoaderUtils.loadClass(className, ProviderFactory.class).newInstance();
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
        Object ctProperty = null;
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
        List<ContextResolver<T>> candidates = new LinkedList<ContextResolver<T>>();
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
                            if (JAXRSUtils.intersectMimeTypes(mTypes, type).size() > 0) {
                                injectContextValues(cr, m);
                                candidates.add((ContextResolver<T>)cr.getProvider());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.size() == 0) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            Collections.sort(candidates, new ClassComparator());
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
                                                        Annotation[] anns) {
        
        if (newParamConverters != null) {
            anns = anns != null ? anns : new Annotation[]{};
            for (ParamConverterProvider newParamConverter : newParamConverters) {
                ParamConverter<T> converter = newParamConverter.getConverter(paramType, genericType, anns);
                if (converter != null) {
                    return converter;
                }
            }
        } 
        return null;
    }
    
    protected <T> boolean handleMapper(ProviderInfo<T> em, 
                                       Class<?> expectedType, 
                                       Message m, 
                                       Class<?> providerClass,
                                       boolean injectContext) {
        
        Class<?> mapperClass = ClassHelper.getRealClass(bus, em.getProvider());
        Type[] types = null;
        if (m != null && MessageUtils.isTrue(m.getContextualProperty(IGNORE_TYPE_VARIABLES))) {
            types = new Type[]{mapperClass};
        } else {
            types = getGenericInterfaces(mapperClass, expectedType);
        }
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)t;
                Type[] args = pt.getActualTypeArguments();
                for (int i = 0; i < args.length; i++) {
                    Type arg = args[i];
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>)arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (int j = 0; j < bounds.length; j++) {
                            Class<?> cls = InjectionUtils.getRawType(bounds[j]);
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
            ReaderInterceptor mbrReader = new ReaderInterceptorMBR(mr, m.getExchange().getInMessage());
            
            List<ReaderInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<ReaderInterceptor>(size + 1);
                List<ProviderInfo<ReaderInterceptor>> readers =
                    getBoundFilters(readerInterceptors, names);
                for (ProviderInfo<ReaderInterceptor> p : readers) {
                    InjectionUtils.injectContexts(p.getProvider(), p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbrReader);
            } else {
                interceptors = Collections.singletonList(mbrReader);
            }
            
            return interceptors;
        } else {
            return null;
        }
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
              
            List<WriterInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<WriterInterceptor>(size + 1);
                List<ProviderInfo<WriterInterceptor>> writers =
                    getBoundFilters(writerInterceptors, names);
                for (ProviderInfo<WriterInterceptor> p : writers) {
                    InjectionUtils.injectContexts(p.getProvider(), p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbwWriter);
            } else {
                interceptors = Collections.singletonList(mbwWriter);
            }
            
            return interceptors;
        } else {
            return null;
        }
    }
    
    
    
    @SuppressWarnings("unchecked")
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {
        for (ProviderInfo<MessageBodyReader<?>> ep : messageReaders) {
            if (matchesReaderCriterias(ep, type, genericType, annotations, mediaType, m)
                && handleMapper(ep, type, m, MessageBodyReader.class, false)) {
                return (MessageBodyReader<T>)ep.getProvider();
            }
        }     
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T> MessageBodyWriter<T> createMessageBodyWriter(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {
        for (ProviderInfo<MessageBodyWriter<?>> ep : messageWriters) {
            if (matchesWriterCriterias(ep, type, genericType, annotations, mediaType, m)
                && handleMapper(ep, type, m, MessageBodyWriter.class, false)) {
                return (MessageBodyWriter<T>)ep.getProvider();
            }
        }
        return null;
    }
    
    protected void setBusProviders() {
        List<Object> extensions = new LinkedList<Object>(); 
        final String alreadySetProp = "bus.providers.set." + this.hashCode();
        if (bus.getProperty(alreadySetProp) == null) {
            addBusExtension(extensions,
                            MessageBodyReader.class,
                            MessageBodyWriter.class,
                            ExceptionMapper.class);
            if (!extensions.isEmpty()) {
                setProviders(true, true, extensions.toArray());
                bus.setProperty(alreadySetProp, "");
            }
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
    protected void setCommonProviders(List<ProviderInfo<? extends Object>> theProviders) {
        List<ProviderInfo<ReaderInterceptor>> readInts = 
            new LinkedList<ProviderInfo<ReaderInterceptor>>();
        List<ProviderInfo<WriterInterceptor>> writeInts = 
            new LinkedList<ProviderInfo<WriterInterceptor>>();
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(bus, provider.getProvider());
            
            if (MessageBodyReader.class.isAssignableFrom(providerCls)) {
                addProviderToList(messageReaders, provider);
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(providerCls)) {
                addProviderToList(messageWriters, provider);
            }
            
            if (ContextResolver.class.isAssignableFrom(providerCls)) {
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
            
            if (ParamConverterProvider.class.isAssignableFrom(providerCls)) {
                //TODO: review the possibility of ParamConverterProvider needing to have Contexts injected
                Object converter = provider.getProvider();
                if (newParamConverters == null) {
                    newParamConverters = new LinkedHashSet<ParamConverterProvider>();
                }
                newParamConverters.add((ParamConverterProvider)converter);
            }
        }
        sortReaders();
        sortWriters();
        sortContextResolvers();
        
        mapInterceptorFilters(readerInterceptors, readInts, ReaderInterceptor.class, true);
        mapInterceptorFilters(writerInterceptors, writeInts, WriterInterceptor.class, true);
        
        injectContextProxies(messageReaders, messageWriters, contextResolvers, 
            readerInterceptors.values(), writerInterceptors.values());
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
            InjectionUtils.injectContextProxiesAndApplication(pi, pi.getProvider(), app);
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
            Collections.sort(messageReaders, new MessageBodyReaderComparator());
        } else {
            doCustomSort(messageReaders);
        }
    }
    private <T> void sortWriters() {
        if (!customComparatorAvailable(MessageBodyWriter.class)) {
            Collections.sort(messageWriters, new MessageBodyWriterComparator());
        } else {
            doCustomSort(messageWriters);
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
                (Comparator<?>)(new ProviderInfoClassComparator((Comparator<Object>)theProviderComparator));
        }
        List<T> theProviders = (List<T>)listOfProviders;
        Comparator<? super T> theComparator = (Comparator<? super T>)theProviderComparator;
        Collections.sort((List<T>)theProviders, theComparator);
    }
    
    private void sortContextResolvers() {
        Collections.sort(contextResolvers, new ContextResolverComparator());
    }
    
        
    
    
    
    private <T> boolean matchesReaderCriterias(ProviderInfo<MessageBodyReader<?>> pi,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType,
                                               Message m) {
        MessageBodyReader<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType), supportedMediaTypes, false);

        if (availableMimeTypes.size() == 0) {
            return false;
        }
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
        }
        return ep.isReadable(type, genericType, annotations, mediaType);
    }
        
    private <T> boolean matchesWriterCriterias(ProviderInfo<MessageBodyWriter<?>> pi,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType,
                                               Message m) {
        MessageBodyWriter<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                          supportedMediaTypes, false);

        if (availableMimeTypes.size() == 0) {
            return false;
        }
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
    
    List<ProviderInfo<ContextResolver<?>>> getContextResolvers() {
        return Collections.unmodifiableList(contextResolvers);
    }
    
     
    public void registerUserProvider(Object provider) {
        setUserProviders(Collections.singletonList(provider));    
    }
    /**
     * Use for injection of entityProviders
     * @param entityProviders the entityProviders to set
     */
    public void setUserProviders(List<?> userProviders) {
        setProviders(true, false, userProviders.toArray());
    }

    private static class MessageBodyReaderComparator 
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
            return compareCustomStatus(p1, p2);
        }
    }
    
    private static class MessageBodyWriterComparator 
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
            return compareCustomStatus(p1, p2);
        }
    }
    
    private static int compareCustomStatus(ProviderInfo<?> p1, ProviderInfo<?> p2) {
        Boolean custom1 = p1.isCustom();
        Boolean custom2 = p2.isCustom();
        int result = custom1.compareTo(custom2) * -1;
        if (result == 0 && custom1) {
            Boolean busGlobal1 = p1.isBusGlobal();
            Boolean busGlobal2 = p2.isBusGlobal();
            result = busGlobal1.compareTo(busGlobal2);
        }
        return result;
    }
    
    private static class ContextResolverComparator 
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
            new MetadataMap<ProviderInfo<T>, String>();
        for (Map.Entry<NameKey, ProviderInfo<T>> entry : boundFilters.entrySet()) {
            String entryName = entry.getKey().getName();
            ProviderInfo<T> provider = entry.getValue();
            if (entryName.equals(DEFAULT_FILTER_NAME_BINDING)) {
                map.put(provider, Collections.<String>emptyList());
            } else {
                if (provider instanceof FilterProviderInfo) {
                    FilterProviderInfo<?> fpi = (FilterProviderInfo<?>)provider;
                    if (fpi.isDynamic() && !names.containsAll(fpi.getNameBinding())) {
                        continue;
                    }
                }
                map.add(provider, entryName);
            }
        }
        List<ProviderInfo<T>> list = new LinkedList<ProviderInfo<T>>();
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
        Set<Object> set = new HashSet<Object>();
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
        }
        return -1;
    }
    
    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass) {
        if (Object.class == cls) {
            return new Type[]{};
        }
        if (expectedClass != null) {
            Type genericSuperType = cls.getGenericSuperclass();
            if (genericSuperType instanceof ParameterizedType) {       
                Class<?> actualType = InjectionUtils.getActualType(genericSuperType);
                if (actualType != null && actualType.isAssignableFrom(expectedClass)) {
                    return new Type[]{genericSuperType};
                } else if (expectedClass.isAssignableFrom(actualType)) {
                    return new Type[]{};    
                }
            }
        }
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            return types;
        }
        return getGenericInterfaces(cls.getSuperclass(), expectedClass);
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
        public ContextResolverProxy(List<ContextResolver<T>> candidates) {
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
        Object instance = null;
        try {
            instance = c.newInstance(cArgs);
        } catch (Throwable ex) {
            throw new RuntimeException("Resource or provider class " + c.getDeclaringClass().getName()
                                       + " can not be instantiated"); 
        }
        Map<Class<?>, ThreadLocalProxy<?>> proxies = 
            new LinkedHashMap<Class<?>, ThreadLocalProxy<?>>();
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
        } else {
            return new ProviderInfo<Object>(instance, proxies, theBus, checkContexts, custom);
        }
    }
    
    protected static class NameKey { 
        private String name;
        private Integer priority;
        private Class<?> providerCls;
        public NameKey(String name, 
                       int priority,
                       Class<?> providerCls) {
            this.name = name;
            this.priority = priority;
            this.providerCls = providerCls;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getPriority() {
            return priority;
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
                map.put(new NameKey(name, priority, p.getClass()), p);
            }
        }
        
    }
    
    protected static Set<String> getFilterNameBindings(ProviderInfo<?> p) {
        Set<String> names = null;
        if (p instanceof FilterProviderInfo) {
            names = ((FilterProviderInfo<?>)p).getNameBinding();
        }
        if (names == null) {
            names = AnnotationUtils.getNameBindings(p.getProvider().getClass().getAnnotations());
        }
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

        public NameKeyComparator(boolean ascending) {
            super(ascending);
        }
        
        @Override
        public int compare(NameKey key1, NameKey key2) {
            int result = compare(key1.getPriority(), key2.getPriority());
            if (result != 0) {
                return result;
            }
            return compare(key1.hashCode(), key2.hashCode());
        }
        
    }
    
    protected static class NameKeyMap<T> extends TreeMap<NameKey, T> {
        private static final long serialVersionUID = -4352258671270502204L;

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
            new ArrayList<ProviderInfo<? extends Object>>(providers.length);
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
                ProviderInfo<Object> theProvider = new ProviderInfo<Object>(provider, getBus(), custom);
                theProvider.setBusGlobal(busGlobal);
                theProviders.add(theProvider);
            }
        }
        return theProviders;
    }
    
    public MessageBodyWriter<?> getRegisteredJaxbWriter() {
        for (ProviderInfo<MessageBodyWriter<?>> pi : this.messageWriters) {    
            Class<?> cls = pi.getProvider().getClass();
            if (cls.getName().equals(JAXB_PROVIDER_NAME)
                || cls.getSuperclass().getName().equals(JAXB_PROVIDER_NAME)) {
                return pi.getProvider();
            }
        }
        return null;
    }

    public void setProviderComparator(Comparator<?> providerComparator) {
        this.providerComparator = providerComparator;
        sortReaders();
        sortWriters();
    }
    
}
