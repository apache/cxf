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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Swagger;

public class Swagger2ApiListingResource extends ApiListingResource {
    private Swagger2Customizer customizer;
    public Swagger2ApiListingResource(Swagger2Customizer customizer) {
        this.customizer = customizer;
    }
    @Override
    protected Swagger process(Application app,
                              ServletContext servletContext,
                              ServletConfig sc,
                              HttpHeaders headers,
                              UriInfo uriInfo) {
        Swagger s = super.process(app, servletContext, sc, headers, uriInfo);
        if (customizer != null) {
            s = customizer.customize(s);
        }
        return s;
    }
}