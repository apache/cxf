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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

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
    private Application cxfApplication;

    @Inject
    private UriInfo uriInfo;

    @Inject
    @ContextResolved
    private UriInfo cxfUriInfo;

    @Inject
    private HttpHeaders httpHeaders;

    @Inject
    @ContextResolved
    private HttpHeaders cxfHttpHeaders;

    @Inject
    private Request request;

    @Inject
    @ContextResolved
    private Request cxfRequest;

    @Inject
    private SecurityContext securityContext;

    @Inject
    @ContextResolved
    private SecurityContext cxfSecurityContext;

    @Inject
    private Providers providers;

    @Inject
    @ContextResolved
    private Providers cxfProviders;

    @SuppressWarnings("rawtypes")
    @Inject
    private ContextResolver contextResolver;

    @SuppressWarnings("rawtypes")
    @Inject
    @ContextResolved
    private ContextResolver cxfContextResolver;

    @Inject
    private HttpServletRequest httpServletRequest;

    @Inject
    @ContextResolved
    private HttpServletRequest cxfHttpServletRequest;

    @Inject
    private HttpServletResponse httpServletResponse;

    @Inject
    @ContextResolved
    private HttpServletRequest cxfhttpServletResponse;

    @Inject
    private ServletContext servletContext;

    @Inject
    @ContextResolved
    private ServletContext cxfServletContext;

    @Inject
    private ResourceContext resourceContext;

    @Inject
    @ContextResolved
    private ResourceContext cxfResourceContext;

    @Inject
    private ResourceInfo resourceInfo;

    @Inject
    @ContextResolved
    private ResourceInfo cxfResourceInfo;

    @Inject
    private Configuration configuration;

    @Inject
    @ContextResolved
    private Configuration cxfConfiguration;

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
