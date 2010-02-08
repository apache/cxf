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
package org.apache.cxf.jaxrs.ext.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.cxf.common.xmlschema.XmlSchemaConstants;

import org.junit.Assert;
import org.junit.Test;

public class XMLSourceTest extends Assert {

    
    @Test
    public void testNodeStringValue() {
        InputStream is = getClass().getResourceAsStream("/book1.xsd");
        XMLSource xp = new XMLSource(is);
        Map<String, String> nsMap = 
            Collections.singletonMap("xs", XmlSchemaConstants.XSD_NAMESPACE_URI);
        String value = xp.getNode("/xs:schema", nsMap, String.class);
        assertFalse(value.contains("<?xml"));
        assertTrue(value.startsWith("<xs:schema"));
    }
    
    @Test
    public void testAttributeValue() {
        InputStream is = new ByteArrayInputStream("<foo><bar attr=\"baz\">barValue</bar></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        assertEquals("baz", xp.getValue("/foo/bar/@attr"));
    }
    
    @Test
    public void testNodeTextValue() {
        InputStream is = new ByteArrayInputStream("<foo><bar attr=\"baz\">barValue</bar></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        assertEquals("barValue", xp.getValue("/foo/bar"));
    }
    
    @Test
    public void testAttributeValues() {
        InputStream is = new ByteArrayInputStream(
            "<foo><bar attr=\"baz\">bar1</bar><bar attr=\"baz2\">bar2</bar></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        List<String> values = Arrays.asList(xp.getValues("/foo/bar/@attr"));
        assertEquals(2, values.size());
        assertTrue(values.contains("baz"));
        assertTrue(values.contains("baz2"));
    }
    
    @Test
    public void testNodeTextValues() {
        InputStream is = new ByteArrayInputStream(
            "<foo><bar attr=\"baz\">bar1</bar><bar attr=\"baz2\">bar2</bar></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        List<String> values = Arrays.asList(xp.getValues("/foo/bar/text()"));
        assertEquals(2, values.size());
        assertTrue(values.contains("bar1"));
        assertTrue(values.contains("bar2"));
    }
    
    @Test
    public void testIntegerValues() {
        InputStream is = new ByteArrayInputStream(
            "<foo><bar attr=\"1\"/><bar attr=\"2\"/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        Integer[] values = xp.getNodes("/foo/bar/@attr", Integer.class);
        assertEquals(2, values.length);
        assertTrue(values[0] == 1 && values[1] == 2 || values[0] == 2 && values[1] == 1);
    }
    
    @Test
    public void testGetNodeNoNamespace() {
        InputStream is = new ByteArrayInputStream("<foo><bar/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        Bar bar = xp.getNode("/foo/bar", Bar.class);
        assertNotNull(bar);
    }
    
    @Test
    public void testGetNodeNamespace() {
        String data = "<x:foo xmlns:x=\"http://baz\"><x:bar/></x:foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://baz");
        Bar2 bar = xp.getNode("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bar);
    }
    
    @Test
    public void testGetNodeBuffering() {
        String data = "<x:foo xmlns:x=\"http://baz\"><x:bar/></x:foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        xp.setBuffering(true);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://baz");
        Bar2 bar = xp.getNode("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bar);
        bar = xp.getNode("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bar);
    }
    
    @Test
    public void testGetNodeNamespace2() {
        String data = "<z:foo xmlns:z=\"http://baz\"><z:bar/></z:foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://baz");
        Bar2 bar = xp.getNode("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bar);
    }
    
    @Test
    public void testGetNodeNamespace3() {
        String data = "<x:foo xmlns:x=\"http://foo\" xmlns:z=\"http://baz\"><z:bar/></x:foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://foo");
        map.put("y", "http://baz");
        Bar2 bar = xp.getNode("/x:foo/y:bar", map, Bar2.class);
        assertNotNull(bar);
    }
    
    @Test
    public void testGetNodeDefaultNamespace() {
        String data = "<foo xmlns=\"http://baz\"><bar/></foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://baz");
        Bar2 bar = xp.getNode("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bar);
    }

    @Test
    public void testGetNodesNoNamespace() {
        InputStream is = new ByteArrayInputStream("<foo><bar/><bar/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        Bar[] bars = xp.getNodes("/foo/bar", Bar.class);
        assertNotNull(bars);
        assertEquals(2, bars.length);
        assertNotSame(bars[0], bars[1]);
    }
    
    @Test
    public void testGetNodesNamespace() {
        String data = "<x:foo xmlns:x=\"http://baz\"><x:bar/><x:bar/></x:foo>"; 
        InputStream is = new ByteArrayInputStream(data.getBytes());
        XMLSource xp = new XMLSource(is);
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("x", "http://baz");
        Bar2[] bars = xp.getNodes("/x:foo/x:bar", map, Bar2.class);
        assertNotNull(bars);
        assertNotNull(bars);
        assertEquals(2, bars.length);
        assertNotSame(bars[0], bars[1]);
    }
    
    @Test
    public void testGetStringValue() {
        InputStream is = new ByteArrayInputStream("<foo><bar/><bar id=\"2\"/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        String value = xp.getValue("/foo/bar/@id");
        assertEquals("2", value);
    }
    
    @Test
    public void testGetRelativeLink() {
        InputStream is = new ByteArrayInputStream("<foo><bar/><bar href=\"/2\"/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        URI value = xp.getLink("/foo/bar/@href");
        assertEquals("/2", value.toString());
    }
    
    @Test
    public void testBaseURI() {
        InputStream is = new ByteArrayInputStream(
            "<foo xml:base=\"http://bar\"><bar/><bar href=\"/2\"/></foo>".getBytes());
        XMLSource xp = new XMLSource(is);
        URI value = xp.getBaseURI();
        assertEquals("http://bar", value.toString());
    }
    
    @XmlRootElement
    private static class Bar {
        
    }
    
    @XmlRootElement(name = "bar", namespace = "http://baz")
    private static class Bar2 {
        
    }
}
