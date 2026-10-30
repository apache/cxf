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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReflectionUtilTest {

    @Test
    public void testGetMethodSignature() throws Exception {
        assertEquals("update(java.lang.String,int)",
            ReflectionUtil.getMethodSignature(Impl.class.getMethod("update", String.class, int.class)));
        assertEquals("update()", ReflectionUtil.getMethodSignature(Impl.class.getMethod("update")));
    }

    @Test
    public void testGetGenericallyOverriddenMethods() throws Exception {
        List<Method> overridden = ReflectionUtil.getGenericallyOverriddenMethods(Impl.class,
            Impl.class.getMethod("update", String.class));
        assertEquals(Collections.singletonList(Repository.class.getMethod("update", Object.class)),
                     overridden);
    }

    @Test
    public void testGetGenericallyOverriddenMethodsIgnoresUnrelatedOverloads() throws Exception {
        assertTrue(ReflectionUtil.getGenericallyOverriddenMethods(Impl.class,
            Impl.class.getMethod("update", Integer.class)).isEmpty());
        assertTrue(ReflectionUtil.getGenericallyOverriddenMethods(Impl.class,
            Impl.class.getMethod("update")).isEmpty());
    }

    @Test
    public void testGetGenericallyOverriddenMethodsWithInheritedImplementation() throws Exception {
        List<Method> overridden = ReflectionUtil.getGenericallyOverriddenMethods(InheritingImpl.class,
            InheritingImpl.class.getMethod("update", String.class));
        assertEquals(Collections.singletonList(Repository.class.getMethod("update", Object.class)),
                     overridden);
    }

    public interface Repository<T> {
        void update(T value);
    }

    public abstract static class AbstractRepository<T> implements Repository<T> { // NOPMD
    }

    public static class Impl extends AbstractRepository<String> {
        @Override
        public void update(String value) {
        }

        public void update(Integer value) {
        }

        public void update(String value, int count) {
        }

        public void update() {
        }
    }

    public static class StringUpdater {
        public void update(String value) {
        }
    }

    public static class InheritingImpl extends StringUpdater implements Repository<String> {
    }
}
