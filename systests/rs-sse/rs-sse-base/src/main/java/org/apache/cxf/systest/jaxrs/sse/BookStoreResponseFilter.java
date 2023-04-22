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

package org.apache.cxf.systest.jaxrs.sse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.Headers;

@Provider
public class BookStoreResponseFilter implements ContainerResponseFilter {
    private static AtomicInteger counter = new AtomicInteger(0);
    @Context private UriInfo uriInfo;
    
    public BookStoreResponseFilter() {
        counter.set(0);
    }

    @Override
    public void filter(ContainerRequestContext reqContext, ContainerResponseContext rspContext) throws IOException {
        final String path = uriInfo.getRequestUri().getPath();
        
        if (!path.endsWith("/filtered/stats")) {
            counter.incrementAndGet();
            
            if (path.endsWith("/headers/sse")) {
                rspContext.setStatus(202);
                rspContext.getHeaders().add("X-My-Header", "headers");
                
                final Message message = JAXRSUtils.getCurrentMessage().getExchange().getOutMessage();
                final Map<String, List<String>> headers = Headers.getSetProtocolHeaders(message);
                headers.put("X-My-ProtocolHeader", Collections.singletonList("protocol-headers"));
            }
        } 
    }
    
    public static int getInvocations() {
        return counter.get();
    }

    public static void reset() {
        counter.set(0);
    }
}
