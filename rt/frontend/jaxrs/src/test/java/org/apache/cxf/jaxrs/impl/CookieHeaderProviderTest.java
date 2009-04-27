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

import javax.ws.rs.core.Cookie;

import org.junit.Assert;
import org.junit.Test;

public class CookieHeaderProviderTest extends Assert {
    
        
    @Test
    public void testFromSimpleString() {
        Cookie c = Cookie.valueOf("foo=bar");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 0 == c.getVersion());
    }
    
    @Test
    public void testNoValue() {
        Cookie c = Cookie.valueOf("foo=");
        assertTrue("".equals(c.getValue())
                   && "foo".equals(c.getName()));
    }
    
    @Test
    public void testFromComplexString() {
        Cookie c = Cookie.valueOf("$Version=2;foo=bar;$Path=path;$Domain=domain");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 2 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain()));
    }
    
    @Test
    public void testToString() {
        Cookie c = new Cookie("foo", "bar", "path", "domain", 2);
        assertEquals("$Version=2;foo=bar;$Path=path;$Domain=domain", 
                     c.toString());
               
    }
    
    @Test
    public void testCookieWithQuotes() {
        Cookie c = Cookie.valueOf("$Version=\"1\"; foo=\"bar\"; $Path=\"/path\"");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "/path".equals(c.getPath())
                   && null == c.getDomain());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullValue() throws Exception {
        Cookie.valueOf(null);
    }
    
}
