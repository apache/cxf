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
import java.io.StringReader;
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

import com.sun.net.httpserver.HttpServer;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
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
import static org.junit.Assert.assertTrue;

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

    private static HttpServer server;
    private static ExecutorService executor;
    private static final List<String> RECEIVED = new CopyOnWriteArrayList<>();

    @BeforeClass
    public static void startServer() throws IOException {
        executor = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(PORT)), 0);
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
