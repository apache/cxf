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
import java.lang.reflect.Type;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ASMHelperTest {
    @Test
    public void testEnumParamType() throws Exception {
        Method method = EnumTest.class.getMethod("test", new Class[] {
            EnumObject.class
        });
        Type[] types = method.getGenericParameterTypes();
        ASMHelper helper = new ASMHelperImpl();
        String classCode = helper.getClassCode(types[0]);
        assertEquals("Lorg/apache/cxf/common/util/ASMHelperTest$EnumObject<Ljava/lang/Enum;>;", classCode);
    }

    @Test
    public void testLoader() throws Exception {
        CustomLoader cl = new CustomLoader(new ExtensionManagerBus());
        Class<?> clz = cl.createCustom();
        assertNotNull(clz);
        assertTrue(cl.isFound());
    }
    public class CustomLoader extends ClassGeneratorClassLoader {
        public CustomLoader(Bus bus) {
            super(bus);
        }
        public Class<?> createCustom() {
            ASMHelper helper = new ASMHelperImpl();
            ASMHelper.ClassWriter cw = helper.createClassWriter();
            OpcodesProxy opCodes = helper.getOpCodes();
            cw.visit(opCodes.V1_5, opCodes.ACC_PUBLIC + opCodes.ACC_SUPER, "test/testClass", null,
                    "java/lang/Object", null);
            cw.visitEnd();

            return loadClass("test.testClass", ASMHelperTest.class, cw.toByteArray());
        }
        public boolean isFound() {
            Class<?> cls = findClass("test.testClass", ASMHelperTest.class);
            return cls != null;
        }
    }
    public class EnumObject<E extends Enum<E>> {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String param) {
            this.name = param;
        }
    }

    public class EnumTest {
        public <T extends Enum<T>> EnumObject<T> test(EnumObject<T> o) {
            return o;
        }
    }

}