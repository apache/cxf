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
package org.apache.cxf.systests.cdi.base;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public abstract class AbstractCdiMultiAppTest extends AbstractCdiSingleAppTest {
    @Test
    public void testAddOneBookWithValidation() {
        final String id = UUID.randomUUID().toString();

        Response r = createWebClient("/rest/v2/bookstore/books").post(
                new Form()
                        .param("id", id));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        assertThat(r.getHeaderString("X-Logged"), nullValue());
    }

    @Test
    public void testGetBookStoreVersion() {
        Response r1 = createWebClient("/rest/v3/bookstore/versioned/version", MediaType.TEXT_PLAIN).get();
        r1.bufferEntity();
        assertEquals(Response.Status.OK.getStatusCode(), r1.getStatus());
        assertThat(r1.readEntity(String.class), startsWith("1.0."));

        Response r2 = createWebClient("/rest/v3/bookstore/versioned/version", MediaType.TEXT_PLAIN).get();
        r2.bufferEntity();
        assertEquals(Response.Status.OK.getStatusCode(), r2.getStatus());
        assertThat(r2.readEntity(String.class), startsWith("1.0."));

        assertThat(r2.readEntity(String.class), not(equalTo(r1.readEntity(String.class))));
    }

    @Test
    public void testResponseHasBeenReceivedWhenQueringRequestScopedBookstore() {
        Response r = createWebClient("/rest/v2/bookstore/request/books").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertThat(r.getHeaderString("X-Logged"), equalTo("true"));
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenQueringCustomScopedBookstore() {
        Response r = createWebClient("/rest/v2/bookstore/custom/books").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertThat(r.getHeaderString("X-Logged"), equalTo("true"));
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenQueringContractBookstore() {
        Response r = createWebClient("/rest/v2/bookstore/contract/books").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertThat(r.getHeaderString("X-Logged"), equalTo("true"));
    }
    
    @Test
    public void testResponseHasBeenReceivedWhenQueringMethodWithNameBinding() {
        Response r = createWebClient("/rest/v2/bookstore/books").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertThat(r.getHeaderString("X-Logged"), equalTo("true"));
    }
    
    protected WebClient createWebClient(final String url) {
        return createWebClient(url, MediaType.APPLICATION_JSON);
    }

    protected WebClient createWebClient(final String url, final String mediaType) {
        final List< ? > providers = Arrays.asList(new JacksonJsonProvider());

        final WebClient wc = WebClient
                .create("http://localhost:" + getPort() + url, providers)
                .accept(mediaType);

        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }

    protected String getBasePath() {
        return "/rest/v1/bookstore";
    }

    protected abstract int getPort();
}
