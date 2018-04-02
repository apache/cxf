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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

@Path("api-docs")
public class SwaggerUiService {
    private static final String FAVICON = "favicon";
    private static final Map<String, String> DEFAULT_MEDIA_TYPES;

    static {
        DEFAULT_MEDIA_TYPES = new HashMap<>();
        DEFAULT_MEDIA_TYPES.put("html", "text/html");
        DEFAULT_MEDIA_TYPES.put("png", "image/png");
        DEFAULT_MEDIA_TYPES.put("gif", "image/gif");
        DEFAULT_MEDIA_TYPES.put("css", "text/css");
        DEFAULT_MEDIA_TYPES.put("js", "application/javascript");
        DEFAULT_MEDIA_TYPES.put("eot", "application/vnd.ms-fontobject");
        DEFAULT_MEDIA_TYPES.put("ttf", "application/font-sfnt");
        DEFAULT_MEDIA_TYPES.put("svg", "image/svg+xml");
        DEFAULT_MEDIA_TYPES.put("woff", "application/font-woff");
        DEFAULT_MEDIA_TYPES.put("woff2", "application/font-woff2");
    }

    
    private final SwaggerUiResourceLocator locator;
    private final Map<String, String> mediaTypes;

    public SwaggerUiService(SwaggerUiResourceLocator locator, Map<String, String> mediaTypes) {
        this.locator = locator;
        this.mediaTypes = mediaTypes;
    }

    @GET
    @Path("{resource:.*}")
    public Response getResource(@Context UriInfo uriInfo, @PathParam("resource") String resourcePath) {
        if (resourcePath.contains(FAVICON)) {
            return Response.status(404).build();
        }
        
        try {
            final URL resourceURL = locator.locate(resourcePath);
            final String path = resourceURL.getPath();

            String mediaType = null;
            int ind = path.lastIndexOf('.');
            if (ind != -1 && ind < path.length()) {
                String resourceExt = path.substring(ind + 1);
                if (mediaTypes != null && mediaTypes.containsKey(resourceExt)) {
                    mediaType = mediaTypes.get(resourceExt);
                } else {
                    mediaType = DEFAULT_MEDIA_TYPES.get(resourceExt);
                }
            }

            ResponseBuilder rb = Response.ok(resourceURL.openStream());
            if (mediaType != null) {
                rb.type(mediaType);
            }
            return rb.build();
        } catch (IOException ex) {
            throw new NotFoundException(ex);
        }
    }
}

