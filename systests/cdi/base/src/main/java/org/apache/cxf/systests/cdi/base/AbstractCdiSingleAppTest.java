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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractCdiSingleAppTest extends AbstractBusClientServerTestBase {
    @Test
    public void testAvailableInjections() {
        assertEquals("configuration=Configuration/"
            + "contextResolver=ContextResolver/"
            + "cxfApplication=Application/"
            + "cxfConfiguration=Configuration/"
            + "cxfContextResolver=ContextResolver/"
            + "cxfHttpHeaders=HttpHeaders/"
            + "cxfHttpServletRequest=HttpServletRequest/"
            + "cxfProviders=Providers/"
            + "cxfRequest=Request/"
            + "cxfResourceContext=ResourceContext/"
            + "cxfResourceInfo=ResourceInfo/"
            + "cxfSecurityContext=SecurityContext/"
            + "cxfServletContext=ServletContext/"
            + "cxfUriInfo=UriInfo/"
            + "cxfhttpServletResponse=HttpServletRequest/"
            + "httpHeaders=HttpHeaders/"
            + "httpServletRequest=HttpServletRequest/"
            + "httpServletResponse=HttpServletResponse/"
            + "providers=Providers/request=Request/"
            + "resourceContext=ResourceContext/"
            + "resourceInfo=ResourceInfo/"
            + "securityContext=SecurityContext/"
            + "servletContext=ServletContext/"
            + "uriInfo=UriInfo",
            createWebClient(getBasePath() + "/injections", MediaType.TEXT_PLAIN).get(String.class).trim());
    }

    @Test
    public void testInjectedVersionIsProperlyReturned() {
        Response r = createWebClient(getBasePath() + "/version", MediaType.TEXT_PLAIN).get();
        String pathInfo = r.getHeaderString(Message.PATH_INFO);
        String httpMethod = r.getHeaderString(Message.HTTP_REQUEST_METHOD);
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        assertEquals("1.0", r.readEntity(String.class));
        assertTrue(pathInfo.endsWith("/bookstore/version"));
        assertEquals("GET", httpMethod);
    }

    @Test
    public void testResponseHasBeenReceivedWhenAddingNewBook() {
        Response r = createWebClient(getBasePath() + "/books").post(
                new Form()
                        .param("id", "1234")
                        .param("name", "Book 1234"));
        assertEquals(Response.Status.CREATED.getStatusCode(), r.getStatus());
    }

    @Test
    public void testResponseHasBeenReceivedWhenQueringAllBooks() {
        Response r = createWebClient(getBasePath() + "/books").get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testResponseHasBeenReceivedWhenQueringBooksById() {
        final String id = UUID.randomUUID().toString();

        Response r = createWebClient(getBasePath() + "/books").post(
                new Form()
                        .param("id", id)
                        .param("name", "Book " + id));
        r.close();
        r = createWebClient(getBasePath() + "/byIds")
                .query("ids", "1234")
                .query("ids", UUID.randomUUID().toString())
                .query("ids", id)
                .get();
        r.close();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testAddAndQueryOneBook() {
        final String id = UUID.randomUUID().toString();

        Response r = createWebClient(getBasePath() + "/books").post(
                new Form()
                        .param("id", id)
                        .param("name", "Book 1234"));
        assertEquals(Response.Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient(getBasePath() + "/books").path(id).get();
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

        Book book = r.readEntity(Book.class);
        assertEquals(id, book.getId());
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
        return "/rest/bookstore";
    }

    protected abstract int getPort();
}
