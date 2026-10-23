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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class HTTPConduitAllowedRedirectUriTest {

    private HttpServer redirectingServer;
    private HttpServer otherServer;
    private final AtomicReference<String> otherServerAuthorization = new AtomicReference<>();

    @After
    public void tearDown() {
        if (redirectingServer != null) {
            redirectingServer.stop(0);
        }
        if (otherServer != null) {
            otherServer.stop(0);
        }
    }

    private HttpServer startServer(String host) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(host), 0), 0);
        server.start();
        return server;
    }

    private void startServers() throws IOException {
        otherServer = startServer("127.0.0.1");
        otherServer.createContext("/", exchange -> {
            otherServerAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        redirectingServer = startServer("localhost");
    }

    private void redirectTo(String location) {
        redirectingServer.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", location);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
    }

    private void invoke(String allowedRedirectUri) throws IOException {
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(getRedirectingServerAddress() + "/service");

        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAutoRedirect(true);
        policy.setConnectionTimeout(5000);
        policy.setReceiveTimeout(5000);
        conduit.setClient(policy);
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("user");
        authPolicy.setPassword("secret");
        conduit.setAuthorization(authPolicy);
        conduit.finalizeConfig();
        conduit.setMessageObserver(m -> { });

        Message message = new MessageImpl();
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<String> contentTypes = new ArrayList<>();
        contentTypes.add("text/xml");
        headers.put("content-type", contentTypes);
        message.put(Message.PROTOCOL_HEADERS, headers);
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put("http.redirect.allowed.uri", allowedRedirectUri);
        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(message);
        message.setExchange(exchange);

        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);
        os.close();
    }

    private String getRedirectingServerAddress() {
        return "http://localhost:" + redirectingServer.getAddress().getPort();
    }

    private String getOtherServerAddress() {
        return "http://127.0.0.1:" + otherServer.getAddress().getPort();
    }

    @Test
    public void testRedirectToOtherHostViaUserInfoIsRejected() throws Exception {
        startServers();
        // http://localhost:<port>@127.0.0.1:<otherPort>/ starts with the allowed URI,
        // but its host is 127.0.0.1
        String allowed = getRedirectingServerAddress();
        redirectTo(allowed + "@" + getOtherServerAddress().substring("http://".length()) + "/");

        try {
            invoke(allowed);
            fail("Redirect to a different host must be rejected");
        } catch (IOException ex) {
            // expected
        }
        assertNull("Credentials must not be sent to the redirect target", otherServerAuthorization.get());
    }

    @Test
    public void testRedirectToAllowedUriIsFollowed() throws Exception {
        startServers();
        redirectTo(getOtherServerAddress() + "/service");
        invoke(getOtherServerAddress() + "/");
        assertEquals("Basic dXNlcjpzZWNyZXQ=", otherServerAuthorization.get());
    }
}
