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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

@ApplicationPath("/")
@GlobalNameBinding
public class BookApplicationNonSpring extends Application {

    private String defaultName;
    private long defaultId;
    @Context
    private UriInfo uriInfo;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(org.apache.cxf.systest.jaxrs.BookStore.class);
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
        classes.add(new OpenApiFeature());
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

}
