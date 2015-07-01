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

package org.apache.cxf.jaxrs.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class OperationResourceInfoComparator implements Comparator<OperationResourceInfo> {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);
    private String httpMethod;
    private boolean getMethod;
    private Message message;
    private ResourceComparator rc;  
    private MediaType contentType = MediaType.WILDCARD_TYPE;
    private List<MediaType> acceptTypes = Collections.singletonList(MediaType.WILDCARD_TYPE);

    public OperationResourceInfoComparator(Message m, String method) {
        this.message = m;
        if (message != null) {
            Object o = m.getExchange().get(Endpoint.class).get("org.apache.cxf.jaxrs.comparator");
            if (o != null) {
                rc = (ResourceComparator)o;
            }
        }
        this.httpMethod = method;
    }
    
    public OperationResourceInfoComparator(Message m, 
                                           String httpMethod,
                                           boolean getMethod,
                                           MediaType contentType,
                                           List<MediaType> acceptTypes) {
        this(m, httpMethod);
        this.contentType = contentType;
        this.acceptTypes = acceptTypes;
        this.getMethod = getMethod;
    }
    
    public int compare(OperationResourceInfo e1, OperationResourceInfo e2) {
        
        if (rc != null) {
            int result = rc.compare(e1, e2, message);
            if (result != 0) {
                return result;
            }
        }
        
        if (!getMethod && HttpMethod.HEAD.equals(httpMethod)) {
            if (HttpMethod.HEAD.equals(e1.getHttpMethod())) {
                return -1;
            } else if (HttpMethod.HEAD.equals(e2.getHttpMethod())) {
                return 1;
            }
        }
            
        int result = URITemplate.compareTemplates(
                          e1.getURITemplate(),
                          e2.getURITemplate());
        
        if (result == 0 && (e1.getHttpMethod() != null && e2.getHttpMethod() == null
                || e1.getHttpMethod() == null && e2.getHttpMethod() != null)) {
            // resource method takes precedence over a subresource locator
            return e1.getHttpMethod() != null ? -1 : 1;
        }
        
        if (result == 0 && !getMethod) {
            result = JAXRSUtils.compareSortedConsumesMediaTypes(
                          e1.getConsumeTypes(), 
                          e2.getConsumeTypes(),
                          contentType);
        }
        
        if (result == 0) {
            //use the media type of output data as the secondary key.
            result = JAXRSUtils.compareSortedAcceptMediaTypes(e1.getProduceTypes(), 
                                                              e2.getProduceTypes(),
                                                              acceptTypes);
        }
        
        if (result == 0) {
            String m1Name = 
                e1.getClassResourceInfo().getServiceClass().getName() + "#" + e1.getMethodToInvoke().getName();
            String m2Name = 
                e2.getClassResourceInfo().getServiceClass().getName() + "#" + e2.getMethodToInvoke().getName();
            LOG.warning("Both " + m1Name + " and " + m2Name + " are equal candidates for handling the current request"
                        + " which can lead to unpredictable results");
        }
        return result;
    }

}
