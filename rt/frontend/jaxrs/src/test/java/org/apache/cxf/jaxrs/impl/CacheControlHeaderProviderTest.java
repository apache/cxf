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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.CacheControl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheControlHeaderProviderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfNull() {
        CacheControl.valueOf(null);
    }

    @Test
    public void testFromSimpleString() {
        CacheControl c = CacheControl.valueOf(
            "public,must-revalidate");
        assertFalse(c.isPrivate() && !c.isNoStore()
                   && c.isMustRevalidate() && !c.isProxyRevalidate());
        assertFalse(c.isNoCache()
                   && !c.isNoTransform() && c.getNoCacheFields().isEmpty()
                   && c.getPrivateFields().isEmpty());
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

    @Test(expected = InternalServerErrorException.class)
    public void testInvalidSeparator() {
        CacheControlHeaderProvider cp = new CacheControlHeaderProvider() {
            protected Message getCurrentMessage() {
                Message m = new MessageImpl();
                m.put(CacheControlHeaderProvider.CACHE_CONTROL_SEPARATOR_PROPERTY, "(e+)+");
                return m;
            }
        };
        cp.fromString("no-store");
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
    public void testMultiplePrivateFields() {
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.getPrivateFields().add("a");
        cc.getPrivateFields().add("b");
        assertTrue(cc.toString().contains("private=\"a,b\""));
    }

    @Test
    public void testMultipleNoCacheFields() {
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.getNoCacheFields().add("c");
        cc.getNoCacheFields().add("d");
        assertTrue(cc.toString().contains("no-cache=\"c,d\""));
    }

    @Test
    public void testReadMultiplePrivateAndNoCacheFields() {
        String s = "private=\"foo1,foo2\",no-store,no-transform,"
            + "must-revalidate,proxy-revalidate,max-age=2,s-maxage=3,no-cache=\"bar1,bar2\","
            + "ext=1";
        CacheControl cc = CacheControl.valueOf(s);

        assertTrue(cc.isPrivate());
        List<String> privateFields = cc.getPrivateFields();
        assertEquals(2, privateFields.size());
        assertEquals("foo1", privateFields.get(0));
        assertEquals("foo2", privateFields.get(1));
        assertTrue(cc.isNoCache());
        List<String> noCacheFields = cc.getNoCacheFields();
        assertEquals(2, noCacheFields.size());
        assertEquals("bar1", noCacheFields.get(0));
        assertEquals("bar2", noCacheFields.get(1));

        assertTrue(cc.isNoStore());
        assertTrue(cc.isNoTransform());
        assertTrue(cc.isMustRevalidate());
        assertTrue(cc.isProxyRevalidate());
        assertEquals(2, cc.getMaxAge());
        assertEquals(3, cc.getSMaxAge());

        Map<String, String> exts = cc.getCacheExtension();
        assertEquals(1, exts.size());
        assertEquals("1", exts.get("ext"));
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
