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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.logging.NoOpFaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

public class JAXRSInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInInterceptor.class);
    private static final String RESOURCE_METHOD = "org.apache.cxf.resource.method";
    private static final String RESOURCE_OPERATION_NAME = "org.apache.cxf.resource.operation.name";
    public JAXRSInInterceptor() {
        super(Phase.UNMARSHAL);
    }


    public void handleMessage(Message message) {
        
        if (message.getExchange().get(OperationResourceInfo.class) != null) {
            // it's a suspended invocation;
            return;
        }
        message.getExchange().put(Message.REST_MESSAGE, Boolean.TRUE);
        
        try {
            processRequest(message);
        } catch (Fault ex) {
            convertExceptionToResponseIfPossible(ex.getCause(), message);
        } catch (RuntimeException ex) {
            convertExceptionToResponseIfPossible(ex, message);
        }
        Response r = message.getExchange().get(Response.class);
        if (r != null) {
            createOutMessage(message, r);
            message.getInterceptorChain().doInterceptStartingAt(message,
                                                                OutgoingChainInterceptor.class.getName());
        }
    }
    
    private void processRequest(Message message) {
        
        ProviderFactory providerFactory = ProviderFactory.getInstance(message);
        
        RequestPreprocessor rp = providerFactory.getRequestPreprocessor();
        if (rp != null) {
            rp.preprocess(message, new UriInfoImpl(message, null));
            if (message.getExchange().get(Response.class) != null) {
                return;
            }
        }
        
        // Global pre-match request filters
        if (JAXRSUtils.runContainerRequestFilters(providerFactory, message, true, null)) {
            return;
        }
        // HTTP method
        String httpMethod = HttpUtils.getProtocolHeader(message, Message.HTTP_REQUEST_METHOD, 
                                                        HttpMethod.POST, true);
        
        // Path to match
        String rawPath = HttpUtils.getPathToMatch(message, true);
        
        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        
        // Content-Type
        String requestContentType = null;
        List<String> ctHeaderValues = protocolHeaders.get(Message.CONTENT_TYPE);
        if (ctHeaderValues != null) {
            requestContentType = ctHeaderValues.get(0);
            message.put(Message.CONTENT_TYPE, requestContentType);
        }
        if (requestContentType == null) {
            requestContentType = (String)message.get(Message.CONTENT_TYPE);
        
            if (requestContentType == null) {
                requestContentType = MediaType.WILDCARD;
            }
        }
        
        // Accept
        String acceptTypes = null;
        List<String> acceptHeaderValues = protocolHeaders.get(Message.ACCEPT_CONTENT_TYPE);
        if (acceptHeaderValues != null) {
            acceptTypes = acceptHeaderValues.get(0);
            message.put(Message.ACCEPT_CONTENT_TYPE, acceptTypes);
        }
        
        if (acceptTypes == null) {
            acceptTypes = HttpUtils.getProtocolHeader(message, Message.ACCEPT_CONTENT_TYPE, null);
            if (acceptTypes == null) {
                acceptTypes = "*/*";
                message.put(Message.ACCEPT_CONTENT_TYPE, acceptTypes);
            }
        }
        List<MediaType> acceptContentTypes = null;
        try {
            acceptContentTypes = JAXRSUtils.sortMediaTypes(acceptTypes, JAXRSUtils.MEDIA_TYPE_Q_PARAM);
        } catch (IllegalArgumentException ex) {
            throw ExceptionUtils.toNotAcceptableException(null, null);
        }
        message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);

        //1. Matching target resource class
        Service service = message.getExchange().get(Service.class);
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)service).getClassResourceInfos();

        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, 
                                          rawPath, 
                                          values,
                                          message);
        if (resource == null) {

            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_ROOT_EXC", 
                                                   BUNDLE,
                                                   message.get(Message.REQUEST_URI),
                                                   rawPath);
            LOG.warning(errorMsg.toString());
            Response resp = JAXRSUtils.createResponse(null, message, errorMsg.toString(), 
                    Response.Status.NOT_FOUND.getStatusCode(), false);
            throw ExceptionUtils.toNotFoundException(null, resp);
        }

        message.getExchange().put(JAXRSUtils.ROOT_RESOURCE_CLASS, resource);

        OperationResourceInfo ori = null;     
        
        boolean operChecked = false;
        List<ProviderInfo<RequestHandler>> shs = providerFactory.getRequestHandlers();
        for (ProviderInfo<RequestHandler> sh : shs) {
            if (ori == null && !operChecked) {
                try {                
                    ori = JAXRSUtils.findTargetMethod(resource, 
                        message, httpMethod, values, 
                        requestContentType, acceptContentTypes, false);
                    setExchangeProperties(message, ori, values, resources.size());
                } catch (WebApplicationException ex) {
                    operChecked = true;
                }
                
            }
            InjectionUtils.injectContexts(sh.getProvider(), sh, message);
            Response response = sh.getProvider().handleRequest(message, resource);
            if (response != null) {
                message.getExchange().put(Response.class, response);
                return;
            }
            
        }
        
        if (ori == null) {
            try {                
                ori = JAXRSUtils.findTargetMethod(resource, message, 
                                            httpMethod, values, requestContentType, acceptContentTypes, true);
                setExchangeProperties(message, ori, values, resources.size());
            } catch (WebApplicationException ex) {
                if (JAXRSUtils.noResourceMethodForOptions(ex.getResponse(), httpMethod)) {
                    Response response = JAXRSUtils.createResponse(resource, null, null, 200, true);
                    message.getExchange().put(Response.class, response);
                    return;
                } else {
                    throw ex;
                }
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Request path is: " + rawPath);
            LOG.fine("Request HTTP method is: " + httpMethod);
            LOG.fine("Request contentType is: " + requestContentType);
            LOG.fine("Accept contentType is: " + acceptTypes);

            LOG.fine("Found operation: " + ori.getMethodToInvoke().getName());
        }
        
        setExchangeProperties(message, ori, values, resources.size());
        
        // Global and name-bound post-match request filters
        if (!ori.isSubResourceLocator()
            && JAXRSUtils.runContainerRequestFilters(providerFactory,
                                                      message,
                                                      false, 
                                                      ori.getNameBindings())) {
            return;
        }
        
        
        //Process parameters
        try {
            List<Object> params = JAXRSUtils.processParameters(ori, values, message);
            message.setContent(List.class, params);
        } catch (IOException ex) {
            convertExceptionToResponseIfPossible(ex, message);
        }
        
    }
    
    private void convertExceptionToResponseIfPossible(Throwable ex, Message message) {
        Response excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
        if (excResponse == null) {
            ProviderFactory.getInstance(message).clearThreadLocalProxies();
            message.getExchange().put(Message.PROPOGATE_EXCEPTION, 
                                      ExceptionUtils.propogateException(message));
            throw ex instanceof RuntimeException ? (RuntimeException)ex 
                : ExceptionUtils.toInternalServerErrorException(ex, null);
        }
        message.getExchange().put(Response.class, excResponse);
    }
    
    private void setExchangeProperties(Message message, OperationResourceInfo ori, 
                                      MultivaluedMap<String, String> values,
                                      int numberOfResources) {
        message.getExchange().put(OperationResourceInfo.class, ori);
        message.put(RESOURCE_METHOD, ori.getMethodToInvoke());
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
        
        String plainOperationName = ori.getMethodToInvoke().getName();
        if (numberOfResources > 1) {
            plainOperationName = ori.getClassResourceInfo().getServiceClass().getSimpleName()
                + "#" + plainOperationName;
        }
        message.getExchange().put(RESOURCE_OPERATION_NAME, plainOperationName);
        
        boolean oneway = ori.isOneway() 
            || MessageUtils.isTrue(HttpUtils.getProtocolHeader(message, Message.ONE_WAY_REQUEST, null));
        message.getExchange().setOneWay(oneway);
    }
    
    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        
        Object mapProp = message.getContextualProperty("map.cxf.interceptor.fault");
        if (MessageUtils.isTrue(mapProp)) {
        
            Throwable ex = message.getContent(Exception.class);
            if (ex instanceof Fault) {
                ex = ((Fault)ex).getCause();
            }
            Response r = JAXRSUtils.convertFaultToResponse(ex, message);
            if (r != null) {
                message.removeContent(Exception.class);
                message.getInterceptorChain().setFaultObserver(null);
                if (message.getContextualProperty(FaultListener.class.getName()) == null) {
                    message.put(FaultListener.class.getName(), new NoOpFaultListener());
                }
                
                createOutMessage(message, r);
                
                Iterator<Interceptor<? extends Message>> iterator = message.getInterceptorChain().iterator();
                while (iterator.hasNext()) {
                    Interceptor<? extends Message> inInterceptor = iterator.next();
                    if (inInterceptor.getClass() == OutgoingChainInterceptor.class) {
                        ((OutgoingChainInterceptor)inInterceptor).handleMessage(message);
                        return;
                    }
                }
            }
        }
        
        
        LOG.fine("Cleanup thread local variables");
        
        Object rootInstance = message.getExchange().remove(JAXRSUtils.ROOT_INSTANCE);
        Object rootProvider = message.getExchange().remove(JAXRSUtils.ROOT_PROVIDER);
        if (rootInstance != null && rootProvider != null) {
            try {
                ((ResourceProvider)rootProvider).releaseInstance(message, rootInstance);
            } catch (Throwable tex) {
                LOG.warning("Exception occurred during releasing the service instance, " + tex.getMessage());
            }
        }
        ProviderFactory.getInstance(message).clearThreadLocalProxies();
        ClassResourceInfo cri = (ClassResourceInfo)message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
        if (cri != null) {
            cri.clearThreadLocalProxies();
        }
    }
    
    private Message createOutMessage(Message inMessage, Response r) {
        Endpoint e = inMessage.getExchange().get(Endpoint.class);
        Message mout = new MessageImpl();
        mout.setContent(List.class, new MessageContentsList(r));
        mout.setExchange(inMessage.getExchange());
        mout = e.getBinding().createMessage(mout);
        mout.setInterceptorChain(
             OutgoingChainInterceptor.getOutInterceptorChain(inMessage.getExchange()));
        inMessage.getExchange().setOutMessage(mout);
        if (r.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
            inMessage.getExchange().put("cxf.io.cacheinput", Boolean.FALSE);
        }
        return mout;
    }
}
