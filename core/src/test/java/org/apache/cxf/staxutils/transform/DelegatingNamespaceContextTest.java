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
package org.apache.cxf.staxutils.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DelegatingNamespaceContextTest {

    @Test
    public void testSomeAddsAndGets() throws Exception {
        DelegatingNamespaceContext dnc = getTestDelegatingNamespaceContext();

        dnc.down(); //1
        dnc.addPrefix("p1", "urn:foo1");
        dnc.addPrefix("p2", "urn:foo2");
        assertEquals("urn:foo0", dnc.getNamespaceURI("p0"));
        assertEquals("urn:foo1", dnc.getNamespaceURI("p1"));
        assertEquals("urn:foo2", dnc.getNamespaceURI("p2"));
        assertEquals("p0", dnc.getPrefix("urn:foo0"));
        assertEquals("p1", dnc.getPrefix("urn:foo1"));
        assertEquals("p2", dnc.getPrefix("urn:foo2"));
        verifyPrefixes(dnc.getPrefixes("urn:foo1"), new String[]{"p1"});
        verifyPrefixes(dnc.getPrefixes("urn:foo2"), new String[]{"p2"});

        dnc.down(); //2
        dnc.addPrefix("p11", "urn:foo1");
        dnc.addPrefix("p2", "urn:foo22");
        dnc.addPrefix("p3", "urn:foo3");
        assertEquals("urn:foo1", dnc.getNamespaceURI("p1"));
        assertEquals("urn:foo1", dnc.getNamespaceURI("p11"));
        assertEquals("urn:foo22", dnc.getNamespaceURI("p2"));
        assertEquals("urn:foo3", dnc.getNamespaceURI("p3"));
        String p = dnc.getPrefix("urn:foo1");
        assertTrue("p1".equals(p) || "p11".equals(p));
        assertNull(dnc.getPrefix("urn:foo2"));
        assertEquals("p2", dnc.getPrefix("urn:foo22"));
        assertEquals("p3", dnc.getPrefix("urn:foo3"));
        p = dnc.findUniquePrefix("urn:foo4");
        assertNotNull(p);
        assertEquals(p, dnc.getPrefix("urn:foo4"));
        assertEquals("urn:foo4", dnc.getNamespaceURI(p));
        verifyPrefixes(dnc.getPrefixes("urn:foo1"), new String[]{"p1", "p11"});
        verifyPrefixes(dnc.getPrefixes("urn:foo2"), new String[]{});
        verifyPrefixes(dnc.getPrefixes("urn:foo22"), new String[]{"p2"});
        verifyPrefixes(dnc.getPrefixes("urn:foo3"), new String[]{"p3"});

        dnc.up(); //1
        assertEquals("urn:foo1", dnc.getNamespaceURI("p1"));
        assertNull(dnc.getNamespaceURI("p11"));
        assertEquals("urn:foo2", dnc.getNamespaceURI("p2"));
        assertNull(dnc.getNamespaceURI("p3"));
        assertEquals("p1", dnc.getPrefix("urn:foo1"));
        assertNull(dnc.getPrefix("urn:foo11"));
        assertEquals("p2", dnc.getPrefix("urn:foo2"));
        assertNull(dnc.getPrefix("urn:foo22"));
        assertNull(dnc.getPrefix("urn:foo3"));
        verifyPrefixes(dnc.getPrefixes("urn:foo1"), new String[]{"p1"});
        verifyPrefixes(dnc.getPrefixes("urn:foo2"), new String[]{"p2"});
        verifyPrefixes(dnc.getPrefixes("urn:foo3"), new String[]{});

        dnc.up(); //0

        try {
            dnc.up(); //-1
            fail("not allowed to go up");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void testSomeAddsWithDuplicatedPrefixName() throws Exception {
        DelegatingNamespaceContext dnc = getTestDelegatingNamespaceContext();

        dnc.down(); // 1
        dnc.addPrefix("p00", "urn:foo0");
        dnc.addPrefix("p1", "urn:foo1");
        dnc.addPrefix("p2", "urn:foo2");
        assertEquals("urn:foo0", dnc.getNamespaceURI("p0"));
        assertEquals("urn:foo0", dnc.getNamespaceURI("p00"));
        assertEquals("urn:foo1", dnc.getNamespaceURI("p1"));
        assertEquals("urn:foo2", dnc.getNamespaceURI("p2"));
        assertTrue("p0".equals(dnc.getPrefix("urn:foo0")) || "p00".equals(dnc.getPrefix("urn:foo0")));
        assertEquals("p1", dnc.getPrefix("urn:foo1"));
        assertEquals("p2", dnc.getPrefix("urn:foo2"));
        verifyPrefixes(dnc.getPrefixes("urn:foo1"), new String[] {"p1"});
        verifyPrefixes(dnc.getPrefixes("urn:foo2"), new String[] {"p2"});
        verifyPrefixes(dnc.getPrefixes("urn:foo0"), new String[] {"p0", "p00"});
    }


    private DelegatingNamespaceContext getTestDelegatingNamespaceContext() {
        return new DelegatingNamespaceContext(
            new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return "p0".equals(prefix) ? "urn:foo0" : null;
                }
                public String getPrefix(String ns) {
                    return "urn:foo0".equals(ns) ? "p0" : null;
                }
                public Iterator<String> getPrefixes(String ns) {
                    return null;
                }
            },
            Collections.singletonMap("urn:foo5", "urn:foo55"));
    }

    private void verifyPrefixes(Iterator<String> prefixes, String[] values) {
        Set<String> tmp = new HashSet<>();
        while (prefixes.hasNext()) {
            tmp.add(prefixes.next());
        }
        for (String v : values) {
            if (tmp.contains(v)) {
                tmp.remove(v);
            } else {
                fail("not expected: " + v);
            }
        }
        assertTrue(tmp.isEmpty());
    }


}