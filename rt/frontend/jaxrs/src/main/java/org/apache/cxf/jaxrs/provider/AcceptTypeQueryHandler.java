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

package org.apache.cxf.jaxrs.provider;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

public class AcceptTypeQueryHandler implements SystemQueryHandler {

    private static final String CONTENT_QUERY = "_contentType";
    private static final Map<String, String> SHORTCUTS;
    static {
        SHORTCUTS = new HashMap<String, String>();
        SHORTCUTS.put("json", "application/json");
        SHORTCUTS.put("text", "text/*");
        SHORTCUTS.put("xml", "application/xml");
        // more to come
    }
    
    public Response handleQuery(Message m,
                                ClassResourceInfo rootResource,
                                MultivaluedMap<String, String> queries) {
        
        String type = queries.getFirst(CONTENT_QUERY);
        if (type != null) {
            if (SHORTCUTS.containsKey(type)) {
                type = SHORTCUTS.get(type);
            }
            String types = (String)m.get(Message.ACCEPT_CONTENT_TYPE);
            types = types == null ? type : types + ',' + type;
            m.getExchange().put(Message.ACCEPT_CONTENT_TYPE, types);
            m.put(Message.ACCEPT_CONTENT_TYPE, type);
        }
        
        return null;
    }

    public boolean supports(MultivaluedMap<String, String> queries) {
        return queries.getFirst(CONTENT_QUERY) != null;
    }

}
