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
package org.apache.cxf.rs.security.oauth2.client;

import java.net.URI;

import org.apache.cxf.jaxrs.client.WebClient;

public class HttpRequestProperties {

    private String hostName;
    private int port;
    private String httpMethod;
    private String requestPath;
    private String requestQuery;
     
    public HttpRequestProperties(WebClient wc, String httpMethod) {
        this(wc.getCurrentURI(), httpMethod);
    }
    
    public HttpRequestProperties(URI uri, String httpMethod) {
        this(uri.getHost(), uri.getPort(), httpMethod,
             uri.getRawPath(), uri.getRawQuery());
    }
    
    public HttpRequestProperties(String hostName, int port, String httpMethod, String requestPath) {
        this(hostName, port, httpMethod, requestPath, null);
    }
    
    public HttpRequestProperties(String hostName, int port, String httpMethod, 
                                 String requestPath, String requestQuery) {
        this.requestPath = requestPath;
        this.hostName = hostName;
        this.port = port;
        this.httpMethod = httpMethod;
    }
    
    public String getRequestPath() {
        return requestPath;
    }
    
    public String getRequestQuery() {
        return requestQuery;
    }

    public String getHostName() {
        return hostName;
    }
    public int getPort() {
        return port;
    }
    public String getHttpMethod() {
        return httpMethod;
    }
}