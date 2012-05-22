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
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerResourceJacksonSpringProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerResourceJacksonSpringProviders.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceJacksonSpringProviders.class, true));
    }
    
    @Test
    public void testGetBook123() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/bookstore/books/123"; 
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();
        assertNotNull(in);           

        assertEquals("Jackson output not correct", 
                     "{\"name\":\"CXF in Action\",\"id\":123}",
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testGetCollectionOfBooks() throws Exception {
        
        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/bookstore/collections"; 
        WebClient wc = WebClient.create(endpointAddress,
            Collections.singletonList(new org.codehaus.jackson.jaxrs.JacksonJsonProvider()));
        wc.accept("application/json");
        Collection<? extends Book> collection = wc.getCollection(Book.class);
        assertEquals(1, collection.size());
        Book book = collection.iterator().next();
        assertEquals(123L, book.getId());
    }
        
    
    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
