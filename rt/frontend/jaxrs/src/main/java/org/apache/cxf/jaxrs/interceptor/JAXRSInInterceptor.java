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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
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
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.logging.NoOpFaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

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
        
        
    }
    
    private void processRequest(Message message) {
        
        ServerProviderFactory providerFactory = ServerProviderFactory.getInstance(message);
        
        RequestPreprocessor rp = providerFactory.getRequestPreprocessor();
        if (rp != null) {
            rp.preprocess(message, new UriInfoImpl(message, null));
        }
        
        // Global pre-match request filters
        if (JAXRSUtils.runContainerRequestFilters(providerFactory, message, true, null, false)) {
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
            throw new NotAcceptableException();
        }
        message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);

        //1. Matching target resource class
        List<ClassResourceInfo> resources = JAXRSUtils.getRootResources(message);
        Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources = 
            JAXRSUtils.selectResourceClass(resources, rawPath, message);
        if (matchedResources == null) {
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_ROOT_EXC", 
                                                   BUNDLE,
                                                   message.get(Message.REQUEST_URI),
                                                   rawPath);
            LOG.warning(errorMsg.toString());
            Response resp = JAXRSUtils.createResponse(resources, message, errorMsg.toString(), 
                    Response.Status.NOT_FOUND.getStatusCode(), false);
            throw new NotFoundException(resp);
        }

        MultivaluedMap<String, String> matchedValues = new MetadataMap<String, String>();
                
        OperationResourceInfo ori = null;     
        
        try {                
            ori = JAXRSUtils.findTargetMethod(matchedResources, message, 
                      httpMethod, matchedValues, requestContentType, acceptContentTypes, true);
            setExchangeProperties(message, ori, matchedValues, resources.size());
        } catch (WebApplicationException ex) {
            if (JAXRSUtils.noResourceMethodForOptions(ex.getResponse(), httpMethod)) {
                Response response = JAXRSUtils.createResponse(resources, null, null, 200, true);
                message.getExchange().put(Response.class, response);
                return;
            } else {
                throw ex;
            }
        }
        

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Request path is: " + rawPath);
            LOG.fine("Request HTTP method is: " + httpMethod);
            LOG.fine("Request contentType is: " + requestContentType);
            LOG.fine("Accept contentType is: " + acceptTypes);

            LOG.fine("Found operation: " + ori.getMethodToInvoke().getName());
        }
        
        setExchangeProperties(message, ori, matchedValues, resources.size());
        
        // Global and name-bound post-match request filters
        if (JAXRSUtils.runContainerRequestFilters(providerFactory,
                                                  message,
                                                  false, 
                                                  ori.getNameBindings(),
                                                  false)) {
            return;
        }
        
        
        //Process parameters
        try {
            List<Object> params = JAXRSUtils.processParameters(ori, matchedValues, message);
            message.setContent(List.class, params);
        } catch (IOException ex) {
            convertExceptionToResponseIfPossible(ex, message);
        }
        
    }
    
    private void convertExceptionToResponseIfPossible(Throwable ex, Message message) {
        Response excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
        if (excResponse == null) {
            ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
            message.getExchange().put(Message.PROPOGATE_EXCEPTION, 
                                      JAXRSUtils.propogateException(message));
            throw ex instanceof RuntimeException ? (RuntimeException)ex : new InternalServerErrorException(ex);
        }
        message.getExchange().put(Response.class, excResponse);
    }
    
    private void setExchangeProperties(Message message, OperationResourceInfo ori, 
                                      MultivaluedMap<String, String> values,
                                      int numberOfResources) {
        message.getExchange().put(OperationResourceInfo.class, ori);
        message.getExchange().put(JAXRSUtils.ROOT_RESOURCE_CLASS, ori.getClassResourceInfo());
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
        if (mapProp == null || MessageUtils.isTrue(mapProp)) {
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
                
                Endpoint e = message.getExchange().get(Endpoint.class);
                Message mout = new MessageImpl();
                mout.setContent(List.class, new MessageContentsList(r));
                mout.setExchange(message.getExchange());
                mout = e.getBinding().createMessage(mout);
                mout.setInterceptorChain(OutgoingChainInterceptor.getOutInterceptorChain(message.getExchange()));
                message.getExchange().setOutMessage(mout);
                
                
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
        ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
        ClassResourceInfo cri = (ClassResourceInfo)message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
        if (cri != null) {
            cri.clearThreadLocalProxies();
        }
    }
}
