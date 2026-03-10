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


import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;

import org.apache.cxf.validation.BeanValidationProvider;
import org.apache.cxf.validation.ValidationConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PackageUtilsTest {
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
            Collections.singletonList(this.getClass()));
        assertEquals(this.getClass().getPackage().getName(), packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesInSamePackage() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(Integer.class, Number.class));
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
    @Test
    public void testSharedPackageNameManyClassesCommonRoot4() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(org.apache.cxf.common.util.PackageUtils.class,
                    org.apache.cxf.bus.CXFBusFactory.class,
                    org.apache.cxf.common.jaxb.JAXBContextCache.class));
        assertEquals("org.apache.cxf", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesCommonRoot5() throws Exception {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(java.lang.annotation.Annotation.class,
                    org.apache.cxf.bus.CXFBusFactory.class,
                    org.apache.cxf.common.jaxb.JAXBContextCache.class));
        assertEquals("", packageName);
    }
    @Test
    public void testSharedPackageNameManyClassesCommonRoot6() {
        String packageName = PackageUtils.getSharedPackageName(
            Arrays.asList(org.apache.cxf.bus.spring.BusApplicationContext.class,
                    org.apache.cxf.configuration.spring.JAXBBeanFactory.class));
        assertEquals("org.apache.cxf", packageName);
    }
    @Test
    public void testSharedPackageNameIgnoreProxyClasses() {
        // build any proxy object resulting in com.sun.proxy...
        Object proxy = ProxyHelper.getProxy(BeanValidationProvider.class.getClassLoader(),
           new Class[]{Serializable.class}, new ReflectionInvokationHandler(new ValidationConfiguration()));
        String packageName = PackageUtils.getSharedPackageName(
           Arrays.asList(proxy.getClass(), org.apache.cxf.bus.spring.BusApplicationContext.class,
              org.apache.cxf.configuration.spring.JAXBBeanFactory.class));
        assertEquals("org.apache.cxf", packageName);
    }

    @Test
    public void testParsePackageName() throws Exception {
        assertEquals("com.example.test.passed",
                PackageUtils.parsePackageName("http://www.example.com/test:passed", " "));
        assertEquals("org.apache.cxf.no_body_parts.wsdl",
                PackageUtils.parsePackageName("urn:org:apache:cxf:no_body_parts/wsdl", ""));
    }

    @Test
    public void testGetPackageNameByNameSpaceURI() throws Exception {
        assertEquals("com.iona.cxf", PackageUtils.getPackageNameByNameSpaceURI("http://www.cxf.iona.com"));
        assertEquals("com.iona.cxf", PackageUtils.getPackageNameByNameSpaceURI("https://www.cxf.iona.com"));
        assertEquals("com.iona._class", PackageUtils.getPackageNameByNameSpaceURI("urn:www.class.iona.com"));
        assertEquals("uri.cxf_apache_org.jstest",
                PackageUtils.getPackageNameByNameSpaceURI("uri:cxf.apache.org:jstest"));
        assertEquals("soapinterface.ems.esendex.com",
                PackageUtils.getPackageNameByNameSpaceURI("com.esendex.ems.soapinterface"));
        assertEquals("ddd.cc.bb.aa._int.fff_v01_00",
                PackageUtils.getPackageNameByNameSpaceURI("http://aa.bb.cc.ddd/Int/fff-v01.00"));
        assertEquals("ddd.cc.bb.aa._int.fff_v01_00",
                PackageUtils.getPackageNameByNameSpaceURI("https://aa.bb.cc.ddd/Int/fff-v01.00"));
        assertEquals("org.apache.cxf._case",
                PackageUtils.getPackageNameByNameSpaceURI("http://www.case.cxf.apache.org"));
        assertEquals("org.apache.cxf._case",
                PackageUtils.getPackageNameByNameSpaceURI("https://www.case.cxf.apache.org"));
        assertEquals("org.apache.cxf._case",
                PackageUtils.getPackageNameByNameSpaceURI("http://www.Case.cxf.apache.org"));
        assertEquals("org.apache.cxf._case",
                PackageUtils.getPackageNameByNameSpaceURI("https://www.Case.cxf.apache.org"));
    }

    @Test
    public void testGetNamespace() throws Exception {
        final String packageName = PackageUtils.getNamespace(getClass().getPackage().getName());
        assertEquals("http://util.common.cxf.apache.org/", packageName);
    }


}