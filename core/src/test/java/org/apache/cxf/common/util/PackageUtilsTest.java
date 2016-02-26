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


import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class PackageUtilsTest extends Assert {
    @Test
    public void testGetClassPackageName() throws Exception {
        String packageName = PackageUtils.getPackageName(this.getClass());       
        assertEquals("Should get same packageName", this.getClass().getPackage().getName(), packageName);
    }
    
    @Test
    public void testGetEmptyPackageName() throws Exception {
        String className = "HelloWorld";
        assertEquals("Should return empty string", "", PackageUtils.getPackageName(className));
    }
    
    @Test
    public void testSharedPackageNameSingleClass() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Collections.<Class<?>>singletonList(this.getClass()));       
        assertEquals(this.getClass().getPackage().getName(), packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesInSamePackage() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.<Class<?>>asList(Integer.class, Number.class));       
        assertEquals("java.lang", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesInDiffPackages() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(Integer.class, this.getClass()));       
        assertEquals("", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesCommonRoot() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(Integer.class, Annotation.class));       
        assertEquals("java.lang", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesCommonRoot2() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(Annotation.class, Integer.class));       
        assertEquals("java.lang", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesCommonRoot3() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(Annotation.class, Array.class));       
        assertEquals("java.lang", packageName);
    }
}
