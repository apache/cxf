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

import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testCallsWithIDReferences() throws Exception {
        String address = "ws://localhost:" + getPort() + "/websocket";

        BookStoreWebSocket resource = JAXRSClientFactory.create(address, BookStoreWebSocket.class, null, true);
        Client client = WebClient.client(resource);
        client.header(HttpHeaders.USER_AGENT, JAXRSClientConduitWebSocketTest.class.getName());

        // call the POST service twice (a unique requestId is automatically included to correlate the response)
        EchoBookIdRunner[] runners = new EchoBookIdRunner[2];
        runners[0] = new EchoBookIdRunner(resource, 549);
        runners[1] = new EchoBookIdRunner(resource, 495);

        new Thread(runners[0]).start();
        new Thread(runners[1]).start();

        long timetowait = 5000;
        while (timetowait > 0) {
            if (runners[0].isCompleted() && runners[1].isCompleted()) {
                break;
            }
            Thread.sleep(500);
            timetowait -= 500;
        }
        assertEquals(Long.valueOf(549), runners[0].getValue());
        assertEquals(Long.valueOf(495), runners[1].getValue());
    }

    private static class EchoBookIdRunner implements Runnable {
        private BookStoreWebSocket resource;
        private long input;
        private Long value;
        private boolean completed;

        EchoBookIdRunner(BookStoreWebSocket resource, long input) {
            this.resource = resource;
            this.input = input;
        }

        public void run() {
            try {
                value = resource.echoBookId(input);
            } finally {
                completed = true;
            }
        }

        public Long getValue() {
            return value;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    protected String getPort() {
        return PORT;
    }

}
