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
package org.apache.cxf.helpers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JavaUtilsTest {

    private boolean originalFipsEnabled;
    private String originalFipsProvider;

    @Before
    public void saveFipsState() throws Exception {
        originalFipsEnabled = JavaUtils.isFIPSEnabled();
        originalFipsProvider = JavaUtils.getFIPSSecurityProvider();
    }

    @After
    public void restoreFipsState() throws Exception {
        FipsTestUtils.setFipsEnabled(originalFipsEnabled);
        FipsTestUtils.setFipsProvider(originalFipsProvider);
    }

    @Test
    public void testIsFIPSEnabledDefaultFalse() {
        // Without the fips.enabled system property, FIPS should be disabled
        assertFalse("FIPS should be disabled by default", JavaUtils.isFIPSEnabled());
    }

    @Test
    public void testSetFIPSEnabled() throws Exception {
        FipsTestUtils.setFipsEnabled(true);
        assertTrue("FIPS should be enabled after setting the field", JavaUtils.isFIPSEnabled());

        FipsTestUtils.setFipsEnabled(false);
        assertFalse("FIPS should be disabled after clearing the field", JavaUtils.isFIPSEnabled());
    }

    @Test
    public void testGetFIPSSecurityProviderDefault() {
        // Without the fips.security.provider property, should return null
        assertNull("FIPS security provider should be null by default",
                   JavaUtils.getFIPSSecurityProvider());
    }

    @Test
    public void testGetFIPSSecurityProviderCustom() throws Exception {
        FipsTestUtils.setFipsProvider("BouncyCastleFIPS");
        assertEquals("BouncyCastleFIPS", JavaUtils.getFIPSSecurityProvider());
    }

    @Test
    public void testIsJavaKeyword() {
        assertTrue(JavaUtils.isJavaKeyword("class"));
        assertTrue(JavaUtils.isJavaKeyword("public"));
        assertTrue(JavaUtils.isJavaKeyword("null"));
        assertTrue(JavaUtils.isJavaKeyword("true"));
        assertFalse(JavaUtils.isJavaKeyword("foo"));
        assertFalse(JavaUtils.isJavaKeyword("Class"));
    }

    @Test
    public void testMakeNonJavaKeyword() {
        assertEquals("_class", JavaUtils.makeNonJavaKeyword("class"));
        assertEquals("_public", JavaUtils.makeNonJavaKeyword("public"));
    }
}
