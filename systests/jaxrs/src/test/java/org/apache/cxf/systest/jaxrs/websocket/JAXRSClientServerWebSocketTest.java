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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerWebSocketTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerWebSocket.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        final Map< String, Object > properties = new HashMap< String, Object >();        
        properties.put("enableWebSocket", "true");

        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new BookServerWebSocket(properties)));
        createStaticBus();
    }
        
    @Test
    public void testBookWithWebSocket() throws Exception {
        String address = "ws://localhost:" + PORT + "/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address, 1);
        wsclient.connect();
        try {
            // call the GET service
            wsclient.sendMessage("GET /web/bookstore/booknames".getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<byte[]> received = wsclient.getReceivedBytes();
            assertEquals(1, received.size());
            String value = new String(received.get(0));
            assertEquals("CXF in Action", value);

            // call another GET service
            wsclient.reset(1);
            wsclient.sendMessage("GET /web/bookstore/books/123".getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedBytes();
            value = new String(received.get(0));
            assertTrue(value.startsWith("<?xml ") && value.endsWith("</Book>"));
            
            // call the GET service using POST
            wsclient.reset(1);
            wsclient.sendMessage("POST /web/bookstore/booknames\r\n\r\n123".getBytes());
            assertFalse("wrong method, no response expected", wsclient.await(3));
            
            // call the POST service
            wsclient.reset(1);
            wsclient.sendMessage("POST /web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n123".getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedBytes();
            value = new String(received.get(0));
            assertEquals("123", value);
            
            // call the GET service returning a continous stream output
            wsclient.reset(6);
            wsclient.sendMessage("GET /web/bookstore/bookbought".getBytes());
            assertTrue("wrong method, no response expected", wsclient.await(5));
            received = wsclient.getReceivedBytes();
            assertEquals(6, received.size());
            assertTrue((new String(received.get(0))).startsWith("Today:"));
            for (int r = 2, i = 1; i < 6; r *= 2, i++) {
                assertEquals(r, Integer.parseInt(new String(received.get(i))));
            }
        } finally {
            wsclient.close();
        }
    }
}
