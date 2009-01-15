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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.ext.MappingsHandler;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public final class ProviderFactory {
    
    private static final Map<String, ProviderFactory> FACTORIES = 
        new HashMap<String, ProviderFactory>();
    private static final ProviderFactory DEFAULT_FACTORY = new ProviderFactory(); 
    private static final String SLASH = "/"; 
    
    private List<ProviderInfo<MessageBodyReader>> defaultMessageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader>>();
    private List<ProviderInfo<MessageBodyWriter>> defaultMessageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter>>();
    private List<ProviderInfo<MessageBodyReader>> userMessageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader>>(1);
    private List<ProviderInfo<MessageBodyWriter>> userMessageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter>>(1);
    private List<ProviderInfo<ContextResolver>> userContextResolvers = 
        new ArrayList<ProviderInfo<ContextResolver>>(1);
    private List<ProviderInfo<ExceptionMapper>> defaultExceptionMappers = 
        new ArrayList<ProviderInfo<ExceptionMapper>>(1);
    private List<ProviderInfo<ExceptionMapper>> userExceptionMappers = 
        new ArrayList<ProviderInfo<ExceptionMapper>>(1);
    private List<ProviderInfo<RequestHandler>> requestHandlers = 
        new ArrayList<ProviderInfo<RequestHandler>>(1);
    private List<ProviderInfo<ResponseHandler>> responseHandlers = 
        new ArrayList<ProviderInfo<ResponseHandler>>(1);
    private List<ProviderInfo<ParameterHandler>> jaxrsParamHandlers = 
        new ArrayList<ProviderInfo<ParameterHandler>>(1);
    private RequestPreprocessor requestPreprocessor;
    
    private ProviderFactory() {
        // TODO : this needs to be done differently,
        // we need to use cxf-jaxrs-extensions
        
        // TODO : make sure the default providers are shared between multiple
        // factories
        
        setProviders(defaultMessageReaders,
                     defaultMessageWriters,
                     userContextResolvers,
                     requestHandlers,
                     responseHandlers,
                     defaultExceptionMappers,
                     jaxrsParamHandlers,
                     new JAXBElementProvider(),
                     new JSONProvider(),
                     new BinaryDataProvider(),
                     new StringProvider(),
                     new SourceProvider(),
                     new FormEncodingReaderProvider(),
                     new PrimitiveTextProvider(),
                     new WebApplicationExceptionMapper(),
                     new MappingsHandler());
    }
    
    public static ProviderFactory getInstance() {
        return getInstance("/");
    }
    
    public static ProviderFactory getInstance(String baseAddress) {
        if (SLASH.equals(baseAddress)) {
            return DEFAULT_FACTORY;
        }
        
        ProviderFactory pf = null;
        synchronized (ProviderFactory.class) { 
            pf = FACTORIES.get(baseAddress);
            if (pf == null) {
                pf = new ProviderFactory();
                FACTORIES.put(baseAddress, pf);
            }
        }
        return pf;
    }

    public <T> ContextResolver<T> createContextResolver(Type contextType, 
                                                        Message m) {
        // TODO : get media type from message  
        return createContextResolver(contextType, m, null);
        
    }
    
    @SuppressWarnings("unchecked")
    public <T> ContextResolver<T> createContextResolver(Type contextType, 
                                                        Message m,
                                                        MediaType type) {
        for (ProviderInfo<ContextResolver> cr : userContextResolvers) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    for (int i = 0; i < args.length; i++) {
                        if (contextType == args[i]) {
                            
                            InjectionUtils.injectContextFields(cr.getProvider(), cr, m);
                            InjectionUtils.injectContextMethods(cr.getProvider(), cr, m);
                            return cr.getProvider();
                        }
                    }
                }
            }
        }
        return null;
    }
    
    
    public <T extends Throwable> ExceptionMapper<T> createExceptionMapper(Class<?> exceptionType, Message m) {
        
        ExceptionMapper<T> mapper = doCreateExceptionMapper(userExceptionMappers,
                                                            exceptionType,
                                                            m);
        if (mapper != null) {
            return mapper;
        }
        
        return doCreateExceptionMapper(defaultExceptionMappers,
                                       exceptionType,
                                       m);
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> ExceptionMapper<T> doCreateExceptionMapper(
        List<ProviderInfo<ExceptionMapper>> mappers, Class<?> exceptionType, Message m) {
        
        List<ExceptionMapper<T>> candidates = new LinkedList<ExceptionMapper<T>>();
        
        for (ProviderInfo<ExceptionMapper> em : mappers) {
            Type[] types = em.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    for (int i = 0; i < args.length; i++) {
                        if (((Class<?>)args[i]).isAssignableFrom(exceptionType)) {
                            InjectionUtils.injectContextFields(em.getProvider(), em, m);
                            InjectionUtils.injectContextMethods(em.getProvider(), em, m);
                            candidates.add(em.getProvider());
                        }
                    }
                }
            }
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ExceptionMapperComparator());
        return candidates.get(0);
    }
    
    @SuppressWarnings("unchecked")
    public <T> ParameterHandler<T> createParameterHandler(Class<?> paramType) {
        
        List<ParameterHandler<T>> candidates = new LinkedList<ParameterHandler<T>>();
        
        for (ProviderInfo<ParameterHandler> em : jaxrsParamHandlers) {
            Type[] types = em.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    for (int i = 0; i < args.length; i++) {
                        if (((Class<?>)args[i]).isAssignableFrom(paramType)) {
                            candidates.add(em.getProvider());
                        }
                    }
                }
            }
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ParameterHandlerComparator());
        return candidates.get(0);
    }
    
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Try user provided providers
        MessageBodyReader<T> mr = chooseMessageReader(userMessageReaders, 
                                                      bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        //If none found try the default ones
        if (mr == null) {
            mr = chooseMessageReader(defaultMessageReaders,
                                     bodyType,
                                     parameterType,
                                     parameterAnnotations,
                                     mediaType,
                                     m);
        }     
        
        return mr;
    }
    
    
    
    public List<ProviderInfo<RequestHandler>> getRequestHandlers() {
        
        return Collections.unmodifiableList(requestHandlers);
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
        MessageBodyWriter<T> mw = chooseMessageWriter(userMessageWriters,
                                                      bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        //If none found try the default ones
        if (mw == null) {
            mw = chooseMessageWriter(defaultMessageWriters,
                                     bodyType,
                                     parameterType,
                                     parameterAnnotations,
                                     mediaType,
                                     m);
        }     
        
        return mw;
    }
    
//CHECKSTYLE:OFF       
    private void setProviders(List<ProviderInfo<MessageBodyReader>> readers, 
                              List<ProviderInfo<MessageBodyWriter>> writers,
                              List<ProviderInfo<ContextResolver>> resolvers,
                              List<ProviderInfo<RequestHandler>> requestFilters,
                              List<ProviderInfo<ResponseHandler>> responseFilters,
                              List<ProviderInfo<ExceptionMapper>> excMappers,
                              List<ProviderInfo<ParameterHandler>> paramHandlers,
                              Object... providers) {
        
        for (Object o : providers) {
            if (MessageBodyReader.class.isAssignableFrom(o.getClass())) {
                readers.add(new ProviderInfo<MessageBodyReader>((MessageBodyReader)o)); 
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(o.getClass())) {
                writers.add(new ProviderInfo<MessageBodyWriter>((MessageBodyWriter)o)); 
            }
            
            if (ContextResolver.class.isAssignableFrom(o.getClass())) {
                resolvers.add(new ProviderInfo<ContextResolver>((ContextResolver)o)); 
            }
            
            if (RequestHandler.class.isAssignableFrom(o.getClass())) {
                requestFilters.add(new ProviderInfo<RequestHandler>((RequestHandler)o)); 
            }
            
            if (ResponseHandler.class.isAssignableFrom(o.getClass())) {
                responseFilters.add(new ProviderInfo<ResponseHandler>((ResponseHandler)o)); 
            }
            
            if (ExceptionMapper.class.isAssignableFrom(o.getClass())) {
                excMappers.add(new ProviderInfo<ExceptionMapper>((ExceptionMapper)o)); 
            }
            
            if (ParameterHandler.class.isAssignableFrom(o.getClass())) {
                paramHandlers.add(new ProviderInfo<ParameterHandler>((ParameterHandler)o)); 
            }
        }
        
        sortReaders(readers);
        sortWriters(writers);
        
        injectContexts(readers, writers, resolvers, requestFilters, responseFilters, excMappers);
    }
//CHECKSTYLE:ON
    
    void injectContexts(List<?> ... providerLists) {
        for (List<?> list : providerLists) {
            for (Object p : list) {
                ProviderInfo pi = (ProviderInfo)p;
                InjectionUtils.injectContextProxies(pi, pi.getProvider());
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
    private void sortReaders(List<ProviderInfo<MessageBodyReader>> entityProviders) {
        Collections.sort(entityProviders, new MessageBodyReaderComparator());
    }
    
    private void sortWriters(List<ProviderInfo<MessageBodyWriter>> entityProviders) {
        Collections.sort(entityProviders, new MessageBodyWriterComparator());
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
    private <T> MessageBodyReader<T> chooseMessageReader(
                                 List<ProviderInfo<MessageBodyReader>> readers, 
                                                         Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        for (ProviderInfo<MessageBodyReader> ep : readers) {
            
            if (matchesReaderCriterias(ep.getProvider(), type, genericType, annotations, mediaType)) {
                InjectionUtils.injectContextFields(ep.getProvider(), ep, m);
                InjectionUtils.injectContextMethods(ep.getProvider(), ep, m);
                return ep.getProvider();
            }
        }     
        
        return null;
        
    }
    
    private <T> boolean matchesReaderCriterias(MessageBodyReader<T> ep,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType) {
        if (!ep.isReadable(type, genericType, annotations, mediaType)) {
            return false;
        }
        
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                          supportedMediaTypes);

        return availableMimeTypes.size() != 0 ? true : false;
        
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
    private <T> MessageBodyWriter<T> chooseMessageWriter(
                          List<ProviderInfo<MessageBodyWriter>> writers, 
                                                         Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        for (ProviderInfo<MessageBodyWriter> ep : writers) {
            if (matchesWriterCriterias(ep.getProvider(), type, genericType, annotations, mediaType)) {
                InjectionUtils.injectContextFields(ep.getProvider(), ep, m);
                InjectionUtils.injectContextMethods(ep.getProvider(), ep, m);
                return ep.getProvider();
            }
        }     
        
        return null;
        
    }
    
    private <T> boolean matchesWriterCriterias(MessageBodyWriter<T> ep,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType) {
        if (!ep.isWriteable(type, genericType, annotations, mediaType)) {
            return false;
        }
        
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                          supportedMediaTypes);

        return availableMimeTypes.size() != 0 ? true : false;
        
    }
    
    List<ProviderInfo<MessageBodyReader>> getDefaultMessageReaders() {
        return Collections.unmodifiableList(defaultMessageReaders);
    }

    List<ProviderInfo<MessageBodyWriter>> getDefaultMessageWriters() {
        return Collections.unmodifiableList(defaultMessageWriters);
    }
    
    List<ProviderInfo<MessageBodyReader>> getUserMessageReaders() {
        return Collections.unmodifiableList(userMessageReaders);
    }
    
    List<ProviderInfo<MessageBodyWriter>> getUserMessageWriters() {
        return Collections.unmodifiableList(userMessageWriters);
    }
    
    List<ProviderInfo<ContextResolver>> getUserContextResolvers() {
        return Collections.unmodifiableList(userContextResolvers);
    }
    
     
    public void registerUserProvider(Object provider) {
        setUserProviders(Collections.singletonList(provider));    
    }
    /**
     * Use for injection of entityProviders
     * @param entityProviders the entityProviders to set
     */
    public void setUserProviders(List<?> userProviders) {
        setProviders(userMessageReaders,
                     userMessageWriters,
                     userContextResolvers,
                     requestHandlers,
                     responseHandlers,
                     userExceptionMappers,
                     jaxrsParamHandlers,
                     userProviders.toArray());
    }

    private static class MessageBodyReaderComparator 
        implements Comparator<ProviderInfo<MessageBodyReader>> {
        
        public int compare(ProviderInfo<MessageBodyReader> p1, 
                           ProviderInfo<MessageBodyReader> p2) {
            MessageBodyReader e1 = p1.getProvider();
            MessageBodyReader e2 = p2.getProvider();
            List<MediaType> types1 = JAXRSUtils.getProviderConsumeTypes(e1);
            types1 = JAXRSUtils.sortMediaTypes(types1);
            List<MediaType> types2 = JAXRSUtils.getProviderConsumeTypes(e2);
            types2 = JAXRSUtils.sortMediaTypes(types2);
    
            return JAXRSUtils.compareSortedMediaTypes(types1, types2);
            
        }
    }
    
    private static class MessageBodyWriterComparator 
        implements Comparator<ProviderInfo<MessageBodyWriter>> {
        
        public int compare(ProviderInfo<MessageBodyWriter> p1, 
                           ProviderInfo<MessageBodyWriter> p2) {
            MessageBodyWriter e1 = p1.getProvider();
            MessageBodyWriter e2 = p2.getProvider();
            
            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e1));
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e2));
    
            return JAXRSUtils.compareSortedMediaTypes(types1, types2);
            
        }
    }
    
    public void setRequestPreprocessor(RequestPreprocessor rp) {
        this.requestPreprocessor = rp;
    }
    
    public RequestPreprocessor getRequestPreprocessor() {
        return requestPreprocessor;
    }
    
    public void clearThreadLocalProxies() {
        clearProxies(defaultMessageReaders,
                     defaultMessageWriters,
                     userMessageReaders,
                     userMessageWriters,
                     userContextResolvers,
                     requestHandlers,
                     responseHandlers,
                     userExceptionMappers);
    }
    
    void clearProxies(List<?> ...lists) {
        for (List<?> list : lists) {
            for (Object p : list) {
                ProviderInfo pi = (ProviderInfo)p;
                pi.clearThreadLocalProxies();
            }
        }
    }
    
    void clearProviders() {
        userMessageReaders.clear();
        userMessageWriters.clear();
        userContextResolvers.clear();
        userExceptionMappers.clear();
        requestHandlers.clear();
        responseHandlers.clear();
        jaxrsParamHandlers.clear();
    }
    
    public void setSchemaLocations(List<String> schemas) {
        setSchemasOnProviders(userMessageReaders, schemas);
        setSchemasOnProviders(defaultMessageReaders, schemas);
    }
    
    private void setSchemasOnProviders(List<ProviderInfo<MessageBodyReader>> providers,
                                       List<String> schemas) {
        for (ProviderInfo<MessageBodyReader> r : providers) {
            try {
                Method m = r.getProvider().getClass().getMethod("setSchemas", 
                                                     new Class[]{List.class});
                m.invoke(r.getProvider(), new Object[]{schemas});
            } catch (Exception ex) {
                // ignore
            }
        }
    }
    
    private static class ExceptionMapperComparator implements 
        Comparator<ExceptionMapper<? extends Throwable>> {

        public int compare(ExceptionMapper<? extends Throwable> em1, 
                           ExceptionMapper<? extends Throwable> em2) {
            return compareClasses(em1.getClass(), em2.getClass());
        }
        
    }
    
    private static class ParameterHandlerComparator implements 
        Comparator<ParameterHandler<? extends Object>> {

        public int compare(ParameterHandler<? extends Object> em1, 
                           ParameterHandler<? extends Object> em2) {
            return compareClasses(em1.getClass(), em2.getClass());
        }
    
    }
    
    private static int compareClasses(Class<?> cl1, Class<?> cl2) {
        Type[] types1 = cl1.getGenericInterfaces();
        Type[] types2 = cl2.getGenericInterfaces();
        
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
}
