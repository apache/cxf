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

import brave.http.HttpServerRequest;
import brave.http.HttpServerResponse;

public interface HttpServerAdapterFactory extends HttpAdapterFactory {
    static HttpServerRequest create(Request request) {
        return new HttpServerRequest() {
            @Override 
            public String method() {
                return request.method();
            }
    
            @Override 
            public String path() {
                return request.uri().getPath();
            }

            @Override
            public String route() {
                return path();
            }
    
            @Override 
            public String url() {
                return request.uri().toString();
            }
    
            @Override 
            public String header(String name) {
                List<String> value = request.headers().get(name);
    
                if (value != null && !value.isEmpty()) {
                    return value.get(0);
                }
    
                return null;
            }

            @Override
            public Request unwrap() {
                return request;
            }
        };
    }
    
    static HttpServerResponse create(Response response) {
        return new HttpServerResponse() {
            @Override
            public String method() {
                return response.method();
            }
            
            @Override
            public String route() {
                return response.path();
            }
            
            @Override 
            public int statusCode() {
                return response.status();
            }

            @Override
            public Response unwrap() {
                return response;
            }
        };
    }
}
