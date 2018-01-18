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

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.servers.Server;

public class OpenApiCustomizer {
    private boolean dynamicBasePath;
    
    public OpenAPIConfiguration customize(OpenAPIConfiguration configuration) {
        if (configuration == null) {
            return configuration;
        }
        
        if (dynamicBasePath) {
            final MessageContext ctx = createMessageContext();
            final String url = StringUtils.substringBeforeLast(ctx.getUriInfo().getRequestUri().toString(), "/");
            
            final Collection<Server> servers = configuration.getOpenAPI().getServers();
            if (servers == null || servers.stream().noneMatch(s -> s.getUrl().equalsIgnoreCase(url))) {
                configuration.getOpenAPI().setServers(Collections.singletonList(new Server().url(url)));
            }
        }
        
        return configuration;
    }
    
    private MessageContext createMessageContext() {
        return JAXRSUtils.createContextValue(JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
    }

    public void setDynamicBasePath(boolean dynamicBasePath) {
        this.dynamicBasePath = dynamicBasePath;
    }
    
    public boolean isDynamicBasePath() {
        return dynamicBasePath;
    }
}
