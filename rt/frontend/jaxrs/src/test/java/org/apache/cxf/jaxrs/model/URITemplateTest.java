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
package org.apache.cxf.jaxrs.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class URITemplateTest extends Assert {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testMatchBasic() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/123/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixAndTemplate() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/123;123456/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123;123456", value);
    }

    @Test
    public void testMatchWithMatrixOnClearPath1() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers;123456/123/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixOnClearPath2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}/orders/{order}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        assertTrue(uriTemplate.match("/customers;123456/123/orders;456/3", values));
        assertEquals("123", values.getFirst("id"));
        assertEquals("3", values.getFirst("order"));
    }

    @Test
    public void testMatchWithMatrixOnClearPath3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/{id}/customers/");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/123/customers;123456/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixOnClearPath4() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        assertTrue(uriTemplate.match("/customers;123456/123/orders;456/3", values));
    }

    @Test
    public void testMatchBasicTwoParametersVariation1() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{name}/{department}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/john/CS", values);
        assertTrue(match);
        String name = values.getFirst("name");
        String department = values.getFirst("department");
        assertEquals("john", name);
        assertEquals("CS", department);
    }

    @Test
    public void testMatchBasicTwoParametersVariation2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/name/{name}/dep/{department}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/name/john/dep/CS", values);
        assertTrue(match);
        String name = values.getFirst("name");
        String department = values.getFirst("department");
        assertEquals("john", name);
        assertEquals("CS", department);
    }

    @Test
    public void testURITemplateWithSubResource() throws Exception {
        // So "/customers" is the URITemplate for the root resource class
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/123", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/123", subResourcePath);
    }

    @Test
    public void testURITemplateWithSubResourceVariation2() throws Exception {
        // So "/customers" is the URITemplate for the root resource class
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/customers/name/john/dep/CS", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/name/john/dep/CS", subResourcePath);
    }

    @Test
    /*
     * Test a sub-resource locator method like this @HttpMethod("GET") @UriTemplate("/books/{bookId}/") public
     * Book getBook(@UriParam("bookId") String id)
     */
    public void testURITemplateWithSubResourceVariation3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId}/");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testBasicCustomExpression() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:[^/]+?}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testBasicCustomExpression2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:123}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testBasicCustomExpression3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:\\d\\d\\d}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testEscaping() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/a.db");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        assertTrue(uriTemplate.match("/books/a.db", values));
        assertFalse(uriTemplate.match("/books/adbc", values));
        assertFalse(uriTemplate.match("/books/acdb", values));

    }

    @Test
    public void testBasicCustomExpression4() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:...\\.}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        assertTrue(uriTemplate.match("/books/123.", values));
        assertEquals("123.", values.getFirst("bookId"));
        values.clear();
        assertTrue(uriTemplate.match("/books/abc.", values));
        assertEquals("abc.", values.getFirst("bookId"));
        assertFalse(uriTemplate.match("/books/abcd", values));
        assertFalse(uriTemplate.match("/books/abc", values));
    }

    @Test
    public void testMultipleExpression2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:123}/chapter/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        assertEquals("1", values.getFirst("id"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", subResourcePath);
    }

    @Test
    public void testFailCustomExpression() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:124}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertFalse(match);
    }

    @Test
    public void testBaseTail1() {
        URITemplate uriTemplate = new URITemplate("/{base:base.+}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertFalse(uriTemplate.match("/base/tails", values));
        assertTrue(uriTemplate.match("/base1/tails", values));
        assertEquals("base1", values.getFirst("base"));
        assertEquals("tails", values.getFirst("tail"));
    }

    @Test
    public void testBaseTail2() {
        URITemplate uriTemplate = new URITemplate("/{base:.+base}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertFalse(uriTemplate.match("/base/tails", values));
        assertFalse(uriTemplate.match("/base1/tails", values));
        assertTrue(uriTemplate.match("/1base/tails", values));
        assertEquals("1base", values.getFirst("base"));
        assertEquals("tails", values.getFirst("tail"));
    }

    @Test
    public void testBaseTail3() {
        URITemplate uriTemplate = new URITemplate("/{base:base.+suffix}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        assertFalse(uriTemplate.match("/base/tails", values));
        assertFalse(uriTemplate.match("/base1/tails", values));
        assertTrue(uriTemplate.match("/base1suffix/tails", values));
        assertEquals("base1suffix", values.getFirst("base"));
        assertEquals("tails", values.getFirst("tail"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteListNull() throws Exception {
        new URITemplate("anything").substitute((List<String>)null);
    }

    @Test
    public void testSubstituteList() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{b:\\d\\d}/{c}");
        List<String> list = Arrays.asList("foo", "99", "baz");
        assertEquals("Wrong substitution", "/foo/foo/99/baz", ut.substitute(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteListWrongPattern() throws Exception {
        URITemplate ut = new URITemplate("/foo/{b:\\d\\d}");
        List<String> list = Arrays.asList("foo", "not-two-digits");
        ut.substitute(list);
    }

    @Test
    public void testSubstituteListSameVars() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a}/{a}");
        List<String> list = Arrays.asList("bar", "baz", "blah");
        assertEquals("Wrong substitution", "/foo/bar/baz/blah", ut.substitute(list));
    }

    @Test
    public void testSubstituteListIncomplete() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{c}/{b}/{d:\\w}");
        List<String> list = Arrays.asList("bar", "baz");
        assertEquals("Wrong substitution", "/foo/bar/baz/{b}/{d:\\w}", ut.substitute(list));
    }

    @Test
    public void testSubstituteListExceeding() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{b}");
        List<String> list = Arrays.asList("bar", "baz", "blah");
        assertEquals("Wrong substitution", "/foo/bar/baz", ut.substitute(list));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteMapNull() throws Exception {
        new URITemplate("anything").substitute((Map<String, String>)null);
    }

    @Test
    public void testSubstituteMap() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{b:\\d\\d}/{c}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("c", "foo");
        map.put("b", "11");
        map.put("a", "bar");
        assertEquals("Wrong substitution", "/foo/bar/11/foo", ut.substitute(map));
    }

    @Test
    public void testSubstituteMapSameVars() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a}/{a}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "bar");
        assertEquals("Wrong substitution", "/foo/bar/bar/bar", ut.substitute(map));
    }

    @Test
    public void testSubstituteMapIncomplete() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{b}/{a:\\d}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("b", "bar");
        assertEquals("Wrong substitution", "/foo/{a}/bar/{a:\\d}", ut.substitute(map));
    }

    @Test
    public void testSubstituteMapSameVarWithPattern() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a:\\d}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "0");
        assertEquals("Wrong substitution", "/foo/0/0", ut.substitute(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteMapSameVarWithPatternFail() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a:\\d}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("a", "not-a-digit");
        ut.substitute(map);
    }

    @Test
    public void testSubstituteMapExceeding() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("b", "baz");
        map.put("a", "blah");
        assertEquals("Wrong substitution", "/foo/blah", ut.substitute(map));
    }

    @Test
    public void testVariables() {
        URITemplate ut = new URITemplate("/foo/{a}/bar{c:\\d}{b:\\w}/{e}/{d}");
        assertEquals(Arrays.asList("a", "c", "b", "e", "d"), ut.getVariables());
        assertEquals(Arrays.asList("c", "b"), ut.getCustomVariables());
    }

    @Test
    public void testTokenizerNoBraces() {
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer("nobraces");
        assertEquals("nobraces", tok.next());
        assertFalse(tok.hasNext());
    }

    @Test
    public void testTokenizerNoNesting() {
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer("foo{bar}baz");
        assertEquals("foo", tok.next());
        assertEquals("{bar}", tok.next());
        assertEquals("baz", tok.next());
        assertFalse(tok.hasNext());
    }

    @Test
    public void testTokenizerNesting() {
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer("foo{bar{baz}}blah");
        assertEquals("foo", tok.next());
        assertEquals("{bar{baz}}", tok.next());
        assertEquals("blah", tok.next());
        assertFalse(tok.hasNext());
    }

    @Test
    public void testTokenizerNoClosing() {
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer("foo{bar}baz{blah");
        assertEquals("foo", tok.next());
        assertEquals("{bar}", tok.next());
        assertEquals("baz", tok.next());
        assertEquals("{blah", tok.next());
        assertFalse(tok.hasNext());
    }

    @Test
    public void testTokenizerNoOpening() {
        CurlyBraceTokenizer tok = new CurlyBraceTokenizer("foo}bar}baz");
        assertEquals("foo}bar}baz", tok.next());
        assertFalse(tok.hasNext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnclosedVariable() {
        new URITemplate("/foo/{var/bar");
    }

    @Test
    public void testUnopenedVariable() {
        URITemplate ut = new URITemplate("/foo/var}/bar");
        assertEquals("/foo/var}/bar", ut.getValue());
    }

    @Test
    public void testNestedCurlyBraces() {
        URITemplate ut = new URITemplate("/foo/{hex:[0-9a-fA-F]{2}}");
        Map<String, String> map = new HashMap<String, String>();
        map.put("hex", "FF");
        assertEquals("Wrong substitution", "/foo/FF", ut.substitute(map));
    }
    
    @Test
    public void testEncodeLiteralCharacters() {
        URITemplate ut = new URITemplate("a {id} b");
        assertEquals("a%20{id}%20b", ut.encodeLiteralCharacters());
    }

    @Test
    public void testEncodeLiteralCharactersNotVariable() {
        URITemplate ut = new URITemplate("a {digit:[0-9]} b");
        System.out.println(ut.encodeLiteralCharacters());
        assertEquals("a%20{digit:[0-9]}%20b", ut.encodeLiteralCharacters());
    }
}
