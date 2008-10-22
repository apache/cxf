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
import org.apache.cxf.interceptor.Fault;
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
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

public class JAXRSInInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String RELATIVE_PATH = "relative.path";
    public static final String ROOT_RESOURCE_CLASS = "root.resource.class";

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInInterceptor.class);

    public JAXRSInInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) {
        
        try {
            processRequest(message);
        } catch (RuntimeException ex) {
            Response excResponse = JAXRSUtils.convertFaultToResponse(ex);
            if (excResponse == null) {
                ProviderFactory.getInstance().cleatThreadLocalProxies();
                throw ex;
            }
            message.getExchange().put(Response.class, excResponse);
        }
        
        
    }
    
    private static String updatePath(String path, String address) {
        if (path.startsWith(address)) {
            path = path.substring(address.length());
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }

        return path;
    }
    
    private void processRequest(Message message) {
        RequestPreprocessor rp = 
            ProviderFactory.getInstance().getRequestPreprocessor();
        if (rp != null) {
            rp.preprocess(message, new UriInfoImpl(message, null));
        }
        
        String path = (String)message.get(Message.REQUEST_URI);
        String address = (String)message.get(Message.BASE_PATH);
        String httpMethod = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String requestContentType = (String)message.get(Message.CONTENT_TYPE);
        if (requestContentType == null) {
            requestContentType = "*/*";
        }
        
        if (address.startsWith("http")) {
            int idx = address.indexOf('/', 7);
            if (idx != -1) {
                address = address.substring(idx);
            }
        }
        path = updatePath(path, address);
        
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
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, path, values);
        if (resource == null) {
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_ROOT_EXC", 
                                                   BUNDLE, 
                                                   path);
            LOG.severe(errorMsg.toString());

            throw new WebApplicationException(404);
        }

        message.getExchange().put(ROOT_RESOURCE_CLASS, resource);

        OperationResourceInfo ori = null;     
        
        List<ProviderInfo<RequestHandler>> shs = 
            ProviderFactory.getInstance().getRequestHandlers();
        for (ProviderInfo<RequestHandler> sh : shs) {
            String newAcceptTypes = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
            if (!acceptTypes.equals(newAcceptTypes) || ori == null) {
                acceptTypes = newAcceptTypes;
                acceptContentTypes = JAXRSUtils.sortMediaTypes(newAcceptTypes);
                message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);
                
                if (ori != null) {
                    values = new MetadataMap<String, String>();
                    resource = JAXRSUtils.selectResourceClass(resources, path, values);
                }
                ori = JAXRSUtils.findTargetMethod(resource, values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                                  httpMethod, values, requestContentType, acceptContentTypes);
                message.getExchange().put(OperationResourceInfo.class, ori);
            }
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
                resource = JAXRSUtils.selectResourceClass(resources, path, values);
            }
            ori = JAXRSUtils.findTargetMethod(resource, values.getFirst(URITemplate.FINAL_MATCH_GROUP), 
                                              httpMethod, values, requestContentType, acceptContentTypes);
            message.getExchange().put(OperationResourceInfo.class, ori);
        }

        
        LOG.fine("Request path is: " + path);
        LOG.fine("Request HTTP method is: " + httpMethod);
        LOG.fine("Request contentType is: " + requestContentType);
        LOG.fine("Accept contentType is: " + acceptTypes);
        
        if (ori == null) {
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_OP_EXC", 
                                                   BUNDLE, 
                                                   path,
                                                   requestContentType,
                                                   acceptTypes);
            LOG.severe(errorMsg.toString());
            throw new Fault(errorMsg);
        }
        LOG.fine("Found operation: " + ori.getMethodToInvoke().getName());
        
        message.getExchange().put(OperationResourceInfo.class, ori);
        message.put(RELATIVE_PATH, values.getFirst(URITemplate.FINAL_MATCH_GROUP));
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
      
        //2. Process parameters
        List<Object> params = JAXRSUtils
            .processParameters(ori, values, message);

        message.setContent(List.class, params);
    }
}
