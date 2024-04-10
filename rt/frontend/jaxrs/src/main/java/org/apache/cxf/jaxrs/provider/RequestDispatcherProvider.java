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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class RequestDispatcherProvider extends AbstractResponseViewProvider {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(RequestDispatcherProvider.class);
    private static final Logger LOG = LogUtils.getL7dLogger(RequestDispatcherProvider.class);

    private static final String ABSOLUTE_PATH_PARAMETER = "absolute.path";
    private static final String BASE_PATH_PARAMETER = "base.path";
    private static final String WEBAPP_BASE_PATH_PARAMETER = "webapp.base.path";
    private static final String RELATIVE_PATH_PARAMETER = "relative.path";

    private static final String REQUEST_SCOPE = "request";
    private static final String SESSION_SCOPE = "session";

    private static final String DEFAULT_RESOURCE_EXTENSION = ".jsp";
    private static final String DEFAULT_LOCATION_PREFIX = "/WEB-INF/";

    private String servletContextPath;
    
    private String scope = REQUEST_SCOPE;
    private String dispatcherName;
    private String servletPath;
    private boolean useCurrentServlet;
    private boolean saveParametersAsAttributes;
    private boolean includeResource;

    protected String getDefaultLocationPrefix() {
        return DEFAULT_LOCATION_PREFIX;
    }
    
    protected String getDefaultResourceExtension() {
        return DEFAULT_RESOURCE_EXTENSION;
    }
    
    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations,
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {

        ServletContext sc = getServletContext();
        HttpServletRequest servletRequest = getMessageContext().getHttpServletRequest();

        String path = getResourcePath(clazz, o);

        String theServletPath = servletPath != null ? servletPath
            : useCurrentServlet ? servletRequest.getServletPath() : "/";

        if (theServletPath.endsWith("/") && path != null && path.startsWith("/")) {
            theServletPath = theServletPath.length() == 1 ? ""
                : theServletPath.substring(0, theServletPath.length() - 1);
        } else if (!theServletPath.endsWith("/") && path != null && !path.startsWith("/")) {
            path = "/" + path;
        }


        RequestDispatcher rd = getRequestDispatcher(sc, clazz, theServletPath + path);

        try {
            if (!includeResource) {
                getMessageContext().put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.TRUE);
            }

            HttpServletRequestFilter requestFilter = new HttpServletRequestFilter(
                servletRequest, path, theServletPath, saveParametersAsAttributes);
            String attributeName = getBeanName(o);
            if (REQUEST_SCOPE.equals(scope)) {
                requestFilter.setAttribute(attributeName, o);
            } else if (SESSION_SCOPE.equals(scope)) {
                requestFilter.getSession(true).setAttribute(attributeName, o);
            }
            setRequestParameters(requestFilter);
            logRedirection(path, attributeName, o);
            if (includeResource) {
                rd.include(requestFilter, getMessageContext().getHttpServletResponse());
            } else {
                rd.forward(requestFilter, getMessageContext().getHttpServletResponse());
            }
        } catch (Throwable ex) {
            handleViewRenderingException(theServletPath + path, ex);
        }
    }

    private void logRedirection(String path, String attributeName, Object o) {
        Level level = isLogRedirects() ? Level.INFO : Level.FINE;
        if (LOG.isLoggable(level)) {
            String message =
                new org.apache.cxf.common.i18n.Message("RESPONSE_REDIRECTED_TO",
                    BUNDLE, o.getClass().getName(), attributeName, path).toString();
            LOG.log(level, message);
        }
    }

    protected ServletContext getServletContext() {
        ServletContext sc = getMessageContext().getServletContext();
        if (servletContextPath != null) {
            sc = sc.getContext(servletContextPath);
            if (sc == null) {
                String message =
                    new org.apache.cxf.common.i18n.Message("RESOURCE_DISPATCH_NOT_FOUND",
                                                           BUNDLE, servletContextPath).toString();
                LOG.severe(message);
                throw ExceptionUtils.toInternalServerErrorException(null, null);
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
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        return rd;
    }

    public void setServletContextPath(String servletContextPath) {
        this.servletContextPath = servletContextPath;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    protected void setRequestParameters(HttpServletRequestFilter request) {
        if (getMessageContext() != null) {
            UriInfo ui = getMessageContext().getUriInfo();
            MultivaluedMap<String, String> params = ui.getPathParameters();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String value = entry.getValue().get(0);
                int ind = value.indexOf(';');
                if (ind > 0) {
                    value = value.substring(0, ind);
                }
                request.setParameter(entry.getKey(), value);
            }

            List<PathSegment> segments = ui.getPathSegments();
            if (!segments.isEmpty()) {
                doSetRequestParameters(request, segments.get(segments.size() - 1).getMatrixParameters());
            }
            doSetRequestParameters(request, ui.getQueryParameters());
            request.setParameter(ABSOLUTE_PATH_PARAMETER, ui.getAbsolutePath().toString());
            request.setParameter(RELATIVE_PATH_PARAMETER, ui.getPath());
            request.setParameter(BASE_PATH_PARAMETER, ui.getBaseUri().toString());
            request.setParameter(WEBAPP_BASE_PATH_PARAMETER, (String)getMessageContext().get("http.base.path"));
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

    public void setSaveParametersAsAttributes(boolean saveParametersAsAttributes) {
        this.saveParametersAsAttributes = saveParametersAsAttributes;
    }

    public void setUseCurrentServlet(boolean useCurrentServlet) {
        this.useCurrentServlet = useCurrentServlet;
    }

    public void setIncludeResource(boolean includeResource) {
        this.includeResource = includeResource;
    }

    protected static class HttpServletRequestFilter extends HttpServletRequestWrapper {

        private Map<String, String[]> params;
        private String path;
        private String servletPath;
        private boolean saveParamsAsAttributes;

        public HttpServletRequestFilter(HttpServletRequest request,
                                        String path,
                                        String servletPath,
                                        boolean saveParamsAsAttributes) {
            super(request);
            this.path = path;
            this.servletPath = servletPath;
            this.saveParamsAsAttributes = saveParamsAsAttributes;
            params = new HashMap<>(request.getParameterMap());
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return path;
        }

        public void setParameter(String name, String value) {
            doSetParameters(name, new String[]{value});
        }

        public void setParameters(String name, List<String> values) {
            doSetParameters(name, values.toArray(new String[0]));
        }

        private void doSetParameters(String name, String[] values) {
            if (saveParamsAsAttributes) {
                super.setAttribute(name, values);
            } else {
                params.put(name, values);
            }
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
        public Map<String, String[]> getParameterMap() {
            return params;
        }
        
        @Override
        public Enumeration<String> getParameterNames() {
            
            final Iterator<String> it = params.keySet().iterator();
            return new Enumeration<String>() {

                @Override
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                @Override
                public String nextElement() {
                    return it.next();
                }

            };
        }
        
        @Override
        public String[] getParameterValues(String name) {
            return params.get(name);
        }

    }

    @Override
    protected boolean resourceAvailable(String resourceName) {
        return ResourceUtils.getClasspathResourceURL(resourceName,
                                              RequestDispatcherProvider.class,
                                              getBus()) != null;
    }
}
