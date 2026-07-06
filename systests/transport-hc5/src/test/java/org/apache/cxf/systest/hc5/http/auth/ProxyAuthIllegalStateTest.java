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
package org.apache.cxf.systest.hc5.http.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Reproducer for CXF-9226 (connection-close + retransmit variant): async
 * HC5 conduit throws IllegalStateException instead of HTTPException(407)
 * when the proxy returns 407, closes the TCP connection (Connection: close),
 * and the client has MaxRetransmits/AutoRedirect configured (as in the JIRA).
 *
 * <p>With MaxRetransmits &gt; 0 or AutoRedirect=true, CXF caches the request
 * body so HC5 marks the entity as repeatable. HC5 then retries after the
 * first 407, reconnects to the proxy, and sends a second request with the
 * (wrong) Basic credentials. While HC5 is reconnecting, a concurrent
 * write-failure on the closed socket calls discardEndpoint() which sets
 * endpointRef to null. When the retry then calls execute() →
 * ensureValid(), it finds endpointRef==null and throws
 * IllegalStateException("Endpoint not acquired / already released").
 *
 * <p>Without the CXF-9226 fix that ISE surfaces to the caller as an opaque
 * IOException; with the fix CXF checks the 407 stored in HttpClientContext
 * and throws HTTPException(407) instead.
 */
public class ProxyAuthIllegalStateTest
        extends AbstractBusClientServerTestBase {

    static final String PROXY_PORT =
        allocatePort(ProxyAuthIllegalStateTest.class);

    private static final QName SERVICE_NAME =
        new QName("http://apache.org/hello_world", "SOAPService");
    private static final QName PORT_NAME =
        new QName("http://apache.org/hello_world", "Mortimer");

    private static ServerSocket proxySocket;

    @BeforeClass
    public static void setup() throws Exception {
        proxySocket = new ServerSocket(Integer.parseInt(PROXY_PORT));
        final ServerSocket socket = proxySocket;
        Thread t = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    drainAndReject(socket.accept());
                } catch (IOException e) {
                    // socket closed; exit loop
                }
            }
        });
        t.setDaemon(true);
        t.start();
        createStaticBus();
    }

    private static void drainAndReject(Socket client) throws IOException {
        try (Socket s = client) {
            InputStream in = s.getInputStream();
            // Slide a 4-byte window; stop at \r\n\r\n (end of headers)
            int[] w = new int[]{-1, -1, -1, -1};
            int b;
            while ((b = in.read()) != -1) {
                System.arraycopy(w, 1, w, 0, 3);
                w[3] = b;
                if (w[0] == '\r' && w[1] == '\n'
                        && w[2] == '\r' && w[3] == '\n') {
                    break;
                }
            }
            OutputStream out = s.getOutputStream();
            out.write(("HTTP/1.1 407 Proxy Authentication Required\r\n"
                + "Proxy-Authenticate: Basic realm=\"test\"\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n"
                + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }
    }

    @AfterClass
    public static void teardown() {
        if (proxySocket != null) {
            try {
                proxySocket.close();
            } catch (IOException e) {
                // ignore
            }
            proxySocket = null;
        }
    }

    /**
     * CXF-9226: wrong proxy credentials must produce HTTPException(407),
     * not IllegalStateException, when the proxy closes the connection and
     * MaxRetransmits/AutoRedirect are configured (exact JIRA reproduction).
     */
    @Test(timeout = 30_000)
    public void testProxyAuthFailsWithHTTPExceptionNotIllegalState()
            throws Exception {
        Assume.assumeFalse("Skipped in forceURLConnection mode",
            Boolean.getBoolean(
                "org.apache.cxf.transport.http.forceURLConnection"));
        URL wsdl = getClass().getResource("../greeting.wsdl");
        assertNotNull("WSDL not found", wsdl);

        SOAPService service = new SOAPService(wsdl, SERVICE_NAME);
        Greeter greeter = service.getPort(PORT_NAME, Greeter.class);
        assertNotNull("Port is null", greeter);

        BindingProvider bp = (BindingProvider) greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "http://localhost:1/greeter");
        bp.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);

        HTTPConduit conduit = (HTTPConduit)
            ClientProxy.getClient(greeter).getConduit();
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(10_000L);
        policy.setReceiveTimeout(10_000L);
        policy.setProxyServer("localhost");
        policy.setProxyServerPort(Integer.parseInt(PROXY_PORT));
        // Match the exact JIRA configuration: MaxRetransmits and AutoRedirect
        // force CXF to cache the request body, which makes the entity
        // repeatable. HC5 then retries after the 407, and the concurrent
        // write-failure on the Connection: close socket can set endpointRef=null
        // before the retry's execute() call, triggering IllegalStateException.
        policy.setMaxRetransmits(5);
        policy.setAutoRedirect(true);
        conduit.setClient(policy);

        ProxyAuthorizationPolicy proxyAuth = new ProxyAuthorizationPolicy();
        proxyAuth.setUserName("unknown-user");
        proxyAuth.setPassword("wrong-password");
        proxyAuth.setAuthorizationType("Basic");
        conduit.setProxyAuthorization(proxyAuth);

        try {
            greeter.sayHi();
            fail("Expected exception for invalid proxy credentials");
        } catch (WebServiceException e) {
            Throwable cause = e.getCause();
            assertNotNull("Expected a cause on the WebServiceException", cause);
            if (!(cause instanceof HTTPException)) {
                fail("Expected HTTPException(407) but got "
                    + cause.getClass().getName() + ": " + cause.getMessage());
            }
            assertEquals("Expected 407 Proxy Authentication Required",
                407, ((HTTPException) cause).getResponseCode());
        }
    }
}
