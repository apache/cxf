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
package org.apache.cxf.jaxrs.spring;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * CXF view provider that delegates view rendering to Spring MVC Views.
 *
 * Sample usage in a spring application:
 * <pre>
 @Bean
 public SpringViewResolverProvider springViewProvider(ViewResolver viewResolver) {
     SpringViewResolverProvider viewProvider = new SpringViewResolverProvider(viewResolver,
            new AcceptHeaderLocaleResolver());
     viewProvider.setUseClassNames(true);
     viewProvider.setBeanName("model");
     viewProvider.setResourcePaths(Collections.singletonMap("/remove", "registeredClients"));
     return viewProvider;
 }
 * </pre>
 */
@Produces("text/html")
@Provider
public class SpringViewResolverProvider extends AbstractConfigurableProvider implements MessageBodyWriter<Object> {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SpringViewResolverProvider.class);

    private static final Logger LOG = LogUtils.getL7dLogger(SpringViewResolverProvider.class);

    private static final String MESSAGE_RESOURCE_PATH_PROPERTY = "redirect.resource.path";

    private static final String DEFAULT_RESOURCE_EXTENSION = "";

    private static final String DEFAULT_LOCATION_PREFIX = "";

    private final ViewResolver viewResolver;

    private String resourcePath;

    private Map<String, String> resourcePaths = Collections.emptyMap();

    private Map<String, String> classResources = Collections.emptyMap();

    private Map<? extends Enum<?>, String> enumResources = Collections.emptyMap();

    private boolean useClassNames;

    private Map<String, String> beanNames = Collections.emptyMap();

    private String beanName;

    private boolean logRedirects;

    private boolean strictPathCheck;

    private String locationPrefix;

    private String resourceExtension;

    private MessageContext mc;

    private LocaleResolver localeResolver;

    private String errorView = "/error";

    public SpringViewResolverProvider(ViewResolver viewResolver, LocaleResolver localeResolver) {
        if (viewResolver == null) {
            throw new IllegalArgumentException("Argument viewResolver is required");
        }
        if (localeResolver == null) {
            throw new IllegalArgumentException("Argument localeResolver is required");
        }
        this.viewResolver = viewResolver;
        this.localeResolver = localeResolver;
    }

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

    private String getViewName(Class<?> type) {
        String viewName = doGetClassResourceName(type);
        if (viewName == null) {
            for (Class<?> in : type.getInterfaces()) {
                viewName = doGetClassResourceName(in);
                if (viewName != null) {
                    break;
                }
            }
        }
        return viewName;
    }

    private Locale getLocale() {
        return localeResolver.resolveLocale(mc.getHttpServletRequest());
    }

    private String doGetClassResourceName(Class<?> type) {
        String simpleName = StringUtils.uncapitalize(type.getSimpleName());
        String thePrefix = locationPrefix == null ? DEFAULT_LOCATION_PREFIX : locationPrefix;
        String theExtension = resourceExtension == null ? DEFAULT_RESOURCE_EXTENSION : resourceExtension;
        String viewName = thePrefix + simpleName + theExtension;
        View view = resolveView(viewName);
        return view != null ? viewName : null;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {

        if (useClassNames && getViewName(type) != null) {
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
        }
        return classResources.containsKey(typeName);
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, MediaType type,
            MultivaluedMap<String, Object> headers, OutputStream os) throws IOException {

        View view = getView(clazz, o);
        String attributeName = getBeanName(o);
        Map<String, Object> model = Collections.singletonMap(attributeName, o);

        try {
            mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.TRUE);
            logRedirection(view, attributeName, o);
            view.render(model, mc.getHttpServletRequest(), mc.getHttpServletResponse());
        } catch (Throwable ex) {
            handleViewRenderingException(view, ex);
        }
    }

    /**
     * By default we'll try to forward to Spring error handler.
     *
     * If no such handler has been set, or if there is an error during error handling,
     * we throw an error and let CXF handle the internal error.
     *
     * @param view view that produced the rendering error
     * @param exception rendering error
     */
    private void handleViewRenderingException(View view, Throwable exception) {
        LOG.log(Level.WARNING, String.format("Error forwarding to '%s': %s", view, exception.getMessage()), exception);
        if (errorView != null) {
            mc.getHttpServletRequest().setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
            mc.getHttpServletRequest().setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
            mc.getHttpServletRequest().setAttribute(RequestDispatcher.ERROR_MESSAGE, exception.getMessage());
            try {
                mc.getServletContext().getRequestDispatcher(errorView).forward(mc.getHttpServletRequest(),
                        mc.getHttpServletResponse());
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

    private void handleInternalViewRenderingException(Throwable exception) {
        mc.put(AbstractHTTPDestination.REQUEST_REDIRECTED, Boolean.FALSE);
        throw ExceptionUtils.toInternalServerErrorException(exception, null);
    }

    private void logRedirection(View view, String attributeName, Object o) {
        Level level = logRedirects ? Level.INFO : Level.FINE;
        if (LOG.isLoggable(level)) {
            String message = new org.apache.cxf.common.i18n.Message("RESPONSE_REDIRECTED_TO",
                    BUNDLE, o.getClass().getName(),
                    attributeName, view).toString();
            LOG.log(level, message);
        }
    }

    View getView(Class<?> cls, Object o) {
        String currentResourcePath = getPathFromMessageContext();
        if (currentResourcePath != null) {
            return resolveView(currentResourcePath);
        }

        if (!resourcePaths.isEmpty()) {

            String path = getRequestPath();
            for (Map.Entry<String, String> entry : resourcePaths.entrySet()) {
                if (path.endsWith(entry.getKey())) {
                    return resolveView(entry.getValue());
                }
            }
        }
        if (!enumResources.isEmpty() || !classResources.isEmpty()) {
            String name = cls.getName();
            if (cls.isEnum()) {
                String enumResource = enumResources.get(o);
                if (enumResource != null) {
                    return resolveView(enumResource);
                }
                name += "." + o.toString();
            }

            String clsResourcePath = classResources.get(name);
            if (clsResourcePath != null) {
                return resolveView(clsResourcePath);
            }
        }

        if (useClassNames) {
            return resolveView(getViewName(cls));
        }

        return resolveView(resourcePath);
    }

    private View resolveView(String viewName) {
        try {
            return viewResolver.resolveViewName(viewName, getLocale());
        } catch (Exception ex) {
            LOG.warning(ExceptionUtils.getStackTrace(ex));
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    private String getPathFromMessageContext() {
        if (mc != null) {
            Object resourcePathProp = mc.get(MESSAGE_RESOURCE_PATH_PROPERTY);
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

    private String getRequestPath() {
        Message inMessage = PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
        return (String) inMessage.get(Message.REQUEST_URI);
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
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
        if (name != null) {
            return name;
        }
        Class<?> resourceClass = bean.getClass();
        if (useClassNames && doGetClassResourceName(resourceClass) == null) {
            for (Class<?> cls : bean.getClass().getInterfaces()) {
                if (doGetClassResourceName(cls) != null) {
                    resourceClass = cls;
                    break;
                }
            }
        }

        return resourceClass.getSimpleName().toLowerCase();
    }

    public void setResourcePaths(Map<String, String> resourcePaths) {
        this.resourcePaths = resourcePaths;
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
}