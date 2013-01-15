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

import org.junit.Assert;
import org.junit.Test;

public class ASMHelperTest extends Assert {
    @Test
    public void testEnumParamType() throws Exception {
        Method method = EnumTest.class.getMethod("test", new Class[] {
            EnumObject.class
        });
        Type[] types = method.getGenericParameterTypes();
        String classCode = ASMHelper.getClassCode(types[0]);
        assertEquals("Lorg/apache/cxf/common/util/ASMHelperTest$EnumObject<Ljava/lang/Enum;>;", classCode);
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
