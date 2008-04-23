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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.AbstractInvoker;

public class JAXRSInvoker extends AbstractInvoker {
    private List<Object> resourceObjects;

    public JAXRSInvoker() {
    }
    
    public JAXRSInvoker(List<Object> resourceObjects) {
        this.resourceObjects = resourceObjects;
    }
    public Object invoke(Exchange exchange, Object request) {
        return invoke(exchange, request, resourceObjects);
    }    
    public Object invoke(Exchange exchange, Object request, List<Object> resources) {
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);

        ClassResourceInfo cri = ori.getClassResourceInfo();
        Method methodToInvoke = cri.getMethodDispatcher().getMethod(ori);
        Object resourceObject = getServiceObject(exchange, resources);
        
        // TODO : update the method dispatcher
        if (Proxy.class.isInstance(resourceObject)) {
            
            for (Class<?> c : resourceObject.getClass().getInterfaces()) {
                try {
                    Method m = c.getMethod(
                        methodToInvoke.getName(), methodToInvoke.getParameterTypes());
                    if (m != null) {
                        methodToInvoke = m;
                        break;
                    }
                } catch (NoSuchMethodException ex) {
                    //ignore
                }
            }
            
        }
        
        if (cri.isRoot()) {
            JAXRSUtils.injectHttpContextValues(resourceObject, 
                                               ori, 
                                               exchange.getInMessage());
            JAXRSUtils.injectServletResourceValues(resourceObject, 
                                               ori, 
                                               exchange.getInMessage());
        }

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
            if (ex.getCause() instanceof WebApplicationException) {
                WebApplicationException wex = (WebApplicationException)ex.getCause();
                if (wex.getResponse() != null) {
                    result = wex.getResponse();
                } else {
                    result = Response.serverError().build();
                }
                return new MessageContentsList(result);
            }
            throw ex;
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
            String acceptContentType = (String)msg.get(Message.ACCEPT_CONTENT_TYPE);
            if (acceptContentType == null) {
                acceptContentType = "*/*";
            }
            ClassResourceInfo subCri = JAXRSUtils.findSubResourceClass(cri, result.getClass());
            OperationResourceInfo subOri = JAXRSUtils.findTargetMethod(subCri, 
                                                                       subResourcePath, 
                                                                       httpMethod, 
                                                                       values, 
                                                                       contentType, 
                                                                       acceptContentType);
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
                }
            }
        }
        
        if (serviceObject == null) {
            serviceObject = cri.getResourceProvider().getInstance();
        }
        
        return serviceObject;
    }
    
    
}
