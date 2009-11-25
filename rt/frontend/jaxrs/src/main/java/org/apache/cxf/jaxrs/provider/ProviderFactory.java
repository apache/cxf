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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public final class ProviderFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);
    private static final ProviderFactory SHARED_FACTORY = new ProviderFactory();
    
    static {
        SHARED_FACTORY.setProviders(createProvider("org.apache.cxf.jaxrs.provider.JAXBElementProvider"),
                                    createProvider("org.apache.cxf.jaxrs.provider.JSONProvider"),
                                    new BinaryDataProvider(),
                                    new SourceProvider(),
                                    new FormEncodingProvider(),
                                    new PrimitiveTextProvider(),
                                    new MultipartProvider(),
                                    new WebApplicationExceptionMapper(),
                                    new WadlGenerator());
    }
    
    private List<ProviderInfo<MessageBodyReader>> messageReaders = 
        new ArrayList<ProviderInfo<MessageBodyReader>>();
    private List<ProviderInfo<MessageBodyWriter>> messageWriters = 
        new ArrayList<ProviderInfo<MessageBodyWriter>>();
    private List<ProviderInfo<ContextResolver>> contextResolvers = 
        new ArrayList<ProviderInfo<ContextResolver>>(1);
    private List<ProviderInfo<ExceptionMapper>> exceptionMappers = 
        new ArrayList<ProviderInfo<ExceptionMapper>>(1);
    private List<ProviderInfo<RequestHandler>> requestHandlers = 
        new ArrayList<ProviderInfo<RequestHandler>>(1);
    private List<ProviderInfo<ResponseHandler>> responseHandlers = 
        new ArrayList<ProviderInfo<ResponseHandler>>(1);
    private List<ProviderInfo<ParameterHandler>> paramHandlers = 
        new ArrayList<ProviderInfo<ParameterHandler>>(1);
    private List<ProviderInfo<ResponseExceptionMapper>> responseExceptionMappers = 
        new ArrayList<ProviderInfo<ResponseExceptionMapper>>(1);
    private RequestPreprocessor requestPreprocessor;
    
    private ProviderFactory() {
    }
    
    private static Object createProvider(String className) {
        try {
            return ClassLoaderUtils.loadClass(className, ProviderFactory.class).newInstance();
        } catch (Throwable ex) {
            String message = "Problem with instantiating the default provider " + className;
            if (ex.getMessage() != null) {
                message += ex.getMessage();
            } else {
                message += ", exception class : " + ex.getClass().getName();  
            }
            LOG.info(message);
        }
        return  null;
    }
    
    public static ProviderFactory getInstance() {
        return new ProviderFactory();
    }
    
    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().get(Endpoint.class);
        return (ProviderFactory)e.get(ProviderFactory.class.getName());
    }
    
    public static ProviderFactory getSharedInstance() {
        return SHARED_FACTORY;
    }
    
    public <T> ContextResolver<T> createContextResolver(Type contextType, 
                                                        Message m) {
        Object mt = m.get(Message.CONTENT_TYPE);
        return createContextResolver(contextType, m,
               mt == null ? MediaType.valueOf("*/*") : MediaType.valueOf(mt.toString()));
        
    }
    
    @SuppressWarnings("unchecked")
    public <T> ContextResolver<T> createContextResolver(Type contextType, 
                                                        Message m,
                                                        MediaType type) {
        for (ProviderInfo<ContextResolver> cr : contextResolvers) {
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
        
        ExceptionMapper<T> mapper = doCreateExceptionMapper(exceptionType, m);
        if (mapper != null || this == SHARED_FACTORY) {
            return mapper;
        }
        
        return SHARED_FACTORY.createExceptionMapper(exceptionType, m);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Throwable> ExceptionMapper<T> doCreateExceptionMapper(
        Class<?> exceptionType, Message m) {
        
        List<ExceptionMapper<T>> candidates = new LinkedList<ExceptionMapper<T>>();
        
        for (ProviderInfo<ExceptionMapper> em : exceptionMappers) {
            handleMapper((List)candidates, em, exceptionType, m);
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
        
        for (ProviderInfo<ParameterHandler> em : paramHandlers) {
            handleMapper((List)candidates, em, paramType, null);
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return candidates.get(0);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ResponseExceptionMapper<T> createResponseExceptionMapper(
                                 Class<?> paramType) {
        
        List<ResponseExceptionMapper<T>> candidates = new LinkedList<ResponseExceptionMapper<T>>();
        
        for (ProviderInfo<ResponseExceptionMapper> em : responseExceptionMappers) {
            handleMapper((List)candidates, em, paramType, null);
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return candidates.get(0);
    }
    
    private static void handleMapper(List<Object> candidates, ProviderInfo em, 
                                     Class<?> expectedType, Message m) {
        
        Class<?> mapperClass =  ClassHelper.getRealClass(em.getProvider());
        Type[] types = getGenericInterfaces(mapperClass);
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)t;
                Type[] args = pt.getActualTypeArguments();
                for (int i = 0; i < args.length; i++) {
                    Type arg = args[i];
                    if (arg instanceof TypeVariable) {
                        // give or take wildcards, this implies that the provider is generic, and 
                        // is willing to take whatever we throw at it. We could, I suppose,
                        // do wildcard analysis. It would be more correct to look at the bounds
                        // and check that they are Object or compatible.
                        if (m != null) {
                            InjectionUtils.injectContextFields(em.getProvider(), em, m);
                            InjectionUtils.injectContextMethods(em.getProvider(), em, m);
                        }
                        candidates.add(em.getProvider());
                        return;
                    }
                    Class<?> actualClass = InjectionUtils.getRawType(args[i]);
                    if (actualClass == null) {
                        continue;
                    }
                    if (actualClass.isAssignableFrom(expectedType)) {
                        if (m != null) {
                            InjectionUtils.injectContextFields(em.getProvider(), em, m);
                            InjectionUtils.injectContextMethods(em.getProvider(), em, m);
                        }
                        candidates.add(em.getProvider());
                        break;
                    }
                }
            }
        }
    }
    
    
    
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> bodyType,
                                                            Type parameterType,
                                                            Annotation[] parameterAnnotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Try user provided providers
        MessageBodyReader<T> mr = chooseMessageReader(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        //If none found try the default ones
        if (mr != null ||  this == SHARED_FACTORY) {
            return mr;
        }
        return SHARED_FACTORY.createMessageBodyReader(bodyType, parameterType, 
                                                        parameterAnnotations, mediaType, m);
    }
    
    
    
    public List<ProviderInfo<RequestHandler>> getRequestHandlers() {
        List<ProviderInfo<RequestHandler>> handlers = null;
        if (requestHandlers.size() == 0) {
            handlers = SHARED_FACTORY.requestHandlers;
        } else {
            handlers = new ArrayList<ProviderInfo<RequestHandler>>(SHARED_FACTORY.requestHandlers);
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
        MessageBodyWriter<T> mw = chooseMessageWriter(bodyType,
                                                      parameterType,
                                                      parameterAnnotations,
                                                      mediaType,
                                                      m);
        
        //If none found try the default ones
        if (mw != null || this == SHARED_FACTORY) {
            return mw;
        }
        return SHARED_FACTORY.createMessageBodyWriter(bodyType, parameterType, 
                                                        parameterAnnotations, mediaType, m);
    }
    
//CHECKSTYLE:OFF       
    private void setProviders(Object... providers) {
        
        for (Object o : providers) {
            if (o == null) {
                continue;
            }
            Class<?> oClass = ClassHelper.getRealClass(o);
            
            if (MessageBodyReader.class.isAssignableFrom(oClass)) {
                messageReaders.add(new ProviderInfo<MessageBodyReader>((MessageBodyReader)o)); 
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(oClass)) {
                messageWriters.add(new ProviderInfo<MessageBodyWriter>((MessageBodyWriter)o)); 
            }
            
            if (ContextResolver.class.isAssignableFrom(oClass)) {
                contextResolvers.add(new ProviderInfo<ContextResolver>((ContextResolver)o)); 
            }
            
            if (RequestHandler.class.isAssignableFrom(oClass)) {
                requestHandlers.add(new ProviderInfo<RequestHandler>((RequestHandler)o)); 
            }
            
            if (ResponseHandler.class.isAssignableFrom(oClass)) {
                responseHandlers.add(new ProviderInfo<ResponseHandler>((ResponseHandler)o)); 
            }
            
            if (ExceptionMapper.class.isAssignableFrom(oClass)) {
                exceptionMappers.add(new ProviderInfo<ExceptionMapper>((ExceptionMapper)o)); 
            }
            
            if (ResponseExceptionMapper.class.isAssignableFrom(oClass)) {
                responseExceptionMappers.add(new ProviderInfo<ResponseExceptionMapper>((ResponseExceptionMapper)o)); 
            }
            
            if (ParameterHandler.class.isAssignableFrom(oClass)) {
                paramHandlers.add(new ProviderInfo<ParameterHandler>((ParameterHandler)o)); 
            }
        }
        
        sortReaders();
        sortWriters();
        
        injectContexts(messageReaders, messageWriters, contextResolvers, requestHandlers, responseHandlers,
                       exceptionMappers);
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
    private void sortReaders() {
        Collections.sort(messageReaders, new MessageBodyReaderComparator());
    }
    
    private void sortWriters() {
        Collections.sort(messageWriters, new MessageBodyWriterComparator());
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
    private <T> MessageBodyReader<T> chooseMessageReader(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        List<MessageBodyReader<T>> candidates = new LinkedList<MessageBodyReader<T>>();
        for (ProviderInfo<MessageBodyReader> ep : messageReaders) {
            if (matchesReaderCriterias(ep.getProvider(), type, genericType, annotations, mediaType)) {
                if (this == SHARED_FACTORY) {
                    InjectionUtils.injectContextFields(ep.getProvider(), ep, m);
                    InjectionUtils.injectContextMethods(ep.getProvider(), ep, m);
                    return ep.getProvider();
                }
                handleMapper((List)candidates, ep, type, m);
            }
        }     
        
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return candidates.get(0);
        
    }
    
    private <T> boolean matchesReaderCriterias(MessageBodyReader<T> ep,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType) {
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType), supportedMediaTypes);

        if (availableMimeTypes.size() == 0) {
            return false;
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
    private <T> MessageBodyWriter<T> chooseMessageWriter(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType,
                                                         Message m) {
        List<MessageBodyWriter<T>> candidates = new LinkedList<MessageBodyWriter<T>>();
        for (ProviderInfo<MessageBodyWriter> ep : messageWriters) {
            if (matchesWriterCriterias(ep.getProvider(), type, genericType, annotations, mediaType)) {
                if (this == SHARED_FACTORY) {
                    InjectionUtils.injectContextFields(ep.getProvider(), ep, m);
                    InjectionUtils.injectContextMethods(ep.getProvider(), ep, m);
                    return ep.getProvider();
                }
                handleMapper((List)candidates, ep, type, m);
            }
        }     
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ClassComparator());
        return candidates.get(0);
    }
    
    private <T> boolean matchesWriterCriterias(MessageBodyWriter<T> ep,
                                               Class<T> type,
                                               Type genericType,
                                               Annotation[] annotations,
                                               MediaType mediaType) {
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);
        
        List<MediaType> availableMimeTypes = 
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                          supportedMediaTypes);

        if (availableMimeTypes.size() == 0) {
            return false;
        }
        return ep.isWriteable(type, genericType, annotations, mediaType); 
    }
    
    List<ProviderInfo<MessageBodyReader>> getMessageReaders() {
        return Collections.unmodifiableList(messageReaders);
    }

    List<ProviderInfo<MessageBodyWriter>> getMessageWriters() {
        return Collections.unmodifiableList(messageWriters);
    }
    
    List<ProviderInfo<ContextResolver>> getContextResolvers() {
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
        clearProxies(messageReaders,
                     messageWriters,
                     contextResolvers,
                     requestHandlers,
                     responseHandlers,
                     exceptionMappers);
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
        messageReaders.clear();
        messageWriters.clear();
        contextResolvers.clear();
        exceptionMappers.clear();
        requestHandlers.clear();
        responseHandlers.clear();
        paramHandlers.clear();
        responseExceptionMappers.clear();
    }
    
    public void setBus(Bus bus) {
        if (bus == null) {
            return;
        }
        for (ProviderInfo<MessageBodyReader> r : messageReaders) {
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
        for (ProviderInfo<MessageBodyReader> r : messageReaders) {
            schemasMethodAvailable = injectProviderProperty(r.getProvider(), "setSchemas", 
                                                            List.class, schemas);
        }
        if (!schemasMethodAvailable) {
            for (ProviderInfo<MessageBodyReader> r : SHARED_FACTORY.messageReaders) {
                try {
                    Method m = r.getProvider().getClass().getMethod("setSchemas", 
                                                         new Class[]{List.class});
                    Object provider = r.getProvider().getClass().newInstance();
                    m.invoke(provider, new Object[]{schemas});
                    registerUserProvider(provider);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }
    
    private static class ExceptionMapperComparator implements 
        Comparator<ExceptionMapper<? extends Throwable>> {

        public int compare(ExceptionMapper<? extends Throwable> em1, 
                           ExceptionMapper<? extends Throwable> em2) {
            return compareClasses(em1, em2);
        }
        
    }
    
    private static class ClassComparator implements 
        Comparator<Object> {
    
        public int compare(Object em1, Object em2) {
            return compareClasses(em1, em2);
        }
        
    }
    
    private static int compareClasses(Object o1, Object o2) {
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
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            return types;
        }
        return getGenericInterfaces(cls.getSuperclass());
    }
}
