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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerNonSpringBookTest extends AbstractBusClientServerTestBase {
    public static final int PORT = BookNonSpringServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
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
        List<Book> books = new ArrayList<Book>();
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
    public void testGetNonExistentBook() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT 
                                        + "/application11/thebooks/bookstore/books/321");
        try {
            wc.accept("*/*").get(Book.class);
            fail();
        } catch (ServerWebApplicationException ex) {
            assertEquals("No book found at all : 321", ex.getMessage());
        }
        
    }
    
    @Test
    public void testBookWithNonExistentMethod() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT
                                        + "/application11/thebooks/bookstore/nonexistent");
        try {
            wc.accept("*/*").get(Book.class);
            fail();
        } catch (ServerWebApplicationException ex) {
            assertEquals("Nonexistent method", ex.getMessage());
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
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        get.setRequestHeader("Accept-Language", "da;q=0.8,en");
        get.setRequestHeader("Book", "1,2,3");
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String content = getStringFromInputStream(get.getResponseBodyAsStream());
            assertEquals("Expected value is wrong", 
                         expectedValue, content);
            if (expectedContentType != null) {
                Header ct = get.getResponseHeader("Content-Type");
                assertEquals("Wrong type of response", expectedContentType, ct.getValue());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

}
