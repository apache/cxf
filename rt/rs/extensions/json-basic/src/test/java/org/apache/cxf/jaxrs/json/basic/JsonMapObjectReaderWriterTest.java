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

package org.apache.cxf.jaxrs.json.basic;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JsonMapObjectReaderWriterTest {

    @Test
    public void testWriteMap() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", "aValue");
        map.put("b", 123);
        map.put("c", Collections.singletonList("cValue"));
        map.put("claim", null);
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertEquals("{\"a\":\"aValue\",\"b\":123,\"c\":[\"cValue\"],\"claim\":null}",
                     json);
    }
    @Test
    public void testWriteDateProperty() throws Exception {
        Date date = new Date();
        Map<String, Object> map = Collections.singletonMap("createdAt", date);
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertEquals("{\"createdAt\":\"" + date.toString() + "\"}", json);
    }
    @Test
    public void testReadMap() throws Exception {
        String json = "{\"a\":\"aValue\",\"b\":123,\"c\":[\"cValue\"],\"f\":null}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(4, map.size());
        assertEquals("aValue", map.get("a"));
        assertEquals(123L, map.get("b"));
        assertEquals(Collections.singletonList("cValue"), map.get("c"));
        assertNull(map.get("f"));
    }
    @Test
    public void testReadMapWithValueCommas() throws Exception {
        String json = "{\"a\":\"aValue1,aValue2\",\"b\":\"bValue1\"\r\n,\"c\":[\"cValue1, cValue2\"],"
            + "\"d\":\"dValue1,dValue2,dValue3,dValue4\"}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(4, map.size());
        assertEquals("aValue1,aValue2", map.get("a"));
        assertEquals("bValue1", map.get("b"));
        assertEquals(Collections.singletonList("cValue1, cValue2"), map.get("c"));
        assertEquals("dValue1,dValue2,dValue3,dValue4", map.get("d"));
    }
    @Test
    public void testReadStringWithLeftCurlyBracketInString() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"x\":{\"y\":\"{\"}}";
        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(1, map.size());
        Map<String, Object> xMap = CastUtils.cast((Map<?, ?>)map.get("x"));
        assertEquals(1, xMap.size());
        assertEquals("{", xMap.get("y"));
    }
    @Test
    public void testReadStringWithLeftCurlyBracketInString2() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"x\":{\"y\":\"{\", \"z\":\"{\"}, \"a\":\"b\"}";
        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(2, map.size());
        assertEquals("b", map.get("a"));
        Map<String, Object> xMap = CastUtils.cast((Map<?, ?>)map.get("x"));
        assertEquals(2, xMap.size());
        assertEquals("{", xMap.get("y"));
        assertEquals("{", xMap.get("z"));
    }
    @Test
    public void testReadStringWithRightCurlyBracketInString() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"x\":{\"y\":\"}\"}}";
        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(1, map.size());
        Map<String, Object> xMap = CastUtils.cast((Map<?, ?>)map.get("x"));
        assertEquals(1, xMap.size());
        assertEquals("}", xMap.get("y"));
    }
    @Test
    public void testReadStringWithRightCurlyBracketInString2() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"x\":{\"y\":\"}\", \"z\":\"}\"}, \"a\":\"b\"}";
        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(2, map.size());
        assertEquals("b", map.get("a"));
        Map<String, Object> xMap = CastUtils.cast((Map<?, ?>)map.get("x"));
        assertEquals(2, xMap.size());
        assertEquals("}", xMap.get("y"));
        assertEquals("}", xMap.get("z"));
    }
    @Test
    public void testReadStringWithCurlyBracketsInString() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"x\":{\"y\":\"{\\\"}\"}}";
        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(1, map.size());
        Map<String, Object> xMap = CastUtils.cast((Map<?, ?>)map.get("x"));
        assertEquals(1, xMap.size());
        assertEquals("{\"}", xMap.get("y"));
    }

    @Test
    public void testEscapedForwardSlashInString() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"kid\":\"4pZbe4shQQGzZXHbeIlbDvmHOc1\\/H6jH6oBk3nUrcZE=\",\"alg\":\"RS256\"}";

        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(s);
        assertEquals(2, map.size());

        String kid = (String)map.get("kid");
        String expectedKid = "4pZbe4shQQGzZXHbeIlbDvmHOc1/H6jH6oBk3nUrcZE=";
        assertEquals(expectedKid, kid);
    }

    @Test(expected = UncheckedIOException.class)
    public void testMalformedInput() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        String s = "{\"nonce\":\"\",:V\"'";
        jsonMapObjectReaderWriter.fromJson(s);
    }

    @Test
    public void testEscapeDoubleQuotes() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        Map<String, Object> content = new HashMap<>();
        content.put("userInput", "a\"");
        String json = jsonMapObjectReaderWriter.toJson(content);
        assertTrue(json.contains("a\\\""));

        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(json);
        assertEquals(1, map.size());
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        assertEquals("userInput", entry.getKey());
        assertEquals("a\"", entry.getValue());
    }

    @Test
    public void testAlreadyEscapedDoubleQuotes() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        Map<String, Object> content = new HashMap<>();
        content.put("userInput", "a\\\"");
        String json = jsonMapObjectReaderWriter.toJson(content);
        assertTrue(json.contains("a\\\""));

        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(json);
        assertEquals(1, map.size());
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        assertEquals("userInput", entry.getKey());
        assertEquals("a\"", entry.getValue());
    }

    @Test
    public void testEscapeBackslash() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        Map<String, Object> content = new HashMap<>();
        content.put("userInput", "a\\");
        String json = jsonMapObjectReaderWriter.toJson(content);
        assertTrue(json.contains("a\\\\"));

        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(json);
        assertEquals(1, map.size());
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        assertEquals("userInput", entry.getKey());
        assertEquals("a\\", entry.getValue());
    }

    @Test
    public void testAlreadyEscapedBackslash() throws Exception {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        Map<String, Object> content = new HashMap<>();
        content.put("userInput", "a\\\\");
        String json = jsonMapObjectReaderWriter.toJson(content);
        assertTrue(json.contains("a\\\\"));

        Map<String, Object> map = jsonMapObjectReaderWriter.fromJson(json);
        assertEquals(1, map.size());
        Map.Entry<String, Object> entry = map.entrySet().iterator().next();
        assertEquals("userInput", entry.getKey());
        assertEquals("a\\", entry.getValue());
    }

}
