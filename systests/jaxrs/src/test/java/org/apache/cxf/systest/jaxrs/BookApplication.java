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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@ApplicationPath("/thebooks")
public class BookApplication extends Application {

    private String defaultName;
    private long defaultId;
    
    
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
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(org.apache.cxf.systest.jaxrs.BookStorePerRequest.class);
        classes.add(org.apache.cxf.systest.jaxrs.jaxws.BookStoreJaxrsJaxws.class);
        classes.add(org.apache.cxf.systest.jaxrs.RuntimeExceptionMapper.class);
        classes.add(BookRequestFilter.class);
        return classes;
    }

    @Override 
    public Set<Object> getSingletons() {
        Set<Object> classes = new HashSet<Object>();
        org.apache.cxf.systest.jaxrs.BookStore store = 
            new org.apache.cxf.systest.jaxrs.BookStore();
        store.setDefaultNameAndId(defaultName, defaultId);
        classes.add(store);
        BookExceptionMapper mapper = new org.apache.cxf.systest.jaxrs.BookExceptionMapper();
        mapper.setToHandle(true);
        classes.add(mapper);
        return classes;
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
    
    public static class BookRequestFilter implements ContainerRequestFilter {
        private UriInfo ui;
        
        public BookRequestFilter(@Context UriInfo ui) {
            this.ui = ui;
        }
        
        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            if (ui.getRequestUri().toString().endsWith("/application11/thebooks/bookstore2/bookheaders")) {
                context.getHeaders().put("BOOK", Arrays.asList("1", "2", "3"));    
            }
            
        }
        
    }
}
