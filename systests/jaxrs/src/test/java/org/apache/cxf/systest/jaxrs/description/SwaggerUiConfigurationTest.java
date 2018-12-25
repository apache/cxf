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
package org.apache.cxf.systest.jaxrs.description;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class SwaggerUiConfigurationTest extends AbstractBusClientServerTestBase {
    private static final String PORT = allocatePort(SwaggerUiConfigurationTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger2.class);
            sf.setResourceProvider(BookStoreSwagger2.class,
                new SingletonResourceProvider(new BookStoreSwagger2()));
            sf.setProvider(new JacksonJsonProvider());
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setRunAsFilter(false);
            feature.setSwaggerUiConfig(new SwaggerUiConfig().url("/swagger.json"));
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
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
        assertTrue("server did not launch correctly", launchServer(Server.class, false));
        createStaticBus();
    }

    @Test
    public void testUiRootResourceRedirect() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones and are accessible.
        final String url = "http://localhost:" + getPort() + "/api-docs";

        WebClient uiClient = WebClient
            .create(url)
            .accept("*/*");

        try (Response response = uiClient.get()) {
            assertThat(response.getStatus(), equalTo(Response.Status.TEMPORARY_REDIRECT.getStatusCode()));
            assertThat(response.getHeaderString("Location"), equalTo(url + "?url=/swagger.json"));
        }
    }

    @Test
    public void testUiRootResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones and are accessible.
        WebClient uiClient = WebClient
            .create("http://localhost:" + getPort() + "/api-docs")
            .query("url", "/swagger.json")
            .accept("*/*");

        try (Response response = uiClient.get()) {
            String html = response.readEntity(String.class);
            assertThat(html, containsString("<!-- HTML"));
            assertThat(response.getMediaType(), equalTo(MediaType.TEXT_HTML_TYPE));
        }
    }

    public static String getPort() {
        return PORT;
    }
}
