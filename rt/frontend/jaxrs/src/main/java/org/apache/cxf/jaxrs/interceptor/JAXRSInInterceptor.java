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
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
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
        final Exchange exchange = message.getExchange(); 
        
        exchange.put(Message.REST_MESSAGE, Boolean.TRUE);
        
        try {
            processRequest(message);
            if (exchange.isOneWay()) {
                ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
            }
        } catch (Fault ex) {
            convertExceptionToResponseIfPossible(ex.getCause(), message);
        } catch (RuntimeException ex) {
            convertExceptionToResponseIfPossible(ex, message);
        }
        
        Response r = exchange.get(Response.class);
        if (r != null) {
            createOutMessage(message, r);
            message.getInterceptorChain().doInterceptStartingAt(message,
                                                                OutgoingChainInterceptor.class.getName());
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
        final Exchange exchange = message.getExchange();
        final ClassResourceInfo cri = ori.getClassResourceInfo();
        exchange.put(OperationResourceInfo.class, ori);
        exchange.put(JAXRSUtils.ROOT_RESOURCE_CLASS, cri);
        message.put(RESOURCE_METHOD, ori.getMethodToInvoke());
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
        
        String plainOperationName = ori.getMethodToInvoke().getName();
        if (numberOfResources > 1) {
            plainOperationName = cri.getServiceClass().getSimpleName() + "#" + plainOperationName;
        }
        exchange.put(RESOURCE_OPERATION_NAME, plainOperationName);
        
        if (ori.isOneway() 
            || MessageUtils.isTrue(HttpUtils.getProtocolHeader(message, Message.ONE_WAY_REQUEST, null))) {
            exchange.setOneWay(true);        
        }
        ResourceProvider rp = cri.getResourceProvider(); 
        if (rp instanceof SingletonResourceProvider) {
            //cri.isSingleton is not guaranteed to indicate we have a 'pure' singleton 
            exchange.put(Message.SERVICE_OBJECT, rp.getInstance(message));
        }
    }
    
    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
    }
    
    private Message createOutMessage(Message inMessage, Response r) {
        Endpoint e = inMessage.getExchange().get(Endpoint.class);
        Message mout = e.getBinding().createMessage();
        mout.setContent(List.class, new MessageContentsList(r));
        mout.setExchange(inMessage.getExchange());
        mout.setInterceptorChain(
             OutgoingChainInterceptor.getOutInterceptorChain(inMessage.getExchange()));
        inMessage.getExchange().setOutMessage(mout);
        return mout;
    }
}
