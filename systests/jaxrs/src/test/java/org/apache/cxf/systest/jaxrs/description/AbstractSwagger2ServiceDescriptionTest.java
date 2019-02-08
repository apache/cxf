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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.parse.SwaggerParseUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.hamcrest.CoreMatchers;
import org.yaml.snakeyaml.Yaml;

import org.junit.Ignore;
import org.junit.Test;


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSwagger2ServiceDescriptionTest extends AbstractBusClientServerTestBase {
    static final String SECURITY_DEFINITION_NAME = "basicAuth";

    protected enum XForwarded {
        NONE,
        ONE_HOST,
        MANY_HOSTS;

        boolean isSet() {
            return this != NONE;
        }
    }

    private static final String CONTACT = "cxf@apache.org";
    private static final String TITLE = "CXF unittest";
    private static final String DESCRIPTION = "API Description";
    private static final String LICENSE = "API License";
    private static final String LICENSE_URL = "API License URL";

    @Ignore
    public abstract static class Server extends AbstractBusTestServerBase {
        protected final String port;
        protected final boolean runAsFilter;

        Server(final String port, final boolean runAsFilter) {
            this.port = port;
            this.runAsFilter = runAsFilter;
        }

        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger2.class);
            sf.setResourceClasses(BookStoreStylesheetsSwagger2.class);
            sf.setResourceProvider(BookStoreSwagger2.class,
                new SingletonResourceProvider(new BookStoreSwagger2()));
            sf.setProvider(new JacksonJsonProvider());
            final Swagger2Feature feature = createSwagger2Feature();
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            sf.setExtensionMappings(
                 Collections.singletonMap("json", "application/json;charset=UTF-8"));
            sf.create();
        }

        protected Swagger2Feature createSwagger2Feature() {
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setRunAsFilter(runAsFilter);
            feature.setContact(CONTACT);
            feature.setTitle(TITLE);
            feature.setDescription(DESCRIPTION);
            feature.setLicense(LICENSE);
            feature.setLicenseUrl(LICENSE_URL);
            feature.setSecurityDefinitions(Collections.singletonMap(SECURITY_DEFINITION_NAME,
               new io.swagger.models.auth.BasicAuthDefinition()));
            return feature;
        }

        protected static void start(final Server s) {
            try {
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    protected static void startServers(final Class< ? extends Server> serverClass) throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(serverClass, false));
        createStaticBus();
    }

    protected abstract String getPort();

    protected abstract String getExpectedFileYaml();

    protected void doTestApiListingIsProperlyReturnedJSON() throws Exception {
        doTestApiListingIsProperlyReturnedJSON(XForwarded.NONE);
    }
    protected void doTestApiListingIsProperlyReturnedJSON(XForwarded useXForwarded) throws Exception {
        doTestApiListingIsProperlyReturnedJSON(createWebClient("/swagger.json"),
                                               useXForwarded);
        checkUiResource();
    }
    protected static void doTestApiListingIsProperlyReturnedJSON(final WebClient client,
                                                          XForwarded useXForwarded) throws Exception {
        if (useXForwarded == XForwarded.ONE_HOST) {
            client.header("USE_XFORWARDED", true);
        } else if (useXForwarded == XForwarded.MANY_HOSTS) {
            client.header("USE_XFORWARDED_MANY_HOSTS", true);
        }

        try {
            String swaggerJson = client.get(String.class);
            UserApplication ap = SwaggerParseUtils.getUserApplicationFromJson(swaggerJson);
            assertNotNull(ap);
            assertEquals(useXForwarded.isSet() ? "/reverse" : "/", ap.getBasePath());

            List<UserResource> urs = ap.getResources();
            assertNotNull(urs);
            assertEquals(1, urs.size());
            UserResource r = urs.get(0);
            String basePath = "";
            if (!"/".equals(r.getPath())) {
                basePath = r.getPath();
            }
            Map<String, UserOperation> map = r.getOperationsAsMap();
            assertEquals(3, map.size());
            UserOperation getBooksOp = map.get("getBooks");
            assertEquals(HttpMethod.GET, getBooksOp.getVerb());
            assertEquals("/bookstore", basePath + getBooksOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBooksOp.getProduces());
            List<Parameter> getBooksOpParams = getBooksOp.getParameters();
            assertEquals(1, getBooksOpParams.size());
            assertEquals(ParameterType.QUERY, getBooksOpParams.get(0).getType());
            UserOperation getBookOp = map.get("getBook");
            assertEquals(HttpMethod.GET, getBookOp.getVerb());
            assertEquals("/bookstore/{id}", basePath + getBookOp.getPath());
            assertEquals(MediaType.APPLICATION_JSON, getBookOp.getProduces());
            List<Parameter> getBookOpParams = getBookOp.getParameters();
            assertEquals(1, getBookOpParams.size());
            assertEquals(ParameterType.PATH, getBookOpParams.get(0).getType());
            UserOperation deleteOp = map.get("delete");
            assertEquals(HttpMethod.DELETE, deleteOp.getVerb());
            assertEquals("/bookstore/{id}", basePath + deleteOp.getPath());
            List<Parameter> delOpParams = deleteOp.getParameters();
            assertEquals(1, delOpParams.size());
            assertEquals(ParameterType.PATH, delOpParams.get(0).getType());

            assertThat(swaggerJson, CoreMatchers.containsString(CONTACT));
            assertThat(swaggerJson, CoreMatchers.containsString(TITLE));
            assertThat(swaggerJson, CoreMatchers.containsString(DESCRIPTION));
            assertThat(swaggerJson, CoreMatchers.containsString(LICENSE));
            assertThat(swaggerJson, CoreMatchers.containsString(LICENSE_URL));
            assertThat(swaggerJson, CoreMatchers.containsString(SECURITY_DEFINITION_NAME));
        } finally {
            client.close();
        }
    }

    @Test
    public void testNonUiResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones.
        WebClient uiClient = WebClient
            .create("http://localhost:" + getPort() + "/css/book.css")
            .accept("text/css");
        String css = uiClient.get(String.class);
        assertThat(css, equalTo("body { background-color: lightblue; }"));
    }

    @Test
    public void testUiResource() {
        // Test that Swagger UI resources do not interfere with
        // application-specific ones and are accessible.
        WebClient uiClient = WebClient
            .create("http://localhost:" + getPort() + "/swagger-ui.css")
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
            .create("http://localhost:" + getPort() + "/api-docs")
            .accept("*/*");

        try (Response response = uiClient.get()) {
            String html = response.readEntity(String.class);
            assertThat(html, containsString("<!-- HTML"));
            assertThat(response.getMediaType(), equalTo(MediaType.TEXT_HTML_TYPE));
        }
    }

    @Test
    @Ignore
    public void testApiListingIsProperlyReturnedYAML() throws Exception {
        final WebClient client = createWebClient("/swagger.yaml");

        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            //REVISIT find a better way of reliably comparing two yaml instances.
            // I noticed that yaml.load instantiates a Map and
            // for an integer valued key, an Integer or a String is arbitrarily instantiated,
            // which leads to the assertion error. So, we serilialize the yamls and compare the re-serialized texts.
            Yaml yaml = new Yaml();
            assertEquals(yaml.load(getExpectedValue(getExpectedFileYaml(), getPort())).toString(),
                         yaml.load(IOUtils.readStringFromStream((InputStream)r.getEntity())).toString());

        } finally {
            client.close();
        }
    }

    protected WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + getPort() + url,
                Arrays.< Object >asList(new JacksonJsonProvider()),
                Arrays.< Feature >asList(new LoggingFeature()),
                null)
            .accept(MediaType.APPLICATION_JSON).accept("application/yaml");
    }

    protected void checkUiResource() {
        WebClient uiClient = WebClient.create("http://localhost:" + getPort() + "/api-docs")
            .accept(MediaType.WILDCARD);
        String uiHtml = uiClient.get(String.class);
        assertTrue(uiHtml.contains("<title>Swagger UI</title>"));
    }

    private static String getExpectedValue(String name, Object... args) throws IOException {
        return String.format(IOUtils.readStringFromStream(
            AbstractSwagger2ServiceDescriptionTest.class.getResourceAsStream(name)), args);
    }
}
