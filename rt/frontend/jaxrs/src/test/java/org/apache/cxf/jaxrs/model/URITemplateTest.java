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
import org.apache.cxf.jaxrs.model.URITemplate.CurlyBraceTokenizer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class URITemplateTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testCompareRegExTemplates() {
        URITemplate t1 = new URITemplate("{entitySetName}{optionalParens: (\\(\\))?}");
        URITemplate t2 = new URITemplate("{entitySetName}{id: \\(.+?\\)}");
        assertTrue(URITemplate.compareTemplates(t1, t2) < 0);
        assertTrue(URITemplate.compareTemplates(t2, t1) > 0);
        assertEquals(0, URITemplate.compareTemplates(t2, t2));
    }

    @Test
    public void testCompareRegExTemplates2() {
        // The purpose of this test is to ensure that enclosing a regex expression does not result
        // in the matcher creating extraneous values.  See CXF-8278
        URITemplate t1 = new URITemplate("/test/{uniqueid : ([0-9a-f]{4}-.*|[0-9a-f]{7}-.*)}/file/{file}");
        URITemplate t2 = new URITemplate("/test/{uniqueid : [0-9a-f]{4}-.*|[0-9a-f]{7}-.*}/file/{file}");
        MultivaluedMap<String, String> template = new MetadataMap<String, String>();
        assertTrue(t1.match("/test/123e-12345/file/test.jpg", template));
        assertEquals(template.getFirst("uniqueid"), "123e-12345");
        assertEquals("test.jpg", template.getFirst("file"));

        template.clear();
        assertTrue(t1.match("/test/123456e-12345/file/test.jpg", template));
        assertEquals(template.getFirst("uniqueid"), "123456e-12345");
        assertEquals(template.getFirst("file"), "test.jpg");

        template.clear();
        assertTrue(t2.match("/test/123e-12345/file/test.jpg", template));
        assertEquals(template.getFirst("uniqueid"), "123e-12345");
        assertEquals(template.getFirst("file"), "test.jpg");

        template.clear();
        assertTrue(t2.match("/test/123456e-12345/file/test.jpg", template));
        assertEquals(template.getFirst("uniqueid"), "123456e-12345");
        assertEquals(template.getFirst("file"), "test.jpg");

        template.clear();
        assertFalse(t2.match("/test/12345678-12345/file/test.jpg", template));

    }


    @Test
    public void testPathCharacters() {
        String pathChars = ":@!$&'*+,;=-._~()";
        assertTrue(new URITemplate(pathChars).match(pathChars,
                                                    new MetadataMap<String, String>()));
    }

    @Test
    public void testMatchBasic() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/customers/123/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixAndTemplate() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/customers/123;123456/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123;123456", value);
    }

    @Test
    public void testMatchWithMatrixWithEmptyPath() throws Exception {
        URITemplate uriTemplate = new URITemplate("/");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/;a=b", values);
        assertTrue(match);
        String finalGroup = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalGroup);
    }

    @Test
    public void testMatchWithMatrixWithEmptyPath2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/;a=b/b/c", values);
        assertTrue(match);
        String finalGroup = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/b/c", finalGroup);
    }

    @Test
    public void testMatchWithMatrixOnClearPath1() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/customers;123456/123/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixOnClearPath2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{id}/orders/{order}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/customers;123456/123/orders;456/3", values));
        assertEquals("123", values.getFirst("id"));
        assertEquals("3", values.getFirst("order"));
    }

    @Test
    public void testMatchWithMultipleMatrixParams() throws Exception {
        URITemplate uriTemplate =
            new URITemplate("renderwidget/id/{id}/type/{type}/size/{size}/locale/{locale}/{properties}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("renderwidget/id/1007/type/1/size/1/locale/en_US/properties;a=b",
                                     values));
        assertEquals("1007", values.getFirst("id"));
    }

    @Test
    public void testMatchWithMultipleMatrixParams2() throws Exception {
        URITemplate uriTemplate =
            new URITemplate("renderwidget/id/{id}/type/{type}/size/{size}/locale/{locale}/{properties}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match(
                   "renderwidget/id/1007/type/1/size/1/locale/en_US/properties;numResults=1;foo=bar",
                    values));
        assertEquals("1007", values.getFirst("id"));
    }

    @Test
    public void testMatchWithMatrixOnClearPath3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/{id}/customers/");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/123/customers;123456/", values);
        assertTrue(match);
        String value = values.getFirst("id");
        assertEquals("123", value);
    }

    @Test
    public void testMatchWithMatrixOnClearPath4() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/customers;123456/123/orders;456/3", values));
    }

    @Test
    public void testMatchWithMatrixOnClearPath5() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/customers;a=b", values));
        String finalGroup = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalGroup);
    }

    @Test
    public void testMatchBasicTwoParametersVariation1() throws Exception {
        URITemplate uriTemplate = new URITemplate("/customers/{name}/{department}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

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
        MultivaluedMap<String, String> values = new MetadataMap<>();

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
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/customers/123", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/123", subResourcePath);
    }

    @Test
    public void testURITemplateWithSubResourceVariation2() throws Exception {
        // So "/customers" is the URITemplate for the root resource class
        URITemplate uriTemplate = new URITemplate("/customers");
        MultivaluedMap<String, String> values = new MetadataMap<>();

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
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testURITemplateWithSubResourceVariation4() throws Exception {
        URITemplate uriTemplate = new URITemplate("/");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/books/123/chapter/1", subResourcePath);
    }

    @Test
    public void testBasicCustomExpression() throws Exception {
        doTestBasicCustomExpression("/books/{bookId:[^/]+?}");
    }

    @Test
    public void testBasicCustomExpressionWithSpaces() throws Exception {
        doTestBasicCustomExpression("/books/{ bookId : [^/]+? }");
    }

    @Test
    public void testBasicCustomExpressionWithSpaces2() throws Exception {
        doTestBasicCustomExpression("/books/{ bookId }/");
    }

    private void doTestBasicCustomExpression(String expression) {
        URITemplate uriTemplate = new URITemplate(expression);
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }


    @Test
    public void testBasicCustomExpression2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:123}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testBasicCustomExpression3() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:\\d\\d\\d}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertTrue(match);
        assertEquals("123", values.getFirst("bookId"));
        String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/chapter/1", subResourcePath);
    }

    @Test
    public void testEscaping() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/a.db");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/a.db", values));
        assertFalse(uriTemplate.match("/books/adbc", values));
        assertFalse(uriTemplate.match("/books/acdb", values));

    }

    @Test
    public void testEscapingWildCard() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/a*");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/a*", values));
        assertFalse(uriTemplate.match("/books/a", values));
        assertFalse(uriTemplate.match("/books/ac", values));
    }

    @Test
    public void testValueWithLiteralPlus() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/ab+");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/ab+", values));
        assertFalse(uriTemplate.match("/books/abb", values));
        assertFalse(uriTemplate.match("/books/ab", values));
        assertFalse(uriTemplate.match("/books/a", values));
    }

    @Test
    public void testValueWithManyLiteralPluses() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/ab+++++");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/ab+++++", values));
        assertFalse(uriTemplate.match("/books/ab++++++", values));
        assertFalse(uriTemplate.match("/books/ab++++", values));
        assertFalse(uriTemplate.match("/books/ab+++", values));
        assertFalse(uriTemplate.match("/books/ab++", values));
        assertFalse(uriTemplate.match("/books/ab+", values));
        assertFalse(uriTemplate.match("/books/ab", values));
        assertFalse(uriTemplate.match("/books/a", values));
    }

    @Test
    public void testValueWithRegExPlus() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{regex:ab+\\+}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/ab+", values));
        assertFalse(uriTemplate.match("/books/abb", values));
        assertFalse(uriTemplate.match("/books/abb", values));
        assertFalse(uriTemplate.match("/books/abc", values));
        assertFalse(uriTemplate.match("/books/a", values));
    }

    @Test
    public void testEncodedSpace() throws Exception {
        URITemplate uriTemplate = new URITemplate("/1 2/%203");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/1%202/%203", values));
        assertFalse(uriTemplate.match("/1 2/%203", values));
    }

    @Test
    public void testBasicCustomExpression4() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:...\\.}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/books/123.", values));
        assertEquals("123.", values.getFirst("bookId"));
        values.clear();
        assertTrue(uriTemplate.match("/books/abc.", values));
        assertEquals("abc.", values.getFirst("bookId"));
        assertFalse(uriTemplate.match("/books/abcd", values));
        assertFalse(uriTemplate.match("/books/abc", values));
    }

    @Test
    public void testExpressionWithNestedGroup() throws Exception {
        URITemplate uriTemplate = new URITemplate("/{resource:.+\\.(js|css|gif|png)}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/script.js", values));
        assertEquals("script.js", values.getFirst("resource"));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);
        values.clear();

        assertTrue(uriTemplate.match("/script.js/bar", values));
        assertEquals("script.js", values.getFirst("resource"));
        finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/bar", finalPath);
        values.clear();

        assertFalse(uriTemplate.match("/script.pdf", values));
    }

    @Test
    public void testExpressionWithNestedGroupAndTwoVars() throws Exception {
        URITemplate uriTemplate = new URITemplate("/foo/{bar}/{resource:.+\\.(js|css|gif|png)}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/foo/1/script.js", values));
        assertEquals("1", values.getFirst("bar"));
        assertEquals("script.js", values.getFirst("resource"));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);
    }

    @Test
    public void testExpressionWithNestedGroupAndTwoVars2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/foo/{bar}{resource:(/format/[^/]+?)?}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/foo/1/format", values));
        assertEquals("1", values.getFirst("bar"));
        assertEquals("/format", values.getFirst("resource"));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);
        values.clear();

        assertTrue(uriTemplate.match("/foo/1/format/2", values));
        assertEquals("1", values.getFirst("bar"));
        assertEquals("/format/2", values.getFirst("resource"));
        finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);
        values.clear();

        assertTrue(uriTemplate.match("/foo/1", values));
        assertEquals("1", values.getFirst("bar"));
        assertNull(values.getFirst("resource"));
        finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);
    }

    @Test
    public void testExpressionWithTwoVars() throws Exception {
        URITemplate uriTemplate = new URITemplate("/{tenant : [^/]*}/resource/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<>();
        boolean match = uriTemplate.match("/t1/resource/1", values);
        assertTrue(match);
        String tenant = values.getFirst("tenant");
        assertEquals("t1", tenant);
        String id = values.getFirst("id");
        assertEquals("1", id);
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);

        values.clear();
        match = uriTemplate.match("//resource/1", values);
        assertTrue(match);
        tenant = values.getFirst("tenant");
        assertEquals("", tenant);
        id = values.getFirst("id");
        assertEquals("1", id);
        finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/", finalPath);

        values.clear();
        match = uriTemplate.match("/t1/resource/1/sub", values);
        assertTrue(match);
        tenant = values.getFirst("tenant");
        assertEquals("t1", tenant);
        id = values.getFirst("id");
        assertEquals("1", id);
        finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/sub", finalPath);
    }

    @Test
    public void testExpressionWithNestedGroupAndManySegments() throws Exception {
        URITemplate uriTemplate = new URITemplate("/foo/{bar}{resource:(/format/[^/]+?)?}/baz");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/foo/1/format/2/baz/3", values));
        assertEquals("1", values.getFirst("bar"));
        assertEquals("/format/2", values.getFirst("resource"));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/3", finalPath);
        values.clear();
    }

    @Test
    public void testExpressionWithNestedGroup2() throws Exception {
        URITemplate uriTemplate =
            new URITemplate("/{resource:.+\\.(js|css|gif|png)}/bar");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/script.js/bar/baz", values));
        assertEquals("script.js", values.getFirst("resource"));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/baz", finalPath);
    }

    @Test
    public void testLiteralExpression() throws Exception {
        URITemplate uriTemplate =
            new URITemplate("/bar");
        MultivaluedMap<String, String> values = new MetadataMap<>();

        assertTrue(uriTemplate.match("/bar/baz", values));
        String finalPath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        assertEquals("/baz", finalPath);
    }

    @Test
    public void testMultipleExpression2() throws Exception {
        URITemplate uriTemplate = new URITemplate("/books/{bookId:123}/chapter/{id}");
        MultivaluedMap<String, String> values = new MetadataMap<>();

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
        MultivaluedMap<String, String> values = new MetadataMap<>();

        boolean match = uriTemplate.match("/books/123/chapter/1", values);
        assertFalse(match);
    }

    @Test
    public void testBaseTail1() {
        URITemplate uriTemplate = new URITemplate("/{base:base.+}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<>();
        assertFalse(uriTemplate.match("/base/tails", values));
        assertTrue(uriTemplate.match("/base1/tails", values));
        assertEquals("base1", values.getFirst("base"));
        assertEquals("tails", values.getFirst("tail"));
    }

    @Test
    public void testBaseTail2() {
        URITemplate uriTemplate = new URITemplate("/{base:.+base}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<>();
        assertFalse(uriTemplate.match("/base/tails", values));
        assertFalse(uriTemplate.match("/base1/tails", values));
        assertTrue(uriTemplate.match("/1base/tails", values));
        assertEquals("1base", values.getFirst("base"));
        assertEquals("tails", values.getFirst("tail"));
    }

    @Test
    public void testBaseTail3() {
        URITemplate uriTemplate = new URITemplate("/{base:base.+suffix}/{tail}");
        MultivaluedMap<String, String> values = new MetadataMap<>();
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
        Map<String, String> map = new HashMap<>();
        map.put("c", "foo");
        map.put("b", "11");
        map.put("a", "bar");
        assertEquals("Wrong substitution", "/foo/bar/11/foo", ut.substitute(map));
    }

    @Test
    public void testSubstituteMapSameVars() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a}/{a}");
        Map<String, String> map = new HashMap<>();
        map.put("a", "bar");
        assertEquals("Wrong substitution", "/foo/bar/bar/bar", ut.substitute(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteMapIncomplete() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{b}/{a:\\d}");
        Map<String, String> map = new HashMap<>();
        map.put("b", "bar");
        ut.substitute(map);
    }

    @Test
    public void testSubstituteMapSameVarWithPattern() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a:\\d}");
        Map<String, String> map = new HashMap<>();
        map.put("a", "0");
        assertEquals("Wrong substitution", "/foo/0/0", ut.substitute(map));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteMapSameVarWithPatternFail() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}/{a:\\d}");
        Map<String, String> map = new HashMap<>();
        map.put("a", "not-a-digit");
        ut.substitute(map);
    }

    @Test
    public void testSubstituteMapExceeding() throws Exception {
        URITemplate ut = new URITemplate("/foo/{a}");
        Map<String, String> map = new HashMap<>();
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

    @Test
    public void testUnclosedVariable() {
        URITemplate ut = new URITemplate("/foo/{var/bar");
        assertEquals("/foo/{var/bar", ut.getValue());
    }

    @Test
    public void testUnopenedVariable() {
        URITemplate ut = new URITemplate("/foo/var}/bar");
        assertEquals("/foo/var}/bar", ut.getValue());
    }

    @Test
    public void testNestedCurlyBraces() {
        URITemplate ut = new URITemplate("/foo/{hex:[0-9a-fA-F]{2}}");
        Map<String, String> map = new HashMap<>();
        map.put("hex", "FF");
        assertEquals("Wrong substitution", "/foo/FF", ut.substitute(map));
    }

    @Test
    public void testEncodeLiteralCharacters() {
        URITemplate ut = new URITemplate("a {id} b");
        assertEquals("a%20{id}%20b", ut.encodeLiteralCharacters(false));
    }

    @Test
    public void testEncodeLiteralCharactersNotVariable() {
        URITemplate ut = new URITemplate("a {digit:[0-9]} b");
        //System.out.println(ut.encodeLiteralCharacters());
        assertEquals("a%20{digit:[0-9]}%20b", ut.encodeLiteralCharacters(false));
    }

    @Test
    public void testCompareNumberOfLiteralCharacters() {
        URITemplate t1 = new URITemplate("/foo");
        URITemplate t2 = new URITemplate("/bar");
        URITemplate t3 = new URITemplate("/foo/bar");
        assertEquals(0, URITemplate.compareTemplates(t1, t1));
        assertTrue(URITemplate.compareTemplates(t1, t3) > 0);
        assertTrue(URITemplate.compareTemplates(t3, t1) < 0);
        assertEquals(Integer.signum(URITemplate.compareTemplates(t1, t2)),
                -Integer.signum(URITemplate.compareTemplates(t2, t1)));
    }

}