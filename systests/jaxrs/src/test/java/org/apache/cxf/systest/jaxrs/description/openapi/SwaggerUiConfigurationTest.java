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
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiOAuth2Config;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwaggerUiConfigurationTest extends AbstractClientServerTestBase {
    private static final String PORT = allocatePort(SwaggerUiConfigurationTest.class);
    private static final SwaggerUiOAuth2Config OAUTH2_CONFIG = new SwaggerUiOAuth2Config()
            .usePkceWithAuthorizationCodeGrant(true)
            .appName("CXF Test App")
            .additionalQueryStringParams(Map.of("key1", "value1", "key2", "value2"))
            .scopes(List.of("scope1", "scope2"));
    
    public static class Server extends AbstractServerTestServerBase {

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreOpenApi.class);
            sf.setResourceProvider(BookStoreOpenApi.class,
                new SingletonResourceProvider(new BookStoreOpenApi()));
            sf.setProvider(new JacksonJsonProvider());
            final OpenApiFeature feature = new OpenApiFeature();
            feature.setRunAsFilter(false);
            feature.setSwaggerUiConfig(new SwaggerUiConfig()
                    .url("/openapi.json")
                    .queryConfigEnabled(false)
                    .oAuth2Config(OAUTH2_CONFIG));
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + PORT + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, false));
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
            assertThat(response.getHeaderString("Location"), equalTo(url + "?url=/openapi.json"));
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
            assertThat(response.getMediaType(), equalTo(MediaType.TEXT_HTML_TYPE.withCharset("utf-8")));
        }
    }

    @Test
    public void testUiRootResourcePicksUrlFromConfigurationOnly() {
        // Test that Swagger UI URL is picked from configuration only and
        // never from the query string (when query config is disabled).
        WebClient uiClient = WebClient
            .create("http://localhost:" + getPort() + "/api-docs")
            .query("url", "http://malicious.site/swagger.json")
            .accept("*/*");

        try (Response response = uiClient.get()) {
            String html = response.readEntity(String.class);
            assertThat(html, containsString("<!-- HTML"));
            assertThat(response.getMediaType(), equalTo(MediaType.TEXT_HTML_TYPE.withCharset("utf-8")));
        }
    }

    @Test
    public void testUiRootResourceReplacesUrlAsConfigured() {
        // With query config disabled or unset, we replace the url value in the Swagger resource with the one
        // configured in SwaggerUiConfig, and ignore the one in query parameter.
        WebClient uiClient = WebClient
                .create("http://localhost:" + getPort() + "/api-docs")
                .path("/swagger-initializer.js")
                .query("url", "another-openapi.json")
                .accept("*/*");

        try (Response response = uiClient.get()) {
            String jsCode = response.readEntity(String.class);
            assertTrue(jsCode.contains("url: \"/openapi.json\""));
            assertFalse(jsCode.contains("another-openapi.json"));
        }
    }
    
    @Test
    public void testUiRootResourceAddsOAuth2ConfigAsConfigured() throws Exception {
        // With query config disabled or unset, we replace the url value in the Swagger resource with the one
        // configured in SwaggerUiConfig, and ignore the one in query parameter.
        WebClient uiClient = WebClient
                .create("http://localhost:" + getPort() + "/api-docs")
                .path("/swagger-initializer.js")
                .accept("*/*");
        
        try (Response response = uiClient.get()) {
            String jsCode = response.readEntity(String.class);
            assertThat(jsCode, containsString("ui.initOAuth(" + OAUTH2_CONFIG.toJsonString() + ")"));
        }
    }
    
    public static String getPort() {
        return PORT;
    }
}
