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

package org.apache.cxf.jaxrs.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.Customer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class AnnotationTestUtilsTest {

    @Test
    public void testGetAnnotatedMethodFromInterface() throws Exception {

        Method m =
            Customer.class.getMethod("setUriInfoContext",
                                     new Class[]{UriInfo.class});
        assertEquals(0, m.getAnnotations().length);
        assertEquals(0, m.getParameterAnnotations()[0].length);
        Method annotatedMethod = AnnotationUtils.getAnnotatedMethod(Customer.class, m);
        assertNotSame(m, annotatedMethod);
        assertEquals(1, annotatedMethod.getParameterAnnotations()[0].length);
    }

    @Test
    public void testGetAnnotatedMethodFromClass() throws Exception {

        Method m =
            Customer.class.getMethod("getContextResolver",
                                     new Class[]{});
        assertEquals(0, m.getAnnotations().length);
        Method annotatedMethod = AnnotationUtils.getAnnotatedMethod(Customer.class, m);
        assertSame(m, annotatedMethod);
    }

    @Test
    public void testCustomHttpMethodValue() throws Exception {
        Method m = ResourceClass.class.getMethod("update", new Class[]{});
        assertEquals("UPDATE", AnnotationUtils.getHttpMethodValue(m));
    }

    @Target({ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("UPDATE")
    public @interface UPDATE {
    }

    public class ResourceClass {
        @UPDATE
        public void update() {

        }
    }
}