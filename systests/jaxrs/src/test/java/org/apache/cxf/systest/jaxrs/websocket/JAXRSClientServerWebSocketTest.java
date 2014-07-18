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

import java.util.List;
import java.util.UUID;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.websocket.WebSocketConstants;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerWebSocketTest extends AbstractBusClientServerTestBase {
    private static final String PORT = BookServerWebSocket.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(new BookServerWebSocket()));
        createStaticBus();
    }
    
    @Test
    public void testBookWithWebSocket() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the GET service
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/web/bookstore/booknames").getBytes());
            assertTrue("one book must be returned", wsclient.await(30000));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
            
            // call the same GET service in the text mode
            wsclient.reset(1);
            wsclient.sendTextMessage("GET " +  getContext() + "/websocket/web/bookstore/booknames");
            assertTrue("one book must be returned", wsclient.await(3));
            received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("CXF in Action", value);

            // call another GET service
            wsclient.reset(1);
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/web/bookstore/books/123").getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedResponses();
            resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("application/xml", resp.getContentType());
            value = resp.getTextEntity();
            assertTrue(value.startsWith("<?xml ") && value.endsWith("</Book>"));
            
            // call the POST service
            wsclient.reset(1);
            wsclient.sendMessage(
                ("POST " +  getContext() + "/websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n123")
                    .getBytes());
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedResponses();
            resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("123", value);
            
            // call the same POST service in the text mode 
            wsclient.reset(1);
            wsclient.sendTextMessage(
                "POST " +  getContext() + "/websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n123");
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedResponses();
            resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            value = resp.getTextEntity();
            assertEquals("123", value);

            // call the GET service returning a continous stream output
            wsclient.reset(6);
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/web/bookstore/bookbought").getBytes());
            assertTrue("response expected", wsclient.await(5));
            received = wsclient.getReceivedResponses();
            assertEquals(6, received.size());
            resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("application/octet-stream", resp.getContentType());
            value = resp.getTextEntity();
            assertTrue(value.startsWith("Today:"));
            for (int r = 2, i = 1; i < 6; r *= 2, i++) {
                // subsequent data should not carry the headers nor the status.
                resp = received.get(i);
                assertEquals(0, resp.getStatusCode());
                assertEquals(r, Integer.parseInt(resp.getTextEntity()));
            }
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testGetBookStream() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            wsclient.reset(5);
            wsclient.sendMessage(
                ("GET " +  getContext() + "/websocket/web/bookstore/bookstream\r\nAccept: application/json\r\n\r\n")
                .getBytes());
            assertTrue("response expected", wsclient.await(5));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(5, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("application/json", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals(value, getBookJson(1));
            for (int i = 2; i <= 5; i++) {
                // subsequent data should not carry the headers nor the status.
                resp = received.get(i - 1);
                assertEquals(0, resp.getStatusCode());
                assertEquals(resp.getTextEntity(), getBookJson(i));
            }
        } finally {
            wsclient.close();
        }
    }
    
    private String getBookJson(int index) {
        return "{\"Book\":{\"id\":" + index + ",\"name\":\"WebSocket" + index + "\"}}";
    }
    
    @Test
    public void testBookWithWebSocketAndHTTP() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the GET service
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/web/bookstore/booknames").getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<Object> received = wsclient.getReceived();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = new WebSocketTestClient.Response(received.get(0));
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
           
            testGetBookHTTPFromWebSocketEndpoint();
            
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testGetBookHTTPFromWebSocketEndpoint() throws Exception {
        String address = "http://localhost:" + getPort() + getContext() + "/websocket/web/bookstore/books/1";
        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals(1L, book.getId());
    }
    
    @Test
    public void testBookWithWebSocketServletStream() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/web/bookstore/booknames/servletstream")
                                 .getBytes());
            assertTrue("one book must be returned", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("CXF in Action", value);
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testWrongMethod() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the GET service using POST
            wsclient.reset(1);
            wsclient.sendMessage(("POST " +  getContext() + "/websocket/web/bookstore/booknames").getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(405, resp.getStatusCode());
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testPathRestriction() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the GET service over the different path
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/bookstore2").getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(400, resp.getStatusCode());
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testCallsWithIDReferences() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the POST service without requestId
            wsclient.sendTextMessage(
                "POST " +  getContext() + "/websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n\r\n459");
            assertTrue("response expected", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertEquals("459", value);
            String id = resp.getId();
            assertNull("response id is incorrect", id);
            
            // call the POST service twice with a unique requestId 
            wsclient.reset(2);
            String reqid1 = UUID.randomUUID().toString();
            String reqid2 = UUID.randomUUID().toString();
            wsclient.sendTextMessage(
                "POST " +  getContext() + "/websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n" 
                + WebSocketConstants.DEFAULT_REQUEST_ID_KEY + ": " + reqid1 + "\r\n\r\n549");
            wsclient.sendTextMessage(
                "POST " +  getContext() + "/websocket/web/bookstore/booksplain\r\nContent-Type: text/plain\r\n" 
                + WebSocketConstants.DEFAULT_REQUEST_ID_KEY + ": " + reqid2 + "\r\n\r\n495");
            assertTrue("response expected", wsclient.await(3));
            received = wsclient.getReceivedResponses();
            for (WebSocketTestClient.Response r : received) {
                assertEquals(200, r.getStatusCode());
                assertEquals("text/plain", r.getContentType());
                value = r.getTextEntity();
                id = r.getId();
                if (reqid1.equals(id)) {
                    assertEquals("549", value);
                } else if (reqid2.equals(id)) {
                    assertEquals("495", value);
                } else {
                    fail("unexpected responseId: " + id);
                }
            }
        } finally {
            wsclient.close();
        }
    }
    
    @Test
    public void testCallsInParallel() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        WebSocketTestClient wsclient = new WebSocketTestClient(address);
        wsclient.connect();
        try {
            // call the GET service that takes a long time to response
            wsclient.reset(2);
            wsclient.sendTextMessage(
                "GET " +  getContext() + "/websocket/web/bookstore/hold/3000");
            wsclient.sendTextMessage(
                "GET " +  getContext() + "/websocket/web/bookstore/hold/3000");
            // each call takes 3 seconds but executed in parallel, so waiting 4 secs is sufficient
            assertTrue("response expected", wsclient.await(4));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(2, received.size());
        } finally {
            wsclient.close();
        }
    }

    protected String getPort() {
        return PORT;
    }
    protected String getContext() {
        return "";
    }
}
