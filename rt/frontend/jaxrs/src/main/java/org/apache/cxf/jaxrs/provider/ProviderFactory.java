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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.Produces;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.HttpHeaders;
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
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorMBR;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.ResourceInfoImpl;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.model.BeanParamInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public final class ProviderFactory {
    public static final String PROVIDER_SELECTION_PROPERTY_CHANGED = "provider.selection.property.changed";
    static final String IGNORE_TYPE_VARIABLES = "org.apache.cxf.jaxrs.providers.ignore.typevars";
    
    private static final Class<?>[] FILTER_INTERCEPTOR_CLASSES = 
        new Class<?>[] {ContainerRequestFilter.class,
                        ContainerResponseFilter.class,
                        ReaderInterceptor.class,
                        WriterInterceptor.class};
    
    private static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";
    private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);
    private static final ProviderFactory SHARED_FACTORY = getInstance();
    
    private static final String DEFAULT_FILTER_NAME_BINDING = "org.apache.cxf.filter.binding";
    private static final String JAXB_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.JAXBElementProvider";
    private static final String JSON_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.json.JSONProvider";
    private static final String BUS_PROVIDERS_ALL = "org.apache.cxf.jaxrs.bus.providers";
    
    static {
        SHARED_FACTORY.setProviders(new BinaryDataProvider<Object>(),
                                    new SourceProvider<Object>(),
                                    new DataSourceProvider<Object>(),
                                    new FormEncodingProvider<Object>(),
                                    new PrimitiveTextProvider<Object>(),
                                    new MultipartProvider(),
                                    new WebApplicationExceptionMapper(),
                                    new WadlGenerator());
        
    }
    
    private List<ProviderInfo<MessageBodyReader<?>>> messageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader<?>>>();
    private List<ProviderInfo<MessageBodyWriter<?>>> messageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter<?>>>();
    private List<ProviderInfo<ContextResolver<?>>> contextResolvers = 
        new ArrayList<ProviderInfo<ContextResolver<?>>>(1);
    private List<ProviderInfo<ContextProvider<?>>> contextProviders = 
        new ArrayList<ProviderInfo<ContextProvider<?>>>(1);
    
    private Set<ParamConverterProvider> newParamConverters;
    private LegacyParamConverterProvider legacyParamConverter; 
    
    private List<ProviderInfo<MessageBodyReader<?>>> jaxbReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader<?>>>();
    private List<ProviderInfo<MessageBodyWriter<?>>> jaxbWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter<?>>>();
    
    private List<ProviderInfo<ReaderInterceptor>> readerInterceptors = 
        new ArrayList<ProviderInfo<ReaderInterceptor>>(1);
    private List<ProviderInfo<WriterInterceptor>> writerInterceptors = 
        new ArrayList<ProviderInfo<WriterInterceptor>>(1);
    
    // Server specific providers
    private List<ProviderInfo<ExceptionMapper<?>>> exceptionMappers = 
        new ArrayList<ProviderInfo<ExceptionMapper<?>>>(1);
    
    // RequestHandler & ResponseHandler will have to be deprecated for 2.7.0
    private List<ProviderInfo<RequestHandler>> requestHandlers = 
        new ArrayList<ProviderInfo<RequestHandler>>(1);
    private List<ProviderInfo<ResponseHandler>> responseHandlers = 
        new ArrayList<ProviderInfo<ResponseHandler>>(1);
    
    // ContainerRequestFilter & ContainerResponseFilter are introduced in JAX-RS 2.0
    private List<ProviderInfo<ContainerRequestFilter>> preMatchContainerRequestFilters = 
        new ArrayList<ProviderInfo<ContainerRequestFilter>>(1);
    //TODO: consider using List as a value type for postmatching filters
    private Map<NameKey, ProviderInfo<ContainerRequestFilter>> postMatchContainerRequestFilters = 
        new LinkedHashMap<NameKey, ProviderInfo<ContainerRequestFilter>>();
    private Map<NameKey, ProviderInfo<ContainerResponseFilter>> postMatchContainerResponseFilters = 
        new LinkedHashMap<NameKey, ProviderInfo<ContainerResponseFilter>>();
    private RequestPreprocessor requestPreprocessor;
    private ProviderInfo<Application> application;
    private List<DynamicFeature> dynamicFeatures = new LinkedList<DynamicFeature>();
    
    // This may be better be kept at OperationResourceInfo ? Though we may have many methods
    // across different resources that use the same BeanParam. 
    private Map<Class<?>, BeanParamInfo> beanParams = new HashMap<Class<?>, BeanParamInfo>();
    
    // List of injected providers
    private Collection<ProviderInfo<?>> injectedProviders = 
        new LinkedList<ProviderInfo<?>>();
    
    
    //TODO: Client-only providers, consider introducing ClientProviderFactory,
    //      will make it easier to split the client API into a separate module
    private List<ProviderInfo<ClientRequestFilter>> clientRequestFilters = 
        new ArrayList<ProviderInfo<ClientRequestFilter>>(1);
    private List<ProviderInfo<ClientResponseFilter>> clientResponseFilters = 
        new ArrayList<ProviderInfo<ClientResponseFilter>>(1);
    private List<ProviderInfo<ResponseExceptionMapper<?>>> responseExceptionMappers = 
        new ArrayList<ProviderInfo<ResponseExceptionMapper<?>>>(1);
   
    private Bus bus;
    
    
    private ProviderFactory(Bus bus) {
        this.bus = bus;
        initJaxbProviders();
        setBusProviders();
    }
    
    public Bus getBus() {
        return bus;
    } 

    // Not ideal but in the end seems like the simplest option compared 
    // to adding default readers/writers to existing messageReaders/Writers 
    // (due to all sort of conflicts with custom providers) and cloning 
    // at the request time
    private void initJaxbProviders() {
        Object jaxbProvider = createProvider(JAXB_PROVIDER_NAME);
        if (jaxbProvider != null) {
            jaxbReaders.add(new ProviderInfo<MessageBodyReader<?>>((MessageBodyReader<?>)jaxbProvider, bus));
            jaxbWriters.add(new ProviderInfo<MessageBodyWriter<?>>((MessageBodyWriter<?>)jaxbProvider, bus));
        }
        Object jsonProvider = createProvider(JSON_PROVIDER_NAME);
        if (jsonProvider != null) {
            jaxbReaders.add(new ProviderInfo<MessageBodyReader<?>>((MessageBodyReader<?>)jsonProvider, bus));
            jaxbWriters.add(new ProviderInfo<MessageBodyWriter<?>>((MessageBodyWriter<?>)jsonProvider, bus));
        }
        injectContextProxies(jaxbReaders, jaxbWriters);
    }
    
    
    
    private static Object createProvider(String className) {
        
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
    
    public static ProviderFactory getInstance() {
        return new ProviderFactory(BusFactory.getThreadDefaultBus());
    }
    
    public static ProviderFactory createInstance(Bus bus) {
        return new ProviderFactory(bus);
    }
    
    public static ProviderFactory getInstance(Bus bus) {
        return (ProviderFactory)bus.getProperty(ProviderFactory.class.getName());
    }
    
    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().get(Endpoint.class);
        return (ProviderFactory)e.get(ProviderFactory.class.getName());
    }
    
    public static ProviderFactory getSharedInstance() {
        return SHARED_FACTORY;
    }
    
    public void addBeanParamInfo(BeanParamInfo bpi) {
        beanParams.put(bpi.getResourceClass(), bpi);
    }
    
    public BeanParamInfo getBeanParamInfo(Class<?> beanClass) {
        return beanParams.get(beanClass);
    }
    
    public <T> ContextResolver<T> createContextResolver(Type contextType, 
                                                        Message m) {
        boolean isRequestor = MessageUtils.isRequestor(m);
        Message requestMessage = isRequestor ? m.getExchange().getOutMessage() 
                                             : m.getExchange().getInMessage();
        HttpHeaders requestHeaders = new HttpHeadersImpl(requestMessage);
        MediaType mt = null;
        
        Message responseMessage = isRequestor ? m.getExchange().getInMessage() 
                                              : m.getExchange().getOutMessage();
        if (responseMessage != null) {
            Object ctProperty = responseMessage.get(Message.CONTENT_TYPE);
            if (ctProperty == null) {
                List<MediaType> accepts = requestHeaders.getAcceptableMediaTypes();
                if (accepts.size() > 0) {
                    mt = accepts.get(0);
                }
            } else {
                mt = JAXRSUtils.toMediaType(ctProperty.toString());
            }
        } else {
            mt = requestHeaders.getMediaType();
        }
        
        return createContextResolver(contextType, m,
               mt == null ? MediaType.WILDCARD_TYPE : mt);
        
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
    
    public <T extends Throwable> ExceptionMapper<T> createExceptionMapper(Class<?> exceptionType,
                                                                          Message m) {
        ExceptionMapper<T> mapper = doCreateExceptionMapper(exceptionType, m);
        if (mapper != null || this == SHARED_FACTORY) {
            return mapper;
        }
        
        return SHARED_FACTORY.createExceptionMapper(exceptionType, m);
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
    
    public <T> ParamConverter<T> createParameterHandler(Class<T> paramType, Annotation[] anns) {
        
        if (newParamConverters != null) {
            anns = anns != null ? anns : new Annotation[]{};
            
            for (ParamConverterProvider newParamConverter : newParamConverters) {
                ParamConverter<T> converter = newParamConverter.getConverter(paramType, paramType, anns);
                if (converter != null) {
                    return converter;
                }
            }
        } else if (legacyParamConverter != null) {
            return legacyParamConverter.getConverter(paramType, null, null);
        } 
        return null;
        
    }
        
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ResponseExceptionMapper<T> createResponseExceptionMapper(
                                 Message m, Class<?> paramType) {
        
        List<ResponseExceptionMapper<?>> candidates = new LinkedList<ResponseExceptionMapper<?>>();
        
        for (ProviderInfo<ResponseExceptionMapper<?>> em : responseExceptionMappers) {
            handleMapper(candidates, em, paramType, m, ResponseExceptionMapper.class, true);
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator(paramType));
        return (ResponseExceptionMapper<T>) candidates.get(0);
    }
    
    private static <T> void handleMapper(List<T> candidates, 
                                     ProviderInfo<T> em, 
                                     Class<?> expectedType, 
                                     Message m, 
                                     Class<?> providerClass,
                                     boolean injectContext) {
        
        Class<?> mapperClass = ClassHelper.getRealClass(em.getProvider());
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
                            if (cls != null && cls.isAssignableFrom(expectedType)) {
                                isResolved = true;
                                break;
                            }
                        }
                        if (!isResolved) {
                            return;
                        }
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        candidates.add(em.getProvider());
                        return;
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
                        candidates.add(em.getProvider());
                        return;
                    }
                }
            } else if (t instanceof Class && providerClass.isAssignableFrom((Class<?>)t)) {
                if (injectContext) {
                    injectContextValues(em, m);
                }
                candidates.add(em.getProvider());
            }
        }
    }
        
    
    public <T> List<ReaderInterceptor> createMessageBodyReaderInterceptor(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m) {
        MessageBodyReader<T> mr = createMessageBodyReader(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        if (mr != null) {
            ReaderInterceptor mbrReader = new ReaderInterceptorMBR(mr, m.getExchange().getInMessage());
            
            int size = readerInterceptors.size();
            List<ReaderInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<ReaderInterceptor>(size + 1);
                for (ProviderInfo<ReaderInterceptor> p : readerInterceptors) {
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
                                                                          Message m) {
        MessageBodyWriter<T> mw = createMessageBodyWriter(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        if (mw != null) {
            
            @SuppressWarnings({
                "unchecked", "rawtypes"
            })
            WriterInterceptor mbwWriter = new WriterInterceptorMBW((MessageBodyWriter)mw, m);
              
            int size = writerInterceptors.size();
            List<WriterInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<WriterInterceptor>(size + 1);
                for (ProviderInfo<WriterInterceptor> p : writerInterceptors) {
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
    
    
    
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Try user provided providers
        MessageBodyReader<T> mr = chooseMessageReader(messageReaders,
                                                      bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        if (mr == null) {
            mr = chooseMessageReader(jaxbReaders,
                                     bodyType,
                                     parameterType,
                                     parameterAnnotations,
                                     mediaType,
                                     m);
        }
        
        if (mr != null || SHARED_FACTORY == this) {
            return mr;
        }
        return SHARED_FACTORY.createMessageBodyReader(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
    }
    
    private boolean isJaxbBasedProvider(Object sharedProvider) {
        String clsName = sharedProvider.getClass().getName();
        return JAXB_PROVIDER_NAME.equals(clsName) || JSON_PROVIDER_NAME.equals(clsName);
    }
    
    public List<ProviderInfo<ContainerRequestFilter>> getPreMatchContainerRequestFilters() {
        return Collections.unmodifiableList(preMatchContainerRequestFilters);
    }
    
    public List<ProviderInfo<ContainerRequestFilter>> getPostMatchContainerRequestFilters(List<String> names) {
        return getPostMatchContainerFilters(postMatchContainerRequestFilters, 
                                            names);
    }
    
    public List<ProviderInfo<ContainerResponseFilter>> getContainerResponseFilters(List<String> names) {
        return getPostMatchContainerFilters(postMatchContainerResponseFilters, 
                                            names);
    }
    
    public List<ProviderInfo<ClientRequestFilter>> getClientRequestFilters() {
        return Collections.unmodifiableList(clientRequestFilters);
    }
    
    public List<ProviderInfo<ClientResponseFilter>> getClientResponseFilters() {
        return Collections.unmodifiableList(clientResponseFilters);
    }

    //TODO: Also sort based on BindingPriority
    private static <T> List<ProviderInfo<T>> getPostMatchContainerFilters(Map<NameKey, ProviderInfo<T>> boundFilters,
                                                                          List<String> names) {
        
        if (boundFilters.isEmpty()) {
            return Collections.emptyList();
        }
        names = names == null ? Collections.<String>emptyList() : names;
        
        MultivaluedMap<ProviderInfo<T>, String> map = 
            new MetadataMap<ProviderInfo<T>, String>();
        for (Map.Entry<NameKey, ProviderInfo<T>> entry : boundFilters.entrySet()) {
            String entryName = entry.getKey().getName();
            if (entryName.equals(DEFAULT_FILTER_NAME_BINDING)) {
                map.put(entry.getValue(), Collections.<String>emptyList());
            } else {
                if (entryName.endsWith(":dynamic") && !names.contains(entryName)) {
                    continue;
                }
                map.add(entry.getValue(), entryName);
            }
        }
        List<ProviderInfo<T>> list = new LinkedList<ProviderInfo<T>>();
        for (Map.Entry<ProviderInfo<T>, List<String>> entry : map.entrySet()) {
            if (names.containsAll(entry.getValue())) {
                list.add(entry.getKey());
            }
        }
        return list;
    }
    
    public List<ProviderInfo<RequestHandler>> getRequestHandlers() {
        List<ProviderInfo<RequestHandler>> handlers = null;
        if (requestHandlers.size() == 0) {
            handlers = SHARED_FACTORY.requestHandlers;
        } else {
            handlers = new ArrayList<ProviderInfo<RequestHandler>>();
            boolean customWADLHandler = false;
            for (int i = 0; i < requestHandlers.size(); i++) {
                if (requestHandlers.get(i).getProvider() instanceof WadlGenerator) {
                    customWADLHandler = true;
                    break;
                }
            }
            if (!customWADLHandler) {
                // TODO : this works only because we know we only have a single 
                // system handler which is a default WADLGenerator, think of a better approach
                handlers.addAll(SHARED_FACTORY.requestHandlers);    
            }
            handlers.addAll(requestHandlers);
            
        }
        return Collections.unmodifiableList(handlers);
    }
    
    public List<ProviderInfo<ResponseHandler>> getResponseHandlers() {
        return Collections.unmodifiableList(responseHandlers);
    }

    public <T> MessageBodyWriter<T> createMessageBodyWriter(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Try user provided providers
        MessageBodyWriter<T> mw = chooseMessageWriter(messageWriters, 
                                                      bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        if (mw == null) {
            mw = chooseMessageWriter(jaxbWriters, 
                                     bodyType,
                                     parameterType,
                                     parameterAnnotations,
                                     mediaType,
                                     m);
        }
        
        if (mw != null || SHARED_FACTORY == this) {
            return mw;
        }
        
        return SHARED_FACTORY.createMessageBodyWriter(bodyType,
                                                  parameterType,
                                                  parameterAnnotations,
                                                  mediaType,
                                                  m);
    }
    
    private void setBusProviders() {
        List<Object> extensions = new LinkedList<Object>(); 
        final String alreadySetProp = "bus.providers.set" + this.hashCode();
        if (bus.getProperty(alreadySetProp) == null) {
            addBusExtension(extensions,
                            MessageBodyReader.class,
                            MessageBodyWriter.class,
                            ExceptionMapper.class);
            if (!extensions.isEmpty()) {
                setProviders(extensions.toArray());
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
    
//CHECKSTYLE:OFF       
    private void setProviders(Object... providers) {
        
        List<ProviderInfo<ContainerRequestFilter>> postMatchRequestFilters = 
            new LinkedList<ProviderInfo<ContainerRequestFilter>>();
        List<ProviderInfo<ContainerResponseFilter>> postMatchResponseFilters = 
            new LinkedList<ProviderInfo<ContainerResponseFilter>>();
        
        for (Object o : providers) {
            if (o == null) {
                continue;
            }
            Class<?> oClass = ClassHelper.getRealClass(o);
            
            if (MessageBodyReader.class.isAssignableFrom(oClass)) {
                messageReaders.add(new ProviderInfo<MessageBodyReader<?>>((MessageBodyReader<?>)o, bus)); 
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(oClass)) {
                messageWriters.add(new ProviderInfo<MessageBodyWriter<?>>((MessageBodyWriter<?>)o, bus)); 
            }
            
            if (ContextResolver.class.isAssignableFrom(oClass)) {
                contextResolvers.add(new ProviderInfo<ContextResolver<?>>((ContextResolver<?>)o, bus)); 
            }
            
            if (ContextProvider.class.isAssignableFrom(oClass)) {
                contextProviders.add(new ProviderInfo<ContextProvider<?>>((ContextProvider<?>)o, bus)); 
            }
            
            if (RequestHandler.class.isAssignableFrom(oClass)) {
                requestHandlers.add(new ProviderInfo<RequestHandler>((RequestHandler)o, bus)); 
            }
            
            if (ResponseHandler.class.isAssignableFrom(oClass)) {
                responseHandlers.add(new ProviderInfo<ResponseHandler>((ResponseHandler)o, bus)); 
            }
            
            if (ContainerRequestFilter.class.isAssignableFrom(oClass)) {
                addContainerFilter(postMatchRequestFilters,
                   new ProviderInfo<ContainerRequestFilter>((ContainerRequestFilter)o, bus),
                   preMatchContainerRequestFilters);
            }
            
            if (ContainerResponseFilter.class.isAssignableFrom(oClass)) {
                addContainerFilter(postMatchResponseFilters,
                   new ProviderInfo<ContainerResponseFilter>((ContainerResponseFilter)o, bus),
                   null); 
            }
            
            if (ReaderInterceptor.class.isAssignableFrom(oClass)) {
                readerInterceptors.add(
                   new ProviderInfo<ReaderInterceptor>((ReaderInterceptor)o, bus));
            }
            
            if (WriterInterceptor.class.isAssignableFrom(oClass)) {
                writerInterceptors.add(
                   new ProviderInfo<WriterInterceptor>((WriterInterceptor)o, bus));
            }
            
            if (DynamicFeature.class.isAssignableFrom(oClass)) {
                dynamicFeatures.add((DynamicFeature)o);
            }
            
            if (ClientRequestFilter.class.isAssignableFrom(oClass)) {
                clientRequestFilters.add(
                   new ProviderInfo<ClientRequestFilter>((ClientRequestFilter)o, bus));
            }
            
            if (ClientResponseFilter.class.isAssignableFrom(oClass)) {
                clientResponseFilters.add(
                   new ProviderInfo<ClientResponseFilter>((ClientResponseFilter)o, bus));
            }
            
            if (ExceptionMapper.class.isAssignableFrom(oClass)) {
                exceptionMappers.add(new ProviderInfo<ExceptionMapper<?>>((ExceptionMapper<?>)o, bus)); 
            }
            
            if (ResponseExceptionMapper.class.isAssignableFrom(oClass)) {
                responseExceptionMappers.add(new ProviderInfo<ResponseExceptionMapper<?>>((ResponseExceptionMapper<?>)o, bus)); 
            }
            
            if (ParamConverterProvider.class.isAssignableFrom(oClass)) {
                if (newParamConverters == null) {
                    newParamConverters = new LinkedHashSet<ParamConverterProvider>();
                }
                newParamConverters.add((ParamConverterProvider)o);
            }
            
            if (ParameterHandler.class.isAssignableFrom(oClass)) {
                if (legacyParamConverter == null) {
                    legacyParamConverter = new LegacyParamConverterProvider();
                }
                legacyParamConverter.add(o, bus);
            }
            
            
        }
        sortReaders();
        sortWriters();
        sortContextResolvers();
        
        Collections.sort(preMatchContainerRequestFilters, new BindingPriorityComparator(true));
        mapContainerFilters(postMatchContainerRequestFilters, postMatchRequestFilters, true);
        mapContainerFilters(postMatchContainerResponseFilters, postMatchResponseFilters, false);
        Collections.sort(readerInterceptors, new BindingPriorityComparator(true));
        Collections.sort(writerInterceptors, new BindingPriorityComparator(false));
        
        Collections.sort(clientRequestFilters, new BindingPriorityComparator(true));
        Collections.sort(clientResponseFilters, new BindingPriorityComparator(false));
        
        injectContextProxies(messageReaders, messageWriters, contextResolvers, 
            requestHandlers, responseHandlers, exceptionMappers,
            postMatchContainerRequestFilters.values(), preMatchContainerRequestFilters,
            postMatchContainerResponseFilters.values(),
            responseExceptionMappers, clientRequestFilters, clientResponseFilters,
            readerInterceptors, writerInterceptors);
    }
//CHECKSTYLE:ON
    
    private static <T> void mapContainerFilters(Map<NameKey, ProviderInfo<T>> map,
                                                List<ProviderInfo<T>> postMatchFilters,
                                                boolean ascending) {
        
        Collections.sort(postMatchFilters, new PostMatchFilterComparator(ascending));
        for (ProviderInfo<T> p : postMatchFilters) { 
            List<String> names = AnnotationUtils.getNameBindings(
                p.getProvider().getClass().getAnnotations());
            names = names.isEmpty() ? Collections.singletonList(DEFAULT_FILTER_NAME_BINDING) : names;
            for (String name : names) {
                map.put(new NameKey(name, AnnotationUtils.getBindingPriority(p.getProvider().getClass())), 
                        p);
            }
        }
        
    }
    
    private static <T> void addContainerFilter(List<ProviderInfo<T>> postMatchFilters,
                                               ProviderInfo<T> p,
                                               List<ProviderInfo<T>> preMatchFilters) {
        T filter = p.getProvider();
        if (preMatchFilters != null && isPrematching(filter.getClass())) {
            preMatchFilters.add(p);
        } else {
            postMatchFilters.add(p);
        }
        
    }
    
    private static boolean isPrematching(Class<?> filterCls) {
        return AnnotationUtils.getAnnotation(filterCls.getAnnotations(), 
                                      PreMatching.class) != null;
    }
    
    static void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
        }
    }
    
    void injectContextProxies(Collection<?> ... providerLists) {
        for (Collection<?> list : providerLists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                if (ProviderFactory.SHARED_FACTORY == this && isJaxbBasedProvider(pi.getProvider())) {
                    continue;
                }
                injectContextProxiesIntoProvider(pi);
            }
        }
    }
    
    void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        if (pi.contextsAvailable()) {
            InjectionUtils.injectContextProxies(pi, pi.getProvider());
            synchronized (injectedProviders) {
                injectedProviders.add(pi);
            }
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
        Collections.sort(messageReaders, new MessageBodyReaderComparator());
    }
    
    private void sortWriters() {
        Collections.sort(messageWriters, new MessageBodyWriterComparator());
    }
    
    private void sortContextResolvers() {
        Collections.sort(contextResolvers, new ContextResolverComparator());
    }
    
        
    
    /**
     * Choose the first body reader provider that matches the requestedMimeType 
     * for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param messageBodyReaders
     * @param type
     * @param requestedMimeType
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> MessageBodyReader<T> chooseMessageReader(List<ProviderInfo<MessageBodyReader<?>>> readers,
                                                         Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        List<MessageBodyReader<?>> candidates = new LinkedList<MessageBodyReader<?>>();
        for (ProviderInfo<MessageBodyReader<?>> ep : readers) {
            if (matchesReaderCriterias(ep, type, genericType, annotations, mediaType, m)) {
                if (this == SHARED_FACTORY) {
                    return (MessageBodyReader<T>) ep.getProvider();
                }
                handleMapper(candidates, ep, type, m, MessageBodyReader.class, false);
            }
        }     
        
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return (MessageBodyReader<T>) candidates.get(0);
        
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
        if (this != SHARED_FACTORY || !isJaxbBasedProvider(ep)) {
            injectContextValues(pi, m);
        }
        return ep.isReadable(type, genericType, annotations, mediaType);
    }
        
    /**
     * Choose the first body writer provider that matches the requestedMimeType 
     * for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param messageBodyWriters
     * @param type
     * @param requestedMimeType
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> MessageBodyWriter<T> chooseMessageWriter(List<ProviderInfo<MessageBodyWriter<?>>> writers,
                                                         Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        List<MessageBodyWriter<?>> candidates = new LinkedList<MessageBodyWriter<?>>();
        for (ProviderInfo<MessageBodyWriter<?>> ep : writers) {
            if (matchesWriterCriterias(ep, type, genericType, annotations, mediaType, m)) {
                if (this == SHARED_FACTORY) {
                    return (MessageBodyWriter<T>) ep.getProvider();
                }
                handleMapper(candidates, ep, type, m, MessageBodyWriter.class, false);
            }
        }     
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return (MessageBodyWriter<T>) candidates.get(0);
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
        if ((this != SHARED_FACTORY || !isJaxbBasedProvider(ep))
            && m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
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
        setProviders(userProviders.toArray());
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
    
            return JAXRSUtils.compareSortedMediaTypes(types1, types2, null);
        }
    }
    
    private static class MessageBodyWriterComparator 
        implements Comparator<ProviderInfo<MessageBodyWriter<?>>> {
        
        public int compare(ProviderInfo<MessageBodyWriter<?>> p1, 
                           ProviderInfo<MessageBodyWriter<?>> p2) {
            MessageBodyWriter<?> e1 = p1.getProvider();
            MessageBodyWriter<?> e2 = p2.getProvider();
            
            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e1), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e2), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
    
            return JAXRSUtils.compareSortedMediaTypes(types1, types2, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
        }
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
    
    public void setApplicationProvider(ProviderInfo<Application> app) {
        application = app;
    }
    
    public void setRequestPreprocessor(RequestPreprocessor rp) {
        this.requestPreprocessor = rp;
    }
    
    public RequestPreprocessor getRequestPreprocessor() {
        return requestPreprocessor;
    }
    
    public void clearExceptionMapperProxies() {
        clearProxies(exceptionMappers);
    }
    
    public void clearThreadLocalProxies() {
        clearProxies(injectedProviders);
        
        if (application != null) {
            application.clearThreadLocalProxies();
        }
        if (this != SHARED_FACTORY) {
            SHARED_FACTORY.clearThreadLocalProxies();
        }
    }
    
    void clearProxies(Collection<?> ...lists) {
        for (Collection<?> list : lists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            synchronized (l2) {
                for (ProviderInfo<?> pi : l2) {
                    pi.clearThreadLocalProxies();
                }
            }
        }
    }
    
    public void clearProviders() {
        messageReaders.clear();
        messageWriters.clear();
        contextResolvers.clear();
        contextProviders.clear();
        exceptionMappers.clear();
        requestHandlers.clear();
        responseHandlers.clear();
        postMatchContainerRequestFilters.clear();
        postMatchContainerResponseFilters.clear();
        preMatchContainerRequestFilters.clear();
        if (legacyParamConverter != null) {
            legacyParamConverter.clear();
        }
        responseExceptionMappers.clear();
        clientRequestFilters.clear();
        clientResponseFilters.clear();
    }
    
    public void setBus(Bus bus) {
        if (bus == null) {
            return;
        }
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            injectProviderProperty(r.getProvider(), "setBus", Bus.class, bus);
        }
    }
    
    public void applyDynamicFeatures(List<ClassResourceInfo> list) {
        if (dynamicFeatures.size() > 0) {
            for (ClassResourceInfo cri : list) {
                doApplyDynamicFeatures(cri);
            }
        }
    }
    
    private void doApplyDynamicFeatures(ClassResourceInfo cri) {
        Set<OperationResourceInfo> oris = cri.getMethodDispatcher().getOperationResourceInfos();
        for (OperationResourceInfo ori : oris) {
            for (DynamicFeature feature : dynamicFeatures) {
                Configurable methodConfigurable = new MethodConfigurable(ori);
                feature.configure(new ResourceInfoImpl(ori), methodConfigurable);
            }
        }
        Collection<ClassResourceInfo> subs = cri.getSubResources();
        for (ClassResourceInfo sub : subs) {
            if (sub != cri) {
                doApplyDynamicFeatures(sub);    
            }
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
        boolean schemasMethodAvailable = false;
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            schemasMethodAvailable = 
                injectProviderProperty(r.getProvider(), "setSchemaLocations", List.class, schemas);
        }
        if (!schemasMethodAvailable) {
            for (ProviderInfo<MessageBodyReader<?>> r : jaxbReaders) {
                injectProviderProperty(r.getProvider(), "setSchemaLocations", List.class, schemas);
            }
        }
    }
    public void initProviders(List<ClassResourceInfo> cris) {
        Set<Object> set = getReadersWriters();
        for (Object o : set) {
            Object provider = ((ProviderInfo<?>)o).getProvider();
            if (provider instanceof AbstractConfigurableProvider) {
                ((AbstractConfigurableProvider)provider).init(cris);
            }
        }
        if (this != SHARED_FACTORY) {
            SHARED_FACTORY.initProviders(cris);
        }
    }
    
    Set<Object> getReadersWriters() {
        Set<Object> set = new HashSet<Object>();
        set.addAll(messageReaders);
        set.addAll(jaxbReaders);
        set.addAll(messageWriters);
        set.addAll(jaxbWriters);
        return set;
    }
    
    private static class ClassComparator implements 
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
    
    private static class PostMatchFilterComparator extends BindingPriorityComparator {
        public PostMatchFilterComparator(boolean ascending) {
            super(ascending);
        }
        
        @Override
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            int result = super.compare(p1, p2);
            if (result == 0) {
                Integer namesSize1 = 
                    AnnotationUtils.getNameBindings(p1.getProvider().getClass().getAnnotations()).size();
                Integer namesSize2 = 
                    AnnotationUtils.getNameBindings(p2.getProvider().getClass().getAnnotations()).size();
                
                // if we have two filters with the same binding priority, 
                // then put a filter with more name bindings upfront 
                // (this effectively puts name bound filters before global ones)
                result = namesSize1.compareTo(namesSize2) * -1;
            }
            return result; 
        }
    }
    
    private static class BindingPriorityComparator extends AbstactBindingPriorityComparator {
        public BindingPriorityComparator(boolean ascending) {
            super(ascending);
        }
    }
    
    private abstract static class AbstactBindingPriorityComparator implements 
        Comparator<ProviderInfo<?>> {
    
        private boolean ascending; 
        
        protected AbstactBindingPriorityComparator(boolean ascending) {
            this.ascending = ascending; 
        }
        
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            Integer b1Value = getBindingPriorityValue(p1);
            Integer b2Value = getBindingPriorityValue(p2);
            
            int result = b1Value.compareTo(b2Value);
            return ascending ? result : result * -1;      
        }
        
        private int getBindingPriorityValue(ProviderInfo<?> p) {
            return AnnotationUtils.getBindingPriority(p.getProvider().getClass());
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
    
    private static class NameKey { 
        private String name;
        private int bindingPriority;
        public NameKey(String name, int priority) {
            this.name = name;
            this.bindingPriority = priority;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPriority() {
            return bindingPriority;
        }
    }
    
    private class MethodConfigurable implements Configurable {
        
        private OperationResourceInfo ori;
        private String nameBinding;
        private boolean bindingSet;
        
        public MethodConfigurable(OperationResourceInfo ori) {
            this.ori = ori;
            nameBinding = DEFAULT_FILTER_NAME_BINDING 
                + ori.getClassResourceInfo().getServiceClass().getName()
                + "."
                + ori.getMethodToInvoke().toString()
                + ":dynamic";
        }
        
        @Override
        public Configurable register(Object provider) {
            return register(provider, AnnotationUtils.getBindingPriority(provider.getClass()));
        }

        @Override
        public Configurable register(Object provider, int bindingPriority) {
            return doRegister(provider, bindingPriority, FILTER_INTERCEPTOR_CLASSES);
        }
        
        @Override
        public <T> Configurable register(Object provider, Class<? super T>... contracts) {
            return register(provider, BindingPriority.USER, contracts);
        }
        
        @Override
        public <T> Configurable register(Object provider, int bindingPriority, Class<? super T>... contracts) {
            return doRegister(provider, bindingPriority, contracts);
        }
        
        @Override
        public Configurable register(Class<?> providerClass) {
            return register(providerClass, AnnotationUtils.getBindingPriority(providerClass));
        }

        @Override
        public Configurable register(Class<?> providerClass, int bindingPriority) {
            return doRegister(createProvider(providerClass), bindingPriority, 
                              FILTER_INTERCEPTOR_CLASSES);
        }

        @Override
        public <T> Configurable register(Class<T> providerClass, Class<? super T>... contracts) {
            return register(providerClass, BindingPriority.USER, contracts);
        }

        @Override
        public <T> Configurable register(Class<T> providerClass, int bindingPriority,
                                         Class<? super T>... contracts) {
            return doRegister(createProvider(providerClass), bindingPriority, contracts);
        }
        
        private Configurable doRegister(Object provider, int bindingPriority, Class<?>... contracts) {
        
            if (provider instanceof Feature) {
                ((Feature)provider).configure(this);
                return this;
            }
            
            boolean setIsNeeded = false;
            for (Class<?> contract : contracts) {
                if (contract == ContainerRequestFilter.class && provider instanceof ContainerRequestFilter) {
                    if (isPrematching(provider.getClass())) {
                        addToInterceptors(preMatchContainerRequestFilters, provider, bindingPriority, true);
                    } else {
                        postMatchContainerRequestFilters = 
                            addToPostMatching(postMatchContainerRequestFilters, provider, bindingPriority, true);
                        setIsNeeded = true;
                    }
                }
                if (contract == ContainerResponseFilter.class && provider instanceof ContainerResponseFilter) {
                    postMatchContainerResponseFilters = 
                        addToPostMatching(postMatchContainerResponseFilters, provider, bindingPriority, false);
                    setIsNeeded = true;    
                }
                if (contract == ReaderInterceptor.class && provider instanceof ReaderInterceptor) {
                    addToInterceptors(readerInterceptors, provider, bindingPriority, true);
                }
                if (contract == WriterInterceptor.class && provider instanceof WriterInterceptor) {
                    addToInterceptors(writerInterceptors, provider, bindingPriority, false);
                }
            }
            
            if (setIsNeeded && !bindingSet) {
                ori.addNameBindings(Collections.singletonList(nameBinding));
                bindingSet = true;
            }

            return this;
        }
        
        @SuppressWarnings("unchecked")
        private <T> void addToInterceptors(List<ProviderInfo<T>> providers, Object provider, 
                                           int priority, boolean asc) {
            int size = providers.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    int providerPriority = AnnotationUtils.getBindingPriority(
                        providers.get(i).getProvider().getClass());
                    if (asc) {
                        if (priority < providerPriority || i + 1 == size) {
                            int index = priority < providerPriority ? i : i + 1;
                            providers.add(index, (ProviderInfo<T>)newProvider(provider));
                            break;
                        }
                    } else if (priority > providerPriority || i + 1 == size) {
                        int index = priority > providerPriority ? i : i + 1; 
                        providers.add(index, (ProviderInfo<T>)newProvider(provider));
                        break;
                    }
                }
            } else {
                providers.add((ProviderInfo<T>)newProvider(provider));
            }
        }
        
        private <T> ProviderInfo<T> newProvider(T provider) {
            ProviderInfo<T> newProvider = new ProviderInfo<T>(provider, bus);
            injectContextProxiesIntoProvider(newProvider);
            return newProvider;
        }
        
        @SuppressWarnings("unchecked")
        private <T> Map<NameKey, ProviderInfo<T>> addToPostMatching(
            Map<NameKey, ProviderInfo<T>> map, Object provider, int priority, boolean asc) {
            Map<NameKey, ProviderInfo<T>> newMap = new LinkedHashMap<NameKey, ProviderInfo<T>>();
            
            Iterator<Map.Entry<NameKey, ProviderInfo<T>>> it = map.entrySet().iterator();
            if (it.hasNext()) {
                boolean added = false;
                while (it.hasNext()) {
                    Map.Entry<NameKey, ProviderInfo<T>> entry = it.next();
                    int providerPriority = entry.getKey().getPriority();
                    // this surely can be collapsed further
                    if (!added && asc && (priority < providerPriority || !it.hasNext())) {
                        addNewProvider(newMap, entry, provider, priority, providerPriority >= priority);
                        added = true;
                    } else if (!added && !asc && (priority > providerPriority || !it.hasNext())) {
                        addNewProvider(newMap, entry, provider, priority, priority > providerPriority);
                        added = true;
                    } else {
                        newMap.put(entry.getKey(), entry.getValue());
                    }   
                }
            } else {
                newMap.put(new NameKey(nameBinding, priority), (ProviderInfo<T>)newProvider(provider));
            }
            return newMap;
            
                
        }
        
        @SuppressWarnings("unchecked")
        private <T> void addNewProvider(Map<NameKey, ProviderInfo<T>> newMap, 
                                        Map.Entry<NameKey, ProviderInfo<T>> entry,
                                        Object provider, 
                                        int priority,
                                        boolean first) {
            if (first) {
                newMap.put(new NameKey(nameBinding, priority), (ProviderInfo<T>)newProvider(provider));
                newMap.put(entry.getKey(), entry.getValue());
            } else {
                newMap.put(entry.getKey(), entry.getValue());
                newMap.put(new NameKey(nameBinding, priority), (ProviderInfo<T>)newProvider(provider));
            }
        }
        
        @Override
        public Collection<Feature> getFeatures() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<Class<?>> getProviderClasses() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set<Object> getProviderInstances() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, Object> getProperties() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getProperty(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Configurable setProperties(Map<String, ?> properties) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Configurable setProperty(String name, Object value) {
            // TODO Auto-generated method stub
            return null;
        }
        
        private Object createProvider(Class<?> cls) {
            try {
                return cls.newInstance();
            } catch (Throwable ex) {
                throw new RuntimeException(ex); 
            }
        }
    }
    
    private static class LegacyParamConverterProvider implements ParamConverterProvider {

        // ParamConverter and ParamConverterProvider is introduced in JAX-RS 2.0
        // ParameterHandler will have to be deprecated
        private List<ProviderInfo<ParameterHandler<?>>> paramHandlers = 
            new ArrayList<ProviderInfo<ParameterHandler<?>>>(1);
        
        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            List<ParameterHandler<?>> candidates = new LinkedList<ParameterHandler<?>>();
            
            for (ProviderInfo<ParameterHandler<?>> em : paramHandlers) {
                handleMapper(candidates, em, rawType, null, ParameterHandler.class, true);
            }
            if (candidates.size() == 0) {
                return null;
            }
            Collections.sort(candidates, new ClassComparator());
            return new LegacyParamConverter((ParameterHandler<T>) candidates.get(0));
        }
        
        public void clear() {
            paramHandlers.clear();
        }
        
        public void add(Object o, Bus bus) {
            paramHandlers.add(new ProviderInfo<ParameterHandler<?>>((ParameterHandler<?>)o, bus));
        }
    }
    
    static class LegacyParamConverter<T> implements ParamConverter<T> {

        private ParameterHandler<T> handler;
        public LegacyParamConverter(ParameterHandler<T> handler) {
            this.handler = handler;
        }
        
        @Override
        public T fromString(String value) throws IllegalArgumentException {
            return handler.fromString(value);
        }

        @Override
        public String toString(Object value) throws IllegalArgumentException {
            // TODO Auto-generated method stub
            return null;
        }
        
        ParameterHandler<T> getHandler() {
            return handler;
        }
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
}
