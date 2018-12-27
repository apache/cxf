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

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.openapi.parse.OpenApiParseUtils;
import org.apache.cxf.systest.jaxrs.description.group2.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OpenApiContextBasedConfigTest extends AbstractBusClientServerTestBase {
    private static final String PORT = allocatePort(OpenApiContextBasedConfigTest.class);

    public static class OpenApiContextBased extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            createServerFactory("/api", "This is first API (api)", BookStoreOpenApi.class);
            createServerFactory("/api2", "This is second API (api2)", BookStore.class);
        }

        protected void createServerFactory(final String context, final String description, final Class<?> resource) {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(resource);
            sf.setProvider(new JacksonJsonProvider());
            final OpenApiFeature feature = createOpenApiFeature(description, resource);
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + PORT + context);
            sf.create();
        }


        protected OpenApiFeature createOpenApiFeature(final String description, final Class<?> resource) {
            final OpenApiCustomizer customizer = new OpenApiCustomizer();
            customizer.setDynamicBasePath(true);

            final OpenApiFeature feature = new OpenApiFeature();
            feature.setDescription(description);
            feature.setCustomizer(customizer);
            feature.setScan(false);
            feature.setUseContextBasedConfig(true);
            feature.setResourceClasses(Collections.singleton(resource.getName()));
            return feature;
        }

        public static void main(String[] args) {
            try {
                new OpenApiContextBased().start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(OpenApiContextBased.class, false));
        createStaticBus();
    }

    @Test
    public void testFirstApi() {
        final String swaggerJson = createWebClient("http://localhost:" + PORT + "/api/openapi.json").get(String.class);
        assertThat(swaggerJson, containsString("This is first API (api)"));

        final UserApplication ap = OpenApiParseUtils.getUserApplicationFromJson(swaggerJson);
        assertNotNull(ap);
        assertThat(ap.getResourcesAsMap().size(), equalTo(1));
        assertThat(ap.getResourcesAsMap().get("").getOperations().size(), equalTo(3));
    }

    @Test
    public void testSecondApi() {
        final String swaggerJson = createWebClient("http://localhost:" + PORT + "/api2/openapi.json").get(String.class);
        assertThat(swaggerJson, containsString("This is second API (api2)"));

        final UserApplication ap = OpenApiParseUtils.getUserApplicationFromJson(swaggerJson);
        assertNotNull(ap);
        assertThat(ap.getResourcesAsMap().size(), equalTo(1));
        assertThat(ap.getResourcesAsMap().get("").getOperations().size(), equalTo(1));
    }

    protected WebClient createWebClient(final String url) {
        return WebClient
            .create(url,
                Arrays.< Object >asList(new JacksonJsonProvider()),
                Arrays.< Feature >asList(new LoggingFeature()),
                null)
            .accept(MediaType.APPLICATION_JSON).accept("application/yaml");
    }
}
