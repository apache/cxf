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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerProxySpringBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerProxySpring.class));
    }
    
    @Test
    public void testGetBookNotFound() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/test/bookstore/books/12345"; 
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "text/plain,application/xml");
        assertEquals(500, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);           

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book_notfound_mapped.txt");

        assertEquals("Exception is not mapped correctly", 
                     getStringFromInputStream(expected).trim(),
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testGetThatBook123() throws Exception {
        getBook("http://localhost:9080/test/bookstorestorage/thosebooks/123");
    }
    
    @Test
    public void testGetThatBook123UserResource() throws Exception {
        getBook("http://localhost:9080/test/2/bookstore/books/123");
    }
    
    @Test
    public void testGetThatBook123UserResourceInterface() throws Exception {
        getBook("http://localhost:9080/test/3/bookstore2/books/123");
    }
    
    private void getBook(String endpointAddress) throws Exception {
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Content-Type", "*/*");
        connect.addRequestProperty("Accept", "application/xml");
        InputStream in = connect.getInputStream();           

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123.txt");
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in));
    }
    
    @Test
    public void testGetThatBookOverloaded() throws Exception {
        getBook("http://localhost:9080/test/bookstorestorage/thosebooks/123/123");
    }
    
    @Test
    public void testGetThatBookOverloaded2() throws Exception {
        getBook("http://localhost:9080/test/bookstorestorage/thosebooks");
    }
    
    @Test
    public void testGetBook123() throws Exception {
        String endpointAddress =
            "http://localhost:9080/test/bookstore/books/123"; 
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();           

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123json.txt");

        //System.out.println("---" + getStringFromInputStream(in));
        assertEquals(getStringFromInputStream(expected), getStringFromInputStream(in)); 
    }

    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        //System.out.println(bos.getOut().toString());        
        return bos.getOut().toString();        
    }

}
