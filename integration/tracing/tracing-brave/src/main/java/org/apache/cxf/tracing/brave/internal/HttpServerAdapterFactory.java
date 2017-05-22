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
package org.apache.cxf.tracing.brave.internal;

import java.util.List;

import brave.http.HttpServerAdapter;

public interface HttpServerAdapterFactory extends HttpAdapterFactory {
    static HttpServerAdapter<Request, Response> create(Request request) {
        return new HttpServerAdapter<Request, Response>() {
            @Override 
            public String method(Request request) {
                return request.method();
            }
    
            @Override 
            public String path(Request request) {
                return request.uri().getPath();
            }
    
            @Override 
            public String url(Request request) {
                return request.uri().toString();
            }
    
            @Override 
            public String requestHeader(Request request, String name) {
                List<String> value = request.headers().get(name);
    
                if (value != null && !value.isEmpty()) {
                    return value.get(0);
                }
    
                return null;
            }
    
            @Override 
            public Integer statusCode(Response response) {
                throw new UnsupportedOperationException("The operation is not supported for request adapter");
            }
        };
    }
    
    static HttpServerAdapter<Request, Response> create(Response response) {
        return new HttpServerAdapter<Request, Response>() {
            @Override 
            public String method(Request request) {
                throw new UnsupportedOperationException("The operation is not supported for response adapter");
            }
    
            @Override 
            public String path(Request request) {
                throw new UnsupportedOperationException("The operation is not supported for response adapter");
            }
    
            @Override 
            public String url(Request request) {
                throw new UnsupportedOperationException("The operation is not supported for response adapter");
            }
    
            @Override 
            public String requestHeader(Request request, String name) {
                throw new UnsupportedOperationException("The operation is not supported for response adapter");
            }
    
            @Override 
            public Integer statusCode(Response response) {
                return response.status();
            }
        };
    }
}
