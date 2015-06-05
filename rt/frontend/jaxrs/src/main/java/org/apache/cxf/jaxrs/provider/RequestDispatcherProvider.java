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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

@Produces("text/html")
@Provider
public class RequestDispatcherProvider extends AbstractConfigurableProvider
    implements MessageBodyWriter<Object> {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(RequestDispatcherProvider.class);
    private static final Logger LOG = LogUtils.getL7dLogger(RequestDispatcherProvider.class);
    
    private static final String ABSOLUTE_PATH_PARAMETER = "absolute.path";
    private static final String BASE_PATH_PARAMETER = "base.path";
    private static final String WEBAPP_BASE_PATH_PARAMETER = "webapp.base.path";
    private static final String RELATIVE_PATH_PARAMETER = "relative.path";
    
    private static final String REQUEST_SCOPE = "request";
    private static final String SESSION_SCOPE = "session";
    
    private static final String MESSAGE_RESOURCE_PATH_PROPERTY = "redirect.resource.path";
    
    private static final String DEFAULT_RESOURCE_EXTENSION = ".jsp";
    private static final String DEFAULT_LOCATION_PREFIX = "/WEB-INF/";
    
    private String servletContextPath; 
    private String resourcePath;
    private Map<String, String> resourcePaths = Collections.emptyMap();
    private Map<String, String> classResources = Collections.emptyMap();
    private Map<? extends Enum<?>, String> enumResources = Collections.emptyMap();
    private boolean useClassNames;
    
    private String scope = REQUEST_SCOPE;
    private Map<String, String> beanNames = Collections.emptyMap();
    private String beanName;
    private String dispatcherName;
    private String servletPath;
    private boolean useCurrentServlet;
    private boolean saveParametersAsAttributes;
    private boolean logRedirects;
    private boolean strictPathCheck;
    private String locationPrefix;
    private String resourceExtension;
    private boolean includeResource; 
    private boolean ignoreContextPath;
    
    private MessageContext mc; 

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
    }

    public void setStrictPathCheck(boolean use) {
        strictPathCheck = use;
    }
    
    public void setUseClassNames(boolean use) {
        useClassNames = use;
    }
    
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    private String getClassResourceName(Class<?> type) {
        String simpleName = type.getSimpleName();
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(simpleName.charAt(0)));
        if (simpleName.length() > 1) {
            sb.append(simpleName.substring(1));
        }
        String thePrefix = locationPrefix == null ? DEFAULT_LOCATION_PREFIX : locationPrefix;
        String theExtension = resourceExtension == null ? DEFAULT_RESOURCE_EXTENSION : resourceExtension;
        return thePrefix + sb.toString() + theExtension;  
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        
        if (useClassNames 
            && ResourceUtils.getClasspathResourceURL(getClassResourceName(type),
                                                     RequestDispatcherProvider.class,
                                                     getBus()) != null) {
            return true;
        }
        if (resourcePath != null || classResourceSupported(type)) {
            return true;
        }
        if (!resourcePaths.isEmpty()) {
            String path = getRequestPath();
            for (String requestPath : resourcePaths.keySet()) {
                boolean result = strictPathCheck ? path.endsWith(requestPath) : path.contains(requestPath);  
                if (result) {
                    return true;
                }
            }
        }
        return mc != null && mc.get(MESSAGE_RESOURCE_PATH_PROPERTY) != null;
    }

    private boolean classResourceSupported(Class<?> type) {
        String typeName = type.getName();
        if (type.isEnum()) {
            for (Object o : enumResources.keySet()) {
                if (o.getClass().getName().equals(typeName)) {
                    return true;
                }
            }
            for (String name : classResources.keySet()) {
                if (name.startsWith(typeName)) {
                    return true;
                }
            }
            return false;
        } else {
            return classResources.containsKey(typeName);
        }
    }
    
    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        
        ServletContext sc = getServletContext();
        HttpServletRequest servletRequest = mc.getHttpServletRequest();
        
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
                mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.TRUE);
            }
            
            HttpServletRequestFilter requestFilter = new HttpServletRequestFilter(
                servletRequest, path, theServletPath, saveParametersAsAttributes, ignoreContextPath);
            String attributeName = getBeanName(o);
            if (REQUEST_SCOPE.equals(scope)) {
                requestFilter.setAttribute(attributeName, o);
            } else if (SESSION_SCOPE.equals(scope)) {
                requestFilter.getSession(true).setAttribute(attributeName, o);
            } 
            setRequestParameters(requestFilter);
            logRedirection(path, attributeName, o);
            if (includeResource) {
                rd.include(requestFilter, mc.getHttpServletResponse());
            } else {
                rd.forward(requestFilter, mc.getHttpServletResponse());
            }
        } catch (Throwable ex) {
            mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.FALSE);
            LOG.warning(ExceptionUtils.getStackTrace(ex));
            throw ExceptionUtils.toInternalServerErrorException(ex, null); 
        }
    }

    private void logRedirection(String path, String attributeName, Object o) {
        Level level = logRedirects ? Level.INFO : Level.FINE;  
        if (LOG.isLoggable(level)) {
            String message = 
                new org.apache.cxf.common.i18n.Message("RESPONSE_REDIRECTED_TO", 
                    BUNDLE, o.getClass().getName(), attributeName, path).toString();
            LOG.log(level, message);
        }
    }
    
    String getResourcePath(Class<?> cls, Object o) {
        if (useClassNames) {
            return getClassResourceName(cls);     
        }
        
        String name = cls.getName();
        if (cls.isEnum()) {
            String enumResource = enumResources.get(o);
            if (enumResource != null) {
                return enumResource;
            }
            name += "." + o.toString();     
        }
        
        String clsResourcePath = classResources.get(name);
        if (clsResourcePath != null) {
            return clsResourcePath;
        }
        if (resourcePath != null) {
            return resourcePath;
        }
        String path = getRequestPath();
        for (Map.Entry<String, String> entry : resourcePaths.entrySet()) {
            if (path.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return getPathFromMessageContext();
        
    }
    
    private String getPathFromMessageContext() {
        Object resourcePathProp = (String)mc.get(MESSAGE_RESOURCE_PATH_PROPERTY);
        if (resourcePathProp != null) {
            StringBuilder sb = new StringBuilder();
            if (locationPrefix != null) {
                sb.append(locationPrefix);
            }
            sb.append(resourcePathProp.toString());
            if (resourceExtension != null) {
                sb.append(resourceExtension);
            }
            return sb.toString();
        }
        return null;
    }
    
    private String getRequestPath() {
        Message inMessage = PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
        return (String)inMessage.get(Message.REQUEST_URI);
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

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public void setLogRedirects(String value) {
        this.logRedirects = Boolean.valueOf(value);
    }

    protected String getBeanName(Object bean) {
        if (beanName != null) {
            return beanName;
        }
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
            request.setParameter(WEBAPP_BASE_PATH_PARAMETER, (String)mc.get("http.base.path"));
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

    public void setResourcePaths(Map<String, String> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }
    
    public void setClassResources(Map<String, String> resources) {
        this.classResources = resources;
    }

    public void setSaveParametersAsAttributes(boolean saveParametersAsAttributes) {
        this.saveParametersAsAttributes = saveParametersAsAttributes;
    }

    public void setEnumResources(Map<? extends Enum<?>, String> enumResources) {
        this.enumResources = enumResources;
    }

    public void setUseCurrentServlet(boolean useCurrentServlet) {
        this.useCurrentServlet = useCurrentServlet;
    }

    public void setIncludeResource(boolean includeResource) {
        this.includeResource = includeResource;
    }

    public void setLocationPrefix(String locationPrefix) {
        this.locationPrefix = locationPrefix;
    }

    public void setResourceExtension(String resourceExtension) {
        this.resourceExtension = resourceExtension;
    }

    public void setIgnoreContextPath(boolean ignoreContextPath) {
        this.ignoreContextPath = ignoreContextPath;
    }

    protected static class HttpServletRequestFilter extends HttpServletRequestWrapper {
        
        private Map<String, String[]> params;
        private String path;
        private String servletPath;
        private boolean saveParamsAsAttributes;
        private boolean ignoreContextPath;
        
        public HttpServletRequestFilter(HttpServletRequest request, 
                                        String path, 
                                        String servletPath,
                                        boolean saveParamsAsAttributes,
                                        boolean ignoreContextPath) {
            super(request);
            this.path = path;
            this.servletPath = servletPath;
            this.saveParamsAsAttributes = saveParamsAsAttributes;
            this.ignoreContextPath = ignoreContextPath;
            params = new HashMap<String, String[]>(request.getParameterMap());
        }
        
        @Override
        public String getContextPath() {
            if (ignoreContextPath) {
                return "/";
            } else {
                return super.getContextPath();
            }
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
            doSetParameters(name, values.toArray(new String[values.size()]));
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
        
    }
}
