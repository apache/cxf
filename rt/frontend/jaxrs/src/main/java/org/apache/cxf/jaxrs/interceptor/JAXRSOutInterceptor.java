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

package org.apache.cxf.jaxrs.interceptor;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class JAXRSOutInterceptor extends AbstractOutDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSOutInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSOutInterceptor.class);

    public JAXRSOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(Message message) {
        
        try {
            processResponse(message);
        } finally {
            Object rootInstance = message.getExchange().remove(JAXRSUtils.ROOT_INSTANCE);
            Object rootProvider = message.getExchange().remove(JAXRSUtils.ROOT_PROVIDER);
            if (rootInstance != null && rootProvider != null) {
                try {
                    ((ResourceProvider)rootProvider).releaseInstance(message, rootInstance);
                } catch (Throwable tex) {
                    LOG.warning("Exception occurred during releasing the service instance, "
                                + tex.getMessage());
                }
            }
            ProviderFactory.getInstance(message).clearThreadLocalProxies();
            ClassResourceInfo cri =
                (ClassResourceInfo)message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
            if (cri != null) {
                cri.clearThreadLocalProxies();
            }
        }
            

    }
    
    private void processResponse(Message message) {
        
        if (isResponseAlreadyHandled(message)) {
            return;
        }
        
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.size() == 0) {
            return;
        }
        
        Object responseObj = objs.get(0);
        
        Response response = null;
        if (responseObj instanceof Response) {
            response = (Response)responseObj;
        } else {
            int status = getStatus(message, responseObj != null ? 200 : 204);
            response = Response.status(status).entity(responseObj).build();
        }
        
        Exchange exchange = message.getExchange();
        OperationResourceInfo ori = (OperationResourceInfo)exchange.get(OperationResourceInfo.class
            .getName());

        List<ProviderInfo<ResponseHandler>> handlers = 
            ProviderFactory.getInstance(message).getResponseHandlers();
        for (ProviderInfo<ResponseHandler> rh : handlers) {
            InjectionUtils.injectContextFields(rh.getProvider(), rh, 
                                               message.getExchange().getInMessage());
            InjectionUtils.injectContextFields(rh.getProvider(), rh, 
                                               message.getExchange().getInMessage());
            Response r = rh.getProvider().handleResponse(message, ori, response);
            if (r != null) {
                response = r;
            }
        }
        
        serializeMessage(message, response, ori, true);        
    }

    private int getStatus(Message message, int defaultValue) {
        Object customStatus = message.getExchange().get(Message.RESPONSE_CODE);
        return customStatus == null ? defaultValue : (Integer)customStatus;
    }
    
    @SuppressWarnings("unchecked")
    private void serializeMessage(Message message, 
                                  Response response, 
                                  OperationResourceInfo ori,
                                  boolean firstTry) {
        int status = response.getStatus();
        Object responseObj = response.getEntity();
        if (status == 200 && !isResponseNull(responseObj) && firstTry 
            && ori != null && JAXRSUtils.headMethodPossible(ori.getHttpMethod(), 
                (String)message.getExchange().getInMessage().get(Message.HTTP_REQUEST_METHOD))) {
            LOG.info(new org.apache.cxf.common.i18n.Message("HEAD_WITHOUT_ENTITY", BUNDLE).toString());
            responseObj = null;
        }
        if (status == -1) {
            status = isResponseNull(responseObj) ? 204 : 200;
        }
        
        setResponseStatus(message, status);
        
        Map<String, List<Object>> theHeaders = 
            (Map<String, List<Object>>)message.get(Message.PROTOCOL_HEADERS);
        if (firstTry && theHeaders != null) {
            // some headers might've been setup by custom cxf interceptors
            theHeaders.putAll((Map)response.getMetadata());
        } else {
            theHeaders = response.getMetadata();
        }
        MultivaluedMap<String, Object> responseHeaders;
        if (!(theHeaders instanceof MultivaluedMap)) {
            responseHeaders = new MetadataMap<String, Object>(theHeaders);
        } else {
            responseHeaders = (MultivaluedMap)theHeaders;
        }
        message.put(Message.PROTOCOL_HEADERS, responseHeaders);
        
        setResponseDate(responseHeaders, firstTry);
        if (isResponseNull(responseObj)) {
            responseHeaders.putSingle("Content-Length", "0");
            return;
        }
        
        Object ignoreWritersProp = message.getExchange().get(JAXRSUtils.IGNORE_MESSAGE_WRITERS);
        boolean ignoreWriters = 
            ignoreWritersProp == null ? false : Boolean.valueOf(ignoreWritersProp.toString());
        if (ignoreWriters) {
            writeResponseToStream(message.getContent(OutputStream.class), responseObj);
            return;
        }
        
        List<MediaType> availableContentTypes = computeAvailableContentTypes(message, response);  
        
        Method invoked = null;
        if (firstTry) {
            invoked = ori == null ? null : ori.getAnnotatedMethod() == null
                ? ori.getMethodToInvoke() : ori.getAnnotatedMethod();
        }
        
        Class<?> targetType = getRawResponseClass(responseObj);
        Type genericType = getGenericResponseType(ori == null ? null : invoked, responseObj, targetType);
        if (genericType instanceof TypeVariable) {
            genericType = InjectionUtils.getSuperType(ori.getClassResourceInfo().getServiceClass(), 
                                                       (TypeVariable)genericType);
        }
        
        Annotation[] annotations = invoked != null ? invoked.getAnnotations() : new Annotation[]{};
        
        MessageBodyWriter writer = null;
        MediaType responseType = null;
        for (MediaType type : availableContentTypes) { 
            writer = ProviderFactory.getInstance(message)
                .createMessageBodyWriter(targetType, genericType, annotations, type, message);
            
            if (writer != null) {
                responseType = type;
                break;
            }
        }
    
        OutputStream outOriginal = message.getContent(OutputStream.class);
        if (writer == null) {
            message.put(Message.CONTENT_TYPE, "text/plain");
            message.put(Message.RESPONSE_CODE, 500);
            writeResponseErrorMessage(outOriginal, "NO_MSG_WRITER", targetType.getSimpleName());
            return;
        }
        boolean enabled = checkBufferingMode(message, writer, firstTry);
        Object entity = getEntity(responseObj);
        try {
            responseType = checkFinalContentType(responseType);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Response content type is: " + responseType.toString());
            }
            message.put(Message.CONTENT_TYPE, responseType.toString());
            
            long size = writer.getSize(entity, targetType, genericType, annotations, responseType);
            if (size > 0) {
                LOG.fine("Setting ContentLength to " + size + " as requested by " 
                         + writer.getClass().getName());
                responseHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, Long.toString(size));
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Response EntityProvider is: " + writer.getClass().getName());
            }
            try {
                writer.writeTo(entity, targetType, genericType, 
                               annotations, 
                               responseType, 
                               responseHeaders, 
                               message.getContent(OutputStream.class));
                
                if (isResponseRedirected(message)) {
                    return;
                }
                
                Object newContentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
                if (newContentType != null) {
                    message.put(Message.CONTENT_TYPE, newContentType.toString());
                }
                checkCachedStream(message, outOriginal, enabled);
            } finally {
                if (enabled) {
                    message.setContent(OutputStream.class, outOriginal);
                    message.put(XMLStreamWriter.class.getName(), null);
                }
            }
            
        } catch (IOException ex) {
            handleWriteException(message, response, ori, ex, entity, firstTry);
        } catch (Throwable ex) {
            handleWriteException(message, response, ori, ex, entity, firstTry);
        }
    }
    
    private boolean isResponseNull(Object o) {
        return o == null || GenericEntity.class.isAssignableFrom(o.getClass()) 
                            && ((GenericEntity)o).getEntity() == null; 
    }
    
    private Object getEntity(Object o) {
        return GenericEntity.class.isAssignableFrom(o.getClass()) ? ((GenericEntity)o).getEntity() : o; 
    }
    
    private boolean checkBufferingMode(Message m, MessageBodyWriter w, boolean firstTry) {
        if (!firstTry) {
            return false;
        }
        Object outBuf = m.getContextualProperty(OUT_BUFFERING);
        boolean enabled = MessageUtils.isTrue(outBuf);
        boolean configurableProvider = w instanceof AbstractConfigurableProvider;
        if (!enabled && outBuf == null && configurableProvider) {
            enabled = ((AbstractConfigurableProvider)w).getEnableBuffering();
        }
        if (enabled) {
            boolean streamingOn = configurableProvider 
                ? ((AbstractConfigurableProvider)w).getEnableStreaming() : false;
            if (streamingOn) {
                m.setContent(XMLStreamWriter.class, new CachingXmlEventWriter());
            } else {
                m.setContent(OutputStream.class, new CachedOutputStream());
            }
        }
        return enabled;
    }
    
    private void checkCachedStream(Message m, OutputStream osOriginal, boolean enabled) throws Exception {
        XMLStreamWriter writer = null;
        if (enabled) {
            writer = m.getContent(XMLStreamWriter.class);
        } else {
            writer = (XMLStreamWriter)m.get(XMLStreamWriter.class.getName());
        }
        if (writer instanceof CachingXmlEventWriter) {
            CachingXmlEventWriter cache = (CachingXmlEventWriter)writer;
            if (cache.getEvents().size() != 0) {
                XMLStreamWriter origWriter = StaxUtils.createXMLStreamWriter(osOriginal);
                for (XMLEvent event : cache.getEvents()) {
                    StaxUtils.writeEvent(event, origWriter);
                }
            }
            m.setContent(XMLStreamWriter.class, null);
            return;
        }
        if (enabled) {
            OutputStream os = m.getContent(OutputStream.class);
            if (os != osOriginal && os instanceof CachedOutputStream) {
                CachedOutputStream cos = (CachedOutputStream)os;
                if (cos.size() != 0) {
                    cos.writeCacheTo(osOriginal);
                }
            }
        }
    }
    
    private void handleWriteException(Message message, 
                                         Response response, 
                                         OperationResourceInfo ori,
                                         Throwable ex,
                                         Object responseObj,
                                         boolean firstTry) {
        OutputStream out = message.getContent(OutputStream.class);
        if (firstTry) {
            Response excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
            if (excResponse != null) {
                serializeMessage(message, excResponse, ori, false);
                return;
            } else {
                ex.printStackTrace();
            }
        }
        setResponseStatus(message, 500);
        writeResponseErrorMessage(out, "SERIALIZE_ERROR", 
                                  responseObj.getClass().getSimpleName()); 
            
    }
    
    
    private void writeResponseErrorMessage(OutputStream out, String errorString, 
                                           String parameter) {
        try {
            org.apache.cxf.common.i18n.Message message = 
                new org.apache.cxf.common.i18n.Message(errorString,
                                                   BUNDLE,
                                                   parameter);
            LOG.warning(message.toString());
            if (out != null) {
                out.write(message.toString().getBytes("UTF-8"));
            }
        } catch (IOException another) {
            // ignore
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<MediaType> computeAvailableContentTypes(Message message, Response response) {
        
        Object contentType = 
            response.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType != null) {
            return Collections.singletonList(MediaType.valueOf(contentType.toString()));
        }
        Exchange exchange = message.getExchange();
        List<MediaType> produceTypes = null;
        OperationResourceInfo operation = exchange.get(OperationResourceInfo.class);
        if (operation != null) {
            produceTypes = operation.getProduceTypes();
        } else {
            produceTypes = Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        }
        List<MediaType> acceptContentTypes = 
            (List<MediaType>)exchange.get(Message.ACCEPT_CONTENT_TYPE);
        if (acceptContentTypes == null) {
            acceptContentTypes = Collections.singletonList(MediaType.WILDCARD_TYPE);
        }        
        return JAXRSUtils.intersectMimeTypes(acceptContentTypes, produceTypes, true);
        
    }
    
    private Class<?> getRawResponseClass(Object targetObject) {
        if (GenericEntity.class.isAssignableFrom(targetObject.getClass())) {
            return ((GenericEntity)targetObject).getRawType();
        } else {
            return ClassHelper.getRealClassFromClass(targetObject.getClass());
        }
    }
    
    private Type getGenericResponseType(Method invoked, Object targetObject, Class<?> targetType) {
        if (GenericEntity.class.isAssignableFrom(targetObject.getClass())) {
            return ((GenericEntity)targetObject).getType();
        } else if (invoked == null || !invoked.getReturnType().isAssignableFrom(targetType)) {
            // when a method has been invoked it is still possible that either an ExceptionMapper
            // or a ResponseHandler filter overrides a response entity; if it happens then 
            // the Type is the class of the response object, unless this new entity is assignable
            // to invoked.getReturnType(); same applies to the case when a method returns Response
            return targetObject.getClass(); 
        } else {
            return invoked.getGenericReturnType();
        }
    }
    
    private MediaType checkFinalContentType(MediaType mt) {
        if (mt.isWildcardType() || mt.isWildcardSubtype()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else if (mt.getParameters().containsKey("q")) {
            return MediaType.valueOf(JAXRSUtils.removeMediaTypeParameter(mt, "q"));
        } else {
            return mt;
        }
        
    }
    
    private void setResponseDate(MultivaluedMap<String, Object> headers, boolean firstTry) {
        if (!firstTry) {
            return;
        }
        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        headers.putSingle(HttpHeaders.DATE, format.format(new Date()));
    }
    
    private boolean isResponseAlreadyHandled(Message m) {
        return isResponseAlreadyCommited(m) || isResponseRedirected(m);
    }
    
    private boolean isResponseAlreadyCommited(Message m) {
        return Boolean.TRUE.equals(m.getExchange().get(AbstractHTTPDestination.RESPONSE_COMMITED));
    }

    private boolean isResponseRedirected(Message outMessage) {
        return Boolean.TRUE.equals(outMessage.get(AbstractHTTPDestination.REQUEST_REDIRECTED));
    }
    
    private void writeResponseToStream(OutputStream os, Object responseObj) {
        try {
            byte[] bytes = responseObj.toString().getBytes("UTF-8");
            os.write(bytes, 0, bytes.length);
        } catch (Exception ex) {
            LOG.severe("Problem with writing the data to the output stream");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
   
    private void setResponseStatus(Message message, int status) {
        message.put(Message.RESPONSE_CODE, status);   
        boolean responseHeadersCopied = isResponseHeadersCopied(message);
        if (responseHeadersCopied) {
            HttpServletResponse response = 
                (HttpServletResponse)message.get(AbstractHTTPDestination.HTTP_RESPONSE);
            response.setStatus(status);
        }
    }
    
    // Some CXF interceptors such as FIStaxOutInterceptor will indirectly initiate
    // an early copying of response code and headers into the HttpServletResponse
    // TODO : Pushing the filter processing and copying response headers into say
    // PRE-LOGICAl and PREPARE_SEND interceptors will most likely be a good thing
    // however JAX-RS MessageBodyWriters are also allowed to add response headers
    // which is reason why a MultipartMap parameter in MessageBodyWriter.writeTo 
    // method is modifiable. Thus we do need to know if the initial copy has already
    // occurred: for now we will just use to ensure the correct status is set
    private boolean isResponseHeadersCopied(Message message) {
        return MessageUtils.isTrue(message.get(AbstractHTTPDestination.RESPONSE_HEADERS_COPIED));
    }
}
