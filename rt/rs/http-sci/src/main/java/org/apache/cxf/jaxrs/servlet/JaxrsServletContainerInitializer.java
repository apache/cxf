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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.cxf.common.util.ClasspathScanner;

@HandlesTypes(Application.class)
public class JaxrsServletContainerInitializer implements ServletContainerInitializer {
    private static final Logger LOG = LogUtils.getL7dLogger(JaxrsServletContainerInitializer.class);
    
    @Override
    public void onStartup(final Set< Class< ? > > classes, final ServletContext ctx) throws ServletException {        
        final Dynamic servlet =  ctx.addServlet("CXFServlet", CXFNonSpringJaxrsServlet.class);
        servlet.addMapping("/*");
        
        final Class< ? > application = findCandidate(classes);
        if (application != null) {
            servlet.setInitParameter("javax.ws.rs.Application", application.getName());            
        } else {
            try {
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > providersAndResources = 
                    ClasspathScanner.findClasses(Arrays.asList(ClasspathScanner.WILDCARD), 
                        Arrays.asList(Provider.class, Path.class));
                
                servlet.setInitParameter("jaxrs.providers", getClassNames(providersAndResources.get(Provider.class)));
                servlet.setInitParameter("jaxrs.serviceClasses", getClassNames(providersAndResources.get(Path.class)));
            } catch (final ClassNotFoundException | IOException ex) {
                throw new ServletException("Unabled to perform classpath scan", ex);
            }
        }
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
            if (!clazz.getPackage().equals(JaxrsServletContainerInitializer.class.getPackage())) {
                LOG.fine("Found JAX-RS application to initialize: " + clazz.getName());
                return clazz;
            }
        }
        
        return null;
    }
}
