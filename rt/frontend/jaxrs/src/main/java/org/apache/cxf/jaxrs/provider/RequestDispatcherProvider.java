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
package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

@Produces("text/html")
public class RequestDispatcherProvider extends AbstractConfigurableProvider
    implements MessageBodyWriter<Object> {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(RequestDispatcherProvider.class);
    private static final Logger LOG = LogUtils.getL7dLogger(RequestDispatcherProvider.class);
    
    private static final String ABSOLUTE_PATH_PARAMETER = "absolute.path";
    private static final String BASE_PATH_PARAMETER = "base.path";
    private static final String RELATIVE_PATH_PARAMETER = "relative.path";
    
    private static final String REQUEST_SCOPE = "request";
    private static final String SESSION_SCOPE = "session";
    
    private String servletContextPath; 
    private String resourcePath;
    private Map<String, String> classResources = Collections.emptyMap();
    
    private String scope = REQUEST_SCOPE;
    private Map<String, String> beanNames = Collections.emptyMap();
    private String dispatcherName;
    private String servletPath;
    
    @Context
    private MessageContext mc; 

    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return resourcePath != null || classResources.containsKey(type.getName());
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        
        ServletContext sc = getServletContext();
        String path = resourcePath != null ? resourcePath : classResources.get(clazz.getName());
        RequestDispatcher rd = getRequestDispatcher(sc, clazz, path);
        
        try {
            mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.TRUE);
            
            String theServletPath = servletPath == null ? "/" : servletPath;
            HttpServletRequestFilter servletRequest = 
                new HttpServletRequestFilter(mc.getHttpServletRequest(), path, theServletPath);
            if (REQUEST_SCOPE.equals(scope)) {
                servletRequest.setAttribute(getBeanName(o), o);
            } else if (SESSION_SCOPE.equals(scope)) {
                servletRequest.getSession(true).setAttribute(getBeanName(o), o);
            } 
            setRequestParameters(servletRequest);
            rd.forward(servletRequest, mc.getHttpServletResponse());
        } catch (Throwable ex) {
            mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.FALSE);
            ex.printStackTrace();
            throw new WebApplicationException(ex);
        }
    }

    protected ServletContext getServletContext() {
        ServletContext sc = mc.getServletContext();
        if (servletContextPath != null) {
            sc = sc.getContext(servletContextPath);
            if (sc == null) {
                String message = 
                    new org.apache.cxf.common.i18n.Message("RESOURCE_DISPATCH_NOT_FOUND", 
                                                           BUNDLE, servletContextPath).toString();
                LOG.severe(message);
                throw new WebApplicationException();
            }
        }
        return sc; 
    }
    
    protected RequestDispatcher getRequestDispatcher(ServletContext sc, Class<?> clazz, String path) {
        
        RequestDispatcher rd = dispatcherName != null ? sc.getNamedDispatcher(dispatcherName)
                                                      : sc.getRequestDispatcher(path);
        if (rd == null) {
            String message = 
                new org.apache.cxf.common.i18n.Message("RESOURCE_PATH_NOT_FOUND", 
                                                       BUNDLE, path).toString();
            LOG.severe(message);
            throw new WebApplicationException();
        }
        return rd;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void setServletContextPath(String servletContextPath) {
        this.servletContextPath = servletContextPath;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setBeanNames(Map<String, String> beanNames) {
        this.beanNames = beanNames;
    }

    protected String getBeanName(Object bean) {
        String name = beanNames.get(bean.getClass().getName());
        return name != null ? name : bean.getClass().getSimpleName().toLowerCase();
    }

    protected void setRequestParameters(HttpServletRequestFilter request) {
        if (mc != null) {
            UriInfo ui = mc.getUriInfo();
            MultivaluedMap<String, String> params = ui.getPathParameters();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String value = entry.getValue().get(0);
                int ind = value.indexOf(";");
                if (ind > 0) {
                    value = value.substring(0, ind);
                }
                request.setParameter(entry.getKey(), value);
            }
            
            List<PathSegment> segments = ui.getPathSegments();
            if (segments.size() > 0) {
                doSetRequestParameters(request, segments.get(segments.size() - 1).getMatrixParameters());
            }
            doSetRequestParameters(request, ui.getQueryParameters());
            request.setParameter(ABSOLUTE_PATH_PARAMETER, ui.getAbsolutePath().toString());
            request.setParameter(RELATIVE_PATH_PARAMETER, ui.getPath());
            request.setParameter(BASE_PATH_PARAMETER, ui.getBaseUri().toString());
        }
    }
    
    protected void doSetRequestParameters(HttpServletRequestFilter req, 
                                          MultivaluedMap<String, String> params) {
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            req.setParameters(entry.getKey(), entry.getValue());
        }
    }
    
    public void setDispatcherName(String name) {
        this.dispatcherName = name;
    }
    
    public void setServletPath(String path) {
        this.servletPath = path;
    }

    private static class HttpServletRequestFilter extends HttpServletRequestWrapper {
        
        private Map<String, String[]> params;
        private String path;
        private String servletPath;
        
        @SuppressWarnings("unchecked")
        public HttpServletRequestFilter(HttpServletRequest request, String path, String servletPath) {
            super(request);
            this.path = path;
            this.servletPath = servletPath;
            params = new HashMap<String, String[]>(request.getParameterMap());
        }
        
        @Override
        public String getServletPath() {
            return servletPath;
        }
        
        @Override
        public String getPathInfo() {
            return path;
        }
        
        @Override
        public String getRequestURI() {
            return path;
        }
        
        public void setParameter(String name, String value) {
            params.put(name, new String[]{value});
        }
        
        public void setParameters(String name, List<String> values) {
            params.put(name, values.toArray(new String[]{}));
        }
        
        @Override
        public String getParameter(String name) {
            String[] values = params.get(name);
            if (values == null || values.length == 0) {
                return null;
            }
            return values[0];
        }
        
        @Override
        public Map getParameterMap() {
            return params;
        }
        
    }
}
