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

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

public class JAXRSInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInInterceptor.class);
    
    public JAXRSInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void handleMessage(Message message) {
        
        try {
            processRequest(message);
        } catch (RuntimeException ex) {
            Response excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
            if (excResponse == null) {
                ProviderFactory.getInstance(message).clearThreadLocalProxies();
                message.getExchange().put(Message.PROPOGATE_EXCEPTION, 
                                          JAXRSUtils.propogateException(message));
                throw ex;
            }
            message.getExchange().put(Response.class, excResponse);
        }
        
        
    }
    
    private void processRequest(Message message) {
        
        if (message.getExchange().get(OperationResourceInfo.class) != null) {
            // it's a suspended invocation;
            return;
        }
        
        RequestPreprocessor rp = ProviderFactory.getInstance(message).getRequestPreprocessor();
        if (rp != null) {
            rp.preprocess(message, new UriInfoImpl(message, null));
            if (message.getExchange().get(Response.class) != null) {
                return;
            }
        }
        
        String requestContentType = (String)message.get(Message.CONTENT_TYPE);
        if (requestContentType == null) {
            requestContentType = "*/*";
        }
        
        String rawPath = HttpUtils.getPathToMatch(message, true);
        
        //1. Matching target resource class
        Service service = message.getExchange().get(Service.class);
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)service).getClassResourceInfos();

        String acceptTypes = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
        if (acceptTypes == null) {
            acceptTypes = "*/*";
            message.put(Message.ACCEPT_CONTENT_TYPE, acceptTypes);
        }
        List<MediaType> acceptContentTypes = JAXRSUtils.sortMediaTypes(acceptTypes);
        message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);

        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, 
                                          rawPath, 
                                          values,
                                          message);
        if (resource == null) {
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_ROOT_EXC", 
                                                   BUNDLE, 
                                                   rawPath);
            LOG.warning(errorMsg.toString());

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        message.getExchange().put(JAXRSUtils.ROOT_RESOURCE_CLASS, resource);

        String httpMethod = (String)message.get(Message.HTTP_REQUEST_METHOD);
        OperationResourceInfo ori = null;     
        
        boolean operChecked = false;
        List<ProviderInfo<RequestHandler>> shs = ProviderFactory.getInstance(message).getRequestHandlers();
        for (ProviderInfo<RequestHandler> sh : shs) {
            String newAcceptTypes = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
            if (!acceptTypes.equals(newAcceptTypes) || (ori == null && !operChecked)) {
                acceptTypes = newAcceptTypes;
                acceptContentTypes = JAXRSUtils.sortMediaTypes(newAcceptTypes);
                message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);
                if (ori != null) {
                    values = new MetadataMap<String, String>();
                    resource = JAXRSUtils.selectResourceClass(resources, 
                                                              rawPath, 
                                                              values,
                                                              message);
                }
                try {                
                    ori = JAXRSUtils.findTargetMethod(resource, 
                        message, httpMethod, values, 
                        requestContentType, acceptContentTypes, false);
                    setMessageProperties(message, ori, values);
                } catch (WebApplicationException ex) {
                    operChecked = true;
                }
                
            }
            InjectionUtils.injectContextFields(sh.getProvider(), sh, message);
            InjectionUtils.injectContextFields(sh.getProvider(), sh, message);
            Response response = sh.getProvider().handleRequest(message, resource);
            if (response != null) {
                message.getExchange().put(Response.class, response);
                return;
            }
        }
        
        String newAcceptTypes = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
        if (!acceptTypes.equals(newAcceptTypes) || ori == null) {
            acceptTypes = newAcceptTypes;
            acceptContentTypes = JAXRSUtils.sortMediaTypes(acceptTypes);
            message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);
            if (ori != null) {
                values = new MetadataMap<String, String>();
                resource = JAXRSUtils.selectResourceClass(resources, 
                                                          rawPath, 
                                                          values,
                                                          message);
            }
            try {                
                ori = JAXRSUtils.findTargetMethod(resource, message, 
                                            httpMethod, values, requestContentType, acceptContentTypes, true);
                setMessageProperties(message, ori, values);
            } catch (WebApplicationException ex) {
                if (ex.getResponse() != null && ex.getResponse().getStatus() == 405 
                    && "OPTIONS".equalsIgnoreCase(httpMethod)) {
                    Response response = JAXRSUtils.createResponseBuilder(resource, 200, true).build();
                    message.getExchange().put(Response.class, response);
                    return;
                } else {
                    throw ex;
                }
            }
        }

        
        LOG.fine("Request path is: " + rawPath);
        LOG.fine("Request HTTP method is: " + httpMethod);
        LOG.fine("Request contentType is: " + requestContentType);
        LOG.fine("Accept contentType is: " + acceptTypes);
        
        LOG.fine("Found operation: " + ori.getMethodToInvoke().getName());
        setMessageProperties(message, ori, values);  
      
        //Process parameters
        List<Object> params = JAXRSUtils.processParameters(ori, values, message);
        message.setContent(List.class, params);
    }
    
    private void setMessageProperties(Message message, OperationResourceInfo ori, 
                                      MultivaluedMap<String, String> values) {
        message.getExchange().put(OperationResourceInfo.class, ori);
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
        message.getExchange().put("org.apache.cxf.management.operation.name", 
                                  ori.getMethodToInvoke().getName());
    }
}
