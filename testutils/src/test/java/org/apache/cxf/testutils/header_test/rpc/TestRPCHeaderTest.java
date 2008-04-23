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
package org.apache.cxf.testutils.header_test.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.jws.WebParam;

import org.apache.header_test.rpc.TestRPCHeader;
import org.junit.Assert;
import org.junit.Test;


public class TestRPCHeaderTest extends Assert {
    Class<TestRPCHeader> cls = TestRPCHeader.class;

    @Test
    public void testHeader1() {
        Method meths[] = cls.getMethods();
        for (Method m : meths) {
            if ("testHeader1".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                assertEquals(1, annotations[1].length);
                assertTrue(annotations[1][0] instanceof WebParam);
                WebParam parm = (WebParam)annotations[1][0];
                assertEquals("http://apache.org/header_test/rpc/types", parm.targetNamespace());
                assertEquals("inHeader", parm.partName());
                assertEquals("headerMessage", parm.name());
                assertTrue(parm.header());
            }
        }
        
    }

    @Test
    public void testInOutHeader() {
        Method meths[] = cls.getMethods();
        for (Method m : meths) {
            if ("testInOutHeader".equals(m.getName())) {
                Annotation annotations[][] = m.getParameterAnnotations();
                assertEquals(2, annotations.length);
                assertEquals(1, annotations[1].length);
                assertTrue(annotations[1][0] instanceof WebParam);
                WebParam parm = (WebParam)annotations[1][0];
                assertEquals("http://apache.org/header_test/rpc/types", parm.targetNamespace());
                assertEquals("inOutHeader", parm.partName());
                assertEquals("headerMessage", parm.name());
                assertTrue(parm.header());
            }
        }
    }

}
