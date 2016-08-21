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
package org.apache.cxf.jaxrs.swagger;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * Implementation of servlet config which merges real servlet init parameters with some extra
 * parameters set on jaxrs endpoint.
 */
public class Swagger2ServletConfig implements ServletConfig {

    private final String servletName;
    private final ServletContext servletContext;
    private final Dictionary<String, String> initParameters;

    public Swagger2ServletConfig(ServletConfig servletConfig, Map<String, Object> properties) {
        this.servletName = servletConfig.getServletName();
        this.servletContext = servletConfig.getServletContext();
        this.initParameters = new Hashtable<>();

        Enumeration<String> parameterNames = servletConfig.getInitParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            initParameters.put(parameterName, servletConfig.getInitParameter(parameterName));
        }

        if (properties != null) {
            for (String key : properties.keySet()) {
                if (key.startsWith("swagger.")) {
                    Object propertyValue = properties.get(key);
                    initParameters.put(key, propertyValue == null ? null : propertyValue.toString());
                }
            }
        }
    }

    @Override
    public String getServletName() {
        return this.servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return initParameters.keys();
    }
}
