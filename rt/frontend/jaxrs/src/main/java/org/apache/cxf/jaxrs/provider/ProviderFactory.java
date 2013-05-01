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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorMBR;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public abstract class ProviderFactory {
    protected static final String DEFAULT_FILTER_NAME_BINDING = "org.apache.cxf.filter.binding";
    protected static final String SERVER_FACTORY_NAME = "org.apache.cxf.jaxrs.provider.ServerProviderFactory";
    protected static final String CLIENT_FACTORY_NAME = "org.apache.cxf.jaxrs.client.ClientProviderFactory";
    
    private static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";
    private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);
    
    private static final String JAXB_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.JAXBElementProvider";
    private static final String JSON_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.json.JSONProvider";
    private static final String BUS_PROVIDERS_ALL = "org.apache.cxf.jaxrs.bus.providers";
    
    protected List<ProviderInfo<ReaderInterceptor>> readerInterceptors = 
        new ArrayList<ProviderInfo<ReaderInterceptor>>(1);
    protected List<ProviderInfo<WriterInterceptor>> writerInterceptors = 
        new ArrayList<ProviderInfo<WriterInterceptor>>(1);
    
    private List<ProviderInfo<MessageBodyReader<?>>> messageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader<?>>>();
    private List<ProviderInfo<MessageBodyWriter<?>>> messageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter<?>>>();
    private List<ProviderInfo<ContextResolver<?>>> contextResolvers = 
        new ArrayList<ProviderInfo<ContextResolver<?>>>(1);
    private List<ProviderInfo<ContextProvider<?>>> contextProviders = 
        new ArrayList<ProviderInfo<ContextProvider<?>>>(1);
    
    private ParamConverterProvider newParamConverter;
    
    // List of injected providers
    private Collection<ProviderInfo<?>> injectedProviders = 
        new LinkedList<ProviderInfo<?>>();
    
    private Bus bus;
    
    private ProviderFactory baseFactory;
    
    protected ProviderFactory(ProviderFactory baseFactory, Bus bus) {
        this.baseFactory = baseFactory;
        this.bus = bus;
    }
    
    protected Bus getBus() {
        return bus;
    }
    
    protected ProviderFactory getBaseFactory() {
        return baseFactory;
    }
    
    protected boolean isBaseFactory() {
        return baseFactory == null;
    }
    
    protected static void initBaseFactory(ProviderFactory factory) {
        factory.setProviders(new BinaryDataProvider<Object>(),
                                    new SourceProvider<Object>(),
                                    new DataSourceProvider<Object>(),
                                    new FormEncodingProvider<Object>(),
                                    new PrimitiveTextProvider<Object>(),
                                    createProvider(JAXB_PROVIDER_NAME),
                                    createProvider(JSON_PROVIDER_NAME),
                                    new MultipartProvider());
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
    
    public <T> ParamConverter<T> createParameterHandler(Class<T> paramType) {
        
        if (newParamConverter != null) {
            return newParamConverter.getConverter(paramType, null, null);
        } else {
            return null;
        }
        
    }
    
    protected static <T> void handleMapper(List<T> candidates, 
                                     ProviderInfo<T> em, 
                                     Class<?> expectedType, 
                                     Message m, 
                                     Class<?> providerClass,
                                     boolean injectContext) {
        
        Class<?> mapperClass =  ClassHelper.getRealClass(em.getProvider());
        Type[] types = getGenericInterfaces(mapperClass);
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
                    if (actualClass.isAssignableFrom(expectedType)) {
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        candidates.add(em.getProvider());
                        return;
                    }
                }
            } else if (t instanceof Class && ((Class<?>)t).isAssignableFrom(providerClass)) {
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
        
        if (mr != null || isBaseFactory()) {
            return mr;
        }
        return baseFactory.createMessageBodyReader(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
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
        
        
        if (mw != null || isBaseFactory()) {
            return mw;
        }
        
        return baseFactory.createMessageBodyWriter(bodyType,
                                                  parameterType,
                                                  parameterAnnotations,
                                                  mediaType,
                                                  m);
    }
    
    protected void setBusProviders() {
        List<Object> extensions = new LinkedList<Object>(); 
        final String alreadySetProp = "bus.providers.set";
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
    @SuppressWarnings("unchecked")
    protected void setProviders(Object... providers) {
        
        for (Object o : providers) {
            if (o == null) {
                continue;
            }
            
            ProviderInfo<? extends Object> provider = null;
            Class<?> providerCls = null;
            Object realObject = null;            
            if (o instanceof ProviderInfo) {
                provider = (ProviderInfo<? extends Object>)o;
                providerCls = provider.getProvider().getClass();
                realObject = provider;
            } else {
                providerCls = ClassHelper.getRealClass(o);
                provider = new ProviderInfo<Object>(o, getBus());
                realObject = o;
            }
            
            
            if (MessageBodyReader.class.isAssignableFrom(providerCls)) {
                messageReaders.add((ProviderInfo<MessageBodyReader<?>>)provider); 
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(providerCls)) {
                messageWriters.add((ProviderInfo<MessageBodyWriter<?>>)provider); 
            }
            
            if (ContextResolver.class.isAssignableFrom(providerCls)) {
                contextResolvers.add((ProviderInfo<ContextResolver<?>>)provider); 
            }
            
            if (ContextProvider.class.isAssignableFrom(providerCls)) {
                contextProviders.add((ProviderInfo<ContextProvider<?>>)provider); 
            }
            
            if (ReaderInterceptor.class.isAssignableFrom(providerCls)) {
                readerInterceptors.add((ProviderInfo<ReaderInterceptor>)provider);
            }
            
            if (WriterInterceptor.class.isAssignableFrom(providerCls)) {
                writerInterceptors.add((ProviderInfo<WriterInterceptor>)provider);
            }
            
            if (ParamConverterProvider.class.isAssignableFrom(providerCls)) {
                //TODO: review the possibility of ParamConverterProvider needing to have Contexts injected
                Object converter = realObject == provider ? provider.getProvider() : realObject;
                newParamConverter = (ParamConverterProvider)converter;
            }
        }
        sortReaders();
        sortWriters();
        sortContextResolvers();
        
        Collections.sort(readerInterceptors, new BindingPriorityComparator(true));
        Collections.sort(writerInterceptors, new BindingPriorityComparator(false));
        
        injectContextProxies(messageReaders, messageWriters, contextResolvers, 
            readerInterceptors, writerInterceptors);
    }
    //CHECKSTYLE:ON
    
    static void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
        }
    }
    
    protected void injectContextProxies(Collection<?> ... providerLists) {
        for (Collection<?> list : providerLists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                injectContextProxiesIntoProvider(pi);
            }
        }
    }
    
    void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        if (pi.contextsAvailable()) {
            InjectionUtils.injectContextProxies(pi, pi.getProvider());
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
                if (isBaseFactory()) {
                    return (MessageBodyReader<T>) ep.getProvider();
                }
                handleMapper(candidates, ep, type, m, MessageBodyReader.class, false);
                if (!candidates.isEmpty()) {
                    break;
                }
            }
        }     
        
        if (candidates.isEmpty()) {
            return null;
        }
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
        boolean injected = false;
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
            injected = true;
        }
        boolean matches = ep.isReadable(type, genericType, annotations, mediaType);
        if (!matches && injected) {
            pi.clearThreadLocalProxies();
        }
        return matches;
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
                if (isBaseFactory()) {
                    return (MessageBodyWriter<T>) ep.getProvider();
                }
                handleMapper(candidates, ep, type, m, MessageBodyWriter.class, false);
                if (!candidates.isEmpty()) {
                    break;
                }
            }
        }     
        if (candidates.isEmpty()) {
            return null;
        }
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
        boolean injected = false;
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
            injected = true;
        }
        boolean matches = ep.isWriteable(type, genericType, annotations, mediaType);
        if (!matches && injected) {
            pi.clearThreadLocalProxies();
        }
        return matches;
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
            
            int result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
            
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
            
            int result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
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
    
    public void clearThreadLocalProxies() {
        clearProxies(injectedProviders);
        if (baseFactory != null) {
            baseFactory.clearThreadLocalProxies();
        }
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
        boolean schemasMethodAvailable = false;
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            schemasMethodAvailable = 
                injectProviderProperty(r.getProvider(), "setSchemaLocations", List.class, schemas);
        }
        if (!schemasMethodAvailable) {
            setProviders(createProvider(JAXB_PROVIDER_NAME),
                         createProvider(JSON_PROVIDER_NAME));
            for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
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
        if (!isBaseFactory()) {
            baseFactory.initProviders(cris);
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
    
        public int compare(Object em1, Object em2) {
            return compareClasses(em1, em2);
        }
    }
    
    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().get(Endpoint.class);
        
        Message outM = m.getExchange().getOutMessage();
        boolean isClient = outM != null && MessageUtils.isRequestor(outM);
        String name = isClient ? CLIENT_FACTORY_NAME : SERVER_FACTORY_NAME;
        
        return (ProviderFactory)e.get(name);
    }
    
    protected static int compareClasses(Object o1, Object o2) {
        Class<?> cl1 = ClassHelper.getRealClass(o1); 
        Class<?> cl2 = ClassHelper.getRealClass(o2);
        
        Type[] types1 = getGenericInterfaces(cl1);
        Type[] types2 = getGenericInterfaces(cl2);
        
        if (types1.length == 0 && types2.length > 0) {
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
    
    private static Type[] getGenericInterfaces(Class<?> cls) {
        if (Object.class == cls) {
            return new Type[]{};
        }
        Type genericSuperCls = cls.getGenericSuperclass();
        if (genericSuperCls instanceof ParameterizedType) {
            return new Type[]{genericSuperCls};
        }
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            return types;
        }
        return getGenericInterfaces(cls.getSuperclass());
    }
    
    protected static class BindingPriorityComparator extends AbstactBindingPriorityComparator {
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
    
    protected ProviderInfo<Object> createProviderFromConstructor(Constructor<?> c, 
                                                                 Map<Class<?>, Object> values) {
        Object[] cArgs = ResourceUtils.createConstructorArguments(c, null, false, values);
        Object instance = null;
        try {
            instance = c.newInstance(cArgs);
        } catch (Throwable ex) {
            throw new RuntimeException("Resource or provider class " + c.getDeclaringClass().getName()
                                       + " can not be instantiated"); 
        }
        Map<Class<?>, ThreadLocalProxy<?>> proxies = 
            new HashMap<Class<?>, ThreadLocalProxy<?>>();
        Class<?>[] paramTypes = c.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (cArgs[i] instanceof ThreadLocalProxy) {
                @SuppressWarnings("unchecked")
                ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>)cArgs[i];
                proxies.put(paramTypes[i], proxy);
            }
        }
        return new ProviderInfo<Object>(instance, proxies, getBus()); 
    }
}
