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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServletResponseHeadersTest {
    @Test
    public void testLazyViewAndUnmodifiableSnapshots() {
        Message message = new MessageImpl();
        Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        HttpServletResponse response = response(actual);
        Map<String, List<String>> view = view(message, response);
        assertTrue(view.isEmpty());
        assertFalse(message.containsKey(Message.PROTOCOL_HEADERS));
        actual.put("X-Servlet", Collections.singletonList("late"));
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Protocol", Collections.singletonList("late-protocol")));
        assertEquals(Collections.singletonList("late"), view.get("X-Servlet"));
        assertEquals(Collections.singletonList("late-protocol"), view.get("X-Protocol"));
        assertEquals(Collections.singletonList("late"), view.get("x-servlet"));
        assertTrue(view.containsKey("x-protocol"));
        assertThrows(UnsupportedOperationException.class, () -> view.put("X", Collections.emptyList()));
        assertThrows(UnsupportedOperationException.class, () -> view.get("X-Servlet").add("changed"));
        assertThrows(UnsupportedOperationException.class, () -> view.entrySet().iterator().next()
            .setValue(Collections.singletonList("changed")));
        assertEquals(1, actual.size());
    }

    @Test
    public void testSameHeadersBeforeAndAfterCopying() {
        for (boolean append : new boolean[] {false, true}) {
            Message message = new MessageImpl();
            message.put(Headers.ADD_HEADERS_PROPERTY, append);
            Map<String, List<String>> protocol = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            protocol.put("Set-Cookie", Arrays.asList("jaxrs=1", "jaxrs=2"));
            protocol.put("Link", Collections.singletonList("<jaxrs>"));
            protocol.put("X-Shared", Arrays.asList("a", "b,c"));
            message.put(Message.PROTOCOL_HEADERS, protocol);
            Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            actual.put("set-cookie", Collections.singletonList("servlet=1"));
            actual.put("link", Collections.singletonList("<servlet>"));
            actual.put("x-shared", Collections.singletonList("servlet"));
            actual.put("X-Servlet-Only", Collections.singletonList("keep"));
            HttpServletResponse response = response(actual);
            Map<String, List<String>> view = view(message, response);
            Map<String, List<String>> before = snapshot(view);
            assertEquals(Arrays.asList("servlet=1", "jaxrs=1", "jaxrs=2"), before.get("Set-Cookie"));
            assertEquals(Arrays.asList("<servlet>", "<jaxrs>"), before.get("Link"));
            assertEquals(append ? Arrays.asList("servlet", "a", "b,c") : Collections.singletonList("a,b,c"),
                         before.get("X-Shared"));
            // Inspection must not feed servlet headers back into headers waiting to be sent.
            assertEquals(Arrays.asList("a", "b,c"), protocol.get("X-Shared"));
            assertFalse(protocol.containsKey("X-Servlet-Only"));
            assertEquals(Collections.singletonList("servlet=1"), actual.get("Set-Cookie"));
            new Headers(message).copyToResponse(response);
            message.put(AbstractHTTPDestination.RESPONSE_HEADERS_COPIED, "true");
            assertEquals(before, snapshot(view));
            assertEquals(before, actual);
        }
    }

    @Test
    public void testTransportFormatsDatesAndLocales() {
        Message message = new MessageImpl();
        Map<String, List<Object>> protocol = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Date date = new Date(0);
        protocol.put("Expires", Collections.singletonList(date));
        protocol.put("Content-Language", Collections.singletonList(Locale.US));
        message.put(Message.PROTOCOL_HEADERS, protocol);
        Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        HttpServletResponse response = response(actual);
        Map<String, List<String>> before = snapshot(view(message, response));
        assertEquals(Collections.singletonList(Headers.toHttpDate(date)), before.get("Expires"));
        new Headers(message).copyToResponse(response);
        assertEquals(before, actual);
    }

    @Test
    public void testContentTypePrecedence() {
        for (boolean append : new boolean[] {false, true}) {
            Message message = new MessageImpl();
            message.put(Headers.ADD_HEADERS_PROPERTY, append);
            message.put(Message.CONTENT_TYPE, "text/plain");
            Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            actual.put("Content-Type", Collections.singletonList("application/servlet"));
            HttpServletResponse response = response(actual);
            Map<String, List<String>> before = snapshot(view(message, response));
            assertEquals(Collections.singletonList("text/plain"), before.get("Content-Type"));
            new Headers(message).copyToResponse(response);
            assertEquals(before, actual);
        }
    }

    @Test
    public void testProtocolContentTypeReplacesServletValue() {
        for (boolean append : new boolean[] {false, true}) {
            Message message = new MessageImpl();
            message.put(Headers.ADD_HEADERS_PROPERTY, append);
            Map<String, List<String>> protocol = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            protocol.put("Content-Type", Collections.singletonList("text/plain"));
            message.put(Message.PROTOCOL_HEADERS, protocol);
            Map<String, List<String>> actual = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            actual.put("Content-Type", Collections.singletonList("application/servlet"));
            HttpServletResponse response = response(actual);
            Map<String, List<String>> before = snapshot(view(message, response));
            assertEquals(Collections.singletonList("text/plain"), before.get("Content-Type"));
            new Headers(message).copyToResponse(response);
            assertEquals(before, actual);
        }
    }

    @Test
    public void testUnsupportedHeaderNamesFallsBackToProtocol() {
        Message message = new MessageImpl();
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("Authorization", Collections.singletonList("secret")));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getHeaderNames()).thenThrow(new UnsupportedOperationException());
        assertEquals(Collections.singletonMap("Authorization", Collections.singletonList("secret")),
                     snapshot(view(message, response)));
    }

    @Test
    public void testFailedHeaderReadDiscardsPartialSnapshot() {
        Message message = new MessageImpl();
        message.put(AbstractHTTPDestination.RESPONSE_HEADERS_COPIED, "true");
        message.put(Message.PROTOCOL_HEADERS,
            Collections.singletonMap("X-Shared", Collections.singletonList("original")));
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getHeaderNames()).thenReturn(Arrays.asList("X-Shared", "X-Failing"));
        when(response.getHeaders("X-Shared")).thenReturn(Collections.singletonList("changed"));
        when(response.getHeaders("X-Failing")).thenThrow(new IllegalStateException("Response recycled"));
        assertEquals(Collections.singletonMap("X-Shared", Collections.singletonList("original")),
                     snapshot(view(message, response)));
    }

    private static Map<String, List<String>> snapshot(Map<String, List<String>> view) {
        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        result.putAll(view);
        return result;
    }

    private static Map<String, List<String>> view(Message message, HttpServletResponse response) {
        Map<String, List<String>> view = Collections.unmodifiableMap(new ServletResponseHeaders(message, response));
        message.put(Message.RESPONSE_HEADERS, view);
        return view;
    }

    private static HttpServletResponse response(Map<String, List<String>> headers) {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getHeaderNames()).thenAnswer(invocation -> headers.keySet());
        when(response.getHeaders(anyString())).thenAnswer(invocation -> headers.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            headers.put(invocation.getArgument(0), Collections.singletonList(invocation.getArgument(1)));
            return null;
        }).when(response).setHeader(anyString(), anyString());
        doAnswer(invocation -> {
            String name = invocation.getArgument(0);
            // like the containers, adding a Content-Type replaces it
            List<String> values = Message.CONTENT_TYPE.equalsIgnoreCase(name)
                ? new ArrayList<>() : new ArrayList<>(headers.getOrDefault(name, Collections.emptyList()));
            values.add(invocation.getArgument(1));
            headers.put(name, values);
            return null;
        }).when(response).addHeader(anyString(), anyString());
        doAnswer(invocation -> {
            headers.put(Message.CONTENT_TYPE, Collections.singletonList(invocation.getArgument(0)));
            return null;
        }).when(response).setContentType(anyString());
        return response;
    }
}
