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
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.HttpUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FormEncodingProviderTest extends Assert {

    private FormEncodingProvider ferp;

    @Before
    public void setUp() {
        ferp = new FormEncodingProvider();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadFrom() throws Exception {
        InputStream is = getClass().getResourceAsStream("singleValPostBody.txt");
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)MultivaluedMap.class, null, 
                new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, is);
        assertEquals("Wrong entry for foo", "bar", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "far", mvMap.getFirst("boo"));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDecoded() throws Exception {
        String values = "foo=1+2&bar=1+3";
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)MultivaluedMap.class, null, 
                new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, 
                new ByteArrayInputStream(values.getBytes()));
        assertEquals("Wrong entry for foo", "1 2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "1 3", mvMap.getFirst("bar"));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEncoded() throws Exception {
        String values = "foo=1+2&bar=1+3";
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)MultivaluedMap.class, null, 
                new Annotation[]{CustomMap.class.getAnnotations()[0]}, 
                    MediaType.APPLICATION_FORM_URLENCODED_TYPE, null, 
                    new ByteArrayInputStream(values.getBytes()));
        assertEquals("Wrong entry for foo", "1+2", mvMap.getFirst("foo"));
        assertEquals("Wrong entry for boo", "1+3", mvMap.getFirst("bar"));

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCustomMapImpl() throws Exception {
        String values = "foo=1+2&bar=1+3&baz=4";
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)CustomMap.class, null, 
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void testMultiLines() throws Exception {
        String values = "foo=1+2&bar=line1%0D%0Aline+2&baz=4";
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)CustomMap.class, null, 
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
        MultivaluedMap<String, String> mvMap = new MetadataMap<String, String>();
        mvMap.add("a", "a1");
        mvMap.add("a", "a2");
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class, 
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE, 
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&a=a2", result);  
    }
    
    @Test
    public void testWriteMultipleValues2() throws Exception {
        MultivaluedMap<String, String> mvMap = new MetadataMap<String, String>();
        mvMap.add("a", "a1");
        mvMap.add("a", "a2");
        mvMap.add("b", "b1");
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class, 
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE, 
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&a=a2&b=b1", result);  
    }
    
    @Test
    public void testWrite() throws Exception {
        MultivaluedMap<String, String> mvMap = new MetadataMap<String, String>();
        mvMap.add("a", "a1");
        mvMap.add("b", "b1");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        ferp.writeTo(mvMap, MultivaluedMap.class, MultivaluedMap.class, 
                     new Annotation[0], MediaType.APPLICATION_FORM_URLENCODED_TYPE, 
                     new MetadataMap<String, Object>(), bos);
        String result = bos.toString();
        assertEquals("Wrong value", "a=a1&b=b1", result);  
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testValidation() throws Exception {
        ferp.setValidator(new CustomFormValidator());
        String values = "foo=1+2&bar=1+3";
        
        try {
            ferp.readFrom((Class)CustomMap.class, null, new Annotation[]{}, 
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
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)MultivaluedMap.class, null,
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
        
        System.out.println(helloStringISO88591);
        
        MultivaluedMap<String, String> mvMap = 
            (MultivaluedMap<String, String>)ferp.readFrom((Class)MultivaluedMap.class, null,
                new Annotation[]{}, 
                MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED + ";charset=ISO-8859-1"), null, 
                new ByteArrayInputStream(iso88591bytes));
        String value = mvMap.getFirst("name");

        System.out.println(value);

    }

    @Test
    public void testReadable() {
        assertTrue(ferp.isReadable(MultivaluedMap.class, null, null, null));
    }

    @Test
    public void testAnnotations() {
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
