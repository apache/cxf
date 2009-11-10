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
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.helpers.IOUtils;



public abstract class AbstractHTTPServlet extends HttpServlet {
    
    /**
     * List of well-known HTTP 1.1 verbs, with POST and GET being the most used verbs at the top 
     */
    private static final List<String> KNOWN_HTTP_VERBS = 
        Arrays.asList(new String[]{"POST", "GET", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"});
    
    private static final String STATIC_RESOURCES_PARAMETER = "static-resources-list";
    
    private static final String REDIRECTS_PARAMETER = "redirects-list";
    private static final String REDIRECT_SERVLET_NAME_PARAMETER = "redirect-servlet-name";
    private static final String REDIRECT_SERVLET_PATH_PARAMETER = "redirect-servlet-path";
    
    private static final Map<String, String> STATIC_CONTENT_TYPES;
    
    static {
        STATIC_CONTENT_TYPES = new HashMap<String, String>();
        STATIC_CONTENT_TYPES.put("html", "text/html");
        STATIC_CONTENT_TYPES.put("txt", "text/plain");
        STATIC_CONTENT_TYPES.put("css", "text/css");
        STATIC_CONTENT_TYPES.put("pdf", "application/pdf");
        // TODO : add more types if needed
    }
    
    private List<String> staticResourcesList;
    private List<String> redirectList; 
    private String dispatcherServletPath;
    private String dispatcherServletName;
    
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        staticResourcesList = parseListSequence(servletConfig.getInitParameter(STATIC_RESOURCES_PARAMETER));
        
        redirectList = parseListSequence(servletConfig.getInitParameter(REDIRECTS_PARAMETER));
        dispatcherServletName = servletConfig.getInitParameter(REDIRECT_SERVLET_NAME_PARAMETER);
        dispatcherServletPath = servletConfig.getInitParameter(REDIRECT_SERVLET_PATH_PARAMETER);
    }
    
    private static List<String> parseListSequence(String values) {
        if (values != null) {
            List<String> list = new LinkedList<String>();
            String[] pathValues = values.split(" ");
            for (String value : pathValues) {
                String theValue = value.trim();
                if (theValue.length() > 0) {
                    list.add(theValue);
                }
            }
            return list;
        } else {
            return null;
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        handleRequest(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        handleRequest(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
        handleRequest(request, response);
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
            handleRequest(request, response);
        }
    }
    
    protected void handleRequest(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException {
        
        if (redirectList != null 
            && matchPath(redirectList, request.getPathInfo())) {
            redirect(request, response, request.getPathInfo());
            return;
        }
        
        if (staticResourcesList != null 
            && matchPath(staticResourcesList, request.getPathInfo())) {
            serveStaticContent(request, response, request.getPathInfo());
            return;
        }
        invoke(request, response);
    }
    
    private static boolean matchPath(List<String> values, String pathInfo) {
        for (String value : values) {
            if (pathInfo.matches(value)) {
                return true;
            }
        }
        return false;
    }
    
    protected void serveStaticContent(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      String pathInfo) throws ServletException {
        InputStream is = super.getServletContext().getResourceAsStream(pathInfo);
        if (is == null) {
            throw new ServletException("Static resource " + pathInfo + " is not available");
        }
        try {
            int ind = pathInfo.lastIndexOf(".");
            if (ind != -1 && ind < pathInfo.length()) {
                String type = STATIC_CONTENT_TYPES.get(pathInfo.substring(ind + 1));
                if (type != null) {
                    response.setContentType(type);
                }
            }
            
            ServletOutputStream os = response.getOutputStream();
            IOUtils.copy(is, os);
            os.flush();
        } catch (IOException ex) {
            throw new ServletException("Static resource " + pathInfo 
                                       + " can not be written to the output stream");
        }
        
    }
    
    protected void redirect(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
        throws ServletException {
        
        String theServletPath = dispatcherServletPath == null ? "/" : dispatcherServletPath;
        
        ServletContext sc = super.getServletContext();
        RequestDispatcher rd = dispatcherServletName != null 
            ? sc.getNamedDispatcher(dispatcherServletName) 
            : sc.getRequestDispatcher(theServletPath + pathInfo);
        if (rd == null) {
            throw new ServletException("No RequestDispatcher can be created for path " + pathInfo);
        }
        try {
            HttpServletRequestFilter servletRequest = 
                new HttpServletRequestFilter(request, pathInfo, theServletPath);
            rd.forward(servletRequest, response);
        } catch (Throwable ex) {
            throw new ServletException("RequestDispatcher for path " + pathInfo + " has failed");
        }   
    }
    
    protected abstract void invoke(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException;
    
    private static class HttpServletRequestFilter extends HttpServletRequestWrapper {
        
        private String pathInfo;
        private String servletPath;
        
        public HttpServletRequestFilter(HttpServletRequest request, 
                                        String pathInfo,
                                        String servletPath) {
            super(request);
            this.pathInfo = pathInfo;
            this.servletPath = servletPath;
        }
        
        @Override
        public String getServletPath() {
            return servletPath;
        }
        
        @Override
        public String getPathInfo() {
            return pathInfo; 
        }
        
        @Override
        public String getRequestURI() {
            String query = super.getQueryString();
            return query != null ? pathInfo + "?" + query : pathInfo; 
        }
        
    }

}
