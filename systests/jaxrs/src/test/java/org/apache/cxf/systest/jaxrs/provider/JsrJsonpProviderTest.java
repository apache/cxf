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

package org.apache.cxf.systest.jaxrs.provider;

import java.util.Arrays;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsrJsonpProviderTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(JsrJsonpProviderTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookJsonStore.class);
            sf.setResourceProvider(BookJsonStore.class,
                new SingletonResourceProvider(new BookJsonStore()));
            sf.setProvider(new JsrJsonpProvider());
            sf.setAddress("http://localhost:" + PORT + "/");
            org.apache.cxf.endpoint.Server s = sf.create();
            try {
                sf.create();
                fail();
            } catch (Exception ex) {
                assertTrue(ex.getCause() instanceof ServiceConstructionException);
                assertTrue(ex.getCause().getMessage().startsWith("There is an endpoint already running on"));
            }
            try {
                sf.create();
                fail();
            } catch (Exception ex) {
                assertTrue(ex.getCause() instanceof ServiceConstructionException);
                assertTrue(ex.getCause().getMessage().startsWith("There is an endpoint already running on"));
            }
            return s;
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void setUp() {
        final Response r = createWebClient("/bookstore/books").delete();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testNoResultsAreReturned() throws Exception {
        final Response r = createWebClient("/bookstore/books/155").get();
        assertEquals(Status.NO_CONTENT.getStatusCode(), r.getStatus());
    }

    @Test
    public void testPostSimpleJsonObject() {
        final Response r = createWebClient("/bookstore/books")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                Json
                    .createObjectBuilder()
                    .add("id", 1)
                    .add("name", "Book 1")
                    .build()
            );
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
    }

    @Test
    public void testPostComplexJsonObject() {
        final Response r = createWebClient("/bookstore/books")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post(
                Json
                    .createObjectBuilder()
                    .add("id", 1)
                    .add("name", "Book 1")
                    .add("chapters",
                        Json.createArrayBuilder()
                            .add(
                                Json.createObjectBuilder()
                                    .add("id", 1)
                                    .add("title", "Chapter 1")
                            )
                            .add(
                                Json.createObjectBuilder()
                                    .add("id", 2)
                                    .add("title", "Chapter 2")
                            )
                    )
                    .build()
            );
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
    }

    @Test
    public void testPostAndGetSimpleJsonObject() {
        testPostSimpleJsonObject();

        final Response r = createWebClient("/bookstore/books/1").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        JsonObject obj = r.readEntity(JsonObject.class);
        assertThat(obj.getInt("id"), equalTo(1));
        assertThat(obj.getString("name"), equalTo("Book 1"));
        assertThat(obj.get("chapters"), nullValue());
    }

    @Test
    public void testPostAndGetComplexJsonObject() {
        testPostComplexJsonObject();

        final Response r = createWebClient("/bookstore/books/1").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        JsonObject obj = r.readEntity(JsonObject.class);
        assertThat(obj.getInt("id"), equalTo(1));
        assertThat(obj.getString("name"), equalTo("Book 1"));
        assertThat(obj.get("chapters"), instanceOf(JsonArray.class));

        final JsonArray chapters = (JsonArray)obj.get("chapters");
        assertThat(chapters.size(), equalTo(2));
        assertThat(((JsonObject)chapters.get(0)).getInt("id"), equalTo(1));
        assertThat(((JsonObject)chapters.get(0)).getString("title"), equalTo("Chapter 1"));
        assertThat(((JsonObject)chapters.get(1)).getInt("id"), equalTo(2));
        assertThat(((JsonObject)chapters.get(1)).getString("title"), equalTo("Chapter 2"));
    }

    @Test
    public void testPostAndGetBooks() {
        testPostSimpleJsonObject();

        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        final JsonArray obj = r.readEntity(JsonArray.class);
        assertThat(obj.size(), equalTo(1));
        assertThat(obj.get(0), instanceOf(JsonObject.class));

        assertThat(((JsonObject)obj.get(0)).getInt("id"), equalTo(1));
        assertThat(((JsonObject)obj.get(0)).getString("name"), equalTo("Book 1"));
    }

    @Test
    public void testPostBadJsonObject() {
        final Response r = createWebClient("/bookstore/books")
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .post("blabla");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    private static WebClient createWebClient(final String url) {
        return WebClient
            .create("http://localhost:" + PORT + url,
                Arrays.< Object >asList(new JsrJsonpProvider()))
            .accept(MediaType.APPLICATION_JSON);
    }

}
