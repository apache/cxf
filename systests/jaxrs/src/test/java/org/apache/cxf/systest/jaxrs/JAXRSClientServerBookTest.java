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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.provider.XSLTJaxbProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer.class));
    }
    
    
    @Test
    public void testOnewayWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/oneway");
        Response r = client.header("OnewayRequest", "true").post(null);
        assertEquals(202, r.getStatus());
    }
    
    @Test
    public void testOnewayProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        proxy.onewayRequest();
        assertEquals(202, WebClient.client(proxy).getResponse().getStatus());
    }
    
    @Test
    public void testPropogateException() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/propogateexception",
                      "", "application/xml", 500);
    }
    
    @Test
    public void testPropogateException2() throws Exception {
        String data = "<ns1:XMLFault xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">"
            + "<ns1:faultstring xmlns:ns1=\"http://cxf.apache.org/bindings/xformat\">"
            + "org.apache.cxf.systest.jaxrs.BookNotFoundFault: Book Exception</ns1:faultstring>"
            + "</ns1:XMLFault>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propogateexception2",
                      data, "application/xml", 500);
    }
    
    @Test
    public void testPropogateException3() throws Exception {
        String data = "<nobook/>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propogateexception3",
                      data, "application/xml", 500);
    }
    
    @Test
    public void testPropogateException4() throws Exception {
        String data = "<nobook/>";
        getAndCompare("http://localhost:" + PORT + "/bookstore/propogateexception4",
                      data, "application/xml", 500);
    }
    
    @Test
    public void testWebApplicationException() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/webappexception",
                      "This is a WebApplicationException",
                      "application/xml", 500);
    }
    
    @Test 
    public void testAddBookProxyResponse() {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b = new Book("CXF rocks", 123L);
        Response r = store.addBook(b);
        assertNotNull(r);
        InputStream is = (InputStream)r.getEntity();
        assertNotNull(is);
        XMLSource source = new XMLSource(is);
        source.setBuffering(true);
        assertEquals(124L, Long.parseLong(source.getValue("Book/id")));
        assertEquals("CXF rocks", source.getValue("Book/name"));
    }
    
    @Test 
    public void testGetBookCollection() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<Book>();
        books.add(b1);
        books.add(b2);
        List<Book> books2 = store.getBookCollection(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }
    
    @Test 
    public void testGetBookArray() throws Exception {
        BookStore store = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        Book[] books = new Book[2];
        books[0] = b1;
        books[1] = b2;
        Book[] books2 = store.getBookArray(books);
        assertNotNull(books2);
        assertNotSame(books, books2);
        assertEquals(2, books2.length);
        Book b11 = books2[0];
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books2[1];
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
    }
    
    @Test
    public void testGetBookByURL() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT 
                               + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testHeadBookByURL() throws Exception {
        WebClient wc = 
            WebClient.create("http://localhost:" + PORT 
                             + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123");
        Response response = wc.head();
        assertTrue(response.getMetadata().size() != 0);
        assertEquals(0, ((InputStream)response.getEntity()).available());
    }
    
    @Test
    public void testWebClientUnwrapBookWithXslt() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate("classpath:/org/apache/cxf/systest/jaxrs/resources/unwrapbook.xsl");
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/wrapper",
                             Collections.singletonList(provider));
        wc.path("{id}", 123);
        Book book = wc.get(Book.class);
        assertNotNull(book);
        assertEquals(123L, book.getId());
        
    }
    
    @Test
    @Ignore
    // uncomment once I can figure out how to set for this test only
    // com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize - JAXB is a pain
    public void testProxyUnwrapBookWithXslt() throws Exception {
        XSLTJaxbProvider provider = new XSLTJaxbProvider();
        provider.setInTemplate("classpath:/org/apache/cxf/systest/jaxrs/resources/unwrapbook2.xsl");
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                             Collections.singletonList(provider));
        Book book = bs.getWrappedBook2(123L);
        assertNotNull(book);
        assertEquals(123L, book.getId());
        
    }
    
    @Test
    public void testOptions() throws Exception {
        WebClient wc = 
            WebClient.create("http://localhost:" 
                             + PORT + "/bookstore/bookurl/http%3A%2F%2Ftest.com%2Frss%2F123");
        Response response = wc.options();
        List<Object> values = response.getMetadata().get("Allow");
        assertNotNull(values);
        assertTrue(values.contains("POST") && values.contains("GET")
                   && values.contains("DELETE") && values.contains("PUT"));
        assertEquals(0, ((InputStream)response.getEntity()).available());
    }
    
    @Test
    public void testEmptyPost() throws Exception {
        WebClient wc = 
            WebClient.create("http://localhost:" 
                             + PORT + "/bookstore/emptypost");
        Response response = wc.post(null);
        assertEquals(204, response.getStatus());
    }
    
    @Test
    public void testEmptyPostProxy() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean(); 
        bean.setAddress("http://localhost:" + PORT);
        bean.setResourceClass(BookStore.class);
        BookStore store = bean.create(BookStore.class);
        store.emptypost();
        assertEquals(204, WebClient.client(store).getResponse().getStatus());
    }
    
    @Test
    public void testGetBookByEncodedQuery() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookquery?"
                               + "urlid=http%3A%2F%2Ftest.com%2Frss%2F123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetGenericBook() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/genericbooks/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetGenericResponseBook() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/genericresponse/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookByArrayQuery() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookidarray?"
                               + "id=1&id=2&id=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
        
    @Test
    public void testNoRootResourceException() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/nobookstore/webappexception",
                      "",
                      "application/xml", 404);
    }
    
    @Test
    public void testNoPathMatch() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/bookqueries",
                      "",
                      "application/xml", 404);
    }
    
    @Test
    public void testWriteAndFailEarly() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/fail-early",
                      "This is supposed to go on the wire",
                      "application/bar, text/plain", 410);
    }
    
    @Test
    public void testWriteAndFailLate() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/books/fail-late",
                      "", "application/bar", 410);
    }
    
    
    @Test
    public void testAcceptTypeMismatch() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/booknames/123",
                      "",
                      "foo/bar", 406);
    }
            
    @Test
    public void testWrongHttpMethod() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/unsupportedcontenttype",
                      "",
                      "foo/bar", 405);
    }
    
    @Test
    public void testWrongQueryParameterType() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/wrongparametertype?p=1",
                      "Parameter Class java.util.Map has no constructor with single String "
                      + "parameter, static valueOf(String) or fromString(String) methods",
                      "*/*", 500);
    }
    
    @Test
    public void testExceptionDuringConstruction() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/exceptionconstruction?p=1",
                      "",
                      "foo/bar", 404);
    }
    
    @Test
    public void testSubresourceMethodNotFound() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/interface/thesubresource",
                      "",
                      "foo/bar", 404);
    }
    
    @Test
    public void testNoMessageWriterFound() throws Exception {
        String msg1 = "No message body writer has been found for response class GregorianCalendar.";
        String msg2 = "No message body writer has been found for response class Calendar.";
        
        getAndCompareStrings("http://localhost:" + PORT + "/bookstore/timetable", 
                             new String[]{msg1, msg2}, "*/*", 500);
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testNoMessageReaderFound() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/binarybooks";

        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", "application/octet-stream");
        post.setRequestHeader("Accept", "text/xml");
        post.setRequestBody("Bar");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(415, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    @Test
    public void testConsumeTypeMismatch() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/unsupportedcontenttype";

        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", "application/bar");
        post.setRequestHeader("Accept", "text/xml");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(415, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    @Test
    public void testBookExists() throws Exception {
        checkBook("http://localhost:" + PORT + "/bookstore/books/check/123", true);
        checkBook("http://localhost:" + PORT + "/bookstore/books/check/125", false);  
    }
    
    @Test
    public void testBookExists2() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        assertTrue(proxy.checkBook2(123L));
        assertFalse(proxy.checkBook2(125L));
    }
    
    private void checkBook(String address, boolean expected) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", "text/plain");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);
            if (expected) {
                assertEquals("Book must be available",
                             "true", get.getResponseBodyAsString());
            } else {
                assertEquals("Book must not be available",
                             "false", get.getResponseBodyAsString());
            }
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }
    
    @Test
    public void testGetBookCustomExpression() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/custom/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetHeadBook123WebClient() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/getheadbook/";
        WebClient client = WebClient.create(address);
        Response r = client.head();
        assertEquals("HEAD_HEADER_VALUE", r.getMetadata().getFirst("HEAD_HEADER"));
    }
    
    @Test
    public void testGetHeadBook123WebClient2() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/getheadbook/";
        WebClient client = WebClient.create(address);
        Book b = client.get(Book.class);
        assertEquals(b.getId(), 123L);
    }
    
    @Test
    public void testGetBook123WithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book b = bs.getBook("123");
        assertEquals(b.getId(), 123);
    }
    
    @Test
    public void testDeleteWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Response r = bs.deleteBook("123");
        assertEquals(200, r.getStatus());
    }
    
    @Test
    public void testCreatePutWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Response r = bs.createBook(777L);
        assertEquals(200, r.getStatus());
    }
    
    @Test
    public void testUpdateWithProxy() throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        Book book = new Book();
        book.setId(888);
        bs.updateBook(book);
        assertEquals(304, WebClient.client(bs).getResponse().getStatus());
    }
    
    @Test
    public void testGetBookTypeAndWildcard() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.8,*/*", 
                               "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/*", 
                               "application/xml", 200);
    }
    
    
    @Test
    public void testGetBook123() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/query?bookId=123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/defaultquery",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/missingquery",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/json, application/xml", 
                               "application/json", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml, application/json", 
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookXmlWildcard() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "*/*", "application/xml", 200);
        
    }
    
    @Test
    public void testGetBookBuffer() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/buffer",
                               "resources/expected_get_book123.txt",
                               "application/bar", 200);
    }    
    
    @Test
    public void testGetBookBySegment() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/matrix2;first=12;second=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore;bar/segment;foo/"
                               + "matrix2;first=12;second=3;third",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookByListOfSegments() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/list/1/2/3",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookByMatrixParameters() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/segment/matrix;first=12;second=3",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore;bar;first=12/segment;foo;"
                               + "second=3/matrix;third",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookByHeader() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }
    
    @Test
    public void testGetBookByHeaderPerRequest() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore2/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }
    
    @Test
    public void testGetBookByHeaderDefault() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/bookheaders2",
                               "resources/expected_get_book123.txt",
                               "application/xml;q=0.5,text/xml", "text/xml", 200);
    }
    
    @Test
    public void testGetBookElement() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/element",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBookAdapter() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/adapter",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
    }
    
    @Test
    public void testGetBook123FromSub() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/interface/subresource",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/books/123",
                               "resources/expected_get_book123json.txt",
                               "application/xml;q=0.1,application/json", "application/json", 200);
    }
    
    @Test
    public void testGetBook123FromSubObject() throws Exception {
        getAndCompareAsStrings(
            "http://localhost:" + PORT + "/bookstore/booksubresourceobject/123/chaptersobject/sub/1",
            "resources/expected_get_chapter1.txt", "application/xml",
            "application/xml;charset=ISO-8859-1", 200);
    }
    
    @Test
    public void testGetChapter() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/booksubresource/123/chapters/1",
                               "resources/expected_get_chapter1.txt",
                               "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }
    
    @Test
    public void testGetChapterEncodingDefault() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" 
                               + PORT + "/bookstore/booksubresource/123/chapters/badencoding/1",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", "application/xml;charset=UTF-8", 200);
    }
    
    @Test
    public void testGetChapterChapter() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" 
                               + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse",
                               "resources/expected_get_chapter1_utf.txt",
                               "application/xml", 200);
        getAndCompareAsStrings("http://localhost:"
                               + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse2",
                               "resources/expected_get_chapter1.txt",
                               "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }
    
    @Test
    public void testGetChapterWithParentIds() throws Exception {
        
        getAndCompareAsStrings(
            "http://localhost:" + PORT + "/bookstore/booksubresource/123/chapters/sub/1/recurse2/ids",
            "resources/expected_get_chapter1.txt",
            "application/xml", "application/xml;charset=ISO-8859-1", 200);
    }
    
    @Test
    public void testGetBook123ReturnString() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/booknames/123",
                               "resources/expected_get_book123_returnstring.txt",
                               "text/plain", 200);
    }
    
    @Test
    public void testAddBookNoBody() throws Exception {
        PostMethod post = new PostMethod("http://localhost:" + PORT + "/bookstore/books");
        post.setRequestHeader("Content-Type", "application/xml");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(400, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    @Test
    public void testAddBook() throws Exception {
        doAddBook("http://localhost:" + PORT + "/bookstore/books");               
    }
    
    @Test
    public void testAddBookXmlAdapter() throws Exception {
        doAddBook("http://localhost:" + PORT + "/bookstore/booksinfo");               
    }
    
    private void doAddBook(String address) throws Exception {
        File input = new File(getClass().getResource("resources/add_book.txt").toURI());         
        PostMethod post = new PostMethod(address);
        post.setRequestHeader("Content-Type", "application/xml");
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
            
            assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    
    @Test
    public void testAddBookCustomFailureStatus() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books/customstatus";
        WebClient client = WebClient.create(endpointAddress);
        Book book = client.type("text/xml").accept("application/xml").post(new Book(), Book.class);
        assertEquals(888L, book.getId());
        Response r = client.getResponse();
        assertEquals("CustomValue", r.getMetadata().getFirst("CustomHeader"));
        assertEquals(333, r.getStatus());
        assertEquals("application/xml", r.getMetadata().getFirst("Content-Type"));
    }
    
    @Test
    public void testUpdateBook() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/books";

        File input = new File(getClass().getResource("resources/update_book.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        // Verify result
        endpointAddress = "http://localhost:" + PORT + "/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        InputStream in = connect.getInputStream();
        assertNotNull(in);

        InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));

        // Roll back changes:
        File input1 = new File(getClass().getResource("resources/expected_get_book123.txt").toURI());
        PutMethod put1 = new PutMethod(endpointAddress);
        RequestEntity entity1 = new FileRequestEntity(input1, "text/xml; charset=ISO-8859-1");
        put1.setRequestEntity(entity1);
        HttpClient httpclient1 = new HttpClient();

        try {
            int result = httpclient1.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put1.releaseConnection();
        }
    }  
    
    @Test
    public void testUpdateBookWithDom() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/bookswithdom";

        File input = new File(getClass().getResource("resources/update_book.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
            String resp = put.getResponseBodyAsString();
            InputStream expected = getClass().getResourceAsStream("resources/update_book.txt");
            assertTrue(resp.indexOf(getStringFromInputStream(expected)) >= 0);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }
    }
    
    @Test
    public void testUpdateBookWithJSON() throws Exception {
        String endpointAddress = "http://localhost:" + PORT + "/bookstore/bookswithjson";

        File input = new File(getClass().getResource("resources/update_book_json.txt").toURI());
        PutMethod put = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "application/json; charset=ISO-8859-1");
        put.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put.releaseConnection();
        }

        // Verify result
        endpointAddress = "http://localhost:" + PORT + "/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        InputStream in = connection.getInputStream();
        assertNotNull(in);

        InputStream expected = getClass().getResourceAsStream("resources/expected_update_book.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));

        // Roll back changes:
        File input1 = new File(getClass().getResource("resources/expected_get_book123.txt").toURI());
        PutMethod put1 = new PutMethod(endpointAddress);
        RequestEntity entity1 = new FileRequestEntity(input1, "text/xml; charset=ISO-8859-1");
        put1.setRequestEntity(entity1);
        HttpClient httpclient1 = new HttpClient();

        try {
            int result = httpclient1.executeMethod(put);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are
            // done
            put1.releaseConnection();
        }
    } 
    
    @Test
    public void testUpdateBookFailed() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books";

        File input = new File(getClass().getResource("resources/update_book_not_exist.txt").toURI());         
        PutMethod post = new PutMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/xml; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(304, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }               
    } 
        
    @Test
    public void testGetCDs() throws Exception {
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/cds");
        CDs cds = wc.get(CDs.class);
        Collection<CD> collection = cds.getCD();
        assertEquals(2, collection.size());
        assertTrue(collection.contains(new CD("BICYCLE RACE", 124)));
        assertTrue(collection.contains(new CD("BOHEMIAN RHAPSODY", 123)));
    }
    
    @Test
    public void testGetCDJSON() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cd/123",
                               "resources/expected_get_cdjson.txt",
                               "application/json", 200);
        
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testGetPlainLong() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/booksplain"; 

        PostMethod post = new PostMethod(endpointAddress);
        post.addRequestHeader("Content-Type" , "text/plain");
        post.addRequestHeader("Accept" , "text/plain");
        post.setRequestBody("12345");
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            assertEquals(post.getResponseBodyAsString(), "12345");
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }  
    }
    
    @Test
    public void testDeleteBook() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/123"; 

        DeleteMethod post = new DeleteMethod(endpointAddress);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }  
    }
    
    @Test
    public void testDeleteBookByQuery() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/id?value=123"; 

        DeleteMethod post = new DeleteMethod(endpointAddress);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }  
    }
    
    @Test
    public void testGetCDsJSON() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/cds"; 

        GetMethod get = new GetMethod(endpointAddress);
        get.addRequestHeader("Accept" , "application/json");

        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);

            InputStream expected123 = getClass().getResourceAsStream("resources/expected_get_cdsjson123.txt");
            InputStream expected124 = getClass().getResourceAsStream("resources/expected_get_cdsjson124.txt");
            
            assertTrue(get.getResponseBodyAsString().indexOf(getStringFromInputStream(expected123)) >= 0);
            assertTrue(get.getResponseBodyAsString().indexOf(getStringFromInputStream(expected124)) >= 0);

        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }  
    }  
    
    @Test
    public void testGetCDXML() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cd/123",
                               "resources/expected_get_cd.txt",
                               "application/xml", 200);
    }
    
    
    @Test
    public void testGetCDWithMultiContentTypesXML() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cd.txt",
                               "application/json;q=0.8,application/xml,*/*", "application/xml", 200);
    }
    
    @Test
    public void testGetCDWithMultiContentTypesCustomXML() throws Exception {
        
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cd.txt",
                               "application/bar+xml", "application/bar+xml", 200);
    }
    
    @Test
    public void testGetCDWithMultiContentTypesJSON() throws Exception {
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cdjson.txt",
                               "application/json", 200);
        getAndCompareAsStrings("http://localhost:" + PORT + "/bookstore/cdwithmultitypes/123",
                               "resources/expected_get_cdjson.txt",
                               "*/*,application/xml;q=0.9,application/json", "application/json", 200);
    }
    
    @Test
    public void testUriInfoMatchedResources() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chapters/sub/1/matched-resources", 
                      "[class org.apache.cxf.systest.jaxrs.BookStore, " 
                      + "class org.apache.cxf.systest.jaxrs.Book, "
                      + "class org.apache.cxf.systest.jaxrs.Chapter]", 
                      "text/plain", "text/plain", 200);
    }
    
    @Test
    public void testUriInfoMatchedResourcesWithObject() throws Exception {
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chaptersobject/sub/1/matched-resources", 
                      "[class org.apache.cxf.systest.jaxrs.BookStore, " 
                      + "class org.apache.cxf.systest.jaxrs.Book, "
                      + "class org.apache.cxf.systest.jaxrs.Chapter]", 
                      "text/plain", "text/plain", 200);
    }

    @Test
    public void testUriInfoMatchedUrisDecode() throws Exception {
        String expected = "[/bookstore/booksubresource/123/, "
                          + "/bookstore/booksubresource/123/chapters/sub/1/, "
                          + "/bookstore/booksubresource/123/chapters/sub/1/matched!uris]";
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chapters/sub/1/matched%21uris?decode=true", 
                      expected, "text/plain", "text/plain", 200);
    }

    @Test
    public void testUriInfoMatchedUrisNoDecode() throws Exception {
        //note '%21' instead of '!'
        String expected = "[/bookstore/booksubresource/123/, "
            + "/bookstore/booksubresource/123/chapters/sub/1/, "
            + "/bookstore/booksubresource/123/chapters/sub/1/matched%21uris]";
        getAndCompare("http://localhost:" + PORT + "/bookstore/"
                      + "booksubresource/123/chapters/sub/1/matched%21uris?decode=false", 
                      expected,
                      "text/plain", "text/plain", 200);
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
    
    private void getAndCompareAsStrings(String address, 
                                        String resourcePath,
                                        String acceptType,
                                        String expectedContentType,
                                        int status) throws Exception {
        String expected = getStringFromInputStream(
                              getClass().getResourceAsStream(resourcePath));
        getAndCompare(address,
                      expected,
                      acceptType,
                      expectedContentType,
                      status);
    }
    
    private void getAndCompare(String address, 
                               String expectedValue,
                               String acceptType,
                               int expectedStatus) throws Exception {
        getAndCompare(address,
                      expectedValue,
                      acceptType,
                      null,
                      expectedStatus);
    }
    
    private void getAndCompare(String address, 
                               String expectedValue,
                               String acceptType,
                               String expectedContentType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        get.addRequestHeader("Cookie", "a=b,c=d");
        get.addRequestHeader("Cookie", "e=f");
        get.setRequestHeader("Accept-Language", "da;q=0.8,en");
        get.setRequestHeader("Book", "1,2,3");
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String content = getStringFromInputStream(get.getResponseBodyAsStream());
            assertEquals("Expected value is wrong", 
                         expectedValue, content);
            if (expectedStatus == 200) {
                assertEquals("123", get.getResponseHeader("BookId").getValue());
                assertNotNull(get.getResponseHeader("Date"));
            }
            if (expectedStatus == 405) {
                assertNotNull(get.getResponseHeader("Allow"));
            }
            if (expectedContentType != null) {
                Header ct = get.getResponseHeader("Content-Type");
                assertEquals("Wrong type of response", expectedContentType, ct.getValue());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    private void getAndCompareStrings(String address, 
                               String[] expectedValue,
                               String acceptType,
                               int expectedStatus) throws Exception {
        assertEquals(2, expectedValue.length);
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String jsonContent = getStringFromInputStream(get.getResponseBodyAsStream());
            assertTrue("Expected value is wrong", 
                       expectedValue[0].equals(jsonContent) || expectedValue[1].equals(jsonContent));
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
