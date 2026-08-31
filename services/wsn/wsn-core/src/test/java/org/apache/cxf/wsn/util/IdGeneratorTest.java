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
package org.apache.cxf.wsn.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdGeneratorTest {

    @Test
    public void testIdsAreNotSequential() {
        IdGenerator generator = new IdGenerator();
        String first = generator.generateId();
        String second = generator.generateId();

        // The variable part must not be a small sequential counter: a client seeing
        // its own id must not be able to derive the ids handed out to other clients
        String firstSuffix = first.substring(first.lastIndexOf(':') + 1);
        String secondSuffix = second.substring(second.lastIndexOf(':') + 1);
        assertFalse("id suffix must not be a bare counter", firstSuffix.matches("[0-9]+"));
        assertFalse("id suffix must not be a bare counter", secondSuffix.matches("[0-9]+"));
        assertTrue("id suffix must carry at least 122 bits of randomness",
                   firstSuffix.length() >= 32);
    }

    @Test
    public void testIdsAreUnique() {
        IdGenerator generator = new IdGenerator();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue("duplicate id generated", ids.add(generator.generateSanitizedId()));
        }
    }
}