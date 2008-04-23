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

package org.apache.cxf.tools.util;

import org.junit.Assert;
import org.junit.Test;


public class NameUtilTest extends Assert {
    
    @Test
    public void testMangleToClassName() {
        assertEquals("Abc100Xyz",
                     NameUtil.mangleNameToClassName("abc100xyz"));
        assertEquals("NameUtilTest",
                     NameUtil.mangleNameToClassName("name_util_test"));
        assertEquals("NameUtil",
                     NameUtil.mangleNameToClassName("nameUtil"));
        assertEquals("Int", NameUtil.mangleNameToClassName("int"));
    }

    @Test
    public void testMangleToVariableName() {
        assertEquals("abc100Xyz",
                     NameUtil.mangleNameToVariableName("abc100xyz"));
        assertEquals("nameUtilTest",
                     NameUtil.mangleNameToVariableName("name_util_test"));
        assertEquals("nameUtil",
                     NameUtil.mangleNameToVariableName("NameUtil"));
        assertEquals("int",
                     NameUtil.mangleNameToVariableName("int"));
    }

    @Test
    public void testIsJavaIdentifier() {
        assertFalse(NameUtil.isJavaIdentifier("int"));
    }
}
