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

package org.apache.cxf.systest.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * CXF-9135: an asynchronous call through the JDK HttpClient conduit with chunking disallowed must send
 * its whole body with a Content-Length, although the caller closes the request before the HttpClient
 * reads it.
 */
public class HttpClientAsyncNoChunkingTest {
    public static final String PORT = TestUtil.getPortNumber(HttpClientAsyncNoChunkingTest.class);
    private static final QName SERVICE = new QName("urn:cxf9135", "EchoService");
    private static final QName PORT_NAME = new QName("urn:cxf9135", "EchoPort");
    private static final String REQUEST = "<echo xmlns=\"urn:cxf9135\">hello</echo>";
    private static final String RESPONSE =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
        + "<echoResponse xmlns=\"urn:cxf9135\">hello</echoResponse></soap:Body></soap:Envelope>";
    private static final int CALLS = 10;

    // the JDK server, not the HttpServer of this package
    private static com.sun.net.httpserver.HttpServer server;
    private static ExecutorService executor;
    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @BeforeClass
    public static void startServer() throws IOException {
        executor = Executors.newCachedThreadPool();
        server = com.sun.net.httpserver.HttpServer.create(
            new InetSocketAddress("localhost", Integer.parseInt(PORT)), 0);
        server.setExecutor(executor);
        // records how each request was framed and how much body it carried
        server.createContext("/echo", ex -> {
            byte[] body;
            try (InputStream is = ex.getRequestBody()) {
                body = is.readAllBytes();
            }
            RECEIVED.add("Content-Length=" + ex.getRequestHeaders().getFirst("Content-Length")
                + ", Transfer-Encoding=" + ex.getRequestHeaders().getFirst("Transfer-Encoding")
                + ", body=" + new String(body, StandardCharsets.UTF_8).contains(REQUEST));
            byte[] response = RESPONSE.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/xml; charset=UTF-8");
            ex.sendResponseHeaders(200, response.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(response);
            }
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
        RECEIVED.clear();
    }

    @Test
    public void testAsyncWithoutChunking() throws Exception {
        Dispatch<Source> dispatch = createDispatch();
        try {
            for (int i = 0; i < CALLS; i++) {
                Source response = dispatch.invokeAsync(new StreamSource(new StringReader(REQUEST)))
                    .get(30, TimeUnit.SECONDS);
                String result = StaxUtils.toString(response);
                assertTrue(result, result.contains(">hello<"));
            }
        } finally {
            ((DispatchImpl<?>)dispatch).getClient().destroy();
        }
        assertEquals(CALLS, RECEIVED.size());
        for (String received : RECEIVED) {
            assertFalse(received, received.startsWith("Content-Length=null"));
            assertTrue(received, received.endsWith("Transfer-Encoding=null, body=true"));
        }
    }

    /**
     * The request is sent on the first write, and the HttpClient waits up to the connection timeout for
     * the rest of the body to know its length. A body written more slowly fails the call, rather than
     * sending a Content-Length which does not match the body.
     */
    @Test
    public void testWithoutChunkingFailsWhenTheBodyIsWrittenTooSlowly() throws Exception {
        // the URLConnection conduit buffers the whole body before it sends the request
        assumeFalse(Boolean.getBoolean("org.apache.cxf.transport.http.forceURLConnection"));
        Dispatch<Source> dispatch = createDispatch();
        ((HTTPConduit)((DispatchImpl<?>)dispatch).getClient().getConduit()).getClient()
            .setConnectionTimeout(500);
        try {
            WebServiceException ex = assertThrows(WebServiceException.class,
                () -> dispatch.invoke(new StreamSource(new SlowReader())));
            assertTrue(stackTraceOf(ex), stackTraceOf(ex).contains("The request body was not written within 500 ms"));
        } finally {
            ((DispatchImpl<?>)dispatch).getClient().destroy();
        }
        assertTrue(RECEIVED.toString(), RECEIVED.isEmpty());
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // A request larger than the writers' buffers, so the request is sent before it is fully written,
    // which pauses for longer than the connection timeout after it started
    private static final class SlowReader extends Reader {
        private final Reader reader = new StringReader("<echo xmlns=\"urn:cxf9135\">"
            + "x".repeat(256 * 1024) + "</echo>");
        private int read;
        private boolean paused;

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (!paused && read >= 128 * 1024) {
                paused = true;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
            int n = reader.read(cbuf, off, Math.min(len, 8192));
            if (n > 0) {
                read += n;
            }
            return n;
        }

        @Override
        public void close() {
        }
    }

    private static Dispatch<Source> createDispatch() {
        Service service = Service.create(SERVICE);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING,
            "http://localhost:" + PORT + "/echo");
        Dispatch<Source> dispatch = service.createDispatch(PORT_NAME, Source.class, Service.Mode.PAYLOAD);
        // use the JDK HttpClient conduit, which is the default when no async conduit is available
        dispatch.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, false);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setAllowChunking(false);
        policy.setReceiveTimeout(30000);
        ((HTTPConduit)((DispatchImpl<?>)dispatch).getClient().getConduit()).setClient(policy);
        return dispatch;
    }
}
