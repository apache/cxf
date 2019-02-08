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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyUtilsTest {
    private static final String TEST_KEY = "my.key";

    @Test
    public void testIsTrueWithMap() {
        Map<String, Object> props = new HashMap<>();
        assertFalse(PropertyUtils.isTrue(props, TEST_KEY));

        props.put(TEST_KEY, "false");
        assertFalse(PropertyUtils.isTrue(props, TEST_KEY));

        props.put(TEST_KEY, Boolean.FALSE);
        assertFalse(PropertyUtils.isTrue(props, TEST_KEY));

        props.put(TEST_KEY, "true");
        assertTrue(PropertyUtils.isTrue(props, TEST_KEY));

        props.put(TEST_KEY, Boolean.TRUE);
        assertTrue(PropertyUtils.isTrue(props, TEST_KEY));
    }

    @Test
    public void testIsFalseWithMap() {
        Map<String, Object> props = new HashMap<>();
        assertFalse(PropertyUtils.isFalse(props, TEST_KEY));

        props.put(TEST_KEY, "true");
        assertFalse(PropertyUtils.isFalse(props, TEST_KEY));

        props.put(TEST_KEY, Boolean.TRUE);
        assertFalse(PropertyUtils.isFalse(props, TEST_KEY));

        props.put(TEST_KEY, "false");
        assertTrue(PropertyUtils.isFalse(props, TEST_KEY));

        props.put(TEST_KEY, Boolean.FALSE);
        assertTrue(PropertyUtils.isFalse(props, TEST_KEY));
    }

    @Test
    public void testTrue() {
        assertTrue(PropertyUtils.isTrue(Boolean.TRUE));
        assertTrue(PropertyUtils.isTrue("true"));
        assertTrue(PropertyUtils.isTrue("TRUE"));
        assertTrue(PropertyUtils.isTrue("TrUe"));

        assertFalse(PropertyUtils.isTrue(Boolean.FALSE));
        assertFalse(PropertyUtils.isTrue("false"));
        assertFalse(PropertyUtils.isTrue("FALSE"));
        assertFalse(PropertyUtils.isTrue("FaLSE"));
        assertFalse(PropertyUtils.isTrue(null));
        assertFalse(PropertyUtils.isTrue(""));
        assertFalse(PropertyUtils.isTrue("other"));
    }

    @Test
    public void testFalse() {
        assertTrue(PropertyUtils.isFalse(Boolean.FALSE));
        assertTrue(PropertyUtils.isFalse("false"));
        assertTrue(PropertyUtils.isFalse("FALSE"));
        assertTrue(PropertyUtils.isFalse("FaLSE"));

        assertFalse(PropertyUtils.isFalse(Boolean.TRUE));
        assertFalse(PropertyUtils.isFalse("true"));
        assertFalse(PropertyUtils.isFalse("TRUE"));
        assertFalse(PropertyUtils.isFalse("TrUe"));
        assertFalse(PropertyUtils.isFalse(null));
        assertFalse(PropertyUtils.isFalse(""));
        assertFalse(PropertyUtils.isFalse("other"));
    }
}