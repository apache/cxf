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

package org.apache.cxf.systest.jaxrs.logging;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.Headers;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * CXF-8228: headers added to the HttpServletResponse by the application must be logged.
 */
public class LoggingServletHeadersTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(LoggingServletHeadersTest.class);
    static final BlockingQueue<LogEvent> RESPONSE_EVENTS = new LinkedBlockingQueue<>();

    public static class HeadersServer extends AbstractServerTestServerBase {
        @Override
        protected Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(HeadersResource.class);
            sf.setResourceProvider(HeadersResource.class,
                new SingletonResourceProvider(new HeadersResource(), false));
            sf.setAddress("http://localhost:" + PORT + "/");
            LoggingFeature logging = new LoggingFeature();
            logging.setSensitiveProtocolHeaderNames(new HashSet<>(Arrays.asList(
                "sEt-CoOkIe", "X-Servlet-Secret", "x-protocol-secret")));
            logging.setOutSender(event -> {
                if (event.getType() == EventType.RESP_OUT) {
                    RESPONSE_EVENTS.add(event);
                }
            });
            sf.getFeatures().add(logging);
            return sf.create();
        }
    }

    @Path("/headers")
    public static class HeadersResource {
        @Context
        private HttpServletResponse servletResponse;

        @GET
        @Path("/body")
        @Produces("text/plain")
        public Response body() {
            servletResponse.setHeader("X-Servlet-Header", "servlet");
            servletResponse.addHeader("Set-Cookie", "servlet=1");
            servletResponse.setHeader("x-servlet-secret", "servlet-secret-value");
            servletResponse.addHeader("Link", "<servlet>");
            return Response.ok("hello").header("X-Response-Header", "jaxrs").header("Set-Cookie", "jaxrs=1")
                .header("X-Protocol-Secret", "protocol-secret-value").header("Link", "<jaxrs>")
                .header("X-Multi", "a").header("X-Multi", "b").build();
        }

        @GET
        @Path("/empty")
        public Response empty() {
            servletResponse.setHeader("X-Servlet-Header", "servlet");
            servletResponse.addHeader("Set-Cookie", "servlet=1");
            servletResponse.setHeader("x-servlet-secret", "servlet-secret-value");
            servletResponse.addHeader("Link", "<servlet>");
            return Response.noContent().header("X-Response-Header", "jaxrs").header("Set-Cookie", "jaxrs=1")
                .header("X-Protocol-Secret", "protocol-secret-value").header("Link", "<jaxrs>")
                .header("X-Multi", "a").header("X-Multi", "b").build();
        }
    }

    @BeforeClass
    public static void startServers() {
        assertTrue("server did not launch correctly", launchServer(HeadersServer.class, true));
    }

    @Before
    public void clearEvents() {
        RESPONSE_EVENTS.clear();
    }

    @Test
    public void testServletHeadersLogged() throws Exception {
        assertHeadersLogged("/headers/body", 200);
    }

    @Test
    public void testServletHeadersLoggedWithoutBody() throws Exception {
        assertHeadersLogged("/headers/empty", 204);
    }

    @Test
    public void testTransportHeaderConstants() {
        HttpServletResponse response = (HttpServletResponse)Proxy.newProxyInstance(
            LoggingServletHeadersTest.class.getClassLoader(), new Class<?>[] {HttpServletResponse.class},
            (proxy, method, args) -> {
                if ("getHeaderNames".equals(method.getName())) {
                    return Collections.singleton("x-shared");
                } else if ("getHeaders".equals(method.getName())) {
                    return Collections.singletonList("servlet");
                }
                throw new UnsupportedOperationException(method.getName());
            });
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        exchange.put("org.apache.cxf.resource.operation.name", "test");
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        // Use the transport constants here to detect drift in the logging module's copies,
        // without adding an HTTP transport dependency to the logging module.
        message.put(AbstractHTTPDestination.HTTP_RESPONSE, response);
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Shared", Collections.singletonList("protocol")));
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        assertEquals("protocol", mapper.map(message).getHeaders().get("X-Shared"));

        message.put(Headers.ADD_HEADERS_PROPERTY, true);
        assertEquals("[servlet, protocol]", mapper.map(message).getHeaders().get("X-Shared"));

        message.remove(Headers.ADD_HEADERS_PROPERTY);
        message.put(AbstractHTTPDestination.RESPONSE_HEADERS_COPIED, "true");
        assertEquals("servlet", mapper.map(message).getHeaders().get("X-Shared"));
    }

    private static void assertHeadersLogged(String path, int status) throws Exception {
        Response response = WebClient.create("http://localhost:" + PORT + path).get();
        assertEquals(status, response.getStatus());
        assertEquals("servlet", response.getHeaderString("X-Servlet-Header"));
        assertEquals("jaxrs", response.getHeaderString("X-Response-Header"));
        assertEquals(Arrays.asList("servlet=1", "jaxrs=1"), response.getStringHeaders().get("Set-Cookie"));

        assertEquals("servlet-secret-value", response.getHeaderString("x-servlet-secret"));
        assertEquals("protocol-secret-value", response.getHeaderString("X-Protocol-Secret"));

        LogEvent event = RESPONSE_EVENTS.poll(5, TimeUnit.SECONDS);
        assertNotNull("no response was logged", event);
        Map<String, String> headers = event.getHeaders();
        assertEquals(headers.toString(), "jaxrs", headers.get("X-Response-Header"));
        assertEquals(headers.toString(), "servlet", headers.get("X-Servlet-Header"));
        assertEquals(headers.toString(), "[<servlet>, <jaxrs>]", headers.get("Link"));
        assertEquals("a,b", headers.get("X-Multi"));
        assertEquals("XXX", headers.get("Set-Cookie"));
        assertEquals("XXX", headers.get("x-servlet-secret"));
        assertEquals("XXX", headers.get("X-Protocol-Secret"));
        for (String value : headers.values()) {
            assertFalse(value.contains("servlet=1"));
            assertFalse(value.contains("jaxrs=1"));
            assertFalse(value.contains("servlet-secret-value"));
            assertFalse(value.contains("protocol-secret-value"));
        }
    }
}
