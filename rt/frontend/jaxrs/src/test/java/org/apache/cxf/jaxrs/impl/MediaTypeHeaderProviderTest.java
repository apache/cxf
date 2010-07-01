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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class MediaTypeHeaderProviderTest extends Assert {
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullValue() throws Exception {
        MediaType.valueOf(null);
    }
    
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
        MediaType mt = MediaType.valueOf("text/html;q=1234;b=4321");
        
        assertEquals("text", mt.getType());
        assertEquals("html", mt.getSubtype());
        Map<String, String> params2 = mt.getParameters();
        assertEquals(2, params2.size());
        assertEquals("1234", params2.get("q"));
        assertEquals("4321", params2.get("b"));
    }
    
    @Test
    public void testSimpleToString() {
        MediaTypeHeaderProvider provider = 
            new MediaTypeHeaderProvider();
        
        assertEquals("simple media type is not serialized", "text/plain",
                     provider.toString(new MediaType("text", "plain")));
    }
    
    @Test
    public void testHeaderFileName() {

        String fileName = "version_2006&#65288;3&#65289;.pdf";
        String header = "application/octet-stream; name=\"%s\"";
        String value = String.format(header, fileName);
        
        MediaTypeHeaderProvider provider = new MediaTypeHeaderProvider();
        MediaType mt = provider.fromString(value);
        assertEquals("application", mt.getType());
        assertEquals("octet-stream", mt.getSubtype());
        Map<String, String> params = mt.getParameters();
        assertEquals(1, params.size());
        assertEquals("\"version_2006&#65288;3&#65289;.pdf\"", params.get("name"));
        
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
