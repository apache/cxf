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

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriInfo;

@PreMatching
@Priority(Priorities.USER + 1)
class SwaggerUiServiceFilter implements ContainerRequestFilter {
    private final SwaggerUiService uiService;
    
    SwaggerUiServiceFilter(SwaggerUiService uiService) {
        this.uiService = uiService;
    }
    
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        if (HttpMethod.GET.equals(rc.getRequest().getMethod())) {
            UriInfo ui = rc.getUriInfo();
            String path = ui.getPath();
            int uiPathIndex = path.lastIndexOf("api-docs");
            if (uiPathIndex >= 0) {
                String resourcePath = uiPathIndex + 8 < path.length()
                    ? path.substring(uiPathIndex + 8) : "";
                rc.abortWith(uiService.getResource(ui, resourcePath));
            }
        }
    }
}
