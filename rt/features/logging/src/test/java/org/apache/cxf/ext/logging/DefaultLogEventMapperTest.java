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

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.Subject;

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
    public void shouldMaskHeadersIgnoringNullNames() {
        final DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        final Message message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        final Map<String, Object> headers = new HashMap<>();
        headers.put(null, Arrays.asList("HTTP/1.1 200 OK"));
        headers.put("testheader", Arrays.asList(TEST_HEADER_VALUE));
        message.put(Message.PROTOCOL_HEADERS, headers);
        final Set<String> sensitiveHeaders = new HashSet<>(Arrays.asList(null, TEST_HEADER_NAME));

        LogEvent event = mapper.map(message, sensitiveHeaders);
        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get("testheader"));
        assertEquals("HTTP/1.1 200 OK", event.getHeaders().get(null));
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
    public void testResponseHeaderViewIsCopiedAndMasked() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Response", Collections.singletonList("effective"));
        headers.put("set-cookie", Arrays.asList("secret=1", "secret=2"));
        Message message = createServerOutMessage();
        message.put(Message.RESPONSE_HEADERS, Collections.unmodifiableMap(headers));
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Response", Collections.singletonList("protocol")));
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Set-Cookie"));
        assertEquals(EventType.RESP_OUT, event.getType());
        assertEquals("effective", event.getHeaders().get("X-Response"));
        assertEquals(MASKED_HEADER_VALUE, event.getHeaders().get("set-cookie"));
        assertEquals(Arrays.asList("secret=1", "secret=2"), headers.get("set-cookie"));
    }

    @Test
    public void testResponseHeadersNotUsedForRequest() {
        Message message = createServerOutMessage();
        Message inMessage = new MessageImpl();
        inMessage.setExchange(message.getExchange());
        inMessage.put(Message.RESPONSE_HEADERS,
            Collections.singletonMap("X-Response", Collections.singletonList("response")));
        inMessage.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Request", Collections.singletonList("request")));
        LogEvent event = new DefaultLogEventMapper().map(inMessage);
        assertEquals(EventType.REQ_IN, event.getType());
        assertEquals(Collections.singletonMap("X-Request", "request"), event.getHeaders());
    }

    @Test
    public void testProtocolHeadersUsedWithoutResponseView() {
        Message message = createServerOutMessage();
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("AUTHORIZATION", Collections.singletonList("secret")));
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Authorization"));
        assertEquals(Collections.singletonMap("AUTHORIZATION", MASKED_HEADER_VALUE), event.getHeaders());
    }

    @Test
    public void testFaultUsesResponseHeaderView() {
        Message message = createServerOutMessage();
        message.getExchange().setOutFaultMessage(message);
        message.put(Message.RESPONSE_HEADERS,
            Collections.singletonMap("set-cookie", Collections.singletonList("secret")));
        LogEvent event = new DefaultLogEventMapper().map(message, Collections.singleton("Set-Cookie"));
        assertEquals(EventType.FAULT_OUT, event.getType());
        assertEquals(Collections.singletonMap("set-cookie", MASKED_HEADER_VALUE), event.getHeaders());
    }

    @Test
    public void testEmptyResponseViewIsAuthoritative() {
        Message message = createServerOutMessage();
        message.put(Message.RESPONSE_HEADERS, Collections.emptyMap());
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Removed", Collections.singletonList("protocol")));
        assertEquals(Collections.emptyMap(), new DefaultLogEventMapper().map(message).getHeaders());
    }

    private static Message createServerOutMessage() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        exchange.put("org.apache.cxf.resource.operation.name", "test");
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        message.put(Message.REQUEST_URI, "test");
        return message;
    }
}
