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
package org.apache.cxf.jaxrs.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Assert;
import org.junit.Test;

public class XMLSourceTest extends Assert {

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
    
    @XmlRootElement
    private static class Bar {
        
    }
    
    @XmlRootElement(name = "bar", namespace = "http://baz")
    private static class Bar2 {
        
    }
}
