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

import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JaxRsJaxWsBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerRestSoap.class));
    }
    
    @Test
    public void testGetBook123() throws Exception {
        
        InputStream in = getRestInputStream("http://localhost:9092/rest/bookstore/123");
        
        InputStream expected = getClass().getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));
                
    }
    
    @Test
    public void testAddGetBookRest() throws Exception {
        
        String endpointAddress =
            "http://localhost:9092/rest/bookstore/books";
        
        File input = new File(getClass().getResource("resources/add_book.txt").toURI());         
        PostMethod post = new PostMethod(endpointAddress);
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
    public void testGetBookSoap() throws Exception {
        String wsdlAddress =
            "http://localhost:9092/soap/bookservice?wsdl"; 
        URL wsdlUrl = new URL(wsdlAddress);
        BookSoapService service = 
            new BookSoapService(wsdlUrl,
                                new QName("http://books.com", "BookService"));
        BookStoreJaxrsJaxws store = service.getBookPort();
        Book book = store.getBook(new Long(123));
        assertEquals("id is wrong", book.getId(), 123);
    }

    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

    private InputStream getRestInputStream(String endpointAddress) throws Exception {
        URL url = new URL(endpointAddress);
        
        for (int count = 0; count < 25; count++) {
            URLConnection connect = url.openConnection();
            connect.addRequestProperty("Accept", "application/xml");
            try {
                return connect.getInputStream();
            } catch (Exception ex) {
                // continue;
            }
        }
        fail("REST endpoint can not be accessed");
        // unreachable
        return null;
    }
}
