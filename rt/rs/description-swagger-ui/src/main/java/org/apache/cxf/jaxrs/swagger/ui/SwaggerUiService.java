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
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;


@Path("api-docs")
public class SwaggerUiService {
    private static final String FAVICON = "favicon";
    private static final Map<String, String> DEFAULT_MEDIA_TYPES;
    private static final Pattern URL_PATTERN = Pattern.compile("url[:]\\s*[\"]([^\"]+)[\"][,]");

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
    private SwaggerUiConfig config;

    public SwaggerUiService(SwaggerUiResourceLocator locator, Map<String, String> mediaTypes) {
        this.locator = locator;
        this.mediaTypes = mediaTypes;
    }
    
    public void setConfig(SwaggerUiConfig config) {
        this.config = config;
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

            // If there are no query parameters and Swagger UI configuration is
            // provided, let us do temporary redirect with the Swagger UI configuration
            // wrapped into the query string. For example, the request to
            //
            //    http://localhost:8080/services/helloservice/api-docs
            //
            // might be redirect to
            //
            //    http://localhost:8080/services/helloservice/api-docs?url=/services/helloservice/openapi.json
            //
            // in case the "url" configuration parameter is provided for Swagger UI.
            if (config != null) {
                if (path.endsWith("/index.html") && uriInfo.getQueryParameters().isEmpty()) {
                    final Map<String, String> params = config.getConfigParameters();
                    
                    if (params != null && !params.isEmpty()) {
                        final UriBuilder builder = params
                            .entrySet()
                            .stream()
                            .reduce(
                                uriInfo.getRequestUriBuilder(), 
                                (b, e) -> b.queryParam(e.getKey(), e.getValue()),
                                (left, right) -> left
                            );
                        return Response.temporaryRedirect(builder.build()).build();
                    }
                }

                // Since Swagger UI 4.1.3, passing the default URL as query parameter, 
                // e.g. `?url=swagger.json` is disabled by default due to security concerns.
                final boolean hasUrlPlaceholder = path.endsWith("/index.html")
                    || path.endsWith("/swagger-initializer.js");
                if (hasUrlPlaceholder && !Boolean.TRUE.equals(config.isQueryConfigEnabled())) {
                    final String url = config.getUrl();
                    if (!StringUtils.isEmpty(url)) {
                        try (InputStream in = resourceURL.openStream()) {
                            final String index = replaceUrl(IOUtils.readStringFromStream(in), url);
                            final ResponseBuilder rb = Response.ok(index);

                            if (mediaType != null) {
                                rb.type(mediaType);
                            }

                            return rb.build();
                        }
                    }
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

    /**
     * Replaces the URL inside Swagger UI index.html file. The implementation does not attempt to 
     * read the file and parse it as valid HTML but uses straightforward approach by looking for 
     * the URL pattern of the SwaggerUIBundle initialization and replacing it.
     * @param index index.html file content
     * @param replacement replacement URL 
     * @return index.html file content with replaced URL
     */
    protected String replaceUrl(final String index, final String replacement) {
        final Matcher matcher = URL_PATTERN.matcher(index);

        if (matcher.find()) {
            return index.substring(0, matcher.start(1)) + replacement + index.substring(matcher.end(1)); 
        }

        return index;
    }
}

