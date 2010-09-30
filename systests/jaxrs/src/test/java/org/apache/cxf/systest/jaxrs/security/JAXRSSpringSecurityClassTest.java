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

package org.apache.cxf.systest.jaxrs.security;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.systest.jaxrs.Book;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSSpringSecurityClassTest extends AbstractSpringSecurityTest {
    public static final String PORT = BookServerSecuritySpringClass.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSecuritySpringClass.class));
    }
    
    @Test
    public void testFailedAuthentication() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "ba", 401);
    }
    
    @Test
    public void testBookFromForm() throws Exception {
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstorestorage/bookforms", 
                                        "foo", "bar", null);
        
        Response r = wc.form(new Form().set("name", "CXF Rocks").set("id", "123"));
        
        Book b = readBook((InputStream)r.getEntity());
        assertEquals("CXF Rocks", b.getName());
        assertEquals(123L, b.getId());
    }
    
    @Test
    public void testBookFromHttpRequestParameters() throws Exception {
        
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstorestorage/bookforms2", 
                                        "foo", "bar", null);
        
        Response r = wc.form(new Form().set("name", "CXF Rocks").set("id", "123"));
        
        Book b = readBook((InputStream)r.getEntity());
        assertEquals("CXF Rocks", b.getName());
        assertEquals(123L, b.getId());
    }
    
    @Test
    public void testGetBookUserAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 200);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    
    @Test
    public void testGetBookUser() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks/123/123"; 
        getBook(endpointAddress, "foo", "bar", 200);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/thosebooks"; 
        getBook(endpointAddress, "foo", "bar", 200); 
        getBook(endpointAddress, "bob", "bobspassword", 403);
    }
    
    private Book readBook(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Book.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Book)u.unmarshal(is);
    }

    @Test
    public void testGetBookSubresourceAdmin() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstorestorage/securebook/self"; 
        getBook(endpointAddress, "foo", "bar", 200); 
        getBook(endpointAddress, "bob", "bobspassword", 403);
    }
    
     
}
