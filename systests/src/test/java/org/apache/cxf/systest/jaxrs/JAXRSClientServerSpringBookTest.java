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
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerSpringBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSpring.class));
    }
    
    @Test
    public void testGetBookByUriInfo() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks/bookstore/bookinfo?"
                               + "param1=12&param2=3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }
    
    @Test
    public void testGetBookXSLTHtml() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks5/bookstore/books/xslt";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xhtml+xml").path(666).matrix("name2", 2).query("name", "Action - ");
        XMLSource source = wc.get(XMLSource.class);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        Book2 b = source.getNode("xhtml:html/xhtml:body/xhtml:ul/xhtml:Book", namespaces, Book2.class);
        assertEquals(666, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }

    @Test
    public void testGetBookByUriInfo2() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks3/bookstore/bookinfo?"
                               + "param1=12&param2=3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
    }
    
    @Test
    public void testGetBook123() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/bookstore/books/123"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
        getBook(endpointAddress, "resources/expected_get_book123json.txt",
                "application/jettison");
    }
    
    @Test
    public void testGetBookAsArray() throws Exception {
        URL url = new URL("http://localhost:9080/the/bookstore/books/list/123");
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();           

        assertEquals("{\"Books\":{\"books\":[{\"id\":123,\"name\":\"CXF in Action\"}]}}", 
                     getStringFromInputStream(in)); 
        
    }
    
    @Test
    public void testGetBookWithEncodedQueryValue() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/bookstore/booksquery?id=12%2B3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt"); 
    }
    
    @Test
    public void testGetBookWithEncodedPathValue() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/bookstore/id=12%2B3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt"); 
    }
    
    @Test
    public void testGetBookWithEncodedPathValue2() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/bookstore/id=12+3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt"); 
    }
    
    @Test
    public void testGetDefaultBook() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/bookstore"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt"); 
    }

    private void getBook(String endpointAddress, String resource) throws Exception {
        getBook(endpointAddress, resource, "application/json");
    }
    
    private void getBook(String endpointAddress, String resource, String type) throws Exception {
        getBook(endpointAddress, resource, type, null);
    }
    
    private void getBook(String endpointAddress, String resource, String type, String mHeader) 
        throws Exception {
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Content-Type", "*/*");
        connect.addRequestProperty("Accept", type);
        if (mHeader != null) {
            connect.addRequestProperty("X-HTTP-Method-Override", mHeader);
        }
        InputStream in = connect.getInputStream();           

        InputStream expected = getClass().getResourceAsStream(resource);
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));
    }
    
    @Test
    public void testAddInvalidXmlBook() throws Exception {
        
        doPost("http://localhost:9080/the/bookstore/books/convert",
               500,
               "application/xml",
               "resources/add_book.txt",
               null);
        
        doPost("http://localhost:9080/the/thebooks/bookstore/books/convert",
               500,
               "application/xml",
               "resources/add_book.txt",
               null);
                
    }
    
    @Test
    public void testAddInvalidJsonBook() throws Exception {
        
        doPost("http://localhost:9080/the/bookstore/books/convert",
               500,
               "application/json",
               "resources/add_book2json_invalid.txt",
               null);
        
        doPost("http://localhost:9080/the/thebooks/bookstore/books/convert",
               500,
               "application/json",
               "resources/add_book2json_invalid.txt",
               null);
                
    }
    
    @Test
    public void testAddValidXmlBook() throws Exception {
        
        doPost("http://localhost:9080/the/bookstore/books/convert",
               200,
               "application/xml",
               "resources/add_book2.txt",
               "resources/expected_get_book123.txt");
        
        doPost("http://localhost:9080/the/thebooks/bookstore/books/convert",
               200,
               "application/xml",
               "resources/add_book2.txt",
               "resources/expected_get_book123.txt");
                
    }
    
    @Test
    public void testGetBookAegis() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks4/bookstore/books/aegis"; 
        getBook(endpointAddress, "resources/expected_add_book_aegis.txt", "application/xml"); 
    }
    
    @Test
    public void testRetrieveBookAegis1() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks4/bookstore/books/aegis/retrieve?_method=RETRIEVE"; 
        getBook(endpointAddress, "resources/expected_add_book_aegis.txt", "application/xml"); 
    }
    
    @Test
    public void testRetrieveBookAegis2() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks4/bookstore/books/aegis/retrieve"; 
        getBook(endpointAddress, "resources/expected_add_book_aegis.txt", "application/xml", "RETRIEVE"); 
    }
    
    @Test
    @Ignore
    public void testRetrieveBookAegis3() throws Exception {
        GetMethod get = new GetMethod("http://localhost:9080/the/thebooks4/bookstore/books/aegis/retrieve");
        get.setRequestHeader("Content-Type", "*/*");
        get.setRequestHeader("Accept", "application/xml");
        HttpClient httpClient = new HttpClient();
        try {
            httpClient.executeMethod(get);           
            String aegisData = getStringFromInputStream(get.getResponseBodyAsStream());
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book_aegis.txt");
            assertEquals(getStringFromInputStream(expected), aegisData);
        } finally {
            get.releaseConnection();
        }
    }
    
    @Test
    public void testGetBookUserResource() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks6/bookstore/books/123"; 
        getBook(endpointAddress, "resources/expected_get_book123.txt", "application/xml"); 
    }
    
    @Test
    public void testGetBookUserResource2() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks7/bookstore/books/123"; 
        getBook(endpointAddress, "resources/expected_get_book123.txt", "application/xml"); 
    }
    
    @Test
    public void testGetBookUserResourceFromProxy() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/the/thebooks6"; 
        BookStoreNoAnnotations bStore = JAXRSClientFactory.createFromModel(
                                         endpointAddress, 
                                         BookStoreNoAnnotations.class,
                                         "classpath:/org/apache/cxf/systest/jaxrs/resources/resources.xml",
                                         null);
        Book b = bStore.getBook(123L);
        assertNotNull(b);
        assertEquals(123L, b.getId());
        assertEquals("CXF in Action", b.getName());
        ChapterNoAnnotations proxy = bStore.getBookChapter(123L);
        ChapterNoAnnotations c = proxy.getItself();
        assertNotNull(c);
        assertEquals(1, c.getId());
        assertEquals("chapter 1", c.getTitle());
    }
    
    @Test
    public void testGetBookXSLTXml() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks5/bookstore/books/xslt";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").path(666).matrix("name2", 2).query("name", "Action - ");
        Book b = wc.get(Book.class);
        assertEquals(666, b.getId());
        assertEquals("CXF in Action - 2", b.getName());
    }
    
    @Test
    public void testReaderWriterFromJaxrsFilters() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks5/bookstore/books/convert2";
        WebClient wc = WebClient.create(endpointAddress);
        wc.type("application/xml").accept("application/xml");
        Book2 b = new Book2();
        b.setId(777L);
        b.setName("CXF - 777");
        Book2 b2 = wc.invoke("PUT", b, Book2.class);
        assertNotSame(b, b2);
        assertEquals(777, b2.getId());
        assertEquals("CXF - 777", b2.getName());
    }
    
    @Test
    public void testReaderWriterFromInterceptors() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks5/bookstore/books/convert";
        WebClient wc = WebClient.create(endpointAddress);
        wc.type("application/xml").accept("application/xml");
        Book2 b = new Book2();
        b.setId(777L);
        b.setName("CXF - 777");
        Book2 b2 = wc.invoke("POST", b, Book2.class);
        assertNotSame(b, b2);
        assertEquals(777, b2.getId());
        assertEquals("CXF - 777", b2.getName());
    }
    
    @Test
    public void testAddValidBookJson() throws Exception {
        doPost("http://localhost:9080/the/bookstore/books/convert",
               200,
               "application/json",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");
        
        doPost("http://localhost:9080/the/thebooks/bookstore/books/convert",
               200,
               "application/json",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");
        
        doPost("http://localhost:9080/the/thebooks/bookstore/books/convert",
               200,
               "application/jettison",
               "resources/add_book2json.txt",
               "resources/expected_get_book123.txt");
    }
    
    private void doPost(String endpointAddress, int expectedStatus, String contentType,
                        String inResource, String expectedResource) throws Exception {
        
        File input = new File(getClass().getResource(inResource).toURI());         
        PostMethod post = new PostMethod(endpointAddress);
        post.setRequestHeader("Content-Type", contentType);
        RequestEntity entity = new FileRequestEntity(input, "text/xml");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(expectedStatus, result);
            
            if (expectedStatus != 500) {
                InputStream expected = getClass().getResourceAsStream(expectedResource);
                assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
            } else {
                assertTrue(post.getResponseBodyAsString()
                               .contains("Cannot find the declaration of element"));
            }
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

        
    @Ignore
    @XmlRootElement(name = "Book", namespace = "http://www.w3.org/1999/xhtml")
    public static class Book2 {
        @XmlElement(name = "id", namespace = "http://www.w3.org/1999/xhtml")
        private long id1;
        @XmlElement(name = "name", namespace = "http://www.w3.org/1999/xhtml")
        private String name1;
        public Book2() {
            
        }
        public long getId() {
            return id1;
        }
        
        public void setId(Long theId) {
            id1 = theId;
        }
        
        public String getName() {
            return name1;
        }
        
        public void setName(String n) {
            name1 = n;
        }
        
    }
}
