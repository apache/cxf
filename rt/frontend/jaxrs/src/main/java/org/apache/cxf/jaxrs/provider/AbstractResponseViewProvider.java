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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

@Produces("text/html")
@Provider
public abstract class AbstractResponseViewProvider extends AbstractConfigurableProvider
    implements MessageBodyWriter<Object> {

    private static final String MESSAGE_RESOURCE_PATH_PROPERTY = "redirect.resource.path";
    
    private boolean useClassNames;
    private boolean strictPathCheck;
    private Map<String, String> beanNames = Collections.emptyMap();
    private String beanName;
    private String resourcePath;
    private Map<String, String> resourcePaths = Collections.emptyMap();
    private Map<String, String> classResources = Collections.emptyMap();
    private Map<? extends Enum<?>, String> enumResources = Collections.emptyMap();
    private String locationPrefix;
    private String resourceExtension;
    private String errorView = "/error";
    private boolean logRedirects;
        
    private MessageContext mc;

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
    }

    public MessageContext getMessageContext() {
        return mc;
    }
    
    public void setUseClassNames(boolean use) {
        useClassNames = use;
    }
    
    public boolean isUseClassNames() {
        return useClassNames;
    }
    
    public void setStrictPathCheck(boolean use) {
        strictPathCheck = use;
    }
    
    public void setBeanNames(Map<String, String> beanNames) {
        this.beanNames = beanNames;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }
    
    public void setResourcePaths(Map<String, String> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }
    
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }
    
    public void setClassResources(Map<String, String> resources) {
        this.classResources = resources;
    }

    public void setEnumResources(Map<? extends Enum<?>, String> enumResources) {
        this.enumResources = enumResources;
    }

    public void setLocationPrefix(String locationPrefix) {
        this.locationPrefix = locationPrefix;
    }

    public void setResourceExtension(String resourceExtension) {
        this.resourceExtension = resourceExtension;
    }
    
    public void setErrorView(String errorView) {
        this.errorView = errorView;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {

        if (useClassNames && getClassResourceName(type) != null) {
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
        return getMessageContext() != null && getMessageContext().get(MESSAGE_RESOURCE_PATH_PROPERTY) != null;
    }

    protected boolean classResourceSupported(Class<?> type) {
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
        }
        return classResources.containsKey(typeName);
    }
    
    protected String getPathFromMessageContext() {
        if (getMessageContext() != null) {
            Object resourcePathProp = getMessageContext().get(MESSAGE_RESOURCE_PATH_PROPERTY);
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
        }
        return null;
    }
    
    protected String getBeanName(Object bean) {
        if (beanName != null) {
            return beanName;
        }
        String name = beanNames.get(bean.getClass().getName());
        if (name != null) {
            return name;
        }
        Class<?> resourceClass = bean.getClass();
        if (isUseClassNames() && doGetClassResourceName(resourceClass) == null) {
            for (Class<?> cls : bean.getClass().getInterfaces()) {
                if (doGetClassResourceName(cls) != null) {
                    resourceClass = cls;
                    break;
                }
            }
        }

        return resourceClass.getSimpleName().toLowerCase();
    }
    
    protected String getResourcePath(Class<?> cls, Object o) {
        String currentResourcePath = getPathFromMessageContext();
        if (currentResourcePath != null) {
            return currentResourcePath;
        }

        if (!resourcePaths.isEmpty()) {

            String path = getRequestPath();
            for (Map.Entry<String, String> entry : resourcePaths.entrySet()) {
                if (path.endsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        if (!enumResources.isEmpty() || !classResources.isEmpty()) {
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
        }

        if (isUseClassNames()) {
            return getClassResourceName(cls);
        }

        return resourcePath;
    }
    
    protected String getRequestPath() {
        Message inMessage = PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
        return (String)inMessage.get(Message.REQUEST_URI);
    }
    
    protected String getClassResourceName(Class<?> type) {
        String resourceName = doGetClassResourceName(type);
        if (resourceName == null) {
            for (Class<?> in : type.getInterfaces()) {
                resourceName = doGetClassResourceName(in);
                if (resourceName != null) {
                    break;
                }
            }
        }
        return resourceName;
    }
    protected String doGetClassResourceName(Class<?> type) {
        String simpleName = StringUtils.uncapitalize(type.getSimpleName());
        String thePrefix = locationPrefix == null ? getDefaultLocationPrefix() : locationPrefix;
        String theExtension = resourceExtension == null ? getDefaultResourceExtension() : resourceExtension;
        String resourceName = thePrefix + simpleName + theExtension;
        if (resourceAvailable(resourceName)) {
            return resourceName;
        }
        return null;
    }
    
    /**
     * By default we'll try to forward to the error handler.
     *
     * If no such handler has been set, or if there is an error during error handling,
     * we throw an error and let CXF handle the internal error.
     *
     * @param viewName name of the view that produced the rendering error
     * @param exception rendering error
     */
    protected void handleViewRenderingException(String viewName, Throwable exception) {
        LOG.log(Level.WARNING,
                String.format("Error forwarding to '%s': %s", viewName, exception.getMessage()), exception);
        if (errorView != null) {
            HttpServletRequest httpRequest = getMessageContext().getHttpServletRequest();  
            httpRequest.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
            httpRequest.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
            httpRequest.setAttribute(RequestDispatcher.ERROR_MESSAGE, exception.getMessage());
            try {
                getMessageContext().getServletContext().getRequestDispatcher(errorView).forward(
                    httpRequest,
                    getMessageContext().getHttpServletResponse());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, String.format("Error forwarding to error page '%s': %s",
                        errorView, e.toString()),
                        e);
                handleInternalViewRenderingException(exception);
            }
        } else {
            handleInternalViewRenderingException(exception);
        }
    }

    protected void handleInternalViewRenderingException(Throwable exception) {
        getMessageContext().put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.FALSE);
        throw ExceptionUtils.toInternalServerErrorException(exception, null);
    }
    
    protected String getDefaultLocationPrefix() {
        return "";
    }

    protected String getDefaultResourceExtension() {
        return "";
    }
    
    protected abstract boolean resourceAvailable(String resourceName);

    public boolean isLogRedirects() {
        return logRedirects;
    }

    public void setLogRedirects(boolean logRedirects) {
        this.logRedirects = logRedirects;
    }
}
