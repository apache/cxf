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
package org.apache.cxf.jaxrs.servlet;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.demo.resources.Book;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class AbstractSciTest extends AbstractBusClientServerTestBase {
    @Test
    public void testResponseHasBeenReceivedWhenQueringBook() {
        Response r = createWebClient("/bookstore/books").path("1").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        Book book = r.readEntity(Book.class);
        assertEquals("1", book.getId());
    }

    protected WebClient createWebClient(final String url) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());

        final WebClient wc = WebClient
            .create("http://localhost:" + getPort(), providers)
            .path(getContextPath())
            .path(url)
            .accept(MediaType.APPLICATION_JSON);

        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }

    protected abstract int getPort();
    protected abstract String getContextPath();
}
