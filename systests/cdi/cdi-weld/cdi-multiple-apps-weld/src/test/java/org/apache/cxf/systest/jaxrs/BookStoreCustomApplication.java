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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationFeature;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.systests.cdi.base.BookStore;
import org.apache.cxf.systests.cdi.base.CustomScopedBookStore;
import org.apache.cxf.systests.cdi.base.RequestScopedBookStore;
import org.apache.cxf.systests.cdi.base.bindings.LoggingFilter;
import org.apache.cxf.systests.cdi.base.contract.BookStoreImpl;

@ApplicationPath("/v2")
public class BookStoreCustomApplication extends Application {
    @Override
    public Set< Object > getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(new JacksonJsonProvider());
        singletons.add(new ValidationExceptionMapper());
        singletons.add(new JAXRSBeanValidationFeature());
        singletons.add(new LoggingFilter());
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new LinkedHashSet<>(Arrays.asList(BookStore.class, RequestScopedBookStore.class, 
            CustomScopedBookStore.class, BookStoreImpl.class));
    }
}
