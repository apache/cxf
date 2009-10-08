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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerSpringBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSpring.class, true));
    }
    
    @Test
    public void testGetBookByUriInfo() throws Exception {
        String endpointAddress =
            "http://localhost:9080/the/thebooks/bookstore/bookinfo?"
                               + "param1=12&param2=3"; 
        getBook(endpointAddress, "resources/expected_get_book123json.txt");
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
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", type);
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

}
