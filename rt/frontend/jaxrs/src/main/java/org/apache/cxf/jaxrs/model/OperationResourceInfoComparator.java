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

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.message.Message;

public class OperationResourceInfoComparator extends OperationResourceInfoComparatorBase
        implements Comparator<OperationResourceInfo> {
    
    private String httpMethod;
    private boolean getMethod;
    private Message message;
    private ResourceComparator rc;
    private MediaType contentType = MediaType.WILDCARD_TYPE;
    private List<MediaType> acceptTypes = Collections.singletonList(MediaType.WILDCARD_TYPE);

    public OperationResourceInfoComparator(Message m, String method) {
        this.message = m;
        if (message != null) {
            Object o = m.getExchange().getEndpoint().get("org.apache.cxf.jaxrs.comparator");
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

    @Override
    public int compare(OperationResourceInfo e1, OperationResourceInfo e2) {
        if (e1 == e2) {
            return 0;
        }
        if (rc != null) {
            int result = rc.compare(e1, e2, message);
            if (result != 0) {
                return result;
            }
        }
        
        return compare(e1, e2, getMethod, httpMethod, contentType, acceptTypes);
    }
}
