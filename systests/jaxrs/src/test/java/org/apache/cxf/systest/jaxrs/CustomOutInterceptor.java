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
package org.apache.cxf.systest.jaxrs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class CustomOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public CustomOutInterceptor() {
        super(Phase.MARSHAL);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) throws Fault {

        String requestUri = (String)message.getExchange().getInMessage().get(Message.REQUEST_URI);
        if (requestUri.endsWith("/outfault")) {
            throw new WebApplicationException(403);
        }

        HttpHeaders requestHeaders = new HttpHeadersImpl(message.getExchange().getInMessage());
        if (requestHeaders.getHeaderString("PLAIN-MAP") != null) {
            Map<String, List<String>> headers = (Map<String, List<String>>)
                message.get(Message.PROTOCOL_HEADERS);
            if (headers == null) {
                headers = new HashMap<>();
                message.put(Message.PROTOCOL_HEADERS, headers);
            }
            headers.put("BookId", Arrays.asList("321"));
            headers.put("MAP-NAME", Arrays.asList(Map.class.getName()));
            message.put(Message.PROTOCOL_HEADERS, headers);
        } else {

            MultivaluedMap<String, Object> headers = new MetadataMap<>();
            headers.putSingle("BookId", "123");
            headers.putSingle("MAP-NAME", MultivaluedMap.class.getName());
            message.put(Message.PROTOCOL_HEADERS, headers);
        }

    }

}
