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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReflectionUtilTest {

    private static class MultiBridgeExample {
        public MultiBridgeExample returnThis() {
            return this;
        }
    }

    private static class MultiBridgeExample2 extends MultiBridgeExample {
        public MultiBridgeExample2 returnThis() {
            return this;
        }
    }

    private static class MultiBridgeExample3 extends MultiBridgeExample2 {
        public MultiBridgeExample3 returnThis() {
            return this;
        }

        // Also has two bridge methods.
    }

    @Test
    public void testFindBridges() {
        MultiBridgeExample3 example = new MultiBridgeExample3();

        Method real = null;
        List<Method> bridges = new ArrayList<>(2);

        for (Method m : example.getClass().getDeclaredMethods()) {
            if (m.isBridge()) {
                bridges.add(m);
            } else {
                assertNull(real);
                real = m;
            }
        }

        assertEquals(2, bridges.size());
        assertNotNull(real);

        Collection<Method> bridgesOfReal = ReflectionUtil.findBridges(real);
        Collection<Method> bridgesOfBridge = ReflectionUtil.findBridges(bridges.get(0));

        assertEquals(bridges, new ArrayList<>(bridgesOfReal));
        assertTrue(bridgesOfBridge.isEmpty());

    }
}
