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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;


import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;


/**
 * A Servlet which supports loading of JAX-WS endpoints from an
 * XML file and handling requests for endpoints created via other means
 * such as Spring beans, or the Java API. All requests are passed on
 * to the {@link ServletController}.
 *
 */
public class CXFServlet extends AbstractCXFServlet implements ApplicationListener {
    
    private GenericApplicationContext childCtx;
    private boolean inRefresh;
    
    
    public static Logger getLogger() {
        return LogUtils.getL7dLogger(CXFServlet.class);
    }
    
    public void loadBus(ServletConfig servletConfig) throws ServletException {
        String springCls = "org.springframework.context.ApplicationContext";
        try {
            ClassLoaderUtils.loadClass(springCls, getClass());
            loadSpringBus(servletConfig);
        } catch (ClassNotFoundException e) {                
            LOG.log(Level.SEVERE, "FAILED_TO_LOAD_SPRING_BUS", new Object[]{e});
            throw new ServletException("Can't load bus with Spring context class", e);
        }
    }
    
    
    private void loadSpringBus(ServletConfig servletConfig) throws ServletException {
        
        // try to pull an existing ApplicationContext out of the
        // ServletContext
        ServletContext svCtx = getServletContext();

        // Spring 1.x
        ApplicationContext ctx = (ApplicationContext)svCtx
            .getAttribute("interface org.springframework.web.context.WebApplicationContext.ROOT");

        // Spring 2.0
        if (ctx == null) {
            Object ctxObject = svCtx
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
            if (ctxObject instanceof ApplicationContext) {
                ctx = (ApplicationContext) ctxObject;
            } else if (ctxObject != null) {
                // it should be a runtime exception                
                Exception ex = (Exception) ctxObject;
                throw new ServletException(ex);
            }                   
        }
        
        updateContext(servletConfig, ctx);

        if (ctx instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)ctx).addApplicationListener(this);
        }
    }
    private void updateContext(ServletConfig servletConfig, ApplicationContext ctx) {
        /* If ctx is null, normally no ContextLoaderListener 
         * was defined in web.xml.  Default bus with all extensions
         * will be created in this case.
         * 
         * If ctx not null, was already created by ContextLoaderListener.
         * Bus with only those extensions defined in the ctx will be created. 
         */
        if (ctx == null) {            
            LOG.info("LOAD_BUS_WITHOUT_APPLICATION_CONTEXT");
            bus = new SpringBusFactory().createBus();
            ctx = bus.getExtension(BusApplicationContext.class);
        } else {
            LOG.info("LOAD_BUS_WITH_APPLICATION_CONTEXT");
            inRefresh = true;
            try {
                bus = new SpringBusFactory(ctx).createBus();
            } finally {
                inRefresh = false;
            }
        }        
        
        ResourceManager resourceManager = bus.getExtension(ResourceManager.class);
        resourceManager.addResourceResolver(new ServletContextResourceResolver(
                                               servletConfig.getServletContext()));
        
        replaceDestinationFactory();

        // Set up the ServletController
        controller = createServletController(servletConfig);
        
        // build endpoints from the web.xml or a config file
        loadAdditionalConfig(ctx, servletConfig);
    }

    private void loadAdditionalConfig(ApplicationContext ctx, 
                                        ServletConfig servletConfig) {
        String location = servletConfig.getInitParameter("config-location");
        if (location == null) {
            location = "/WEB-INF/cxf-servlet.xml";
        }
        InputStream is = null;
        try {
            is = servletConfig.getServletContext().getResourceAsStream(location);
            
            if (is == null || is.available() == -1) {
                URIResolver resolver = new URIResolver(location);

                if (resolver.isResolved()) {
                    is = resolver.getInputStream();
                }
            }
        } catch (IOException e) {
            //throw new ServletException(e);
        }
        
        if (is != null) {
            LOG.log(Level.INFO, "BUILD_ENDPOINTS_FROM_CONFIG_LOCATION", new Object[]{location});
            childCtx = new GenericApplicationContext(ctx);
            
            XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(childCtx);
            reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
            reader.loadBeanDefinitions(new InputStreamResource(is, location));
            
            childCtx.refresh();
        } 
    }

    public void destroy() {
        if (childCtx != null) {
            childCtx.destroy();
        }
        super.destroy();        
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (!inRefresh && event instanceof ContextRefreshedEvent && getServletConfig() != null) {
            //need to re-do the bus/controller stuff
            try {
                inRefresh = true;
                updateContext(this.getServletConfig(), 
                          ((ContextRefreshedEvent)event).getApplicationContext());
            } finally {
                inRefresh = false;
            }
        }
    }

    
}
