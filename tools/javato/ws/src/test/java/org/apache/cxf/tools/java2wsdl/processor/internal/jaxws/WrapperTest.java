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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.lang.reflect.Method;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.model.JavaClass;
import org.junit.Assert;
import org.junit.Test;

public class WrapperTest extends Assert {
    @Test
    public void testGetWrapperBeanClassFromQName() {
        QName qname = new QName("http://cxf.apache.org", "sayHi");

        Wrapper wrapper = new Wrapper();
        wrapper.setName(qname);

        JavaClass jClass = wrapper.getWrapperBeanClass(qname);

        assertEquals("org.apache.cxf", jClass.getPackageName());
        assertEquals("SayHi", jClass.getName());
        assertEquals("http://cxf.apache.org", jClass.getNamespace());
    }

    @Test
    public void testGetWrapperBeanClassFromMethod() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.classnoanno.docwrapped";
        Class<?> stockClass = Class.forName(pkgName + ".Stock");
        Method method = stockClass.getMethod("getPrice", String.class);

        Wrapper wrapper = new Wrapper();
        wrapper.setMethod(method);

        JavaClass jClass = wrapper.getWrapperBeanClass(method);
        assertNotNull(jClass);
        assertNull(jClass.getPackageName());
        assertNull(jClass.getName());

        wrapper = new RequestWrapper();
        jClass = wrapper.getWrapperBeanClass(method);
        assertEquals("GetPrice", jClass.getName());
        assertEquals(pkgName + ".jaxws", jClass.getPackageName());

        wrapper = new ResponseWrapper();
        jClass = wrapper.getWrapperBeanClass(method);
        assertEquals("GetPriceResponse", jClass.getName());
        assertEquals(pkgName + ".jaxws", jClass.getPackageName());
    }
    
    @Test
    public void testIsWrapperBeanClassNotExist() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.classnoanno.docwrapped";
        Class<?> stockClass = Class.forName(pkgName + ".Stock");
        Method method = stockClass.getMethod("getPrice", String.class);
        
        Wrapper wrapper = new RequestWrapper();
        wrapper.setMethod(method);
        assertTrue(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName + ".jaxws", wrapper.getJavaClass().getPackageName());
        assertEquals("GetPrice", wrapper.getJavaClass().getName());
        
        pkgName = "org.apache.cxf.tools.fortest.withannotation.doc";
        stockClass = Class.forName(pkgName + ".Stock");
        method = stockClass.getMethod("getPrice", String.class);

        wrapper = new RequestWrapper();
        wrapper.setMethod(method);
        assertFalse(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName + ".jaxws", wrapper.getJavaClass().getPackageName());
        assertEquals("GetPrice", wrapper.getJavaClass().getName());

        pkgName = "org.apache.cxf.tools.fortest.withannotation.doc";
        Class<?> clz = Class.forName(pkgName + ".Greeter");
        method = clz.getMethod("sayHi");

        wrapper = new RequestWrapper();
        wrapper.setMethod(method);
        assertFalse(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName, wrapper.getJavaClass().getPackageName());
        assertEquals("SayHi", wrapper.getJavaClass().getName());

        wrapper = new ResponseWrapper();
        wrapper.setMethod(method);
        assertEquals(pkgName, wrapper.getJavaClass().getPackageName());
        assertEquals("SayHiResponse", wrapper.getJavaClass().getName());
    }
}
