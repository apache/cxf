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
package org.apache.cxf.jaxrs.swagger.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;

/**
 * Generic trait to support Swagger UI integration for Swagger 1.5.x and
 * OpenAPI v3.x (Swagger 2.x) integrations.
 */
public interface SwaggerUiSupport {
    String SUPPORT_UI_PROPERTY = "support.swagger.ui";
    
    /**
     * Holds the resources and/or providers which are required for
     * Swagger UI integration to be plugged in. 
     */
    class Registration {
        private final List<Object> resources = new ArrayList<>();
        private final List<Object> providers = new ArrayList<>();
        
        public List<Object> getResources() {
            return resources;
        }
        
        public List<Object> getProviders() {
            return providers;
        }
    }
    
    /**
     * Detects the presence of Swagger UI in classpath with respect to properties and
     * configuration provided. 
     * @param bus bus instance 
     * @param swaggerProps Swagger properties (usually externalized) 
     * @param runAsFilter "true" if Swagger integration is run as a filter, "false" otherwise. 
     * @return the Swagger UI registration
     */
    default Registration getSwaggerUi(Bus bus, Properties swaggerProps, boolean runAsFilter) {
        final Registration registration = new Registration();
        
        if (checkSupportSwaggerUiProp(swaggerProps)) {
            String swaggerUiRoot = findSwaggerUiRoot();
            
            if (swaggerUiRoot != null) {
                final SwaggerUiResourceLocator locator = new SwaggerUiResourceLocator(swaggerUiRoot);
                SwaggerUiService swaggerUiService = new SwaggerUiService(locator, getSwaggerUiMediaTypes());
                swaggerUiService.setConfig(getSwaggerUiConfig());
                
                if (!runAsFilter) {
                    registration.resources.add(swaggerUiService);
                } else {
                    registration.providers.add(new SwaggerUiServiceFilter(swaggerUiService));
                }

                registration.providers.add(new SwaggerUiResourceFilter(locator));
                bus.setProperty("swagger.service.ui.available", "true");
            }
        }
        
        return registration;
    }
    
    /**
     * Checks the Swagger properties to determine if Swagger UI support is available or not.
     * @param props Swagger properties (usually externalized) 
     * @return
     */
    default boolean checkSupportSwaggerUiProp(Properties props) {
        Boolean theSupportSwaggerUI = isSupportSwaggerUi();
        if (theSupportSwaggerUI == null && props != null && props.containsKey(SUPPORT_UI_PROPERTY)) {
            theSupportSwaggerUI = PropertyUtils.isTrue(props.get(SUPPORT_UI_PROPERTY));
        }
        if (theSupportSwaggerUI == null) {
            theSupportSwaggerUI = true;
        }
        return theSupportSwaggerUI;
    }

    /**
     * Checks if Swagger UI support is available or not.
     * @return "true" if Swagger UI support is available, "false" otherwise
     */
    Boolean isSupportSwaggerUi();

    /**
     * Detects the Swagger UI in root with respect to properties and configuration 
     * provided. 
     * @return Swagger UI in root or "null" if not available
     */
    String findSwaggerUiRoot();
    
    /**
     * Returns media types supported by Swagger UI
     * @return media types supported by Swagger UI
     */
    Map<String, String> getSwaggerUiMediaTypes();
    
    /**
     * Returns Swagger UI configuration parameters.
     * @return Swagger UI configuration parameters or "null" if not available
     */
    SwaggerUiConfig getSwaggerUiConfig();
}
