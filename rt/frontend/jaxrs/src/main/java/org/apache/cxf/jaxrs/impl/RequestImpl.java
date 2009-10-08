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

package org.apache.cxf.jaxrs.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

/**
 * TODO : deal with InvalidStateExceptions
 *
 */

public class RequestImpl implements Request {
    
    private final Message m;
    
    public RequestImpl(Message m) {
        this.m = m;
    }

    

    public Variant selectVariant(List<Variant> vars) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }



    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        String ifMatch = getHeaderValue("If-Match");
        
        if (ifMatch == null || ifMatch.equals("*")) {
            return null;
        }
        
        try {
            EntityTag requestTag = EntityTag.valueOf(ifMatch);
            if (requestTag.equals(eTag) && !requestTag.isWeak()) {
                return null;
            }
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        return Response.status(412).tag(eTag);
    }



    public ResponseBuilder evaluatePreconditions(Date lastModified) {
        String ifModifiedSince = getHeaderValue(HttpHeaders.IF_MODIFIED_SINCE);
        
        if (ifModifiedSince == null) {
            return null;
        }
        
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();

        dateFormat.setLenient(false);
        Date dateSince = null;
        try {
            dateSince = dateFormat.parse(ifModifiedSince);
        } catch (ParseException ex) {
            // invalid header value, request should continue
            return null;
        }
        
        if (dateSince.before(lastModified)) {
            // request should continue
            return null;
        }
        
        return Response.status(Response.Status.NOT_MODIFIED);
    }



    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        ResponseBuilder rb = evaluatePreconditions(eTag);
        if (rb != null) {
            return rb;
        }
        return evaluatePreconditions(lastModified);
                
    }
    
    @SuppressWarnings("unchecked")
    private String getHeaderValue(String name) {
        Map<String, List<String>> headers = 
            (Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS);
        if (headers == null) {
            return null;
        }
        List<String> values = headers.get(name);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

}
