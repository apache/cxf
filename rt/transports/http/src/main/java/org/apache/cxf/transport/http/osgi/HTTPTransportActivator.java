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

package org.apache.cxf.transport.http.osgi;

import java.util.Dictionary;
import java.util.Properties;

import javax.servlet.Servlet;

import org.apache.cxf.bus.blueprint.BlueprintNameSpaceHandlerFactory;
import org.apache.cxf.bus.blueprint.NamespaceHandlerRegisterer;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.DestinationRegistryImpl;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.blueprint.HttpBPHandler;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

public class HTTPTransportActivator 
    implements BundleActivator {
    private static final String CXF_CONFIG_SCOPE = "org.apache.cxf.osgi";
    private static final String DISABLE_DEFAULT_HTTP_TRANSPORT = CXF_CONFIG_SCOPE + ".http.transport.disable";
    
    public void start(BundleContext context) throws Exception {
        
        ConfigAdminHttpConduitConfigurer conduitConfigurer = new ConfigAdminHttpConduitConfigurer();
        
        registerService(context, ManagedServiceFactory.class, conduitConfigurer, 
                        ConfigAdminHttpConduitConfigurer.FACTORY_PID);
        registerService(context, HTTPConduitConfigurer.class, conduitConfigurer, 
                        "org.apache.cxf.http.conduit-configurer");
        
        if (PropertyUtils.isTrue(context.getProperty(DISABLE_DEFAULT_HTTP_TRANSPORT))) {
            //TODO: Review if it also makes sense to support "http.transport.disable" 
            //      directly in the CXF_CONFIG_SCOPE properties file
            return;
        }
        
        DestinationRegistry destinationRegistry = new DestinationRegistryImpl();
        HTTPTransportFactory transportFactory = new HTTPTransportFactory(destinationRegistry);
        Servlet servlet = new CXFNonSpringServlet(destinationRegistry , false);
        ServletConfigurer servletConfig = new ServletConfigurer(context, servlet);

        context.registerService(DestinationRegistry.class.getName(), destinationRegistry, null);
        context.registerService(HTTPTransportFactory.class.getName(), transportFactory, null);
        registerService(context, ManagedService.class, servletConfig, CXF_CONFIG_SCOPE);

        BlueprintNameSpaceHandlerFactory factory = new BlueprintNameSpaceHandlerFactory() {
            
            @Override
            public Object createNamespaceHandler() {
                return new HttpBPHandler();
            }
        };
        NamespaceHandlerRegisterer.register(context, factory,
                                            "http://cxf.apache.org/transports/http/configuration");  
    }

    private void registerService(BundleContext context, Class<?> serviceInterface,
                                        Object serviceObject, String servicePid) {
        Properties servProps = new Properties();
        servProps.put(Constants.SERVICE_PID,  servicePid);  
        context.registerService(serviceInterface.getName(), serviceObject, servProps);
    }

    public void stop(BundleContext context) throws Exception {
    }

    
    class ServletConfigurer implements ManagedService {
        private ServiceRegistration reg;
        private BundleContext context;
        private Servlet servlet;
        private ServiceRegistration serviceRegistration;
        
        public ServletConfigurer(BundleContext context, Servlet servlet) {
            this.servlet = servlet;
            this.context = context;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void updated(Dictionary properties) throws ConfigurationException {
            if (reg != null) {
                reg.unregister();
            }
            if (properties == null) {
                properties = new Properties();
            }
            Properties sprops = new Properties();
            sprops.put("init-prefix", "");
            
            sprops.put("alias",
                       getProp(properties, "org.apache.cxf.servlet.context", "/cxf"));
            sprops.put("servlet-name", 
                       getProp(properties, "org.apache.cxf.servlet.name", "cxf-osgi-transport-servlet"));
            sprops.put("hide-service-list-page",
                       getProp(properties, "org.apache.cxf.servlet.hide-service-list-page", "false"));
            sprops.put("disable-address-updates",
                       getProp(properties, "org.apache.cxf.servlet.disable-address-updates", "true"));
            sprops.put("base-address",
                       getProp(properties, "org.apache.cxf.servlet.base-address", ""));
            sprops.put("service-list-path",
                       getProp(properties, "org.apache.cxf.servlet.service-list-path", ""));
            sprops.put("static-resources-list",
                       getProp(properties, "org.apache.cxf.servlet.static-resources-list", ""));
            sprops.put("redirects-list",
                       getProp(properties, "org.apache.cxf.servlet.redirects-list", ""));
            sprops.put("redirect-servlet-name",
                       getProp(properties, "org.apache.cxf.servlet.redirect-servlet-name", ""));
            sprops.put("redirect-servlet-path",
                       getProp(properties, "org.apache.cxf.servlet.redirect-servlet-path", ""));
            sprops.put("service-list-all-contexts",
                       getProp(properties, "org.apache.cxf.servlet.service-list-all-contexts", ""));
            sprops.put("service-list-page-authenticate",
                       getProp(properties, "org.apache.cxf.servlet.service-list-page-authenticate", "false"));
            sprops.put("service-list-page-authenticate-realm",
                       getProp(properties, "org.apache.cxf.servlet.service-list-page-authenticate-realm", "karaf"));
            sprops.put("use-x-forwarded-headers",
                       getProp(properties, "org.apache.cxf.servlet.use-x-forwarded-headers", "false"));
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

            serviceRegistration = context.registerService(Servlet.class.getName(), servlet, sprops);
        }

        @SuppressWarnings("rawtypes")
        private Object getProp(Dictionary properties, String key, Object defaultValue) {
            Object value = properties.get(key);
            return value == null ? defaultValue : value;
        }
        
    }
}
