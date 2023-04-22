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
package org.apache.cxf.systests.cdi.base;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.cdi.ContextResolved;

@ApplicationScoped
@SuppressWarnings("unused")
public class Injections {
    /* this one is not supposed to work in the systests
    @Inject
    private Application application;
    */

    @Inject
    @ContextResolved
    private Application cxfApplication; //NOPMD

    @Inject
    private UriInfo uriInfo; //NOPMD

    @Inject
    @ContextResolved
    private UriInfo cxfUriInfo; //NOPMD

    @Inject
    private HttpHeaders httpHeaders; //NOPMD

    @Inject
    @ContextResolved
    private HttpHeaders cxfHttpHeaders; //NOPMD

    @Inject
    private Request request; //NOPMD

    @Inject
    @ContextResolved
    private Request cxfRequest; //NOPMD

    @Inject
    private SecurityContext securityContext; //NOPMD

    @Inject
    @ContextResolved
    private SecurityContext cxfSecurityContext; //NOPMD

    @Inject
    private Providers providers; //NOPMD

    @Inject
    @ContextResolved
    private Providers cxfProviders; //NOPMD

    @SuppressWarnings("rawtypes")
    @Inject
    private ContextResolver contextResolver; //NOPMD

    @SuppressWarnings("rawtypes")
    @Inject
    @ContextResolved
    private ContextResolver cxfContextResolver; //NOPMD

    @Inject
    private HttpServletRequest httpServletRequest; //NOPMD

    @Inject
    @ContextResolved
    private HttpServletRequest cxfHttpServletRequest; //NOPMD

    @Inject
    private HttpServletResponse httpServletResponse; //NOPMD

    @Inject
    @ContextResolved
    private HttpServletRequest cxfhttpServletResponse; //NOPMD

    @Inject
    private ServletContext servletContext; //NOPMD

    @Inject
    @ContextResolved
    private ServletContext cxfServletContext; //NOPMD

    @Inject
    private ResourceContext resourceContext; //NOPMD

    @Inject
    @ContextResolved
    private ResourceContext cxfResourceContext; //NOPMD

    @Inject
    private ResourceInfo resourceInfo; //NOPMD

    @Inject
    @ContextResolved
    private ResourceInfo cxfResourceInfo; //NOPMD

    @Inject
    private Configuration configuration; //NOPMD

    @Inject
    @ContextResolved
    private Configuration cxfConfiguration; //NOPMD

    public String state() {
        return Stream.of(Injections.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Inject.class))
                .map(f -> {
                    if (!f.isAccessible()) {
                        f.setAccessible(true);
                    }
                    try {
                        // the standard name otherwise not portable
                        return f.get(Injections.this) != null ? f.getName() + "=" + f.getType().getSimpleName() : "";
                    } catch (final IllegalAccessException e) {
                        return "";
                    }
                })
                .sorted() // be deterministic in the test, java reflection is no more since java7
                .collect(Collectors.joining("/"));
    }
}
