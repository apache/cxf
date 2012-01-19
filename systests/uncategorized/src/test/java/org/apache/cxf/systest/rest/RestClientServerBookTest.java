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

package org.apache.cxf.systest.rest;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.customer.book.Book;
import org.apache.cxf.customer.book.BookService;
import org.apache.cxf.customer.book.BookServiceWrapped;
import org.apache.cxf.customer.book.GetAnotherBook;
import org.apache.cxf.customer.book.GetBook;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;


public class RestClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;
    static final Logger LOG = LogUtils.getLogger(RestClientServerBookTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(BookServer.class));
    }

    @Test
    public void testGetBookWithXmlRootElement() throws Exception {
        JaxWsProxyFactoryBean sf = new JaxWsProxyFactoryBean();
        sf.setServiceClass(BookService.class);

        // Turn off wrapped mode to make our xml prettier
        sf.getServiceFactory().setWrapped(false);

        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.getClientFactoryBean().setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:" + PORT + "/xml/");
        BookService bs = (BookService)sf.create();
        GetBook getBook = new GetBook();
        getBook.setId(123);
        Book book = bs.getBook(getBook);
        assertEquals(book.getId(), (long)123);
        assertEquals(book.getName(), "CXF in Action");
    }
    
    @Test
    public void testGetBookWithOutXmlRootElement() throws Exception {
        JaxWsProxyFactoryBean sf = new JaxWsProxyFactoryBean();
        sf.setServiceClass(BookService.class);

        // Turn off wrapped mode to make our xml prettier
        sf.getServiceFactory().setWrapped(false);

        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.getClientFactoryBean().setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:" + PORT + "/xml/");
        BookService bs = (BookService)sf.create();
        GetAnotherBook getAnotherBook = new GetAnotherBook();
        getAnotherBook.setId(123);
        Book book = bs.getAnotherBook(getAnotherBook);
        assertEquals(book.getId(), (long)123);
        assertEquals(book.getName(), "CXF in Action");
    }
    
    @Test
    public void testGetBookWrapped() throws Exception {
        JaxWsProxyFactoryBean sf = new JaxWsProxyFactoryBean();
        sf.setServiceClass(BookServiceWrapped.class);
        sf.getServiceFactory().setWrapped(true);

        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.getClientFactoryBean().setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:" + PORT + "/xmlwrapped/");
        BookServiceWrapped bs = (BookServiceWrapped)sf.create();
        Book book = bs.getBook(123);
        assertEquals(book.getId(), (long)123);
        assertEquals(book.getName(), "CXF in Action");
    }
    
    @Test
    public void testGetBookWrappedUsingURL() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/xmlwrapped/books/123"; 
        URL url = new URL(endpointAddress);
        InputStream in = url.openStream();
        assertNotNull(in);           

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("a1", "http://book.acme.com");
        ns.put("a2", "http://book.customer.cxf.apache.org/");
        Document doc = XMLUtils.parse(in);
        XPathUtils xp = new XPathUtils(ns);
        assertTrue(xp.isExist("/a2:getBookResponse", doc.getDocumentElement(), XPathConstants.NODE));
        assertTrue(xp.isExist("/a2:getBookResponse/a2:Book", doc.getDocumentElement(), XPathConstants.NODE));
        assertTrue(xp.isExist("/a2:getBookResponse/a2:Book/a1:id",
                              doc.getDocumentElement(), XPathConstants.NODE));
        assertEquals("123", xp.getValue("/a2:getBookResponse/a2:Book/a1:id",
                                       doc.getDocumentElement(), XPathConstants.STRING));
        assertEquals("CXF in Action", xp.getValue("/a2:getBookResponse/a2:Book/a1:name",
                                        doc.getDocumentElement(), XPathConstants.STRING));
        
    }
    
    @Test
    public void testGetBooksJSON() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/json/books"; 
        URL url = new URL(endpointAddress);
        InputStream in = url.openStream();
        assertNotNull(in);           

        InputStream expected = getClass().getResourceAsStream("resources/expected_json_books.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));   
    }
    
    @Test
    public void testGetBookJSON() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/json/books/123"; 
        URL url = new URL(endpointAddress);
        InputStream in = url.openStream();
        assertNotNull(in);           

        InputStream expected = getClass().getResourceAsStream("resources/expected_json_book123.txt");

        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));   
    }  
    
    @Test
    public void testAddBookJSON() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/json/books"; 

        File input = new File(getClass().getResource("resources/add_book_json.txt").toURI());         
        PostMethod post = new PostMethod(endpointAddress);
        RequestEntity entity = new FileRequestEntity(input, "text/plain; charset=ISO-8859-1");
        post.setRequestEntity(entity);
        HttpClient httpclient = new HttpClient();
        
        try {
            int result = httpclient.executeMethod(post);
            assertEquals(200, result);
            //System.out.println("Response status code: " + result);
            //System.out.println("Response body: ");
            //System.out.println(post.getResponseBodyAsString());
            
            InputStream expected = getClass().getResourceAsStream("resources/expected_add_book_json.txt");
            
            assertEquals(getStringFromInputStream(expected), post.getResponseBodyAsString());
        } finally {
            // Release current connection to the connection pool once you are done
            post.releaseConnection();
        }               
    }      
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        return IOUtils.toString(in);
    }

}
