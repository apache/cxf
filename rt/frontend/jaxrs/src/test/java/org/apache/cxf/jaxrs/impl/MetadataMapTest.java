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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;



public class MetadataMapTest extends Assert {
    
    @Test
    public void testPutSingle() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        List<Object> value1 = new ArrayList<Object>();
        value1.add("bar");
        value1.add("foo");
        m.put("baz", value1);
        
        m.putSingle("baz", "clazz");
        List<Object> value2 = m.get("baz");
        assertEquals("Only a single value should be in the list", 1, value2.size());
        assertEquals("Value is wrong", "clazz", value2.get(0));
    }
    
    @Test
    public void testAddAndGetFirst() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
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
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<String, Object>(m);
        m.remove("baz");
        m.add("baz", "foo");
        assertEquals("bar", m2.getFirst("baz"));
        assertEquals("foo", m.getFirst("baz"));
        
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testReadOnly() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<String, Object>(m, true, false);
        m2.remove("baz");
    }
    
    @Test
    public void testGetCaseInsensitive() {
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.add("Baz", "bar");
        MetadataMap<String, Object> m2 = new MetadataMap<String, Object>(m, true, true);
        assertEquals("bar", m2.getFirst("baZ"));
        assertEquals("bar", m2.getFirst("Baz"));
        assertTrue(m2.containsKey("BaZ"));
        assertTrue(m2.containsKey("Baz"));
        List<Object> values = m2.get("baz");
        assertEquals(1, values.size());
        assertEquals("bar", values.get(0).toString());
    }
    
    
    
}
