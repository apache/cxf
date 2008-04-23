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

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.JAXRSUtils;
import org.apache.cxf.jaxrs.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.SystemQueryHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

public class JAXRSInInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String RELATIVE_PATH = "relative.path";

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInInterceptor.class);

    public JAXRSInInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) {
        String path = (String)message.get(Message.PATH_INFO);
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

        if (path.startsWith(address)) {
            path = path.substring(address.length());
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }
        
        //TODO : make sure we do this parsing only once
        MultivaluedMap<String, String> queries = 
            JAXRSUtils.getStructuredParams((String)message.get(Message.QUERY_STRING), 
                                            "&", true);
        SystemQueryHandler sqh = ProviderFactory.getInstance().getQueryHandler(queries);
        if (sqh != null) {
            // TODO : if Response != null then make sure no invocations happen
            // TODO : root resource class needs be selected earlier
            sqh.handleQuery(message, null, queries);
        }
        
        String acceptContentTypes = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
        if (acceptContentTypes == null) {
            acceptContentTypes = "*/*";
        }
        message.getExchange().put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);
        
        LOG.fine("Request path is: " + path);
        LOG.fine("Request HTTP method is: " + httpMethod);
        LOG.fine("Request contentType is: " + requestContentType);
        LOG.fine("Accept contentType is: " + acceptContentTypes);
        
        //1. Matching target resource classes and method
        Service service = message.getExchange().get(Service.class);
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)service).getClassResourceInfos();

        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        OperationResourceInfo ori = JAXRSUtils.findTargetResourceClass(resources, 
                                                                       path, 
                                                                       httpMethod, 
                                                                       values,
                                                                       requestContentType, 
                                                                       acceptContentTypes);

        if (ori == null) {
            String errorMessage = "No operation found for path: " + path + ", contentType: " 
                + requestContentType + ", Accept contentType: " + acceptContentTypes;
            LOG.severe(errorMessage);
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OP_EXC", 
                                                                   BUNDLE, 
                                                                   path,
                                                                   requestContentType,
                                                                   acceptContentTypes));
        }
        LOG.info("Found operation: " + ori.getMethod().getName());
        
        message.getExchange().put(OperationResourceInfo.class, ori);
        message.put(RELATIVE_PATH, values.getFirst(URITemplate.FINAL_MATCH_GROUP));
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
      
        //2. Process parameters
        List<Object> params = JAXRSUtils
            .processParameters(ori, values, message);

        message.setContent(List.class, params);
    }
}
