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
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodInvocationInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.AbstractInvoker;

public class JAXRSInvoker extends AbstractInvoker {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSServiceFactoryBean.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInvoker.class);
    private static final String SERVICE_LOADER_AS_CONTEXT = "org.apache.cxf.serviceloader-context";
    
    public JAXRSInvoker() {
    }

    public Object invoke(Exchange exchange, Object request) {
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
        
        return invoke(exchange, request, getServiceObject(exchange));
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Exchange exchange, Object request, Object resourceObject) {

        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        ClassResourceInfo cri = ori.getClassResourceInfo();
        
        pushOntoStack(ori, ClassHelper.getRealClass(resourceObject), exchange.getInMessage());
                
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

        List<Object> params = null;
        if (request instanceof List) {
            params = CastUtils.cast((List<?>)request);
        } else if (request != null) {
            params = new MessageContentsList(request);
        }

        Object result = null;
        ClassLoader contextLoader = null;
        try {
            if (setServiceLoaderAsContextLoader(exchange.getInMessage())) {
                contextLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(resourceObject.getClass().getClassLoader());
            }
            result = invoke(exchange, resourceObject, methodToInvoke, params);
        } catch (Fault ex) {
            Response excResponse = JAXRSUtils.convertFaultToResponse(ex.getCause(), 
                                                                     exchange.getInMessage());
            if (excResponse == null) {
                ProviderFactory.getInstance(exchange.getInMessage()).clearThreadLocalProxies();
                ClassResourceInfo criRoot =
                    (ClassResourceInfo)exchange.get(JAXRSInInterceptor.ROOT_RESOURCE_CLASS);
                if (criRoot != null) {
                    criRoot.clearThreadLocalProxies();
                }
                exchange.put(Message.PROPOGATE_EXCEPTION, Boolean.TRUE);
                throw ex;
            }
            return new MessageContentsList(excResponse);
        } finally {
            if (contextLoader != null) {
                Thread.currentThread().setContextClassLoader(contextLoader);
            }
        }

        if (ori.isSubResourceLocator()) {
            try {
                Message msg = exchange.getInMessage();
                MultivaluedMap<String, String> values = getTemplateValues(msg);
                String subResourcePath = (String)msg.get(JAXRSInInterceptor.RELATIVE_PATH);
                String httpMethod = (String)msg.get(Message.HTTP_REQUEST_METHOD);
                String contentType = (String)msg.get(Message.CONTENT_TYPE);
                if (contentType == null) {
                    contentType = "*/*";
                }
                List<MediaType> acceptContentType =
                    (List<MediaType>)msg.getExchange().get(Message.ACCEPT_CONTENT_TYPE);

                result = checkResultObject(result, subResourcePath);

                ClassResourceInfo subCri = cri.getSubResource(
                     methodToInvoke.getReturnType(),
                     ClassHelper.getRealClass(result));
                if (subCri == null) {
                    org.apache.cxf.common.i18n.Message errorM =
                        new org.apache.cxf.common.i18n.Message("NO_SUBRESOURCE_FOUND",
                                                               BUNDLE,
                                                               subResourcePath);
                    LOG.severe(errorM.toString());
                    throw new WebApplicationException(404);
                }

                OperationResourceInfo subOri = JAXRSUtils.findTargetMethod(subCri,
                                                         subResourcePath,
                                                         httpMethod,
                                                         values,
                                                         contentType,
                                                         acceptContentType);


                exchange.put(OperationResourceInfo.class, subOri);
                msg.put(JAXRSInInterceptor.RELATIVE_PATH,
                        values.getFirst(URITemplate.FINAL_MATCH_GROUP));
                msg.put(URITemplate.TEMPLATE_PARAMETERS, values);
                // work out request parameters for the sub-resouce class. Here we
                // presume Inputstream has not been consumed yet by the root resource class.
                //I.e., only one place either in the root resource or sub-resouce class can
                //have a parameter that read from entitybody.
                List<Object> newParams = JAXRSUtils.processParameters(subOri, values, msg);
                msg.setContent(List.class, newParams);

                return this.invoke(exchange, newParams, result);
            } catch (WebApplicationException ex) {
                Response excResponse = JAXRSUtils.convertFaultToResponse(ex, 
                                                                         exchange.getInMessage());
                return new MessageContentsList(excResponse);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap getTemplateValues(Message msg) {
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        MultivaluedMap<String, String> oldValues = 
            (MultivaluedMap<String, String>)msg.get(URITemplate.TEMPLATE_PARAMETERS);
        if (oldValues != null) {
            values.putAll(oldValues);
        }
        return values;
    }
    
    private boolean setServiceLoaderAsContextLoader(Message inMessage) {
        Object en = inMessage.getContextualProperty(SERVICE_LOADER_AS_CONTEXT);
        return Boolean.TRUE.equals(en) || "true".equals(en);
    }
    
    public Object getServiceObject(Exchange exchange) {
        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        ClassResourceInfo cri = ori.getClassResourceInfo();

        return cri.getResourceProvider().getInstance(exchange.getInMessage());
    }

    private static Object checkResultObject(Object result, String subResourcePath) {
        

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
        if (result == null) {
            org.apache.cxf.common.i18n.Message errorM =
                new org.apache.cxf.common.i18n.Message("NULL_SUBRESOURCE",
                                                       BUNDLE,
                                                       subResourcePath);
            LOG.info(errorM.toString());
            throw new WebApplicationException(404);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void pushOntoStack(OperationResourceInfo ori, Class<?> realClass, Message msg) {
        OperationResourceInfoStack stack = msg.get(OperationResourceInfoStack.class);
        if (stack == null) {
            stack = new OperationResourceInfoStack();
            msg.put(OperationResourceInfoStack.class, stack);
        }
        
        
        MultivaluedMap<String, String> params = 
            (MultivaluedMap)msg.get(URITemplate.TEMPLATE_PARAMETERS);
        List<String> values = null;
        if (params == null || params.size() == 1) {
            values = Collections.emptyList();
        } else {
            values = new ArrayList<String>(params.size() - 1);
            for (Parameter pm : ori.getParameters()) {
                if (pm.getType() == ParameterType.PATH) {
                    List<String> paramValues = params.get(pm.getName());
                    if (paramValues != null) {
                        values.addAll(paramValues);
                    }
                    
                }
            }
        }
        stack.push(new MethodInvocationInfo(ori, realClass, values));
    }
}
