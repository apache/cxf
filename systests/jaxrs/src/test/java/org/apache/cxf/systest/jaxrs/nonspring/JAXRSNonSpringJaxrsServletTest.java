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

package org.apache.cxf.systest.jaxrs.nonspring;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import io.swagger.v3.core.util.Json;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * A test for launching the JAX-RS service using CXFNonSpringJaxrsServlet
 */
public class JAXRSNonSpringJaxrsServletTest extends AbstractBusClientServerTestBase {
    public static final String PORT = NonSpringJaxrsServletBookServer.PORT;
    public static final String PORT2 = NonSpringJaxrsServletBookServer2.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(NonSpringJaxrsServletBookServer.class, true));
        assertTrue("server did not launch correctly",
                   launchServer(NonSpringJaxrsServletBookServer2.class, true));
        createStaticBus();
    }

    @Test
    public void testFeatureOnResourceClass() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/;JSESSIONID=xxx";
        WebClient wc = WebClient.create(address);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("root", book.getName());

        // Check OpenAPI feature is working correctly
        wc = WebClient.create("http://localhost:" + PORT + "/openapi.json");
        Response openAPIResponse = wc.get();
        assertEquals(200, openAPIResponse.getStatus());
    }

    @Test
    public void testFeatureOnResourceClassUsingApplication() throws Exception {
        final JsonSerializer<Object> defaultNullKeySerializer = Json
            .mapper()
            .getSerializerProvider()
            .getDefaultNullKeySerializer();

        try {
            // Swagger Core v3 does not interpret FormParam("") properly, sets property key as 'null' and fails the
            // serialization with "Null key for a Map not allowed in JSON (use a converting NullKeySerializer?)"
            Json.mapper().getSerializerProvider().setNullKeySerializer(new JsonSerializer<Object>() {
                @Override
                public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeFieldName("");
                }
            });

            String address = "http://localhost:" + PORT2 + "/bookstore/;JSESSIONID=xxx";
            WebClient wc = WebClient.create(address);
            Book book = wc.get(Book.class);
            assertEquals(124L, book.getId());
            assertEquals("root", book.getName());
    
            // Check OpenAPI feature is working correctly
            wc = WebClient.create("http://localhost:" + PORT2 + "/openapi.json");
            Response openAPIResponse = wc.get();
            assertEquals(200, openAPIResponse.getStatus());
        } finally {
            if (defaultNullKeySerializer != null) {
                Json.mapper().getSerializerProvider().setNullKeySerializer(defaultNullKeySerializer);
            }
        }
    }

}