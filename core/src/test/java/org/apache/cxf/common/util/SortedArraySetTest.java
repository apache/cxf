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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SortedArraySetTest {

    @Test
    public void testRemove() {
        SortedArraySet<Integer> set = new SortedArraySet<>();

        assertTrue(set.add(17238));
        assertEquals(1, set.size());

        assertFalse(set.remove(17270));
        assertEquals(1, set.size());

        assertTrue(set.remove(17238));
        assertEquals(0, set.size());
    }


    @Test
    public void testAdd() {

        Random rnd = new Random(1234567);
        SortedArraySet<Integer> set = new SortedArraySet<>();
        List<Integer> inputs = IntStream.generate(rnd::nextInt)
                .limit(100).boxed().collect(Collectors.toList());
        set.addAll(inputs);
        Collections.sort(inputs);
        assertEquals(inputs, Arrays.asList(set.toArray()));
    }


    @Test
    public void testContainsAll() {
        SortedArraySet<Integer> set = new SortedArraySet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        assertTrue(set.containsAll(Arrays.asList(2, 4, 6)));
        assertFalse(set.containsAll(Arrays.asList(1, 2, 99)));
    }

}
