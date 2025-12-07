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
package org.apache.cxf.systest.jaxrs.description.openapi;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.systest.jaxrs.description.group1.BookStore;
import org.apache.cxf.systest.jaxrs.description.group1.BookStoreStylesheetsOpenApi;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.BeforeClass;
import org.junit.Test;

public class OpenApiNonAnnotatedServiceDescriptionTest extends AbstractOpenApiServiceDescriptionTest {
    private static final String PORT = allocatePort(OpenApiNonAnnotatedServiceDescriptionTest.class);

    public static class OpenApiRegularNonAnnotated extends Server {
        public OpenApiRegularNonAnnotated() {
            super(PORT, false);
        }

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceClasses(BookStoreStylesheetsOpenApi.class);
            sf.setResourceProvider(BookStore.class,
                new SingletonResourceProvider(new BookStore()));
            sf.setProvider(new JacksonJsonProvider());
            final OpenApiFeature feature = createOpenApiFeature();
            feature.setResourcePackages(Collections.singleton("org.apache.cxf.systest.jaxrs.description.group1"));
            feature.setReadAllResources(true);
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new OpenApiRegularNonAnnotated().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startServers(OpenApiRegularNonAnnotated.class);
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON();
    }
}
