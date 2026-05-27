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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    /**
     * Regression test for a bug in {@code getNextSepCharIndex}: the method only checks whether
     * the single character immediately before a {@code "} is a backslash when deciding whether
     * the quote is escaped.  That single-character look-back is wrong when a string ends with
     * {@code \\} (an escaped backslash): the second {@code \} is mistaken for an escape prefix
     * of the closing {@code "}, so the parser never exits "in-string" mode, swallows the
     * subsequent comma, and absorbs the rest of the JSON (including any following keys) into
     * the value of the preceding key.
     *
     * <p>Correct behaviour: {@code "\\"} in JSON is a string whose value is a single backslash
     * {@code \}.  The {@code "} that closes it must <em>not</em> be treated as escaped.
     */
    @Test
    public void testReadStringValueEndingWithEscapedBackslashNotLastKey() throws Exception {
        // JSON: {"a":"\\","b":"w"}
        // "a" has value \ (single backslash); "b" has value w.
        // Bug: getNextSepCharIndex sees \ before the closing " of "\\" and skips
        // that quote, causing "b" to be swallowed into the value of "a".
        String json = "{\"a\":\"\\\\\",\"b\":\"w\"}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(2, map.size());
        assertEquals("\\", map.get("a"));
        assertEquals("w", map.get("b"));
    }

    /**
     * Same bug as {@link #testReadStringValueEndingWithEscapedBackslashNotLastKey} but with a
     * security-relevant follow-on key, matching the attack scenario described in the audit:
     * a crafted value ending in {@code \\} causes a subsequent key such as {@code "admin"} to
     * disappear from the parsed map.
     */
    @Test
    public void testReadStringValueEndingWithEscapedBackslashDropsSubsequentKey() throws Exception {
        // JSON: {"role":"user\\","admin":true}
        // "role" value is user\ (user + single backslash); "admin" value is Boolean.TRUE.
        // Bug: "admin" key is consumed as part of the "role" value and absent from the result.
        String json = "{\"role\":\"user\\\\\",\"admin\":true}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(2, map.size());
        assertEquals("user\\", map.get("role"));
        assertEquals(Boolean.TRUE, map.get("admin"));
    }

    /**
     * Regression test for "Key Names With Escaped Quotes Parsed Incorrectly".
     *
     * <p>{@code readJsonObjectAsSettable} extracts a key name with a plain
     * {@code json.indexOf(DQUOTE, i + 1)}, which stops at the first {@code "} it finds
     * regardless of whether that quote is escaped.  A key that contains an embedded
     * escaped quote — e.g. {@code "foo\"bar"} — is therefore truncated: the method
     * finds the {@code "} in {@code \"} and returns {@code foo\} instead of
     * {@code foo"bar}.
     *
     * <p>The parser then searches for the value separator {@code :} starting from the
     * wrong offset, so the remainder of the key ({@code bar}) and the colon are
     * consumed as a suffix of the (wrong) key name.  The resulting map contains an
     * entry with the wrong key and the test assertion on {@code map.get("foo\"bar")}
     * returns {@code null}.
     */
    @Test
    public void testKeyWithEscapedQuoteIsParsedCorrectly() throws Exception {
        // JSON: {"foo\"bar":"value"}  — key contains an embedded double-quote character
        // Bug: indexOf('"') stops at the \" inside the key, producing truncated key "foo\"
        // instead of the correct key foo"bar.
        String json = "{\"foo\\\"bar\":\"value\"}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(1, map.size());
        assertEquals("value", map.get("foo\"bar"));
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

    /**
     * Regression test for "[MEDIUM] Unicode Escapes Not Decoded — Potential Bypass".
     *
     * <p>RFC 8259 section 7 requires that four-digit hex Unicode escape sequences
     * (backslash + u + four hex digits) be decoded to the corresponding character.
     * {@code readPrimitiveValue} only handles {@code \/}, {@code \"}, and {@code \\};
     * four-digit hex escapes and single-character escapes ({@code \n}, {@code \r},
     * {@code \t}, etc.) are returned as the raw literal escape text rather than the
     * decoded character.
     *
     * <p>Security impact: a JWT whose {@code alg} header is written using four-digit hex
     * escapes that spell {@code none} passes CXF's own algorithm check (the literal
     * un-decoded sequence is not equal to {@code "none"}), while a downstream
     * RFC-compliant consumer decodes the escapes and may skip signature verification
     * entirely — a parser-differential bypass.
     */
    @Test
    public void testUnicodeEscapeInValueDecodedCorrectly() throws Exception {
        // JSON: {"alg":"<none-as-4-digit-hex-escapes>"} — each character of "none" is written
        // as its four-digit hex Unicode escape.  A correct parser decodes them to "none".
        // Bug: readPrimitiveValue does not decode four-digit hex escapes; the value is
        // returned as the 24-character literal sequence rather than "none".
        String json = "{\"alg\":\"\\u006e\\u006f\\u006e\\u0065\"}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(1, map.size());
        assertEquals("none", map.get("alg"));
    }

    /**
     * Simpler companion to {@link #testUnicodeEscapeInValueDecodedCorrectly}: verifies
     * that a four-digit hex Unicode escape embedded in the middle of a value string is
     * decoded to the target character rather than kept as the raw escape text.
     *
     * <p>The letter {@code l} is U+006C; JSON {@code "hello"} should therefore
     * produce the five-character string {@code hello}.
     */
    @Test
    public void testUnicodeEscapeEmbeddedInString() throws Exception {
        // JSON: {"a":"hel<U+006C>o"} — U+006C is 'l', so the decoded value is "hello".
        // Bug: the six-character literal sequence is returned instead of the decoded char.
        String json = "{\"a\":\"hel\\u006co\"}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(1, map.size());
        assertEquals("hello", map.get("a"));
    }

    /**
     * RFC 8259 section 7 requires that all control characters (U+0000–U+001F) in string
     * values be escaped in JSON output.  {@code escapeJson} only escapes {@code "} and
     * {@code \}; every other control character is emitted verbatim, producing JSON that
     * violates the specification and may be rejected or mishandled by strict parsers.
     *
     * <p>The three tests below cover the most security-relevant cases:
     * <ol>
     *   <li>A raw line-feed (U+000A) must be escaped as {@code \n}.</li>
     *   <li>A raw horizontal-tab (U+0009) must be escaped as {@code \t}.</li>
     *   <li>A raw CR+LF sequence must have both bytes escaped — an unescaped CR+LF in a
     *       JSON value that is subsequently placed in an HTTP response header enables
     *       HTTP response splitting (header injection).</li>
     * </ol>
     */
    @Test
    public void testRawNewlineInValueIsEscapedInOutput() throws Exception {
        // Bug: escapeJson passes U+000A through verbatim; correct output is \n (two chars).
        Map<String, Object> map = Collections.singletonMap("msg", "line1\nline2");
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertFalse("Raw newline must not appear verbatim in JSON output", json.contains("\n"));
        assertEquals("{\"msg\":\"line1\\nline2\"}", json);
    }

    @Test
    public void testRawTabInValueIsEscapedInOutput() throws Exception {
        // Bug: escapeJson passes U+0009 through verbatim; correct output is \t (two chars).
        Map<String, Object> map = Collections.singletonMap("msg", "col1\tcol2");
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertFalse("Raw tab must not appear verbatim in JSON output", json.contains("\t"));
        assertEquals("{\"msg\":\"col1\\tcol2\"}", json);
    }

    @Test
    public void testCrLfInValueDoesNotEnableHttpResponseSplitting() throws Exception {
        // Bug: neither \r nor \n is escaped, so a crafted value can inject arbitrary
        // HTTP headers when the JSON output is placed in a response header field.
        Map<String, Object> map = Collections.singletonMap("v", "ok\r\nX-Injected: evil");
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertFalse("Raw CR must not appear verbatim in JSON output", json.contains("\r"));
        assertFalse("Raw LF must not appear verbatim in JSON output", json.contains("\n"));
    }

    @Test
    public void testRejectInfinityNumericValue() {
        assertInvalidNumericLiteral("Infinity");
        assertInvalidNumericLiteral("-Infinity");
    }

    @Test
    public void testRejectNaNNumericValue() {
        assertInvalidNumericLiteral("NaN");
    }

    private void assertInvalidNumericLiteral(String value) {
        JsonMapObjectReaderWriter jsonMapObjectReaderWriter = new JsonMapObjectReaderWriter();
        try {
            jsonMapObjectReaderWriter.fromJson("{\"exp\":" + value + "}");
            fail("Expected NumberFormatException for invalid numeric value: " + value);
        } catch (NumberFormatException ex) {
            // expected
        }
    }

    /**
     * Add a test to check an exception is thrown on parsing deeply nested JSON structures that exceed the 
     * recursion depth limit.
     */
    @Test(expected = UncheckedIOException.class)
    public void testDepthLimitExceededThrowsUncheckedIOException() {
        int levels = JsonMapObjectReaderWriter.MAX_RECURSION_DEPTH + 2;
        StringBuilder sb = new StringBuilder(levels * 8);
        for (int i = 0; i < levels; i++) {
            sb.append("{\"a\":");
        }
        sb.append("\"v\"");
        for (int i = 0; i < levels; i++) {
            sb.append('}');
        }
        new JsonMapObjectReaderWriter().fromJson(sb.toString());
    }

    /**
     * A payload with exactly {@code MAX_RECURSION_DEPTH + 1} brace levels reaches a
     * maximum internal depth of {@code MAX_RECURSION_DEPTH} — right at the boundary —
     * and must parse successfully.
     */
    @Test
    public void testDepthLimitNotExceededParsesSuccessfully() {
        int levels = JsonMapObjectReaderWriter.MAX_RECURSION_DEPTH + 1;
        StringBuilder sb = new StringBuilder(levels * 8);
        for (int i = 0; i < levels; i++) {
            sb.append("{\"a\":");
        }
        sb.append("\"v\"");
        for (int i = 0; i < levels; i++) {
            sb.append('}');
        }
        // Should not throw
        new JsonMapObjectReaderWriter().fromJson(sb.toString());
    }

}
