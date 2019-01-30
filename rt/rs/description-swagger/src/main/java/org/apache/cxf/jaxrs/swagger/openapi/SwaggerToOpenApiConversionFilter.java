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
package org.apache.cxf.jaxrs.swagger.openapi;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import io.swagger.util.Json;

@Provider
@PreMatching
public final class SwaggerToOpenApiConversionFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String SWAGGER_PATH = "swagger.json";
    private static final String OPEN_API_PATH = "openapi.json";
    private static final String OPEN_API_PROPERTY = "openapi";

    private OpenApiConfiguration openApiConfig;
    private String openApiJsonPath = OPEN_API_PATH;
    
    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        String path = reqCtx.getUriInfo().getPath();
        if (path.endsWith(openApiJsonPath)) {
            reqCtx.setRequestUri(URI.create(SWAGGER_PATH));
            reqCtx.setProperty(OPEN_API_PROPERTY, Boolean.TRUE);
        }

    }
    
    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        if (Boolean.TRUE == reqCtx.getProperty(OPEN_API_PROPERTY)) {
            final Object entity = respCtx.getEntity();
            // Right before 1.5.18, the entity was always a String but became a model object
            // (io.swagger.models.Swagger) after. For now, let us serialize it to JSON manually.
            String swaggerJson = entity instanceof String ? (String)entity : Json.pretty(entity);
            String openApiJson = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerJson(
                    createMessageContext(), swaggerJson, openApiConfig);
            respCtx.setEntity(openApiJson);
        }
    }

    private MessageContext createMessageContext() {
        return JAXRSUtils.createContextValue(
                JAXRSUtils.getCurrentMessage(), null, MessageContext.class);
    }

    public void setOpenApiConfig(OpenApiConfiguration openApiConfig) {
        this.openApiConfig = openApiConfig;
    }

    public void setOpenApiJsonPath(String openApiJsonPath) {
        this.openApiJsonPath = openApiJsonPath;
    }
}
