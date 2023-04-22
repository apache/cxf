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

package org.apache.cxf.jaxrs.openapi;

import java.util.Objects;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.apache.cxf.jaxrs.common.openapi.DelegatingServletConfig;
import org.apache.cxf.jaxrs.common.openapi.SyntheticServletConfig;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;

import io.swagger.v3.oas.integration.api.OpenApiContext;

class ServletConfigProvider implements ContextProvider<ServletConfig> {
    private final String contextId;
    
    ServletConfigProvider(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public ServletConfig createContext(Message message) {
        final ServletConfig sc = (ServletConfig)message.get("HTTP.CONFIG");

        // When deploying into OSGi container, it is possible to use embedded Jetty
        // transport. In this case, the ServletConfig is not available and Swagger
        // does not take into account certain configuration parameters. To overcome
        // that, the ServletConfig is synthesized from ServletContext instance.
        if (sc == null) {
            final ServletContext context = (ServletContext)message.get("HTTP.CONTEXT");
            if (context != null) {
                return new SyntheticServletConfig(context) {
                    @Override
                    public String getInitParameter(String name) {
                        if (Objects.equals(OpenApiContext.OPENAPI_CONTEXT_ID_KEY, name)) {
                            return contextId;
                        } else {
                            return super.getInitParameter(name);
                        }
                    }
                };
            }
        } else {
            return new DelegatingServletConfig(sc) {
                @Override
                public String getInitParameter(String name) {
                    if (Objects.equals(OpenApiContext.OPENAPI_CONTEXT_ID_KEY, name)) {
                        return contextId;
                    } else {
                        return super.getInitParameter(name);
                    }
                }
            };
        }

        return sc;
    }
}
