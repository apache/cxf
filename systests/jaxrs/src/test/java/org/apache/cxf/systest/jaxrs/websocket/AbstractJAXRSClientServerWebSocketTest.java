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
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.websocket.WebSocketConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract class AbstractJAXRSClientServerWebSocketTest extends AbstractBusClientServerTestBase {

    @Test
    public void testBookWithWebSocket() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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
        }
    }

    @Test
    public void testGetBookStream() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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
        }
    }

    @Test
    public void testGetBookStreamWithIDReferences() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
            wsclient.reset(5);
            String reqid = UUID.randomUUID().toString();
            wsclient.sendMessage(
                ("GET " +  getContext() + "/websocket/web/bookstore/bookstream\r\nAccept: application/json\r\n"
                    + WebSocketConstants.DEFAULT_REQUEST_ID_KEY + ": " + reqid + "\r\n\r\n")
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
                // subsequent data should not carry the status but the id header
                resp = received.get(i - 1);
                assertEquals(0, resp.getStatusCode());
                assertEquals(reqid, resp.getId());
                assertEquals(resp.getTextEntity(), getBookJson(i));
            }
        }
    }

    private String getBookJson(int index) {
        return "{\"Book\":{\"id\":" + index + ",\"name\":\"WebSocket" + index + "\"}}";
    }

    @Test
    public void testBookWithWebSocketAndHTTP() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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
        }
    }

    @Test
    public void testWrongMethod() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
            // call the GET service using POST
            wsclient.reset(1);
            wsclient.sendMessage(("POST " +  getContext() + "/websocket/web/bookstore/booknames").getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(405, resp.getStatusCode());
        }
    }

    @Test
    public void testPathRestriction() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
            // call the GET service over the different path
            wsclient.sendMessage(("GET " +  getContext() + "/websocket/bookstore2").getBytes());
            assertTrue("error response expected", wsclient.await(3));
            List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
            assertEquals(1, received.size());
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(400, resp.getStatusCode());
        }
    }

    @Test
    public void testCallsWithIDReferences() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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
        }
    }

    @Test
    public void testCallsInParallel() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient = new WebSocketTestClient(address)) {
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
        }
    }

    @Test
    public void testStreamRegisterAndUnregister() throws Exception {
        String address = "ws://localhost:" + getPort() + getContext() + "/websocket/web/bookstore";

        try (WebSocketTestClient wsclient1 = new WebSocketTestClient(address);
            WebSocketTestClient wsclient2 = new WebSocketTestClient(address)) {
            String regkey = UUID.randomUUID().toString();

            EventCreatorRunner runner = new EventCreatorRunner(wsclient2, regkey, 1000, 1000);
            new Thread(runner).start();

            // register for the event stream with requestId ane expect to get 2 messages
            wsclient1.reset(3);
            wsclient1.sendTextMessage(
                "GET " +  getContext() + "/websocket/web/bookstore/events/register\r\n"
                    + WebSocketConstants.DEFAULT_REQUEST_ID_KEY + ": " + regkey + "\r\n\r\n");
            assertFalse("only 2 responses expected", wsclient1.await(5));
            List<WebSocketTestClient.Response> received = wsclient1.getReceivedResponses();
            assertEquals(2, received.size());

            // the first response is the registration confirmation
            WebSocketTestClient.Response resp = received.get(0);
            assertEquals(200, resp.getStatusCode());
            assertEquals("text/plain", resp.getContentType());
            String value = resp.getTextEntity();
            assertTrue(value.startsWith("Registered " + regkey));
            String id = resp.getId();
            assertEquals("unexpected responseId", regkey, id);

            // the second response is the event news
            resp = received.get(1);
            assertEquals(0, resp.getStatusCode());
            value = resp.getTextEntity();
            assertEquals("News: event Hello created", value);
            id = resp.getId();
            assertEquals("unexpected responseId", regkey, id);

            String[] values = runner.getValues();
            assertTrue(runner.isCompleted());
            assertEquals("Hello created", values[0]);
            assertTrue(values[1].startsWith("Unregistered: " + regkey));
            assertEquals("Hola created", values[2]);
        }
    }

    private class EventCreatorRunner implements Runnable {
        private WebSocketTestClient wsclient;
        private String key;
        private long delay1;
        private long delay2;
        private String[] values = new String[3];
        private boolean completed;

        EventCreatorRunner(WebSocketTestClient wsclient, String key, long delay1, long delay2) {
            this.wsclient = wsclient;
            this.key = key;
            this.delay1 = delay1;
            this.delay2 = delay2;
        }

        public void run() {
            try {
                Thread.sleep(delay1);
                // creating an event and the event stream will see this event
                wsclient.sendTextMessage(
                    "GET " +  getContext() + "/websocket/web/bookstore/events/create/Hello\r\n\r\n");
                assertTrue("response expected", wsclient.await(3));
                List<WebSocketTestClient.Response> received = wsclient.getReceivedResponses();
                WebSocketTestClient.Response resp = received.get(0);
                values[0] = resp.getTextEntity();

                Thread.sleep(delay2);
                wsclient.reset(1);
                // unregistering the event stream
                wsclient.sendTextMessage(
                    "GET " +  getContext() + "/websocket/web/bookstore/events/unregister/" + key + "\r\n\r\n");
                assertTrue("response expected", wsclient.await(3));
                received = wsclient.getReceivedResponses();
                resp = received.get(0);
                values[1] = resp.getTextEntity();

                wsclient.reset(1);
                // creating another event and the event stream will not see this event
                wsclient.sendTextMessage(
                    "GET " +  getContext() + "/websocket/web/bookstore/events/create/Hola\r\n\r\n");
                assertTrue("response expected", wsclient.await(3));
                received = wsclient.getReceivedResponses();
                resp = received.get(0);
                values[2] = resp.getTextEntity();
            } catch (InterruptedException e) {
                // ignore
            } finally {
                completed = true;
            }
        }

        public String[] getValues() {
            return values;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    abstract String getPort();

    protected String getContext() {
        return "";
    }
}
