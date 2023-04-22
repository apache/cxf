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

package org.apache.cxf.systest.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class JAXRSClientServerQueryParamCollectionBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;
    private final Boolean threadSafe;
    
    public JAXRSClientServerQueryParamCollectionBookTest(Boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    @Parameters(name = "Client is thread safe = {0}")
    public static Collection<Boolean> data() {
        return Arrays.asList(new Boolean[] {null, true, false});
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", 
            launchServer(new BookServer(Collections.singletonMap("parse.query.value.as.collection", "true"))));
        createStaticBus();
    }

    @Test
    public void testListOfLongAndDoubleQuery() throws Exception {
        BookStore client = createClient();
        Book book = client.getBookFromListOfLongAndDouble(Arrays.asList(1L, 2L, 3L), Arrays.asList());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testListOfLongAndDoubleQueryEmpty() throws Exception {
        BookStore client = createClient();
        Book book = client.getBookFromListOfLongAndDouble(Arrays.asList(), Arrays.asList());
        assertEquals(0L, book.getId());
    }

    @Test
    public void testListOfLongAndDoubleQueryWebClient() throws Exception {
        WebClient wc = createWebClient();
        
        Response r = wc
            .path("/bookstore/listoflonganddouble")
            .query("value", Arrays.asList(1L, 2L, 3L))
            .accept("text/xml")
            .get();

        assertThat(wc.getCurrentURI().toString(), endsWith("value=1,2,3"));
        try (InputStream is = (InputStream)r.getEntity()) {
            XMLSource source = new XMLSource(is);
            assertEquals(123L, Long.parseLong(source.getValue("Book/id")));
        }
    }

    @Test
    public void testListOfLongAndDoubleQueryAsManyWebClient() throws Exception {
        WebClient wc = createWebClient();
        
        Response r = wc
                .path("/bookstore/listoflonganddouble")
                .query("value", "1")
                .query("value", "2")
                .query("value", "3")
                .accept("text/xml")
                .get();

        assertThat(wc.getCurrentURI().toString(), endsWith("value=1,2,3"));
        try (InputStream is = (InputStream)r.getEntity()) {
            XMLSource source = new XMLSource(is);
            assertEquals(123L, Long.parseLong(source.getValue("Book/id")));
        }
    }
    
    @Test
    public void testListOfStringsWebClient() throws Exception {
        WebClient wc = createWebClient();
        
        Response r = wc
                .path("/bookstore/querysub/listofstrings")
                .query("value", "this is")
                .query("value", "the book")
                .query("value", "title")
                .accept("text/xml")
                .get();

        assertThat(wc.getCurrentURI().toString(), endsWith("value=this+is,the+book,title"));
        try (InputStream is = (InputStream)r.getEntity()) {
            XMLSource source = new XMLSource(is);
            assertEquals("this is the book title", source.getValue("Book/name"));
        }
    }

    @Test
    public void testListOfStringsJaxrsClient() throws Exception {
        WebTarget client = createJaxrsClient();
        
        Response r = client
                .path("/bookstore/querysub/listofstrings")
                .queryParam("value", "this is")
                .queryParam("value", "the book")
                .queryParam("value", "title")
                .request()
                .accept("text/xml")
                .get();

        try (InputStream is = (InputStream)r.getEntity()) {
            XMLSource source = new XMLSource(is);
            assertEquals("this is the book title", source.getValue("Book/name"));
        }
    }

    @Test
    public void testListOfStrings() throws Exception {
        BookStore bookStore = createClient();
        
        Book book = bookStore.getQuerySub().getBookFromListStrings(
            Arrays.asList("this is", "the book", "title"));

        assertEquals("this is the book title", book.getName());
    }

    @Test
    public void testListOfLongAndDoubleQueryEmptyWebClient() throws Exception {
        WebClient wc = createWebClient();
        
        Response r = wc
                .path("/bookstore/listoflonganddouble")
                .query("value", "")
                .accept("text/xml")
                .get();

        assertThat(wc.getCurrentURI().toString(), endsWith("value="));
        try (InputStream is = (InputStream)r.getEntity()) {
            XMLSource source = new XMLSource(is);
            assertEquals(0L, Long.parseLong(source.getValue("Book/id")));
        }
    }
    
    @Test
    public void testListOfLongAndDoubleQueryAsString() throws Exception {
        final URIBuilder builder = new URIBuilder("http://localhost:" + PORT + "/bookstore/listoflonganddouble");
        builder.setCustomQuery("value=1,2,3");

        final CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(builder.build());
        get.addHeader("Accept", "text/xml");

        try (CloseableHttpResponse response = client.execute(get)) {
            final byte[] content = EntityUtils.toByteArray(response.getEntity());
            try (InputStream is = new ByteArrayInputStream(content)) {
                XMLSource source = new XMLSource(is);
                assertEquals(123L, Long.parseLong(source.getValue("Book/id")));
            }
        }
    }
    
    private WebClient createWebClient() {
        if (threadSafe == null) {
            return WebClient.create("http://localhost:" + PORT, 
                Collections.singletonMap("expand.query.value.as.collection", "true"));
        } else {
            return WebClient.create("http://localhost:" + PORT, Collections.emptyList(),
                Collections.singletonMap("expand.query.value.as.collection", "true"), true);
        }
    }
    
    private BookStore createClient() {
        if (threadSafe == null) {
            return JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                Collections.singletonMap("expand.query.value.as.collection", "true"));
        } else {
            return JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class, Collections.emptyList(),
                Collections.singletonMap("expand.query.value.as.collection", "true"), threadSafe);
        }
    }
    
    private WebTarget createJaxrsClient() {
        if (threadSafe == null) {
            return ClientBuilder
                .newClient()
                .property("expand.query.value.as.collection", "true")
                .target("http://localhost:" + PORT);
        } else {
            return ClientBuilder
                .newClient()
                .property("expand.query.value.as.collection", "true")
                .property("thread.safe.client", threadSafe)
                .target("http://localhost:" + PORT);
        }
    }
}