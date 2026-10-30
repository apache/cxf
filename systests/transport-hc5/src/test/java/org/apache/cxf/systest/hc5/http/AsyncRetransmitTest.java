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

package org.apache.cxf.systest.hc5.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.io.CachedConstants;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * CXF-7232: an asynchronous call must not block the caller thread until the response is received,
 * when the request is cached to be retransmitted on a redirect or with MaxRetransmits.
 */
@RunWith(Parameterized.class)
public class AsyncRetransmitTest {
    public static final String PORT = TestUtil.getPortNumber(AsyncRetransmitTest.class);
    private static final QName SERVICE = new QName("urn:cxf7232", "EchoService");
    private static final QName PORT_NAME = new QName("urn:cxf7232", "EchoPort");
    private static final String REQUEST = "<echo xmlns=\"urn:cxf7232\">hello</echo>";
    private static final String RESPONSE =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
        + "<echoResponse xmlns=\"urn:cxf7232\">hello</echoResponse></soap:Body></soap:Envelope>";

    private static HttpServer server;
    private static ExecutorService executor;
    private static final AtomicInteger REDIRECTS = new AtomicInteger();
    private static final AtomicBoolean RESPONDED = new AtomicBoolean();
    private static volatile CountDownLatch release;
    private static final BlockingQueue<byte[]> REQUESTS = new LinkedBlockingQueue<>();
    private static final AtomicInteger CHALLENGES = new AtomicInteger();

    @Rule
    public TemporaryFolder cacheFolder = new TemporaryFolder();

    private final String conduit;

    public AsyncRetransmitTest(String conduit) {
        this.conduit = conduit;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"HttpClient"}, {"URLConnection"}, {"hc5"}});
    }

    @BeforeClass
    public static void startServer() throws IOException {
        executor = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(PORT)), 0);
        server.setExecutor(executor);
        // redirects at once
        server.createContext("/redirect", ex -> {
            captureRequest(ex);
            REDIRECTS.incrementAndGet();
            ex.getResponseHeaders().add("Location", address("/echo"));
            ex.sendResponseHeaders(307, -1);
            ex.close();
        });
        // answers when the test releases it
        server.createContext("/echo", ex -> {
            captureRequest(ex);
            awaitRelease();
            respond(ex);
        });
        server.createContext("/auth", ex -> {
            captureRequest(ex);
            awaitRelease();
            if ("Basic dXNlcjpwYXNz".equals(ex.getRequestHeaders().getFirst("Authorization"))) {
                respond(ex);
            } else {
                CHALLENGES.incrementAndGet();
                RESPONDED.set(true);
                ex.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"cxf7232\"");
                ex.sendResponseHeaders(401, -1);
                ex.close();
            }
        });
        server.createContext("/loop", ex -> {
            captureRequest(ex);
            awaitRelease();
            RESPONDED.set(true);
            ex.getResponseHeaders().add("Location", address("/loop"));
            ex.sendResponseHeaders(307, -1);
            ex.close();
        });
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Before
    public void reset() {
        REDIRECTS.set(0);
        CHALLENGES.set(0);
        REQUESTS.clear();
        RESPONDED.set(false);
        release = new CountDownLatch(1);
    }

    @After
    public void releaseServer() {
        release.countDown();
    }

    @Test
    public void testAutoRedirect() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAutoRedirect(true);
        invokeAsync("/redirect", policy);
        assertEquals(1, REDIRECTS.get());
    }

    @Test
    public void testAutoRedirectWithoutRedirect() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAutoRedirect(true);
        invokeAsync("/echo", policy);
        assertEquals(0, REDIRECTS.get());
    }

    @Test
    public void testMaxRetransmits() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setMaxRetransmits(1);
        invokeAsync("/echo", policy);
        assertEquals(0, REDIRECTS.get());
    }

    @Test
    public void testRedirectWithDiskCachedRequest() throws Exception {
        String payload = "<echo xmlns=\"urn:cxf7232\">" + "0123456789abcdef".repeat(16384) + "</echo>";
        File directory = cacheFolder.newFolder("request-cache");
        Bus bus = BusFactory.getThreadDefaultBus();
        Object oldDirectory = bus.getProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP);
        Object oldThreshold = bus.getProperty(CachedConstants.THRESHOLD_BUS_PROP);
        bus.setProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP, directory.getAbsolutePath());
        bus.setProperty(CachedConstants.THRESHOLD_BUS_PROP, "1024");
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAutoRedirect(true);
        Dispatch<Source> dispatch = createDispatch("/redirect", policy);
        try {
            Response<Source> response = dispatch.invokeAsync(new StreamSource(new StringReader(payload)));
            assertFalse("The caller thread waited for the response", RESPONDED.get());
            File[] files = directory.listFiles();
            assertNotNull(files);
            assertTrue("The request was not cached on disk", files.length > 0);
            release.countDown();
            assertResponse(response);
            assertRequests(payload, 2);
            assertEquals(1, REDIRECTS.get());
        } finally {
            release.countDown();
            destroy(dispatch);
            bus.setProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP, oldDirectory);
            bus.setProperty(CachedConstants.THRESHOLD_BUS_PROP, oldThreshold);
        }
    }

    @Test
    public void testAuthenticationChallenge() throws Exception {
        Dispatch<Source> dispatch = createDispatch("/auth", new HTTPClientPolicy());
        AtomicInteger authorizations = new AtomicInteger();
        ((HTTPConduit)((DispatchImpl<?>)dispatch).getClient().getConduit()).setAuthSupplier(new HttpAuthSupplier() {
            @Override
            public boolean requiresRequestCaching() {
                return true;
            }

            @Override
            public String getAuthorization(AuthorizationPolicy policy, URI uri, Message message, String header) {
                if (header == null) {
                    return null;
                }
                authorizations.incrementAndGet();
                return "Basic dXNlcjpwYXNz";
            }
        });
        try {
            assertResponse(invokeAsync(dispatch, REQUEST));
            assertRequests(REQUEST, 2);
            assertEquals(1, CHALLENGES.get());
            assertEquals(1, authorizations.get());
        } finally {
            destroy(dispatch);
        }
    }

    @Test
    public void testRedirectLoopCompletesExceptionally() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAutoRedirect(true);
        Dispatch<Source> dispatch = createDispatch("/loop", policy);
        try {
            Response<Source> response = invokeAsync(dispatch, REQUEST);
            try {
                response.get(30, TimeUnit.SECONDS);
                fail("A redirect loop must fail the asynchronous response");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                while (cause != null && !(cause instanceof IOException)) {
                    cause = cause.getCause();
                }
                assertNotNull("Expected an IOException for the redirect loop", cause);
                assertTrue(cause.getMessage(), cause.getMessage().contains("Redirect loop"));
            }
        } finally {
            destroy(dispatch);
        }
    }

    private void invokeAsync(String path, HTTPClientPolicy policy) throws Exception {
        Dispatch<Source> dispatch = createDispatch(path, policy);
        try {
            assertResponse(invokeAsync(dispatch, REQUEST));
            assertRequests(REQUEST, "/redirect".equals(path) ? 2 : 1);
        } finally {
            destroy(dispatch);
        }
    }

    private Dispatch<Source> createDispatch(String path, HTTPClientPolicy policy) {
        Service service = Service.create(SERVICE);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, address(path));
        Dispatch<Source> dispatch = service.createDispatch(PORT_NAME, Source.class, Service.Mode.PAYLOAD);
        dispatch.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, "hc5".equals(conduit));
        dispatch.getRequestContext().put("force.urlconnection.http.conduit", "URLConnection".equals(conduit));
        policy.setReceiveTimeout(30000);
        ((HTTPConduit)((DispatchImpl<?>)dispatch).getClient().getConduit()).setClient(policy);
        return dispatch;
    }

    private static Response<Source> invokeAsync(Dispatch<Source> dispatch, String payload) {
        Response<Source> response = dispatch.invokeAsync(new StreamSource(new StringReader(payload)));
        assertFalse("The caller thread waited for the response", RESPONDED.get());
        release.countDown();
        return response;
    }

    private static void assertResponse(Response<Source> response) throws Exception {
        String result = StaxUtils.toString(response.get(30, TimeUnit.SECONDS));
        assertTrue(result, result.contains(">hello<"));
    }

    private static void assertRequests(String payload, int count) throws Exception {
        byte[] original = REQUESTS.poll(5, TimeUnit.SECONDS);
        assertNotNull("No request received", original);
        String expected = StaxUtils.read(new StringReader(payload)).getDocumentElement().getTextContent();
        assertEquals(expected, StaxUtils.read(new ByteArrayInputStream(original))
            .getElementsByTagNameNS("urn:cxf7232", "echo").item(0).getTextContent());
        for (int i = 1; i < count; i++) {
            byte[] replay = REQUESTS.poll(5, TimeUnit.SECONDS);
            assertNotNull("No retransmitted request received", replay);
            assertArrayEquals("Retransmission changed the request body", original, replay);
        }
        assertTrue("Unexpected extra request", REQUESTS.isEmpty());
    }

    private static void destroy(Dispatch<Source> dispatch) {
        ((DispatchImpl<?>)dispatch).getClient().destroy();
    }

    private static String address(String path) {
        return "http://localhost:" + PORT + path;
    }

    private static void awaitRelease() {
        try {
            release.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void respond(HttpExchange ex) throws IOException {
        RESPONDED.set(true);
        byte[] body = RESPONSE.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/xml; charset=UTF-8");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static void captureRequest(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            REQUESTS.add(is.readAllBytes());
        }
    }
}
