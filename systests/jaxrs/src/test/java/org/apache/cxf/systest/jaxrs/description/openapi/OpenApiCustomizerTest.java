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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;

import org.junit.BeforeClass;
import org.junit.Test;


public class OpenApiCustomizerTest extends AbstractOpenApiServiceDescriptionTest {
    private static final String PORT = allocatePort(OpenApiCustomizerTest.class);

    public static class OpenApiRegular extends Server {
        public OpenApiRegular() {
            super(PORT, false);
        }

        public static void main(String[] args) throws Exception {
            new OpenApiRegular().start();
        }

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = RuntimeDelegate
                .getInstance()
                .createEndpoint(new BookStoreApplication(), JAXRSServerFactoryBean.class);
            sf.setResourceClasses(BookStoreOpenApi.class);
            sf.setResourceClasses(BookStoreStylesheetsOpenApi.class);
            sf.setResourceProvider(BookStoreOpenApi.class,
                new SingletonResourceProvider(new BookStoreOpenApi()));
            sf.setProvider(new JacksonJsonProvider());
            final OpenApiFeature feature = createOpenApiFeature();
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/api");
            return sf.create();
        }
        @Override
        protected OpenApiFeature createOpenApiFeature() {
            final OpenApiCustomizer customizer = new OpenApiCustomizer();
            customizer.setDynamicBasePath(true);
            
            final OpenApiFeature feature = super.createOpenApiFeature();
            feature.setCustomizer(customizer);
            feature.setScan(false);
            feature.setResourcePackages(Collections.singleton(getClass().getPackage().getName()));

            return feature;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startServers(OpenApiRegular.class);
    }

    @Override
    protected String getPort() {
        return PORT;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + getPort() + getApplicationPath();
    }

    protected String getApplicationPath() {
        return "/api";
    }

    @Test
    public void testApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(false, "http://localhost:" + getPort());
    }
}
