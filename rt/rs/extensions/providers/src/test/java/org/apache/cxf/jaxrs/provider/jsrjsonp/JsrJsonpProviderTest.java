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
package org.apache.cxf.jaxrs.provider.jsrjsonp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class JsrJsonpProviderTest {
    private JsrJsonpProvider provider;

    @Before
    public void setUp() {
        provider = new JsrJsonpProvider();
    }

    @Test(expected = IOException.class)
    public void testReadWithNullStream() throws Exception {
        provider.readFrom(JsonStructure.class, null, null, null, null, null);
    }

    @Test
    public void testGetSizeReturnsMinusOne() throws Exception {
        assertThat(provider.getSize(null, JsonStructure.class, null, null, null),
            equalTo(-1L));
    }

    @Test
    public void testReadableTypes() throws Exception {
        assertThat(provider.isReadable(JsonArray.class, null, null, null),
            equalTo(true));
        assertThat(provider.isReadable(JsonStructure.class, null, null, null),
            equalTo(true));
        assertThat(provider.isReadable(JsonObject.class, null, null, null),
            equalTo(true));
        assertThat(provider.isReadable(JsonValue.class, null, null, null),
            equalTo(false));
    }

    @Test
    public void testWritableTypes() throws Exception {
        assertThat(provider.isWriteable(JsonArray.class, null, null, null),
            equalTo(true));
        assertThat(provider.isWriteable(JsonStructure.class, null, null, null),
            equalTo(true));
        assertThat(provider.isWriteable(JsonObject.class, null, null, null),
            equalTo(true));
        assertThat(provider.isWriteable(JsonValue.class, null, null, null),
            equalTo(false));
    }

    @Test(expected = IOException.class)
    public void testWriteWithNullStream() throws Exception {
        final JsonObject obj = Json.createObjectBuilder()
            .add("firstName", "Tom")
            .add("lastName", "Tommyknocker")
            .build();

        provider.writeTo(obj, JsonObject.class, null, null, null, null, null);
    }

    @Test
    public void testReadMalformedJson() throws Exception {
        byte[] bytes = "junk".getBytes();

        try {
            provider.readFrom(JsonStructure.class, null, null, null, null,
                new ByteArrayInputStream(bytes));
            fail("400 BAD REQUEST is expected");
        } catch (WebApplicationException ex) {
            assertThat(ex.getResponse().getStatus(),
                equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testReadJsonObject() throws Exception {
        final StringWriter writer = new StringWriter();

        Json.createGenerator(writer)
            .writeStartObject()
            .write("firstName", "Tom")
            .write("lastName", "Tommyknocker")
            .writeEnd()
            .close();

        final String str = writer.toString();
        writer.close();

        final JsonStructure obj = provider.readFrom(JsonStructure.class, null, null, null, null,
            new ByteArrayInputStream(str.getBytes()));

        assertThat(obj, instanceOf(JsonObject.class));
        assertThat(((JsonObject)obj).getString("firstName"), equalTo("Tom"));
        assertThat(((JsonObject)obj).getString("lastName"), equalTo("Tommyknocker"));
    }

    @Test
    public void testReadJsonArray() throws Exception {
        final StringWriter writer = new StringWriter();

        Json.createGenerator(writer)
            .writeStartArray()
            .write("Tom")
            .write("Tommyknocker")
            .writeEnd()
            .close();

        final JsonStructure obj = provider.readFrom(JsonStructure.class, null, null, null, null,
            new ByteArrayInputStream(writer.toString().getBytes()));

        assertThat(obj, instanceOf(JsonArray.class));
        assertThat(((JsonArray)obj).getString(0), equalTo("Tom"));
        assertThat(((JsonArray)obj).getString(1), equalTo("Tommyknocker"));
        assertThat(((JsonArray)obj).size(), equalTo(2));
    }

    @Test
    public void testWriteJsonObject() throws Exception {
        final JsonObject obj = Json.createObjectBuilder()
            .add("firstName", "Tom")
            .add("lastName", "Tommyknocker")
            .build();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        provider.writeTo(obj, JsonStructure.class, null, null, null, null, out);
        out.close();

        assertThat(new String(out.toByteArray()),
            equalTo("{\"firstName\":\"Tom\",\"lastName\":\"Tommyknocker\"}"));
    }

    @Test
    public void testWriteJsonArray() throws Exception {
        final JsonArray obj = Json.createArrayBuilder()
            .add(
                Json.createObjectBuilder()
                    .add("firstName", "Tom")
                    .add("lastName", "Tommyknocker")
            )
            .add(
                Json.createObjectBuilder()
                    .add("firstName", "Bob")
                    .add("lastName", "Bobbyknocker")
            )
            .build();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        provider.writeTo(obj, JsonStructure.class, null, null, null, null, out);
        out.close();

        assertThat(new String(out.toByteArray()),
            equalTo("[{\"firstName\":\"Tom\",\"lastName\":\"Tommyknocker\"},"
                    + "{\"firstName\":\"Bob\",\"lastName\":\"Bobbyknocker\"}]"));
    }

}
