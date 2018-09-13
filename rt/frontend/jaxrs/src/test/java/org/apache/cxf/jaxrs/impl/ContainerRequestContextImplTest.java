package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainerRequestContextImplTest {

    @Test
    public void shouldSplitHeadersWhenPropertySet() {
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", new ArrayList<>(Collections.singletonList("bar,baz")));
        m.put(Message.PROTOCOL_HEADERS, headers);
        m.put("org.apache.cxf.http.header.split", true);

        final ContainerRequestContextImpl context = new ContainerRequestContextImpl(m, false, false);
        final MultivaluedMap<String, String> actual = context.getHeaders();
        assertEquals(1, actual.size());
        assertEquals(Arrays.asList("bar", "baz"), actual.get("foo"));
    }

    @Test
    public void shouldConcatenateHeadersByDefault() {
        Message m = new MessageImpl();
        m.setExchange(new ExchangeImpl());
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", new ArrayList<>(Collections.singletonList("bar,baz")));
        m.put(Message.PROTOCOL_HEADERS, headers);

        final ContainerRequestContextImpl context = new ContainerRequestContextImpl(m, false, false);
        final MultivaluedMap<String, String> actual = context.getHeaders();
        assertEquals(1, actual.size());
        assertEquals(Collections.singletonList("bar,baz"), actual.get("foo"));
    }
}