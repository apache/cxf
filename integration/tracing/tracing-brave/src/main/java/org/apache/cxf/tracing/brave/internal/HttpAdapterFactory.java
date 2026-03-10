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

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface HttpAdapterFactory {
    final class Request {
        private final Map<String, List<String>> headers;
        private final URI uri;
        private final String method;
        
        Request(Map<String, List<String>> headers, URI uri, String method) {
            this.headers = headers;
            this.uri = uri;
            this.method = method;
        }
        
        Map<String, List<String>> headers() {
            return headers;
        }
        
        URI uri() {
            return uri;
        }
        
        String method() {
            return method;
        }
    }
    
    final class Response {
        private final String method;
        private final String path;
        private final Integer status;
        
        Response(String method, String path, Integer status) {
            this.method = method;
            this.path = path;
            this.status = status;
        }
        
        String path() {
            return path;
        }
        
        String method() {
            return method;
        }
        
        Integer status() {
            return status;
        }
    }
    
    static Request request(Map<String, List<String>> headers, URI uri, String method) {
        return new Request(headers, uri, method);
    }
    
    static Response response(String method, String path, Integer status) {
        return new Response(method, path, status);
    }
}
