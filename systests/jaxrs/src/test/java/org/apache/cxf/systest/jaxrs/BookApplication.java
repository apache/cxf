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
package org.apache.cxf.systest.jaxrs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

@ApplicationPath("/thebooks")
@GlobalNameBinding
public class BookApplication extends Application {

    private String defaultName;
    private long defaultId;
    @Context
    private UriInfo uriInfo;

    public BookApplication(@Context ServletContext sc) {
        if (sc == null) {
            throw new IllegalArgumentException("ServletContext is null");
        }
        if (!"contextParamValue".equals(sc.getInitParameter("contextParam"))) {
            throw new IllegalStateException("ServletContext is not initialized");
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(org.apache.cxf.systest.jaxrs.BookStorePerRequest.class);
        classes.add(org.apache.cxf.systest.jaxrs.jaxws.BookStoreJaxrsJaxws.class);
        classes.add(org.apache.cxf.systest.jaxrs.RuntimeExceptionMapper.class);
        classes.add(BookRequestFilter.class);
        classes.add(BookRequestFilter2.class);
        classes.add(BookWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> classes = new HashSet<>();
        org.apache.cxf.systest.jaxrs.BookStore store =
            new org.apache.cxf.systest.jaxrs.BookStore(uriInfo);
        store.setDefaultNameAndId(defaultName, defaultId);
        classes.add(store);
        BookExceptionMapper mapper = new org.apache.cxf.systest.jaxrs.BookExceptionMapper();
        mapper.setToHandle(true);
        classes.add(mapper);
        return classes;
    }


    @Override
    public Map<String, Object> getProperties() {
        return Collections.<String, Object>singletonMap("book", "cxf");
    }

    public void setDefaultName(String name) {
        defaultName = name;
    }

    public void setDefaultId(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(id);
        }
        defaultId = Long.valueOf(sb.toString());
    }

    @GlobalNameBinding
    public static class BookWriter implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException,
            WebApplicationException {
            context.getHeaders().putSingle("BookWriter", "TheBook");
            
            final Object property = context.getProperty("property");
            if (property != null) {
                context.getHeaders().putSingle("X-Property-WriterInterceptor", property);
            }
            
            context.proceed();
        }

    }

    @Priority(1)
    public static class BookRequestFilter implements ContainerRequestFilter {
        private UriInfo ui;
        private Application ap;

        public BookRequestFilter(@Context UriInfo ui, @Context Application ap) {
            this.ui = ui;
            this.ap = ap;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ap == null) {
                throw new RuntimeException();
            }
            String uri = ui.getRequestUri().toString();
            if (uri.endsWith("/application11/thebooks/bookstore2/bookheaders")
                || uri.contains("/application6")) {
                context.getHeaders().put("BOOK", Arrays.asList("1", "2"));
            }
            
            final String value = context.getUriInfo().getQueryParameters().getFirst("property");
            if (value != null) {
                context.setProperty("property", value);
            }
        }

    }

    @Priority(2)
    public static class BookRequestFilter2 implements ContainerRequestFilter {
        private UriInfo ui;
        @Context
        private Application ap;

        @Context
        public void setUriInfo(UriInfo context) {
            this.ui = context;
        }

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ap == null) {
                throw new RuntimeException();
            }
            String uri = ui.getRequestUri().toString();
            if (uri.endsWith("/application11/thebooks/bookstore2/bookheaders")
                || uri.contains("/application6")) {
                context.getHeaders().add("BOOK", "3");
            }

        }

    }
}
