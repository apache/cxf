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

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.HttpUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FormEncodingProviderTest {


    @Test
    public void testReadFrom() throws Exception {
        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();
        InputStream is = getClass().getResourceAsStream("singleValPostBody.txt");
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, is);
        assertEquals("Wrong entry for foo", "bar", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }

    @Test
    public void testReadFromForm() throws Exception {
        FormEncodingProvider<Form> ferp = new FormEncodingProvider<>();
        InputStream is = getClass().getResourceAsStream("singleValPostBody.txt");
        Form form = ferp.readFrom(Form.class, null,
                new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, is);
        MultivaluedMap<String, String> mvMap = form.asMap();
        assertEquals("Wrong entry for foo", "bar", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDecoded() throws Exception {
        String values = "foo=1+2&bar=1+3";

        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();
        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null,
            new ByteArrayInputStream(values.getBytes()));
        assertEquals("Wrong entry for foo", "1 2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "1 3", mvMap.getFirst("bar"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEncoded() throws Exception {
        String values = "foo=1+2&bar=1+3";
        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();
        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{CustomMap.class.getAnnotations()[0]},
                MediaType.APPLICATION_FORM_URLENCODED_TYPE, null,
                new ByteArrayInputStream(values.getBytes()));
        assertEquals("Wrong entry for foo", "1+2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "1+3", mvMap.getFirst("bar"));

    }

    @Test
    public void testCustomMapImpl() throws Exception {
        String values = "foo=1+2&bar=1+3&baz=4";
        FormEncodingProvider<CustomMap> ferp = new FormEncodingProvider<>();

        MultivaluedMap<String, String> mvMap = ferp.readFrom(CustomMap.class, null,
                          new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null,
                          new ByteArrayInputStream(values.getBytes()));
        assertEquals(3, mvMap.size());
        assertEquals(1,  mvMap.get("foo").size());
        assertEquals(1,  mvMap.get("bar").size());
        assertEquals(1,  mvMap.get("baz").size());
        assertEquals("Wrong entry for foo", "1 2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "1 3", mvMap.getFirst("bar"));
        assertEquals("Wrong entry for baz", "4", mvMap.getFirst("baz"));

    }

    @Test
    public void testMultiLines() throws Exception {
        String values = "foo=1+2&bar=line1%0D%0Aline+2&baz=4";
        FormEncodingProvider<CustomMap> ferp = new FormEncodingProvider<>();

        MultivaluedMap<String, String> mvMap = ferp.readFrom(CustomMap.class, null,
                          new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null,
                          new ByteArrayInputStream(values.getBytes()));
        assertEquals(3, mvMap.size());
        assertEquals(1,  mvMap.get("foo").size());
        assertEquals(1,  mvMap.get("bar").size());
        assertEquals(1,  mvMap.get("baz").size());
        assertEquals("Wrong entry for foo", "1 2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry line for bar",
            HttpUtils.urlDecode("line1%0D%0Aline+2"), mvMap.get("bar").get(0));
        assertEquals("Wrong entry for baz", "4", mvMap.getFirst("baz"));

    }

    @Test
    public void testWriteMultipleValues() throws Exception {
        MultivaluedMap<String, String> mvMap = new MetadataMap<>();
        mvMap.add("a", "a1");
        mvMap.add("a", "a2");

        FormEncodingProvider<MultivaluedMap<?, ?>> ferp
            = new FormEncodingProvider<>();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class,
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE,
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&a=a2", result);
    }

    @Test
    public void testWriteMultipleValues2() throws Exception {
        MultivaluedMap<String, String> mvMap = new MetadataMap<>();
        mvMap.add("a", "a1");
        mvMap.add("a", "a2");
        mvMap.add("b", "b1");

        FormEncodingProvider<MultivaluedMap<?, ?>> ferp
            = new FormEncodingProvider<>();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class,
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE,
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&a=a2&b=b1", result);
    }

    @Test
    public void testWrite() throws Exception {
        MultivaluedMap<String, String> mvMap = new MetadataMap<>();
        mvMap.add("a", "a1");
        mvMap.add("b", "b1");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FormEncodingProvider<MultivaluedMap<?, ?>> ferp
            = new FormEncodingProvider<>();
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class,
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE,
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&b=b1", result);
    }

    @Test
    public void testWriteForm() throws Exception {
        Form form = new Form(new MetadataMap<String, String>());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FormEncodingProvider<Form> ferp
            = new FormEncodingProvider<>();
        ferp.writeTo(form.param("a", "a1").param("b", "b1"), Form.class, Form.class,
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE,
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&b=b1", result);
    }

    @Test
    public void testValidation() throws Exception {
        FormEncodingProvider<CustomMap> ferp
            = new FormEncodingProvider<>();
        ferp.setValidator(new CustomFormValidator());
        String values = "foo=1+2&bar=1+3";

        try {
            ferp.readFrom(CustomMap.class, null, new Annotation[]{},
                          MediaType.APPLICATION_FORM_URLENCODED_TYPE, null,
                new ByteArrayInputStream(values.getBytes()));
            fail();
        } catch (WebApplicationException ex) {
            // ignore
        }

    }


    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromMultiples() throws Exception {
        InputStream is = getClass().getResourceAsStream("multiValPostBody.txt");

        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();

        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, is);

        List<String> vals = mvMap.get("foo");

        assertEquals("Wrong size for foo params", 2, vals.size());
        assertEquals("Wrong size for foo params", 1, mvMap.get("boo").size());
        assertEquals("Wrong entry for foo 0", "bar", vals.get(0));
        assertEquals("Wrong entry for foo 1", "bar2", vals.get(1));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromISO() throws Exception {

        String eWithAcute = "\u00E9";
        String helloStringUTF16 = "name=F" + eWithAcute + "lix";

        byte[] iso88591bytes = helloStringUTF16.getBytes("ISO-8859-1");
        String helloStringISO88591 = new String(iso88591bytes, "ISO-8859-1");
        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();

        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{},
            MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED + ";charset=ISO-8859-1"), null,
            new ByteArrayInputStream(iso88591bytes));
        String value = mvMap.getFirst("name");
        assertEquals(helloStringISO88591, "name=" + value);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadChineeseChars() throws Exception {

        String s = "name=中文";

        @SuppressWarnings("rawtypes")
        FormEncodingProvider<MultivaluedMap> ferp
            = new FormEncodingProvider<>();

        MultivaluedMap<String, String> mvMap =
            ferp.readFrom(MultivaluedMap.class, null,
            new Annotation[]{},
            MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED + ";charset=UTF-8"), null,
            new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
        String value = mvMap.getFirst("name");
        assertEquals(s, "name=" + value);
    }

    @Test
    public void testReadableMap() {
        FormEncodingProvider<MultivaluedMap<String, String>> ferp
            = new FormEncodingProvider<>();
        assertTrue(ferp.isReadable(MultivaluedMap.class, null, null, null));
    }

    @Test
    public void testReadableForm() {
        FormEncodingProvider<Form> ferp
            = new FormEncodingProvider<>();
        assertTrue(ferp.isReadable(Form.class, null, null, null));
    }

    @Test
    public void testAnnotations() {
        FormEncodingProvider<Form> ferp
            = new FormEncodingProvider<>();
        assertEquals("application/x-www-form-urlencoded", ferp.getClass().getAnnotation(Consumes.class)
                     .value()[0]);
    }

    @Encoded
    public static class CustomMap extends MetadataMap<String, String> {

    }

    private static class CustomFormValidator implements FormValidator {

        public void validate(MultivaluedMap<String, ? extends Object> params) {
            throw new WebApplicationException();
        }

    }
}