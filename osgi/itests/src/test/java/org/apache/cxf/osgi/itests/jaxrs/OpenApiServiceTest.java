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
package org.apache.cxf.osgi.itests.jaxrs;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.osgi.itests.AbstractServerActivator;
import org.apache.cxf.osgi.itests.CXFOSGiTestSupport;
import org.osgi.framework.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OpenApiServiceTest extends CXFOSGiTestSupport {

    private static final String BASE_URL = "http://localhost:" + PORT + "/cxf/jaxrs/";
    private static final String SWAGGER_PATH = "api-docs";
    private static final String OPEN_API_FILE_PATH = "openapi.json";

    private final WebTarget wt = ClientBuilder.newClient().target(BASE_URL);

    @Test
    public void testGetSwaggerUi() {
        WebTarget swaggerWt = wt.path(SWAGGER_PATH).queryParam("url", "/cxf/jaxrs/openapi.json");
        
        // It may take some time for all bundles to start and get activated
        await(5, TimeUnit.SECONDS, () -> swaggerWt.request().head().getStatus() == 200);
        
        Response response = swaggerWt.request().get();
        String swaggerFileHtml = response.readEntity(String.class);

        assertStatus(Status.OK, response);
        assertTrue(swaggerFileHtml.contains("<html"));
        assertTrue(swaggerFileHtml.contains("<head>"));
        assertTrue(swaggerFileHtml.contains("<title>Swagger UI</title>"));
        assertTrue(swaggerFileHtml.contains("</html>"));
    }

    @Test
    public void testGetOpenApiJsonFile() {
        WebTarget openApiWt = wt.path(OPEN_API_FILE_PATH);
        
        // It may take some time for all bundles to start and get activated
        await(5, TimeUnit.SECONDS, () -> openApiWt.request().head().getStatus() == 200);

        Response response = openApiWt.request().get();
        String openApiJson = response.readEntity(String.class);

        assertStatus(Status.OK, response);
        try {
            new ObjectMapper().readValue(openApiJson, Object.class);
            assertTrue(openApiJson.contains("/bookstore/"));
        } catch (JsonProcessingException e) {
            fail();
        }
    }

    private static void assertStatus(Status expectedStatus, Response response) {
        assertEquals(expectedStatus.getStatusCode(), response.getStatus());
    }

    @Configuration
    public Option[] config() {
        return OptionUtils.combine(
            cxfBaseConfig(),
            features(cxfUrl, "cxf-core", "cxf-wsdl", "cxf-jaxrs", "cxf-bean-validation-core", "cxf-bean-validation",
                    "cxf-rs-description-openapi-v3"),
            mavenBundle("org.webjars", "swagger-ui").versionAsInProject(),
            logLevel(LogLevel.INFO),
            testUtils(),
            provision(serviceBundle())
        );
    }

    private static InputStream serviceBundle() {
        if (JavaUtils.isJava11Compatible()) {
            return TinyBundles.bundle()
                  .add(AbstractServerActivator.class)
                  .add(OpenApiTestActivator.class)
                  .add(Book.class)
                  .add(BookStore.class)
                  .add(OpenApiBookStore.class)
                  .set(Constants.BUNDLE_ACTIVATOR, OpenApiTestActivator.class.getName())
                  .set("Require-Capability", "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=11))\"")
                  .build(TinyBundles.withBnd());
        } else {
            return TinyBundles.bundle()
                .add(AbstractServerActivator.class)
                .add(OpenApiTestActivator.class)
                .add(Book.class)
                .add(BookStore.class)
                .add(OpenApiBookStore.class)
                .set(Constants.BUNDLE_ACTIVATOR, OpenApiTestActivator.class.getName())
                .build(TinyBundles.withBnd());
        }
    }
    
    private static void await(int timeout, TimeUnit unit, Supplier<Boolean> condition) {
        final int periods = 10;
        final long millis = Math.max(1, unit.toMillis(timeout) / periods);
        
        for (int i = 0; i < periods; ++i) {
            if (condition.get()) {
                return;
            }
            
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        fail("Waited at most " + unit.toMillis(timeout) + "ms, but condition was not met");
    }
}
