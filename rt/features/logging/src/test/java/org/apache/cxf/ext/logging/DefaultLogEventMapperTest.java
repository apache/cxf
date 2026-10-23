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
package org.apache.cxf.ext.logging;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.Subject;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.apache.cxf.ext.logging.event.DefaultLogEventMapper.MASKED_HEADER_VALUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

public class DefaultLogEventMapperTest {

    public static final String TEST_HEADER_VALUE = "TestValue";
    public static final String TEST_HEADER_NAME = "TestHeader";

    @Test
    public void testRest() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEvent event = mapper.map(message, Collections.emptySet());
        assertEquals("GET[test]", event.getOperationName());
    }

    @Test
    public void testPreflightRequestEventType() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "OPTIONS");
        message.put(Message.REQUEST_URI, "test");
        message.put(Message.RESPONSE_CODE, 200);
        Exchange exchange = new ExchangeImpl();
        // operation name not included
        exchange.put("org.apache.cxf.rs.security.cors.CrossOriginResourceSharingFilter", "preflight_passed");
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        LogEvent event = mapper.map(message, Collections.emptySet());
        assertEquals(EventType.RESP_OUT, event.getType());
    }

    /**
     * Test for NPE described in CXF-6436
     */
    @Test
    public void testNullValues() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, null);
        message.put(Message.REQUEST_URI, null);
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEvent event = mapper.map(message, Collections.emptySet());
        assertEquals("", event.getOperationName());
    }

    /**
     * Test for address concatenation in CXF-8127
     */
    @Test
    public void testUriValue() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        LogEvent event = mapper.map(message, Collections.emptySet());
        assertEquals("http://localhost:9001/api", event.getAddress());
    }

    @Test
    public void shouldMaskHeaders() {
        final DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        final Message message = new MessageImpl();
        message.put(Message.ENDPOINT_ADDRESS, "http://localhost:9001/");
        message.put(Message.REQUEST_URI, "/api");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        final Map<String, Object> headers = new HashMap<>();
        headers.put(TEST_HEADER_NAME, Arrays.asList(TEST_HEADER_VALUE));
        final Set<String> sensitiveHeaders = new HashSet<>();
        sensitiveHeaders.add(TEST_HEADER_NAME);
        message.put(Message.PROTOCOL_HEADERS, headers);

        LogEvent event = mapper.map(message, sensitiveHeaders);
        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get(TEST_HEADER_NAME));
    }

    @Test
    public void testMapNullSensitiveProtocolHeaders() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "POST");
        message.put(Message.REQUEST_URI, "nullTest");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        LogEvent event = mapper.map(message, null);

        assertEquals("POST[nullTest]", event.getOperationName());
    }

    @Test
    public void testMap() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "PUT");
        message.put(Message.REQUEST_URI, "test");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        LogEvent event = mapper.map(message);

        assertEquals("PUT[test]", event.getOperationName());
    }

    @Test
    public void testMultiplePrincipalsReturnedByAccessControllerContext() {
        assumeThat("Subject::getSubject is not supported in JDK-23 and above",
            Runtime.version().feature(), lessThanOrEqualTo(22));
        
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        Set<Principal> principals = IntStream.range(0, 3)
                .mapToObj(i -> new SimplePrincipal("principal-" + i))
                .collect(Collectors.toSet());

        Subject subject = new Subject(false, principals, Set.of(), Set.of());

        LogEvent event = Subject.doAs(subject, (PrivilegedAction<LogEvent>) () -> mapper.map(message));
        String[] splitPrincipals = event.getPrincipal().split(",");
        Set<String> expected = Set.of("principal-0", "principal-1", "principal-2");

        assertEquals(expected, Arrays.stream(splitPrincipals).collect(Collectors.toSet()));
    }

    @Test
    public void testNoSubjectReturned() {
        assumeThat("Subject::getSubject is not supported in JDK-23 and above",
            Runtime.version().feature(), greaterThanOrEqualTo(23));
        
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        Set<Principal> principals = IntStream.range(0, 3)
                .mapToObj(i -> new SimplePrincipal("principal-" + i))
                .collect(Collectors.toSet());

        Subject subject = new Subject(false, principals, Set.of(), Set.of());

        LogEvent event = Subject.doAs(subject, (PrivilegedAction<LogEvent>) () -> mapper.map(message));
        assertThat(event.getPrincipal(), is(nullValue()));
    }

    @Test
    public void testServletResponseHeaders() {
        Map<String, List<String>> servletHeaders = new LinkedHashMap<>();
        servletHeaders.put("X-Servlet-Header", Collections.singletonList("servlet"));
        servletHeaders.put("X-Multi", Arrays.asList("a", "b"));
        servletHeaders.put("x-protocol-header", Collections.singletonList("copied"));
        servletHeaders.put("Set-Cookie", Collections.singletonList("secret"));
        Map<String, List<String>> protocolHeaders = new HashMap<>();
        protocolHeaders.put("X-Protocol-Header", Collections.singletonList("protocol"));

        Message message = createServerOutMessage(servletHeaders, protocolHeaders);
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Set-Cookie"));

        assertEquals(EventType.RESP_OUT, event.getType());
        Map<String, String> expected = new HashMap<>();
        expected.put("X-Servlet-Header", "servlet");
        expected.put("X-Multi", "a,b");
        expected.put("X-Protocol-Header", "protocol");
        expected.put("Set-Cookie", MASKED_HEADER_VALUE);
        assertEquals(expected, event.getHeaders());
    }

    @Test
    public void testServletResponseHeadersNotLoggedForRequest() {
        Message message = createServerOutMessage(
            Collections.singletonMap("X-Servlet-Header", Collections.singletonList("servlet")), null);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(message.getExchange());
        inMessage.put("HTTP.RESPONSE", message.get("HTTP.RESPONSE"));

        LogEvent event = new DefaultLogEventMapper().map(inMessage, Collections.emptySet());

        assertEquals(EventType.REQ_IN, event.getType());
        assertEquals(Collections.emptyMap(), event.getHeaders());
    }

    @Test
    public void testServletHeaderMaskingIgnoresCase() {
        Message message = createServerOutMessage(
            Collections.singletonMap("set-cookie", Collections.singletonList("secret")),
            Collections.singletonMap("AUTHORIZATION", Collections.singletonList("token")));
        LogEvent event = new DefaultLogEventMapper().map(message,
            new HashSet<>(Arrays.asList("Set-Cookie", "Authorization")));
        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get("set-cookie"));
        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get("AUTHORIZATION"));
    }

    @Test
    public void testSharedServletHeadersBeforeCopying() {
        Map<String, List<String>> servletHeaders = new LinkedHashMap<>();
        servletHeaders.put("set-cookie", Arrays.asList("servlet=1", "servlet=2"));
        servletHeaders.put("link", Collections.singletonList("<servlet>"));
        servletHeaders.put("x-shared", Collections.singletonList("servlet"));
        Map<String, List<String>> protocolHeaders = new LinkedHashMap<>();
        protocolHeaders.put("Set-Cookie", Collections.singletonList("jaxrs=1"));
        protocolHeaders.put("Link", Collections.singletonList("<jaxrs>"));
        protocolHeaders.put("X-Shared", Collections.singletonList("jaxrs"));
        Message message = createServerOutMessage(servletHeaders, protocolHeaders);
        Map<String, String> headers = new DefaultLogEventMapper().map(message).getHeaders();
        assertEquals("[servlet=1, servlet=2, jaxrs=1]", headers.get("Set-Cookie"));
        assertEquals("[<servlet>, <jaxrs>]", headers.get("Link"));
        assertEquals("jaxrs", headers.get("X-Shared"));

        message.put("org.apache.cxf.http.add-headers", true);
        headers = new DefaultLogEventMapper().map(message).getHeaders();
        assertEquals("[servlet, jaxrs]", headers.get("X-Shared"));
    }

    @Test
    public void testSharedServletHeadersAfterCopying() {
        Map<String, List<String>> servletHeaders = new LinkedHashMap<>();
        servletHeaders.put("set-cookie", Arrays.asList("servlet=1", "jaxrs=1"));
        servletHeaders.put("x-shared", Collections.singletonList("actual"));
        Map<String, List<String>> protocolHeaders = new LinkedHashMap<>();
        protocolHeaders.put("Set-Cookie", Collections.singletonList("jaxrs=1"));
        protocolHeaders.put("X-Shared", Collections.singletonList("protocol"));
        Message message = createServerOutMessage(servletHeaders, protocolHeaders);
        message.put("http.headers.copied", "true");
        Map<String, String> headers = new DefaultLogEventMapper().map(message).getHeaders();
        assertEquals("[servlet=1, jaxrs=1]", headers.get("Set-Cookie"));
        assertEquals("actual", headers.get("X-Shared"));
    }

    @Test
    public void testMultiValueFormattingBeforeAndAfterCopying() {
        Map<String, List<String>> protocolHeaders = Collections.singletonMap("X-Multi", Arrays.asList("a", "b,c"));
        Message message = createServerOutMessage(Collections.emptyMap(), protocolHeaders);
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        assertEquals("a,b,c", mapper.map(message).getHeaders().get("X-Multi"));

        Message copied = createServerOutMessage(
            Collections.singletonMap("x-multi", Collections.singletonList("a,b,c")), protocolHeaders);
        copied.put("http.headers.copied", "true");
        assertEquals(mapper.map(message).getHeaders(), mapper.map(copied).getHeaders());

        message.put("org.apache.cxf.http.add-headers", true);
        Message appended = createServerOutMessage(
            Collections.singletonMap("x-multi", Arrays.asList("a", "b,c")), protocolHeaders);
        appended.put("http.headers.copied", "true");
        appended.put("org.apache.cxf.http.add-headers", true);
        assertEquals("[a, b,c]", mapper.map(message).getHeaders().get("X-Multi"));
        assertEquals(mapper.map(message).getHeaders(), mapper.map(appended).getHeaders());
    }

    @Test
    public void testLateServletHelperLinkageFailureKeepsMaskedProtocolHeaders() throws Exception {
        ClassLoader loader = new ClassLoader(DefaultLogEventMapper.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("org.apache.cxf.ext.logging.event.HttpServletResponseHeaders")) {
                    throw new NoClassDefFoundError("Servlet helper unavailable");
                }
                if (!name.equals(DefaultLogEventMapper.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        String resource = name.replace('.', '/') + ".class";
                        try (InputStream in = getParent().getResourceAsStream(resource)) {
                            byte[] bytes = in.readAllBytes();
                            type = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException ex) {
                            throw new ClassNotFoundException(name, ex);
                        }
                    }
                    if (resolve) {
                        resolveClass(type);
                    }
                    return type;
                }
            }
        };
        Class<?> mapperClass = loader.loadClass(DefaultLogEventMapper.class.getName());
        Object mapper = mapperClass.getConstructor().newInstance();
        Message message = createServerOutMessage(Collections.emptyMap(),
            Collections.singletonMap("AUTHORIZATION", Collections.singletonList("secret")));
        LogEvent event = (LogEvent)mapperClass.getMethod("map", Message.class, Set.class)
            .invoke(mapper, message, Collections.singleton("Authorization"));
        assertEquals(Collections.singletonMap("AUTHORIZATION", MASKED_HEADER_VALUE), event.getHeaders());
    }

    @Test
    public void testUnsupportedServletHeaderNamesKeepsMaskedProtocolHeaders() {
        Message message = createServerOutMessage(Collections.emptyMap(),
            Collections.singletonMap("AUTHORIZATION", Collections.singletonList("secret")));
        message.put("HTTP.RESPONSE", new HttpServletResponseWrapper((HttpServletResponse)message.get("HTTP.RESPONSE")) {
            @Override
            public Collection<String> getHeaderNames() {
                throw new UnsupportedOperationException("Header names unavailable");
            }
        });
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Authorization"));
        assertEquals(Collections.singletonMap("AUTHORIZATION", MASKED_HEADER_VALUE), event.getHeaders());
    }

    @Test
    public void testRecycledServletHeadersDiscardsPartialMerge() {
        Map<String, List<String>> servletHeaders = new LinkedHashMap<>();
        servletHeaders.put("x-shared", Collections.singletonList("changed"));
        servletHeaders.put("X-Servlet-Only", Collections.singletonList("partial"));
        servletHeaders.put("X-Failing", Collections.singletonList("unavailable"));
        Map<String, List<String>> protocolHeaders = new LinkedHashMap<>();
        protocolHeaders.put("X-Shared", Collections.singletonList("original"));
        protocolHeaders.put("AUTHORIZATION", Collections.singletonList("secret"));
        Message message = createServerOutMessage(servletHeaders, protocolHeaders);
        message.put("http.headers.copied", "true");
        message.put("HTTP.RESPONSE", new HttpServletResponseWrapper((HttpServletResponse)message.get("HTTP.RESPONSE")) {
            @Override
            public Collection<String> getHeaders(String name) {
                if ("X-Failing".equals(name)) {
                    throw new IllegalStateException("Response recycled");
                }
                return super.getHeaders(name);
            }
        });
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Authorization"));
        Map<String, String> expected = new HashMap<>();
        expected.put("X-Shared", "original");
        expected.put("AUTHORIZATION", MASKED_HEADER_VALUE);
        assertEquals(expected, event.getHeaders());
    }

    @Test
    public void testMaskSensitiveHeadersWithNullKeysAndNames() {
        MaskSensitiveHelper helper = new MaskSensitiveHelper();
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(null, "HTTP/1.1 200 OK");
        headerMap.put("Authorization", "Bearer secret");
        headerMap.put("Content-Type", "text/plain");

        Set<String> sensitiveNames = new HashSet<>(Arrays.asList(null, "authorization"));
        helper.maskHeaders(headerMap, sensitiveNames);

        assertEquals("HTTP/1.1 200 OK", headerMap.get(null));
        assertEquals(MASKED_HEADER_VALUE, headerMap.get("Authorization"));
        assertEquals("text/plain", headerMap.get("Content-Type"));

        helper.maskHeaders(null, sensitiveNames);
        helper.maskHeaders(headerMap, null);
        helper.maskHeaders(headerMap, Collections.emptySet());
    }

    @Test
    public void testServletHeadersWithNullNamesAndValues() {
        Map<String, List<String>> servletHeaders = new LinkedHashMap<>();
        servletHeaders.put(null, Collections.singletonList("null-key-value"));
        servletHeaders.put("X-Servlet", Collections.singletonList("servlet-val"));
        servletHeaders.put("X-Empty-Val", Collections.emptyList());
        servletHeaders.put("X-Null-Val", Collections.singletonList(null));

        Map<String, List<String>> protocolHeaders = new LinkedHashMap<>();
        protocolHeaders.put(null, Collections.singletonList("HTTP/1.1 200 OK"));
        protocolHeaders.put("AUTHORIZATION", Collections.singletonList("secret"));

        Message message = createServerOutMessage(servletHeaders, protocolHeaders);
        Set<String> sensitiveHeaders = new HashSet<>(Arrays.asList(null, "Authorization"));
        LogEvent event = new DefaultLogEventMapper().map(message, sensitiveHeaders);

        Map<String, String> headers = event.getHeaders();
        assertEquals("HTTP/1.1 200 OK", headers.get(null));
        assertEquals(MASKED_HEADER_VALUE, headers.get("AUTHORIZATION"));
        assertEquals("servlet-val", headers.get("X-Servlet"));
        assertEquals("", headers.get("X-Null-Val"));
    }

    @Test
    public void testServletResponseWithNullHeaderNamesDoesNotThrow() {
        HttpServletResponse response = (HttpServletResponse)Proxy.newProxyInstance(
            DefaultLogEventMapperTest.class.getClassLoader(), new Class<?>[] {HttpServletResponse.class},
            (proxy, method, args) -> {
                if ("getHeaderNames".equals(method.getName())) {
                    return null;
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
        message.put("HTTP.RESPONSE", response);
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Test", Collections.singletonList("value")));

        LogEvent event = new DefaultLogEventMapper().map(message);
        assertEquals("value", event.getHeaders().get("X-Test"));
    }

    private static Message createServerOutMessage(Map<String, List<String>> servletHeaders,
                                                  Map<String, List<String>> protocolHeaders) {
        HttpServletResponse response = (HttpServletResponse)Proxy.newProxyInstance(
            DefaultLogEventMapperTest.class.getClassLoader(), new Class<?>[] {HttpServletResponse.class},
            (proxy, method, args) -> {
                if ("getHeaderNames".equals(method.getName())) {
                    return servletHeaders.keySet();
                } else if ("getHeaders".equals(method.getName())) {
                    return servletHeaders.get(args[0]);
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
        message.put("HTTP.RESPONSE", response);
        if (protocolHeaders != null) {
            message.put(Message.PROTOCOL_HEADERS, protocolHeaders);
        }
        return message;
    }
}
