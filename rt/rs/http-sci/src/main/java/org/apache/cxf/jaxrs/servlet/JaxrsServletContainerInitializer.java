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
package org.apache.cxf.jaxrs.servlet;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.annotation.HandlesTypes;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;

@HandlesTypes({ Application.class, Provider.class, Path.class })
public class JaxrsServletContainerInitializer implements ServletContainerInitializer {  
    private static final Logger LOG = LogUtils.getL7dLogger(JaxrsServletContainerInitializer.class);
    private static final String IGNORE_PACKAGE = "org.apache.cxf";
    
    private static final String IGNORE_APP_PATH_PARAM = "jaxrs.application.address.ignore";
    private static final String SERVICE_CLASSES_PARAM = "jaxrs.serviceClasses";
    private static final String PROVIDERS_PARAM = "jaxrs.providers";
    private static final String JAXRS_APPLICATION_PARAM = "javax.ws.rs.Application";
    
    @Override
    public void onStartup(final Set< Class< ? > > classes, final ServletContext ctx) throws ServletException {        
        final Dynamic servlet =  ctx.addServlet("CXFServlet", CXFNonSpringJaxrsServlet.class);
        servlet.addMapping("/*");
        
        final Class< ? > application = findCandidate(classes);
        if (application != null) {
            servlet.setInitParameter(JAXRS_APPLICATION_PARAM, application.getName());
            servlet.setInitParameter(IGNORE_APP_PATH_PARAM, "false");
        } else {
            final Map< Class< ? extends Annotation >, Collection< Class< ? > > > providersAndResources = 
                groupByAnnotations(classes);
            
            servlet.setInitParameter(PROVIDERS_PARAM, getClassNames(providersAndResources.get(Provider.class)));
            servlet.setInitParameter(SERVICE_CLASSES_PARAM, getClassNames(providersAndResources.get(Path.class)));
        }
    }
    
    private Map< Class< ? extends Annotation >, Collection< Class< ? > > > groupByAnnotations(
        final Set< Class< ? > > classes) {
        
        final Map< Class< ? extends Annotation >, Collection< Class< ? > > > grouped = 
            new HashMap< Class< ? extends Annotation >, Collection< Class< ? > > >();
        
        grouped.put(Provider.class, new ArrayList< Class< ? > >());
        grouped.put(Path.class, new ArrayList< Class< ? > >());
        
        for (final Class< ? > clazz: classes) {
            if (!classShouldBeIgnored(clazz)) {
                for (final Class< ? extends Annotation > annotation: grouped.keySet()) {
                    if (clazz.isAnnotationPresent(annotation)) {
                        grouped.get(annotation).add(clazz);
                    }
                }
            }
        }
        
        return grouped;
    }

    private static boolean classShouldBeIgnored(final Class<?> clazz) {
        return clazz.getPackage().getName().startsWith(IGNORE_PACKAGE);
    }

    private static String getClassNames(final Collection< Class< ? > > classes) {
        final StringBuilder classNames = new StringBuilder();
        
        for (final Class< ? > clazz: classes) {
            classNames
                .append((classNames.length() > 0) ? "," : "")
                .append(clazz.getName());
        }
        
        return classNames.toString();
    }
    
    private static Class< ? > findCandidate(final Set< Class< ? > > classes) {
        for (final Class< ? > clazz: classes) {
            if (Application.class.isAssignableFrom(clazz) && !classShouldBeIgnored(clazz)) {
                LOG.fine("Found JAX-RS application to initialize: " + clazz.getName());
                return clazz;
            }
        }
        
        return null;
    }
}
