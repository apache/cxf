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

package org.apache.cxf.jaxrs.ext;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

public class SystemQueryHandler implements RequestHandler {

    private static final String CONTENT_QUERY = "_type";
    private static final String METHOD_QUERY = "_method";
    
    
    private static final Map<String, String> SHORTCUTS;
    static {
        SHORTCUTS = new HashMap<String, String>();
        SHORTCUTS.put("json", "application/json");
        SHORTCUTS.put("text", "text/*");
        SHORTCUTS.put("xml", "application/xml");
        // more to come
    }
    
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        
        MultivaluedMap<String, String> queries = new UriInfoImpl(m, null).getQueryParameters();
        handleTypeQuery(m, queries);
        handleMethodQuery(m, queries);
        
        return null;
    }

    private void handleMethodQuery(Message m, MultivaluedMap<String, String> queries) {
        String method = queries.getFirst(METHOD_QUERY);
        if (method != null) {
            m.put(Message.HTTP_REQUEST_METHOD, method);
        }
    }
    
    private void handleTypeQuery(Message m, MultivaluedMap<String, String> queries) {
        String type = queries.getFirst(CONTENT_QUERY);
        if (type != null) {
            if (SHORTCUTS.containsKey(type)) {
                type = SHORTCUTS.get(type);
            }
            updateAcceptTypeHeader(m, type);
        }
    }
    
    private void updateAcceptTypeHeader(Message m, String anotherValue) {
        m.put(Message.ACCEPT_CONTENT_TYPE, anotherValue);
    }
    
}
