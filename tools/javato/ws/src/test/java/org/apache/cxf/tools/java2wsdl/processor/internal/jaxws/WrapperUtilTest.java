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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WrapperUtilTest extends Assert {
    String pkgName = "org.apache.cxf.tools.fortest.classnoanno.docwrapped";
    Class<?> stockClass;
    Method method;

    @Before
    public void setUp() throws Exception {
        stockClass = Class.forName(pkgName + ".Stock");
        assertNotNull(stockClass);
        method = stockClass.getMethod("getPrice", String.class);
        assertNotNull(method);
    }

    @Test
    public void testIsWrapperClassExists() {
        assertTrue(WrapperUtil.isWrapperClassExists(method));
    }

    @Test
    public void testWrapperClassNotExists() throws Exception {
        Class<?> helloClass = Class.forName("org.apache.cxf.tools.fortest.withannotation.doc.HelloWrapped");
        assertNotNull(helloClass);
        Method helloMethod = helloClass.getMethod("sayHiWithoutWrapperClass");
        assertNotNull(helloMethod);
        assertFalse(WrapperUtil.isWrapperClassExists(helloMethod));
    }
}
