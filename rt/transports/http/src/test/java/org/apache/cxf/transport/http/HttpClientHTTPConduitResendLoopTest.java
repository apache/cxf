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

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * CXF-9250: a failed exchange for a request without a body (e.g. a GET request,
 * such as fetching a remote WSDL, whose connection attempt or TLS handshake
 * never succeeds) must be reported once, not endlessly re-sent in the background
 * from the CompletableFuture's exceptionally() callback of HttpClientHTTPConduit.
 *
 * This reproduces the scenario described by the issue reporter: a raw server
 * socket that accepts every connection and immediately drops it, so the
 * HttpClient never completes an exchange.
 */
public class HttpClientHTTPConduitResendLoopTest {

    private ServerSocket serverSocket;
    private volatile boolean stopped;
    private final AtomicInteger connectionAttempts = new AtomicInteger();

    @After
    public void tearDown() throws IOException {
        stopped = true;
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private int startFailingServer() throws IOException {
        serverSocket = new ServerSocket(0, 50, InetAddress.getByName("localhost"));
        Thread t = new Thread(() -> {
            while (!stopped) {
                try {
                    Socket s = serverSocket.accept();
                    connectionAttempts.incrementAndGet();
                    // Simulate a connection / TLS handshake failure: accept and
                    // immediately drop the connection without ever completing
                    // the HTTP exchange.
                    s.close();
                } catch (IOException e) {
                    // expected once the server socket is closed in tearDown()
                    break;
                }
            }
        });
        t.setDaemon(true);
        t.setName("CXF-9250-failing-server");
        t.start();
        return serverSocket.getLocalPort();
    }

    private Message getNewMessage() {
        Message message = new MessageImpl();
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String> contentTypes = new ArrayList<>();
        contentTypes.add("text/xml");
        headers.put("content-type", contentTypes);
        message.put(Message.PROTOCOL_HEADERS, headers);
        return message;
    }

    @Test
    public void testFailedGetIsNotResentEndlessly() throws Exception {
        int port = startFailingServer();

        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://localhost:" + port + "/");

        HttpClientHTTPConduit conduit = new HttpClientHTTPConduit(bus, ei, null);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(2000);
        policy.setReceiveTimeout(2000);
        conduit.setClient(policy);
        conduit.finalizeConfig();

        Message message = getNewMessage();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");

        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);

        try {
            os.close();
            fail("Expected an IOException to be reported since the server drops every connection");
        } catch (IOException expected) {
            // expected: the failed exchange must be reported to the caller
        }

        // Give a resend loop, if any, a chance to run for a while before asserting on it.
        // Without the fix, hundreds of connection attempts would happen well within this window.
        Thread.sleep(3000);

        int attempts = connectionAttempts.get();
        assertTrue("Expected only a small, bounded number of connection attempts, but the server "
                   + "observed " + attempts + " - the failed request appears to have been "
                   + "resent in a loop from the async failure callback (CXF-9250)",
                   attempts <= 5);
    }
}
