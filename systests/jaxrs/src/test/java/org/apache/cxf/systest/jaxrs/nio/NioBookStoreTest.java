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

package org.apache.cxf.systest.jaxrs.nio;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NioBookStoreTest extends AbstractBusClientServerTestBase {
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(NioBookStoreServer.class, true));
        createStaticBus();
    }

    @Test
    public void testGetAllBooks() throws Exception {
        try (Response response = createWebClient("/bookstore", MediaType.TEXT_PLAIN).get()) {
            assertEquals(200, response.getStatus());

            assertThat(response.readEntity(String.class), equalTo(IOUtils.readStringFromStream(
                getClass().getResourceAsStream("/files/books.txt"))));
        }
    }

    @Test
    public void testGetAllBooksIs() throws Exception {
        try (Response response = createWebClient("/bookstore/is", MediaType.TEXT_PLAIN).get()) {
            assertEquals(200, response.getStatus());

            assertThat(response.readEntity(String.class), equalTo(IOUtils.readStringFromStream(
                getClass().getResourceAsStream("/files/books.txt"))));
        }
    }

    @Test
    public void testPostBookStore() throws IOException {
        byte[] bytes = IOUtils.readBytesFromStream(getClass().getResourceAsStream("/files/books.txt"));
        try (Response response = createWebClient("/bookstore", MediaType.TEXT_PLAIN)
            .type(MediaType.TEXT_PLAIN)
            .post(bytes)) {
            assertEquals(200, response.getStatus());
            assertThat(response.readEntity(String.class), equalTo("Book Store uploaded: " + bytes.length + " bytes"));
        }
    }

    protected WebClient createWebClient(final String url, final String mediaType) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());

        final WebClient wc = WebClient
            .create("http://localhost:" + NioBookStoreServer.PORT + url, providers)
            .accept(mediaType);

        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }
}

