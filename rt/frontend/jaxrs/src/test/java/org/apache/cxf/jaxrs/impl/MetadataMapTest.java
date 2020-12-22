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

package org.apache.cxf.jaxrs.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;



public class MetadataMapTest {

    @Test
    public void testPutSingle() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        List<Object> value1 = new ArrayList<>();
        value1.add("bar");
        value1.add("foo");
        m.put("baz", value1);

        m.putSingle("baz", "clazz");
        List<Object> value2 = m.get("baz");
        assertEquals("Only a single value should be in the list", 1, value2.size());
        assertEquals("Value is wrong", "clazz", value2.get(0));
        assertNull(m.get("baZ"));
    }

    @Test
    public void testPutSingleNullKey() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle(null, "null");
        m.putSingle(null, "null2");
        assertEquals(1, m.get(null).size());
        assertEquals("null2", m.getFirst(null));
    }

    @Test
    public void testPutSingleNullKeyCaseSensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        m.putSingle(null, "null");
        m.putSingle(null, "null2");
        assertEquals(1, m.get(null).size());
        assertEquals("null2", m.getFirst(null));
    }

    @Test
    public void testPutSingleNullKeyCaseSensitive2() {
        MetadataMap<String, Object> map = new MetadataMap<>(false, true);
        Object obj1 = new Object();
        Object obj2 = new Object();
        map.putSingle("key", obj1);
        map.putSingle("key", obj2);
        map.putSingle(null, obj2);
        map.putSingle(null, obj1);
        assertEquals(2, map.size());
        assertEquals(1, map.get(null).size());
        assertSame(map.getFirst("key"), obj2);
        assertSame(map.getFirst(null), obj1);
    }

    @Test
    public void testAddFirst() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.addFirst("baz", "foo");
        List<Object> values = m.get("baz");
        assertEquals(1, values.size());
        assertEquals("foo", values.get(0));

        m.addFirst("baz", "clazz");
        values = m.get("baz");
        assertEquals(2, values.size());
        assertEquals("clazz", values.get(0));
        assertEquals("foo", values.get(1));
    }

    @Test
    public void testAddFirstUnmodifiableListFirst() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.put("baz", Arrays.<Object>asList("foo"));
        List<Object> values = m.get("baz");
        assertEquals(1, values.size());
        assertEquals("foo", values.get(0));

        m.addFirst("baz", "clazz");
        values = m.get("baz");
        assertEquals(2, values.size());
        assertEquals("clazz", values.get(0));
        assertEquals("foo", values.get(1));
    }

    @Test
    public void testAddAll() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        List<Object> values = new ArrayList<>();
        values.add("foo");
        m.addAll("baz", values);
        values = m.get("baz");
        assertEquals(1, values.size());
        assertEquals("foo", values.get(0));

        m.addAll("baz", Collections.<Object>singletonList("foo2"));
        values = m.get("baz");
        assertEquals(2, values.size());
        assertEquals("foo", values.get(0));
        assertEquals("foo2", values.get(1));
    }

    @Test
    public void testPutSingleCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        List<Object> value1 = new ArrayList<>();
        value1.add("bar");
        value1.add("foo");
        m.put("baz", value1);

        m.putSingle("baz", "clazz");
        assertEquals(1, m.size());

        List<Object> value2 = m.get("baz");
        assertEquals("Only a single value should be in the list", 1, value2.size());
        assertEquals("Value is wrong", "clazz", value2.get(0));

        m.putSingle("Baz", "clazz2");
        assertEquals(1, m.size());
        value2 = m.get("baz");
        assertEquals("Only a single value should be in the list", 1, value2.size());
        assertEquals("Value is wrong", "clazz2", value2.get(0));

        assertTrue(m.containsKey("Baz"));
        assertTrue(m.containsKey("baz"));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        m.putSingle("a", "b");
        assertTrue(m.containsKey("a"));
        assertTrue(m.containsKey("A"));
    }

    @Test
    public void testContainsKeyCaseSensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("a", "b");
        assertTrue(m.containsKey("a"));
        assertFalse(m.containsKey("A"));
    }


    @Test
    public void testKeySetCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        m.putSingle("a", "b");
        assertTrue(m.keySet().contains("a"));
        assertTrue(m.keySet().contains("A"));
    }

    @Test
    public void testKeySetCaseSensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.putSingle("a", "b");
        assertTrue(m.keySet().contains("a"));
        assertFalse(m.keySet().contains("A"));
    }

    @Test
    public void testPutAllCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        List<Object> value1 = new ArrayList<>();
        value1.add("bar");
        value1.add("foo");
        m.put("baz", value1);
        assertEquals(1, m.size());
        List<Object> values = m.get("baz");
        assertEquals(2, values.size());
        assertEquals("bar", values.get(0));
        assertEquals("foo", values.get(1));

        MetadataMap<String, Object> m2 = new MetadataMap<>(false, true);
        List<Object> value2 = new ArrayList<>();
        value2.add("bar2");
        value2.add("foo2");
        m2.put("BaZ", value2);

        m.putAll(m2);

        assertEquals(1, m.size());
        values = m.get("Baz");
        assertEquals(2, values.size());
        assertEquals("bar2", values.get(0));
        assertEquals("foo2", values.get(1));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>(false, true);
        List<Object> value1 = new ArrayList<>();
        value1.add("bar");
        value1.add("foo");
        m.put("baz", value1);

        m.putSingle("baz", "clazz");
        assertEquals(1, m.size());

        m.remove("Baz");
        assertEquals(0, m.size());
    }

    @Test
    public void testAddAndGetFirst() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("baz", "bar");

        List<Object> value = m.get("baz");
        assertEquals("Only a single value should be in the list", 1, value.size());
        assertEquals("Value is wrong", "bar", value.get(0));

        m.add("baz", "foo");

        value = m.get("baz");
        assertEquals("Two values should be in the list", 2, value.size());
        assertEquals("Value1 is wrong", "bar", value.get(0));
        assertEquals("Value2 is wrong", "foo", value.get(1));

        assertEquals("GetFirst value is wrong", "bar", m.getFirst("baz"));
    }

    @Test
    public void testCopyAndUpdate() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<>(m);
        m.remove("baz");
        m.add("baz", "foo");
        assertEquals("bar", m2.getFirst("baz"));
        assertEquals("foo", m.getFirst("baz"));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyRemove() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<>(m, true, false);
        m2.remove("baz");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyAdd() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<>(m, true, false);
        m2.add("bar", "foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyAddFirst() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<>(m, true, false);
        m2.addFirst("baz", "bar2");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyAdd2() {
        Map<String, List<String>> values = new HashMap<>();
        List<String> list = new LinkedList<>();
        list.add("bar");
        values.put("baz", list);
        MultivaluedMap<String, String> map =
            new MetadataMap<>(values, false, true, true);
        map.add("baz", "baz");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyAddFirst2() {
        Map<String, List<String>> values = new HashMap<>();
        List<String> list = new LinkedList<>();
        list.add("bar");
        values.put("baz", list);
        MultivaluedMap<String, String> map =
            new MetadataMap<>(values, false, true, true);
        map.addFirst("baz", "bar2");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnlyPutSingle() {
        Map<String, List<String>> values = new HashMap<>();
        MultivaluedMap<String, String> map =
            new MetadataMap<>(values, false, true, true);
        map.putSingle("baz", "baz");
    }

    @Test
    public void testGetCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        m.add("Baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<>(m, true, true);
        assertEquals("bar", m2.getFirst("baZ"));
        assertEquals("bar", m2.getFirst("Baz"));
        assertTrue(m2.containsKey("BaZ"));
        assertTrue(m2.containsKey("Baz"));
        List<Object> values = m2.get("baz");
        assertEquals(1, values.size());
        assertEquals("bar", values.get(0).toString());
    }

    @Test
    public void testGetFirstEmptyMap() {
        MetadataMap<String, Object> m = new MetadataMap<>();
        assertNull(m.getFirst("key"));
        m.add("key", "1");
        m.get("key").clear();
        assertNull(m.getFirst("key"));
    }

    @Test
    public void testCompareIgnoreValueOrder() {
        MetadataMap<String, String> m = new MetadataMap<>();
        m.add("baz", "bar1");
        m.add("baz", "bar2");
        List<String> values = m.get("baz");
        assertEquals("bar1", values.get(0));
        assertEquals("bar2", values.get(1));

        MetadataMap<String, String> m2 = new MetadataMap<>();
        m2.add("baz", "bar2");
        m2.add("baz", "bar1");
        values = m2.get("baz");
        assertEquals("bar2", values.get(0));
        assertEquals("bar1", values.get(1));

        assertTrue(m.equalsIgnoreValueOrder(m2));
        assertTrue(m.equalsIgnoreValueOrder(m));
        assertTrue(m2.equalsIgnoreValueOrder(m));

        MetadataMap<String, String> m3 = new MetadataMap<>();
        m3.add("baz", "bar1");
        assertFalse(m.equalsIgnoreValueOrder(m3));
        assertFalse(m2.equalsIgnoreValueOrder(m3));
    }

}