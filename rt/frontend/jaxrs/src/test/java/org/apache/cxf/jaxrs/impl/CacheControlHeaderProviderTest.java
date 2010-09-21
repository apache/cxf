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

import javax.ws.rs.core.CacheControl;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Assert;
import org.junit.Test;

public class CacheControlHeaderProviderTest extends Assert {
    
    @Test
    public void testFromSimpleString() {
        CacheControl c = CacheControl.valueOf(
            "public,must-revalidate");
        assertTrue(!c.isPrivate() && !c.isNoStore()
                   && c.isMustRevalidate() && !c.isProxyRevalidate());
        assertTrue(!c.isNoCache()
                   && !c.isNoTransform() && c.getNoCacheFields().size() == 0
                   && c.getPrivateFields().size() == 0);
    }
    
    @Test
    public void testFromComplexString() {
        CacheControl c = CacheControl.valueOf(
            "private=\"foo\",no-cache=\"bar\",no-store,no-transform,"
            + "must-revalidate,proxy-revalidate,max-age=2,s-maxage=3");
        assertTrue(c.isPrivate() && c.isNoStore()
                   && c.isMustRevalidate() && c.isProxyRevalidate() && c.isNoCache());
        assertTrue(c.isNoTransform() && c.getNoCacheFields().size() == 1
                   && c.getPrivateFields().size() == 1);
        assertEquals("foo", c.getPrivateFields().get(0));
        assertEquals("bar", c.getNoCacheFields().get(0));
        
    }
    
    @Test
    public void testFromComplexStringWithSemicolon() {
        CacheControlHeaderProvider cp = new CacheControlHeaderProvider() {
            protected Message getCurrentMessage() {
                Message m = new MessageImpl();
                m.put(CacheControlHeaderProvider.CACHE_CONTROL_SEPARATOR_PROPERTY, ";");
                return m;
            }
        };
        CacheControl c = cp.fromString(
            "private=\"foo\";no-cache=\"bar\";no-store;no-transform;"
            + "must-revalidate;proxy-revalidate;max-age=2;s-maxage=3");
        assertTrue(c.isPrivate() && c.isNoStore()
                   && c.isMustRevalidate() && c.isProxyRevalidate() && c.isNoCache());
        assertTrue(c.isNoTransform() && c.getNoCacheFields().size() == 1
                   && c.getPrivateFields().size() == 1);
        assertEquals("foo", c.getPrivateFields().get(0));
        assertEquals("bar", c.getNoCacheFields().get(0));
        
    }
    
    
    @Test
    public void testToString() {
        String s = "private=\"foo\",no-cache=\"bar\",no-store,no-transform,"
            + "must-revalidate,proxy-revalidate,max-age=2,s-maxage=3";
        String parsed = CacheControl.valueOf(s).toString();
        assertEquals(s, parsed);       
    }
    
    @Test
    public void testNoCacheEnabled() {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        assertEquals("no-cache,no-transform", cc.toString());
    }
    
    @Test
    public void testNoCacheDisabled() {
        CacheControl cc = new CacheControl();
        cc.setNoCache(false);
        assertEquals("no-transform", cc.toString());
    }
    
    @Test
    public void testCacheExtensionToString() {
        CacheControl cc = new CacheControl();
        cc.getCacheExtension().put("ext1", null);
        cc.getCacheExtension().put("ext2", "value2");
        cc.getCacheExtension().put("ext3", "value 3");
        String value = cc.toString();
        assertTrue(value.indexOf("ext1") != -1 && value.indexOf("ext1=") == -1);
        assertTrue(value.indexOf("ext2=value2") != -1);
        assertTrue(value.indexOf("ext3=\"value 3\"") != -1);
    }
    
}
