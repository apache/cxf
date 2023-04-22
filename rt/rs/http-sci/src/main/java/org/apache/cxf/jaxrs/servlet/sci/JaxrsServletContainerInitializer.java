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
package org.apache.cxf.jaxrs.servlet.sci;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

@HandlesTypes({ Application.class, Provider.class, Path.class })
public class JaxrsServletContainerInitializer implements ServletContainerInitializer {
    private static final Logger LOG = LogUtils.getL7dLogger(JaxrsServletContainerInitializer.class);
    private static final String IGNORE_PACKAGE = "org.apache.cxf";

    private static final String JAXRS_APPLICATION_SERVLET_NAME = "jakarta.ws.rs.core.Application";
    private static final String JAXRS_APPLICATION_PARAM = "jakarta.ws.rs.Application";
    private static final String CXF_JAXRS_APPLICATION_PARAM = "jaxrs.application";
    private static final String CXF_JAXRS_CLASSES_PARAM = "jaxrs.classes";

    @Override
    public void onStartup(final Set< Class< ? > > classes, final ServletContext ctx) throws ServletException {
        Application app = null;
        String servletName = null;
        String servletMapping = null;

        final Class< ? > appClass = findCandidate(classes);
        if (appClass != null) {
            // The best effort at detecting a CXFNonSpringJaxrsServlet handling this application.
            // Custom servlets using non-standard mechanisms to create Application will not be detected
            if (isApplicationServletAvailable(ctx, appClass)) {
                return;
            }
            try {
                app = (Application)appClass.getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                throw new ServletException(t);
            }
            // Servlet name is the application class name
            servletName = appClass.getName();
            ApplicationPath appPath = ResourceUtils.locateApplicationPath(appClass);
            // If ApplicationPath is available - use its value as a mapping otherwise get it from
            // a servlet registration with an application implementation class name
            if (appPath != null) {
                servletMapping = appPath.value() + "/*";
            } else {
                servletMapping = getServletMapping(ctx, servletName);
            }
        }
        // If application is null or empty then try to create a new application from available
        // resource and provider classes
        if (app == null
            || (app.getClasses().isEmpty() && app.getSingletons().isEmpty())) {
            // The best effort at detecting a CXFNonSpringJaxrsServlet
            // Custom servlets using non-standard mechanisms to create Application will not be detected
            if (isCxfServletAvailable(ctx)) {
                return;
            }
            final Map< Class< ? extends Annotation >, Collection< Class< ? > > > providersAndResources =
                groupByAnnotations(classes);
            if (!providersAndResources.get(Path.class).isEmpty()
                || !providersAndResources.get(Provider.class).isEmpty()) {
                if (app == null) {
                    // Servlet name is a JAX-RS Application class name
                    servletName = JAXRS_APPLICATION_SERVLET_NAME;
                    // Servlet mapping is obtained from a servlet registration
                    // with a JAX-RS Application class name
                    servletMapping = getServletMapping(ctx, servletName);
                }
                final Map<String, Object> appProperties =
                    app != null ? app.getProperties() : Collections.emptyMap();
                app = new Application() {
                    @Override
                    public Set<Class<?>> getClasses() {
                        Set<Class<?>> set = new HashSet<>();
                        set.addAll(providersAndResources.get(Path.class));
                        set.addAll(providersAndResources.get(Provider.class));
                        return set;
                    }
                    @Override
                    public Map<String, Object> getProperties() {
                        return appProperties;
                    }
                };
            }
        }

        if (app == null) {
            return;
        }
        CXFNonSpringJaxrsServlet cxfServlet = new CXFNonSpringJaxrsServlet(app);
        final Dynamic servlet = ctx.addServlet(servletName, cxfServlet);
        servlet.addMapping(servletMapping);
    }

    private boolean isCxfServletAvailable(ServletContext ctx) {
        for (Map.Entry<String, ? extends ServletRegistration> entry : ctx.getServletRegistrations().entrySet()) {
            if (entry.getValue().getInitParameter(CXF_JAXRS_CLASSES_PARAM) != null) {
                return true;
            }
        }
        return false;
    }

    private String getServletMapping(final ServletContext ctx, final String name) throws ServletException {
        ServletRegistration sr = ctx.getServletRegistration(name);
        if (sr != null) {
            return sr.getMappings().iterator().next();
        }
        final String error = "Servlet with a name " + name + " is not available";
        throw new ServletException(error);
    }

    private boolean isApplicationServletAvailable(final ServletContext ctx, final Class<?> appClass) {
        for (Map.Entry<String, ? extends ServletRegistration> entry : ctx.getServletRegistrations().entrySet()) {
            String appParam = entry.getValue().getInitParameter(JAXRS_APPLICATION_PARAM);
            if (appParam == null) {
                appParam = entry.getValue().getInitParameter(CXF_JAXRS_APPLICATION_PARAM);
            }
            if (appParam != null && appParam.equals(appClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private Map< Class< ? extends Annotation >, Collection< Class< ? > > > groupByAnnotations(
        final Set< Class< ? > > classes) {

        final Map< Class< ? extends Annotation >, Collection< Class< ? > > > grouped =
            new HashMap<>();

        grouped.put(Provider.class, new ArrayList< Class< ? > >());
        grouped.put(Path.class, new ArrayList< Class< ? > >());

        if (classes != null) {
            for (final Class< ? > clazz: classes) {
                if (!classShouldBeIgnored(clazz)) {
                    for (final Entry<Class<? extends Annotation>, Collection<Class<?>>> entry : grouped.entrySet()) {
                        if (clazz.isAnnotationPresent(entry.getKey())) {
                            entry.getValue().add(clazz);
                        }
                    }
                }
            }
        }

        return grouped;
    }

    private static boolean classShouldBeIgnored(final Class<?> clazz) {
        return clazz.getPackage().getName().startsWith(IGNORE_PACKAGE);
    }

    private static Class< ? > findCandidate(final Set< Class< ? > > classes) {
        if (classes != null) {
            for (final Class< ? > clazz: classes) {
                if (Application.class.isAssignableFrom(clazz) && !classShouldBeIgnored(clazz)) {
                    LOG.fine("Found JAX-RS application to initialize: " + clazz.getName());
                    return clazz;
                }
            }
        }

        return null;
    }
}
