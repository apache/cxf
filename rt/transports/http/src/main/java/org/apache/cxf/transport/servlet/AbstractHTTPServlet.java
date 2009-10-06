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
package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public abstract class AbstractHTTPServlet extends HttpServlet {
    
    /**
     * List of well-known HTTP 1.1 verbs, with POST and GET being the most used verbs at the top 
     */
    private static final List<String> KNOWN_HTTP_VERBS = 
        Arrays.asList(new String[]{"POST", "GET", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"});
    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        invoke(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        invoke(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        invoke(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        invoke(request, response);
    }
    
    /**
     * {@inheritDoc}
     * 
     * javax.http.servlet.HttpServlet does not let to override the code which deals with
     * unrecognized HTTP verbs such as PATCH (being standardized), WebDav ones, etc.
     * Thus we let CXF servlets process unrecognized HTTP verbs directly, otherwise we delegate
     * to HttpService  
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException {
        
        HttpServletRequest      request;
        HttpServletResponse     response;
        
        try {
            request = (HttpServletRequest) req;
            response = (HttpServletResponse) res;
        } catch (ClassCastException e) {
            throw new ServletException("Unrecognized HTTP request or response object");
        }
        
        String method = request.getMethod();
        if (KNOWN_HTTP_VERBS.contains(method)) {
            super.service(request, response);
        } else {
            invoke(request, response);
        }
    }
    
    protected abstract void invoke(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException;

}
