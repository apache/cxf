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

import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSClientServerNonSpringBookTest extends AbstractBusClientServerTestBase {
    public static final int PORT = BookNonSpringServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookNonSpringServer.class, true));
        createStaticBus();
    }


    @Test
    public void testGetBook123Singleton() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/singleton/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);

    }

    @Test
    public void testGetStaticResource() throws Exception {
        String address = "http://localhost:" + PORT + "/singleton/staticmodel.xml";
        WebClient wc = WebClient.create(address);
        String response = wc.get(String.class);
        assertTrue(response.startsWith("<model"));
        assertEquals("application/xml+model", wc.getResponse().getMetadata().getFirst("Content-Type"));

    }
    @Test
    public void testGetPathFromUriInfo() throws Exception {
        String address = "http://localhost:" + PORT + "/application/bookstore/uifromconstructor";
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain");
        String response = wc.get(String.class);
        assertEquals(address + "?prop=cxf", response);

    }

    @Test
    public void testGetBook123UserModel() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/usermodel/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);

    }

    @Test
    public void testGetBook123UserModelAuthorize() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://localhost:" + PORT + "/usermodel/bookstore/books");
        bean.setUsername("Barry");
        bean.setPassword("password");
        bean.setModelRef("classpath:org/apache/cxf/systest/jaxrs/resources/resources.xml");
        WebClient proxy = bean.createWebClient();
        proxy.path("{id}/authorize", 123);

        Book book = proxy.get(Book.class);
        assertEquals(123L, book.getId());



    }

    @Test
    public void testGetChapterUserModel() throws Exception {

        getAndCompareAsStrings("http://localhost:" + PORT + "/usermodel/bookstore/books/123/chapter",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", 200);
    }

    @Test
    public void testGetBook123UserModelInterface() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/usermodel2/bookstore2/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);

    }

    @Test
    public void testGetBooksUserModelInterface() throws Exception {
        BookStoreNoAnnotationsInterface proxy =
            JAXRSClientFactory.createFromModel("http://localhost:" + PORT + "/usermodel2",
                                              BookStoreNoAnnotationsInterface.class,
                              "classpath:org/apache/cxf/systest/jaxrs/resources/resources2.xml", null);
        Book book = new Book("From Model", 1L);
        List<Book> books = new ArrayList<>();
        books.add(book);
        books = proxy.getBooks(books);
        assertEquals(1, books.size());
        assertNotSame(book, books.get(0));
        assertEquals("From Model", books.get(0).getName());

    }

    @Test
    public void testUserModelInterfaceOneWay() throws Exception {
        BookStoreNoAnnotationsInterface proxy =
            JAXRSClientFactory.createFromModel("http://localhost:" + PORT + "/usermodel2",
                                              BookStoreNoAnnotationsInterface.class,
                              "classpath:org/apache/cxf/systest/jaxrs/resources/resources2.xml", null);

        proxy.pingBookStore();
        assertEquals(202, WebClient.client(proxy).getResponse().getStatus());
    }

    @Test
    public void testGetBook123ApplicationSingleton() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/application/bookstore/default");
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals("default", book.getName());
        assertEquals(543L, book.getId());
    }

    @Test
    public void testGetBook123ApplicationPerRequest() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/application/bookstore2/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);

    }

    @Test
    public void testGetBook123Application11Singleton() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/application11/thebooks/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);

    }

    @Test
    public void testGetBook123Application11PerRequest() throws Exception {
        Response r =
            doTestPerRequest("http://localhost:" + PORT + "/application11/thebooks/bookstore2/bookheaders");
        assertEquals("TheBook", r.getHeaderString("BookWriter"));
    }
    
    @Test
    public void testGetBook123PropagatingContextPropertyToWriterInterceptor() throws Exception {
        Response r = 
            doTestPerRequest("http://localhost:" + PORT + "/application6/thebooks/bookstore2/bookheaders",
                new SimpleEntry<>("property", "PropValue"));
        assertEquals("PropValue", r.getHeaderString("X-Property-WriterInterceptor"));
    }


    @Test
    public void testGetBook123TwoApplications() throws Exception {
        doTestPerRequest("http://localhost:" + PORT + "/application6/thebooks/bookstore2/bookheaders");
        doTestPerRequest("http://localhost:" + PORT + "/application6/the%20books2/bookstore2/book%20headers");
    }

    @SafeVarargs
    private final Response doTestPerRequest(String address, Map.Entry<String, String> ... params) throws Exception {
        WebClient wc = WebClient.create(address);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(100000000L);
        wc.accept("application/xml");
        
        for (Map.Entry<String, String> param: params) {
            wc.query(param.getKey(), param.getValue());
        }
        
        Response r = wc.get();
        Book book = r.readEntity(Book.class);
        assertEquals("CXF in Action", book.getName());
        assertEquals(123L, book.getId());
        return r;
    }

    @Test
    public void testGetNonExistentBook() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT
                                        + "/application11/thebooks/bookstore/books/321");
        try {
            wc.accept("*/*").get(Book.class);
            fail();
        } catch (InternalServerErrorException ex) {
            assertEquals("No book found at all : 321", ex.getResponse().readEntity(String.class));
        }

    }

    @Test
    public void testBookWithNonExistentMethod() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT
                                        + "/application11/thebooks/bookstore/nonexistent");
        try {
            wc.accept("*/*").get(Book.class);
            fail();
        } catch (WebApplicationException ex) {
            assertEquals("Nonexistent method", ex.getResponse().readEntity(String.class));
        }

    }

    private void getAndCompareAsStrings(String address,
                                        String resourcePath,
                                        String acceptType,
                                        int status) throws Exception {
        String expected = getStringFromInputStream(
                              getClass().getResourceAsStream(resourcePath));
        getAndCompare(address,
                      expected,
                      acceptType,
                      acceptType,
                      status);
    }



    private void getAndCompare(String address,
                               String expectedValue,
                               String acceptType,
                               String expectedContentType,
                               int expectedStatus) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);
        get.setHeader("Accept", acceptType);
        get.setHeader("Accept-Language", "da;q=0.8,en");
        get.setHeader("Book", "1,2,3");
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            assertEquals("Expected value is wrong",
                         stripXmlInstructionIfNeeded(expectedValue), stripXmlInstructionIfNeeded(content));
            if (expectedContentType != null) {
                Header ct = response.getFirstHeader("Content-Type");
                assertEquals("Wrong type of response", expectedContentType, ct.getValue());
            }
        } finally {
            get.releaseConnection();
        }
    }

    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
