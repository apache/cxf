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
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Provider
@PreMatching
public final class SwaggerToOpenApiConversionFilter implements ContainerRequestFilter,
    ContainerResponseFilter, WriterInterceptor {

    private static final String SWAGGER_PATH = "swagger.json";
    private static final String OPEN_API_PATH = "openapi.json";
    private static final String OPEN_API_PROPERTY = "openapi";
    
    private OpenApiConfiguration openApiConfig;
    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        String path = reqCtx.getUriInfo().getPath();
        if (path.endsWith(OPEN_API_PATH)) {
            reqCtx.setRequestUri(URI.create(SWAGGER_PATH));
            JAXRSUtils.getCurrentMessage().getExchange().put(OPEN_API_PROPERTY, Boolean.TRUE);
        }
        
    }
    
    public OpenApiConfiguration getOpenApiConfig() {
        return openApiConfig;
    }

    public void setOpenApiConfig(OpenApiConfiguration openApiConfig) {
        this.openApiConfig = openApiConfig;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (isOpenApiRequested()) {
            OutputStream os = context.getOutputStream();
            CachedOutputStream cos = new CachedOutputStream();
            context.setOutputStream(cos);
            context.proceed();
            String swaggerJson = IOUtils.readStringFromStream(cos.getInputStream());
            String openApiJson = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerJson(swaggerJson, openApiConfig);
            os.write(StringUtils.toBytesUTF8(openApiJson));
            os.flush();
            
        }
    }

    private boolean isOpenApiRequested() {
        return Boolean.TRUE == JAXRSUtils.getCurrentMessage().getExchange().get(OPEN_API_PROPERTY);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        if (isOpenApiRequested() && responseContext.getEntity() instanceof String) {
            String swaggerJson = (String)responseContext.getEntity();
            String openApiJson = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerJson(swaggerJson, openApiConfig);
            responseContext.setEntity(openApiJson);
            JAXRSUtils.getCurrentMessage().getExchange().remove(OPEN_API_PROPERTY);
        }
        
    }
}
