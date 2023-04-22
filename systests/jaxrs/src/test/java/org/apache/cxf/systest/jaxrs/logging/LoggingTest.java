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

package org.apache.cxf.systest.jaxrs.logging;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class LoggingTest extends AbstractBusClientServerTestBase {
    @BeforeClass
    public static void startServers() {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(LoggingServer.class, true));
    }

    @Test
    public void testEchoBookElement() {
        final Response response = createWebClient("/bookstore/books/element/echo", MediaType.APPLICATION_XML)
                .post(new Book("CXF", 123L));
        assertEquals(200, response.getStatus());
        assertEquals(96, response.getLength());

        final Book book = response.readEntity(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF", book.getName());
    }

    protected WebClient createWebClient(final String url, final String mediaType) {
        final List<?> providers = Collections.singletonList(new JacksonJsonProvider());

        return WebClient
                .create("http://localhost:" + LoggingServer.PORT + url, providers)
                .accept(mediaType);
    }
}
