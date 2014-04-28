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

package org.apache.cxf.systest.jaxrs.websocket;

import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class JAXRSClientConduitWebSocketTest extends AbstractBusClientServerTestBase {
    private static final String PORT = BookServerWebSocket.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new BookServerWebSocket()));
        createStaticBus();
    }
    
    @Test
    public void testBookWithWebSocket() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket";

        BookStoreWebSocket resource = JAXRSClientFactory.create(address, BookStoreWebSocket.class);
        Client client = WebClient.client(resource);
        client.header(HttpHeaders.USER_AGENT, JAXRSClientConduitWebSocketTest.class.getName());
        
        // call the GET service
        assertEquals("CXF in Action", new String(resource.getBookName()));

        // call the GET service in text mode
        //TODO add some way to control the client to switch between the bytes and text modes
        assertEquals("CXF in Action", new String(resource.getBookName()));

        // call another GET service
        Book book = resource.getBook(123);
        assertEquals("CXF in Action", book.getName());

        // call the POST service
        assertEquals(Long.valueOf(123), resource.echoBookId(123));
        
        // call the same POST service in the text mode
        //TODO add some way to control the client to switch between the bytes and text modes
        assertEquals(Long.valueOf(123), resource.echoBookId(123));

        // call the GET service returning a continous stream output
        //TODO there is no way to get the continuous stream at the moment
        //resource.getBookBought();

    }
    
    protected String getPort() {
        return PORT;
    }

}
