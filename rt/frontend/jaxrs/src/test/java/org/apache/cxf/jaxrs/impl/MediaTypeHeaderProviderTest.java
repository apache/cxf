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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class MediaTypeHeaderProviderTest extends Assert {
    
    @Test
    public void testSimpleType() {
        MediaType m = MediaType.valueOf("text/html");
        assertEquals("Media type was not parsed correctly", 
                     m, new MediaType("text", "html"));
        assertEquals("Media type was not parsed correctly", 
                     MediaType.valueOf("text/html "), new MediaType("text", "html"));
    }
    
    @Test
    public void testShortWildcard() {
        MediaType m = MediaType.valueOf("*");
        assertEquals("Media type was not parsed correctly", 
                     m, new MediaType("*", "*"));
    }
    
    @Test
    public void testShortWildcardWithParameters() {
        MediaType m = MediaType.valueOf("*;q=0.2");
        assertEquals("Media type was not parsed correctly", 
                     m, new MediaType("*", "*"));
    }
    
    @Test
    public void testBadType() {
        try {
            new MediaTypeHeaderProvider().fromString("texthtml");
            fail("Parse exception must've been thrown");
        } catch (IllegalArgumentException pe) {
            // expected
        }
        
    }
    
    @Test
    public void testBadParameter() {
        try {
            new MediaTypeHeaderProvider().fromString("text/html;*");
            fail("Parse exception must've been thrown");
        } catch (IllegalArgumentException pe) {
            // expected
        }
    }
    
    @Test
    public void testTypeWithParameters() {
        MediaType m = MediaType.valueOf("text/html;q=1234;b=4321");
        
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", "1234");
        params.put("b", "4321");
        
        MediaType expected = new MediaType("text", "html", params);
        
        assertEquals("Media type was not parsed correctly", expected, m);
    }
    
    @Test
    public void testSimpleToString() {
        MediaTypeHeaderProvider provider = 
            new MediaTypeHeaderProvider();
        
        assertEquals("simple media type is not serialized", "text/plain",
                     provider.toString(new MediaType("text", "plain")));
    }
    
    @Test
    public void testComplexToString() {
        MediaTypeHeaderProvider provider = 
            new MediaTypeHeaderProvider();
        
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("foo", "bar");
        params.put("q", "0.2");
        
        assertEquals("complex media type is not serialized", "text/plain;foo=bar;q=0.2",
                     provider.toString(new MediaType("text", "plain", params)));
        
    }

}
