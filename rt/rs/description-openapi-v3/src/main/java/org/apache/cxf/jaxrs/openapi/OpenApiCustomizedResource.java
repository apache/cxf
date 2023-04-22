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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.jaxrs2.integration.ServletConfigContextUtils;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.GenericOpenApiContext;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;

@Path("/openapi.{type:json|yaml}")
public class OpenApiCustomizedResource extends BaseOpenApiResource {

    private final OpenApiCustomizer customizer;

    public OpenApiCustomizedResource(final OpenApiCustomizer customizer) {
        this.customizer = customizer;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/yaml" })
    @Operation(hidden = true)
    public Response getOpenApi(@Context Application app, @Context ServletConfig config, 
            @Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {

        if (customizer != null) {
            final OpenAPIConfiguration configuration = customizer.customize(getOpenApiConfiguration());
            setOpenApiConfiguration(configuration);

            // By default, the OpenApiContext instance is cached. It means that the configuration
            // changes won't be taken into account (due to the deep copying rather than reference 
            // passing). In order to reflect any changes which customization may do, we have to 
            // update reader's configuration directly.
            OpenApiContext ctx = getOpenApiContext(config);
            if (ctx == null) {
                // If there is no context associated with the servlet config, let us
                // try to fallback to default one. 
                ctx = getOpenApiContext(null);
            }
            
            if (ctx instanceof GenericOpenApiContext<?>) {
                ((GenericOpenApiContext<?>) ctx).getOpenApiReader().setConfiguration(configuration);
                
                final OpenAPI oas = ctx.read();
                customizer.customize(oas);
                
                if (!Objects.equals(configuration.getOpenAPI().getInfo(), oas.getInfo())) {
                    configuration.getOpenAPI().setInfo(oas.getInfo());
                }
                
                if (!Objects.equals(configuration.getOpenAPI().getComponents(), oas.getComponents())) {
                    configuration.getOpenAPI().setComponents(oas.getComponents());
                }
                
                if (!Objects.equals(configuration.getOpenAPI().getExternalDocs(), oas.getExternalDocs())) {
                    configuration.getOpenAPI().setExternalDocs(oas.getExternalDocs());
                }
                
                if (!Objects.equals(configuration.getOpenAPI().getPaths(), oas.getPaths())) {
                    configuration.getOpenAPI().setPaths(oas.getPaths());
                }
                
                if (!Objects.equals(configuration.getOpenAPI().getTags(), oas.getTags())) {
                    configuration.getOpenAPI().setTags(oas.getTags());
                }
                
                if (!Objects.equals(configuration.getOpenAPI().getExtensions(), oas.getExtensions())) {
                    configuration.getOpenAPI().setExtensions(oas.getExtensions());
                }
            }
        }

        return super.getOpenApi(headers, config, app, uriInfo, type);
    }

    private OpenApiContext getOpenApiContext(ServletConfig config) {
        final String ctxId = ServletConfigContextUtils.getContextIdFromServletConfig(config);
        return OpenApiContextLocator.getInstance().getOpenApiContext(ctxId);
    }
}
