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

import java.io.InputStream;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.swagger.SwaggerFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public abstract class AbstractSwaggerServiceDescriptionTest extends AbstractBusClientServerTestBase {
    private static final JsonObject DELETE_METHOD_SPEC = Json.createObjectBuilder()
        .add("method", "DELETE")
        .add("summary", "Delete book")
        .add("notes", "Delete book")
        .add("type", "void")
        .add("nickname", "delete")
        .add("parameters", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("name", "id")
                .add("description", "id")
                .add("required", true)
                .add("type", "string")
                .add("paramType", "path")
                .add("allowMultiple", false)
            )
        ).build();
    
    private static final JsonObject GET_BY_ID_METHOD_SPEC = Json.createObjectBuilder()
        .add("method", "GET")
        .add("summary", "Get book by Id")
        .add("notes", "Get book by Id")
        .add("type", "Book")
        .add("nickname", "getBook")
        .add("produces", Json.createArrayBuilder().add("application/json"))
        .add("parameters", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("name", "id")
                .add("description", "id")
                .add("required", true)
                .add("type", "integer")
                .add("format", "int64")
                .add("paramType", "path")
                .add("allowMultiple", false)
            )
        ).build();
    
    private static final JsonObject GET_METHOD_SPEC = Json.createObjectBuilder()
        .add("method", "GET")
        .add("summary", "Get books")
        .add("notes", "Get books")
        .add("type", "array")
        .add("items", Json.createObjectBuilder().add("$ref", "Book"))
        .add("nickname", "getBooks")
        .add("produces", Json.createArrayBuilder().add("application/json"))
        .add("parameters", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add("name", "page")
                .add("description", "Page to fetch")
                .add("defaultValue", "1")
                .add("required", true)
                .add("type", "integer")
                .add("format", "int32")
                .add("paramType", "query")
                .add("allowMultiple", false)
            )
        ).build();
    
    private static final JsonObject BOOK_MODEL_SPEC = Json.createObjectBuilder()
        .add("Book", Json.createObjectBuilder()
            .add("id", "Book")
            .add("discriminator", "class")
            .add("properties", Json.createObjectBuilder()
                .add("name", Json.createObjectBuilder().add("type", "string"))
                .add("id", Json.createObjectBuilder()
                    .add("type", "integer")
                    .add("format", "int64")
                )
            )
        ).build();
    
    @Ignore
    public abstract static class Server extends AbstractBusTestServerBase {
        private final String port;
        private final boolean runAsFilter;
        
        Server(final String port, final boolean runAsFilter) {
            this.port = port;
            this.runAsFilter = runAsFilter;
        }
        
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger.class);
            sf.setResourceProvider(BookStoreSwagger.class, 
                new SingletonResourceProvider(new BookStoreSwagger()));
            sf.setProvider(new JacksonJsonProvider());
            final SwaggerFeature feature = new SwaggerFeature();
            feature.setRunAsFilter(runAsFilter);
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + port + "/");
            sf.create();
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
    
    @Test
    public void testApiListingIsProperlyReturned() throws Exception {
        final WebClient client = createWebClient("/api-docs");
        
        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
            
            assertThat(IOUtils.readStringFromStream((InputStream)r.getEntity()), 
                equalTo(Json.createObjectBuilder()
                    .add("apiVersion", "1.0.0")
                    .add("swaggerVersion", "1.2")
                    .add("apis", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("path", "/bookstore")
                            .add("description", "Sample JAX-RS service with Swagger documentation")
                        )
                    )
                    .add("info", Json.createObjectBuilder()
                        .add("title", "Sample REST Application")
                        .add("description", "The Application")
                        .add("contact", "committer@apache.org")
                        .add("license", "Apache 2.0 License")
                        .add("licenseUrl", "http://www.apache.org/licenses/LICENSE-2.0.html")
                    ).build().toString()));
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testApiResourcesAreProperlyReturned() throws Exception {
        final WebClient client = createWebClient("/api-docs/bookstore");
        
        try {
            final Response r = client.get();
            assertEquals(Status.OK.getStatusCode(), r.getStatus());
                    
            assertThat(IOUtils.readStringFromStream((InputStream)r.getEntity()), 
                equalTo(Json.createObjectBuilder()
                    .add("apiVersion", "1.0.0")
                    .add("swaggerVersion", "1.2")
                    .add("basePath", "http://localhost:" + getPort() + "/")
                    .add("resourcePath", "/bookstore")
                    .add("apis", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("path", "/bookstore/{id}")
                            .add("operations", Json.createArrayBuilder()
                                .add(DELETE_METHOD_SPEC)
                                .add(GET_BY_ID_METHOD_SPEC)))
                        .add(Json.createObjectBuilder()
                            .add("path", "/bookstore")
                            .add("operations", Json.createArrayBuilder().add(GET_METHOD_SPEC))))
                    .add("models", BOOK_MODEL_SPEC).build().toString()));
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testNonRegisteredApiResourcesAreNotReturned() throws Exception {
        final Response r = createWebClient("/api-docs/books").get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }
    
    private WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + getPort() + url, 
                Arrays.< Object >asList(new JacksonJsonProvider()))
            .accept(MediaType.APPLICATION_JSON);
    }
}
