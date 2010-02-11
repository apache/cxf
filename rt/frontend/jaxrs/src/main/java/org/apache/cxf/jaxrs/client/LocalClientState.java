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
package org.apache.cxf.jaxrs.client;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.impl.MetadataMap;

/**
 * Keeps the client state such as the baseURI, currentURI, requestHeaders, current response
 *
 */
public class LocalClientState implements ClientState {
    
    private MultivaluedMap<String, String> requestHeaders = new MetadataMap<String, String>();
    private ResponseBuilder responseBuilder;
    private URI baseURI;
    private UriBuilder currentBuilder;
    
    public LocalClientState() {
        
    }
    
    public LocalClientState(URI baseURI) {
        this.baseURI = baseURI;
        this.currentBuilder = UriBuilder.fromUri(baseURI);
    }
    
    public LocalClientState(LocalClientState cs) {
        this.requestHeaders = new MetadataMap<String, String>(cs.requestHeaders);
        this.responseBuilder = cs.responseBuilder != null ? cs.responseBuilder.clone() : null;
        this.baseURI = cs.baseURI;
        this.currentBuilder = cs.currentBuilder != null ? cs.currentBuilder.clone() : null;
        
    }
    
    
    
    public void setCurrentBuilder(UriBuilder currentBuilder) {
        this.currentBuilder = currentBuilder;
    }
    
    public UriBuilder getCurrentBuilder() {
        return currentBuilder;
    }
    
    public void setBaseURI(URI baseURI) {
        this.baseURI = baseURI;
    }
    
    public URI getBaseURI() {
        return baseURI;
    }
    
    public void setResponseBuilder(ResponseBuilder responseBuilder) {
        this.responseBuilder = responseBuilder;
    }
    
    public ResponseBuilder getResponseBuilder() {
        return responseBuilder;
    }
    
    public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
    
    public MultivaluedMap<String, String> getRequestHeaders() {
        return requestHeaders;
    }
    
    public void reset() {
        requestHeaders.clear();
        responseBuilder = null;
        currentBuilder = UriBuilder.fromUri(baseURI);
    }
    
    public ClientState newState(URI newBaseURI, MultivaluedMap<String, String> headers) {
        ClientState state = new LocalClientState(newBaseURI);
        if (headers != null) {
            state.setRequestHeaders(headers);
        }
        return state;
    }
}
