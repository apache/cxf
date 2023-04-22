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
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.DefaultMethod;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public abstract class OperationResourceInfoComparatorBase {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);
    
    protected int compare(OperationResourceInfo e1, OperationResourceInfo e2, String httpMethod) {
        return compare(e1, e2, httpMethod, MediaType.WILDCARD_TYPE, 
            Collections.singletonList(MediaType.WILDCARD_TYPE));
    }

    protected int compare(OperationResourceInfo e1, OperationResourceInfo e2, String httpMethod, 
            final MediaType contentType, final List<MediaType> acceptTypes) {
        return compare(e1, e2, HttpMethod.GET.equals(httpMethod), 
            httpMethod, contentType, acceptTypes);
    }

    protected int compare(OperationResourceInfo e1, OperationResourceInfo e2, boolean getMethod, 
            String httpMethod, final MediaType contentType, final List<MediaType> acceptTypes) {

        String e1HttpMethod = e1.getHttpMethod();
        String e2HttpMethod = e2.getHttpMethod();

        int result;
        if (!getMethod && HttpMethod.HEAD.equals(httpMethod)) {
            result = compareWithHead(e1HttpMethod, e2HttpMethod);
            if (result != 0) {
                return result;
            }
        }

        result = URITemplate.compareTemplates(
                          e1.getURITemplate(),
                          e2.getURITemplate());

        if (result == 0 && (e1HttpMethod != null && e2HttpMethod == null
                || e1HttpMethod == null && e2HttpMethod != null)) {
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

        if (result == 0 && e1HttpMethod != null && e2HttpMethod != null) {
            boolean e1IsDefault = DefaultMethod.class.getSimpleName().equals(e1HttpMethod);
            boolean e2IsDefault = DefaultMethod.class.getSimpleName().equals(e2HttpMethod);
            if (e1IsDefault && !e2IsDefault) {
                result = 1;
            } else if (!e1IsDefault && e2IsDefault) {
                result = -1;
            }
        } 
        if (result == 0) {
            result = JAXRSUtils.compareMethodParameters(e1.getInParameterTypes(), e2.getInParameterTypes());
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
    
    private static int compareWithHead(String e1HttpMethod, String e2HttpMethod) {
        if (HttpMethod.HEAD.equals(e1HttpMethod)) {
            return -1;
        } else if (HttpMethod.HEAD.equals(e2HttpMethod)) {
            return 1;
        }
        return 0;
    }
}