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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.openapi.parse.OpenApiParseUtils;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.hamcrest.CoreMatchers;

import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractOpenApiServiceDescriptionTest extends AbstractClientServerTestBase {
    static final String SECURITY_DEFINITION_NAME = "basicAuth";

    private static final String CONTACT = "cxf@apache.org";
    private static final String TITLE = "CXF unittest";
    private static final String DESCRIPTION = "API Description";
    private static final String LICENSE = "API License";
    private static final String LICENSE_URL = "API License URL";

    public abstract static class Server extends AbstractServerTestServerBase {
        protected final String port;
        protected final boolean runAsFilter;

        Server(final String port, final boolean runAsFilter) {
            this.port = port;
            this.runAsFilter = runAsFilter;
        }

        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            // Make sure default JSON-P/JSON-B providers are not loaded
            bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreOpenApi.class);
            sf.setResourceClasses(BookStoreStylesheetsOpenApi.class);
            sf.setResourceProvider(BookStoreOpenApi.class,
                new SingletonResourceProvider(new BookStoreOpenApi()));
            sf.setProvider(new JacksonJsonProvider());
            final OpenApiFeature feature = createOpenApiFeature();
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            sf.setBus(bus);
            return sf.create();
        }

        protected OpenApiFeature createOpenApiFeature() {
            final OpenApiFeature feature = new OpenApiFeature();
            feature.setRunAsFilter(runAsFilter);
            feature.setContactName(CONTACT);
            feature.setTitle(TITLE);
            feature.setDescription(DESCRIPTION);
            feature.setLicense(LICENSE);
            feature.setLicenseUrl(LICENSE_URL);

            feature.setSecurityDefinitions(Collections.singletonMap(SECURITY_DEFINITION_NAME,
                new SecurityScheme().type(Type.HTTP)));

            return feature;
        }
    }

    protected static void startServers(final Class< ? extends Server> serverClass) throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(serverClass, false));

        final Bus bus = BusFactory.getThreadDefaultBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    protected abstract String getPort();

    protected void doTestApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(false);
    }
    protected void doTestApiListingIsProperlyReturnedJSON(boolean useXForwarded) throws Exception {
        doTestApiListingIsProperlyReturnedJSON(useXForwarded, null);
    }
    protected void doTestApiListingIsProperlyReturnedJSON(boolean useXForwarded, String basePath) throws Exception {
        doTestApiListingIsProperlyReturnedJSON(createWebClient("/openapi.json"), useXForwarded, basePath);
        checkUiResource();
    }
    protected void doTestApiListingIsProperlyReturnedJSON(final WebClient client,
            boolean useXForwarded, String basePath) throws Exception {
        if (useXForwarded) {
            client.header("USE_XFORWARDED", true);
        }
        try {
            String swaggerJson = client.get(String.class);
            UserApplication ap = OpenApiParseUtils.getUserApplicationFromJson(swaggerJson);
            assertNotNull(ap);

            if (basePath == null) {
                assertEquals(useXForwarded ? "/reverse" : "/", ap.getBasePath());
            } else {
                assertEquals(basePath, ap.getBasePath());
            }

            List<UserResource> urs = ap.getResources();
            assertNotNull(urs);
            assertEquals(1, urs.size());
            UserResource r = urs.get(0);

            Map<String, UserOperation> map = r.getOperationsAsMap();
            assertEquals(3, map.size());
            UserOperation getBooksOp = map.get("getBooks");
            assertEquals(HttpMethod.GET, getBooksOp.getVerb());
            assertEquals(getApplicationPath() + "/bookstore", getBooksOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBooksOp.getProduces());
            List<Parameter> getBooksOpParams = getBooksOp.getParameters();
            assertEquals(1, getBooksOpParams.size());
            assertEquals(ParameterType.QUERY, getBooksOpParams.get(0).getType());
            UserOperation getBookOp = map.get("getBook");
            assertEquals(HttpMethod.GET, getBookOp.getVerb());
            assertEquals(getApplicationPath() + "/bookstore/{id}", getBookOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBookOp.getProduces());
            List<Parameter> getBookOpParams = getBookOp.getParameters();
            assertEquals(1, getBookOpParams.size());
            assertEquals(ParameterType.PATH, getBookOpParams.get(0).getType());
            UserOperation deleteOp = map.get("delete");
            assertEquals(HttpMethod.DELETE, deleteOp.getVerb());
            assertEquals(getApplicationPath() + "/bookstore/{id}", deleteOp.getPath());
            List<Parameter> delOpParams = deleteOp.getParameters();
            assertEquals(1, delOpParams.size());
            assertEquals(ParameterType.PATH, delOpParams.get(0).getType());

            assertThat(swaggerJson, CoreMatchers.containsString(getContract()));
            assertThat(swaggerJson, CoreMatchers.containsString(getTitle()));
            assertThat(swaggerJson, CoreMatchers.containsString(getDescription()));
            assertThat(swaggerJson, CoreMatchers.containsString(getLicense()));
            assertThat(swaggerJson, CoreMatchers.containsString(getLicenseUrl()));
            assertThat(swaggerJson, CoreMatchers.containsString(getSecurityDefinitionName()));
            assertThat(swaggerJson, CoreMatchers.containsString(getTags()));
        } finally {
            client.close();
        }
    }

    protected String getTags() {
        return "";
    }

    protected String getSecurityDefinitionName() {
        return SECURITY_DEFINITION_NAME;
    }

    protected String getLicenseUrl() {
        return LICENSE_URL;
    }

    protected String getLicense() {
        return LICENSE;
    }

    protected String getDescription() {
        return DESCRIPTION;
    }

    protected String getTitle() {
        return TITLE;
    }

    protected String getContract() {
        return CONTACT;
    }

    @Test
    public void testNonUiResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones.
        WebClient uiClient = WebClient
            .create(getBaseUrl() + "/css/book.css")
            .accept("text/css");
        String css = uiClient.get(String.class);
        assertThat(css, equalTo("body { background-color: lightblue; }"));
    }

    @Test
    public void testUiResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones and are accessible.
        WebClient uiClient = WebClient
            .create(getBaseUrl() + "/swagger-ui.css")
            .accept("text/css");

        try (Response response = uiClient.get()) {
            String css = response.readEntity(String.class);
            assertThat(css, containsString(".swagger-ui{"));
            assertThat(response.getMediaType(), equalTo(MediaType.valueOf("text/css")));
        }
    }

    @Test
    public void testUiRootResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones and are accessible.
        WebClient uiClient = WebClient
            .create(getBaseUrl() + "/api-docs")
            .accept("*/*");

        try (Response response = uiClient.get()) {
            String html = response.readEntity(String.class);
            assertThat(html, containsString("<!-- HTML"));
            assertThat(response.getMediaType(), equalTo(MediaType.TEXT_HTML_TYPE));
        }
    }

    protected String getApplicationPath() {
        return "";
    }

    protected String getBaseUrl() {
        return "http://localhost:" + getPort();
    }

    protected WebClient createWebClient(final String url) {
        return WebClient
            .create(getBaseUrl() + url,
                Arrays.< Object >asList(new JacksonJsonProvider()),
                Arrays.< Feature >asList(new LoggingFeature()),
                null)
            .accept(MediaType.APPLICATION_JSON).accept("application/yaml");
    }

    protected void checkUiResource() {
        WebClient uiClient = WebClient.create(getBaseUrl() + "/api-docs")
            .accept(MediaType.WILDCARD);
        String uiHtml = uiClient.get(String.class);
        assertTrue(uiHtml.contains("<title>Swagger UI</title>"));
    }
}
