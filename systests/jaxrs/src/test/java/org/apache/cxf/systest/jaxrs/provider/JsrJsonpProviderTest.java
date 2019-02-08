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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsrJsonpProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JsrJsonpProviderTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookJsonStore.class);
            sf.setResourceProvider(BookJsonStore.class,
                new SingletonResourceProvider(new BookJsonStore()));
            sf.setProvider(new JsrJsonpProvider());
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
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
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
