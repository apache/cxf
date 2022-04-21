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

package org.apache.cxf.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CollectionUtilsTest {

    @Test
    public void testDiff() throws Exception {
        Collection<String> l1 = Arrays.asList("1", "2", "3");
        Collection<String> l2 = Arrays.asList("2", "4", "5");
        Collection<String> l3 = CollectionUtils.diff(l1, l2);
        assertEquals(2, l3.size());
        assertTrue(l3.contains("1"));
        assertTrue(l3.contains("3"));

        l3 = CollectionUtils.diff(l1, null);
        assertEquals(3, l3.size());
        assertTrue(l3.containsAll(l1));

        l3 = CollectionUtils.diff(null, null);
        assertNull(l3);
    }

    @Test
    public void testIsEmpty() throws Exception {
        Collection<String> c = null;
        Map<String, String> m = null;
        assertTrue(CollectionUtils.isEmpty(c));
        assertTrue(CollectionUtils.isEmpty(m));

        Collection<String> l = Arrays.asList(null, null);
        assertTrue(CollectionUtils.isEmpty(l));
    }

    @Test
    public void testToDictionaryNull() throws Exception {
        Dictionary<?, ?> d = CollectionUtils.toDictionary(null);
        assertNull(d.elements());
        assertNull(d.get(""));
        assertTrue(d.isEmpty());
        assertNull(d.keys());
        assertEquals(0, d.size());
    }

    @Test
    public void testSingletonDictionary() throws Exception {
        String key = "k";
        String value = "v";
        Dictionary<String, String> d = CollectionUtils.singletonDictionary(key, value);
        assertNotNull(d.elements());
        assertEquals(value, d.get(key));
        assertFalse(d.isEmpty());
        assertNotNull(d.keys());
        assertEquals(1, d.size());
    }
}