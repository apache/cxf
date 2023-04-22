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
package org.apache.cxf.systest.jaxrs.cdi;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationFeature;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.systests.cdi.base.BookStoreService;

@ApplicationPath("/")
public class BookStoreProducerApplication extends Application {
    @Produces protected BookStoreValidatingFeed bookStoreValidatingFeed = new BookStoreValidatingFeed();
    @Inject private BookStoreService service;

    @Produces @Singleton
    public ValidationExceptionMapper validationExceptionMapper() {
        return new ValidationExceptionMapper();
    }

    @Produces
    public Feature sampleFeature() {
        return new SampleFeature();
    }

    @Produces
    public BookStoreFeed bookStoreFeed(ServerFactoryDebugExtension debugExtension) {
        return new BookStoreFeed(service, debugExtension);
    }

    @Produces
    public org.apache.cxf.feature.Feature validationFeature() {
        return new JAXRSBeanValidationFeature();
    }
}
