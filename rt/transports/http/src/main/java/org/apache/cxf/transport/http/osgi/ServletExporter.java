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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;

import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

class ServletExporter implements ManagedService {
    protected static final Logger LOG = LogUtils.getL7dLogger(ServletExporter.class); 
    private String alias;
    private Servlet servlet;
    private ServiceRegistration serviceRegistration;
    private HttpService httpService;
    
    public ServletExporter(Servlet servlet, HttpService httpService) {
        this.servlet = servlet;
        this.httpService = httpService;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        if (alias != null) {
            httpService.unregister(alias);
            alias = null;
        }
        if (properties == null) {
            properties = new Properties();
        }
        Properties sprops = new Properties();
        sprops.put("init-param", 
                   getProp(properties, "org.apache.cxf.servlet.init-param", ""));
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
        alias = (String)getProp(properties, "org.apache.cxf.servlet.context", "/cxf");
        HttpContext context = httpService.createDefaultHttpContext();
        try {
            httpService.registerServlet(alias, servlet, sprops, context);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error registering CXF OSGi servlet " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("rawtypes")
    private Object getProp(Dictionary properties, String key, Object defaultValue) {
        Object value = properties.get(key);
        return value == null ? defaultValue : value;
    }
    
}