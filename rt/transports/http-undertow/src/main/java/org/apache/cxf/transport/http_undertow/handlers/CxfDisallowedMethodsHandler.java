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
package org.apache.cxf.transport.http_undertow.handlers;

import org.apache.cxf.transport.http_undertow.CXFUndertowHttpHandler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.util.HttpString;

public class CxfDisallowedMethodsHandler implements CXFUndertowHttpHandler {
    
    private HttpHandler next;
    private DisallowedMethodsHandler buildDisallowedMethodHandler;
    private String[] methods; 
    
    
    public CxfDisallowedMethodsHandler(String[] methods) {
        this.setMethods(methods);
    }
    
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        
        if (buildDisallowedMethodHandler == null) {
            buildDisallowedMethodHandler();
        }
        this.buildDisallowedMethodHandler.handleRequest(exchange);
    }

    @Override
    public void setNext(HttpHandler nextHandler) {
        this.next = nextHandler;
        
    }
    
    private void buildDisallowedMethodHandler() {
        HttpString[] strings = new HttpString[methods.length];
        for (int i = 0; i < methods.length; ++i) {
            strings[i] = new HttpString(methods[i]);
        }

        HttpHandler handler = this.next;
        this.buildDisallowedMethodHandler = 
            new DisallowedMethodsHandler(handler, strings);

    }


    public String[] getMethods() {
        return methods;
    }


    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    
    
}

