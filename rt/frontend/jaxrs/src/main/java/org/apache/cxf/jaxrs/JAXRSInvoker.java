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

package org.apache.cxf.jaxrs;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.AbstractInvoker;

public class JAXRSInvoker extends AbstractInvoker {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSServiceFactoryBean.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInvoker.class);
    
    private List<Object> resourceObjects;

    public JAXRSInvoker() {
    }
    
    public JAXRSInvoker(List<Object> resourceObjects) {
        this.resourceObjects = resourceObjects;
    }
    public Object invoke(Exchange exchange, Object request) {
        return invoke(exchange, request, resourceObjects);
    }    
    @SuppressWarnings("unchecked")
    public Object invoke(Exchange exchange, Object request, List<Object> resources) {
        
        Response response = exchange.get(Response.class);
        if (response != null) {
            // this means a blocking request filter provided a Response
            // or earlier exception has been converted to Response
            
            //TODO: should we remove response from exchange ?
            //      or should we rather ignore content list and have 
            //      Response set here for all cases and extract it 
            //      in the out interceptor instead of dealing with the contents list ?  
            return new MessageContentsList(response);    
        }
        
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);

        ClassResourceInfo cri = ori.getClassResourceInfo();
        Object resourceObject = getServiceObject(exchange, resources);
        
        Method methodToInvoke = InjectionUtils.checkProxy(
             cri.getMethodDispatcher().getMethod(ori), resourceObject);
        
        if (cri.isRoot()) {
            JAXRSUtils.handleSetters(ori, resourceObject, 
                                     exchange.getInMessage());
            
            InjectionUtils.injectContextFields(resourceObject, 
                                               ori.getClassResourceInfo(), 
                                               exchange.getInMessage());
            InjectionUtils.injectResourceFields(resourceObject, 
                                            ori.getClassResourceInfo(), 
                                            exchange.getInMessage());
        }

        String baseAddress = HttpUtils.getOriginalAddress(exchange.getInMessage());
        
        List<Object> params = null;
        if (request instanceof List) {
            params = CastUtils.cast((List<?>)request);
        } else if (request != null) {
            params = new MessageContentsList(request);
        }

        Object result = null;
        try {
            result = invoke(exchange, resourceObject, methodToInvoke, params);
        } catch (Fault ex) {
            Response excResponse = JAXRSUtils.convertFaultToResponse(ex.getCause(), baseAddress);
            if (excResponse == null) {
                ProviderFactory.getInstance(baseAddress).clearThreadLocalProxies();
                ClassResourceInfo criRoot =
                    (ClassResourceInfo)exchange.get(JAXRSInInterceptor.ROOT_RESOURCE_CLASS);
                if (criRoot != null) {
                    criRoot.clearThreadLocalProxies();
                }
                throw ex;
            }
            return new MessageContentsList(excResponse);
        }
        
        if (ori.isSubResourceLocator()) {
            //the result becomes the object that will handle the request
            if (result != null) {
                if (result instanceof MessageContentsList) {
                    result = ((MessageContentsList)result).get(0);
                } else if (result instanceof List) {
                    result = ((List)result).get(0);
                } else if (result.getClass().isArray()) {
                    result = ((Object[])result)[0];
                } 
            }
            List<Object> newResourceObjects = new ArrayList<Object>();
            newResourceObjects.add(result);
        
            Message msg = exchange.getInMessage();
            MultivaluedMap<String, String> values = new MetadataMap<String, String>();                 
            String subResourcePath = (String)msg.get(JAXRSInInterceptor.RELATIVE_PATH);
            String httpMethod = (String)msg.get(Message.HTTP_REQUEST_METHOD); 
            String contentType = (String)msg.get(Message.CONTENT_TYPE);
            if (contentType == null) {
                contentType = "*/*";
            }
            List<MediaType> acceptContentType = 
                (List<MediaType>)msg.getExchange().get(Message.ACCEPT_CONTENT_TYPE);

            ClassResourceInfo subCri = JAXRSUtils.findSubResourceClass(cri, result.getClass());
            if (subCri == null) {
                org.apache.cxf.common.i18n.Message errorM = 
                    new org.apache.cxf.common.i18n.Message("NO_SUBRESOURCE_FOUND",  
                                                           BUNDLE, 
                                                           subResourcePath);
                LOG.severe(errorM.toString());
                throw new WebApplicationException(404);
            }
            
            OperationResourceInfo subOri = null;
            try {
                subOri = JAXRSUtils.findTargetMethod(subCri, 
                                                     subResourcePath, 
                                                     httpMethod, 
                                                     values, 
                                                     contentType, 
                                                     acceptContentType);
            } catch (WebApplicationException ex) {
                Response excResponse = JAXRSUtils.convertFaultToResponse(ex, baseAddress);
                return new MessageContentsList(excResponse);
            }
            
            exchange.put(OperationResourceInfo.class, subOri);
            msg.put(JAXRSInInterceptor.RELATIVE_PATH, values.getFirst(URITemplate.FINAL_MATCH_GROUP));
            msg.put(URITemplate.TEMPLATE_PARAMETERS, values);
            // work out request parameters for the sub-resouce class. Here we
            // presume Inputstream has not been consumed yet by the root resource class.
            //I.e., only one place either in the root resource or sub-resouce class can
            //have a parameter that read from entitybody.
            List<Object> newParams = JAXRSUtils.processParameters(subOri, values, msg);
            msg.setContent(List.class, newParams);
            
            return this.invoke(exchange, newParams, newResourceObjects);
        }
        
        return result;
    }    
    
    public Object getServiceObject(Exchange exchange) {
        return getServiceObject(exchange, resourceObjects);
    }
    public Object getServiceObject(Exchange exchange, List<Object> resources) {
        Object serviceObject = null;
        
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        ClassResourceInfo cri = ori.getClassResourceInfo();
        
        if (resources != null) {
            Class c  = cri.getResourceClass();
            for (Object resourceObject : resources) {
                if (c.isInstance(resourceObject)) {
                    serviceObject = resourceObject;
                    break;
                }
            }
        }
        
        if (serviceObject == null) {
            serviceObject = cri.getResourceProvider().getInstance();
        }
        
        return serviceObject;
    }
    
    
}
