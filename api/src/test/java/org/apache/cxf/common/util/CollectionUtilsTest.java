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
import java.util.Collection;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;

public class CollectionUtilsTest extends Assert {
    
    @Test
    public void testDiff() throws Exception {
        List<String> l1 = Arrays.asList(new String[]{"1", "2", "3"});
        List<String> l2 = Arrays.asList(new String[]{"2", "4", "5"});
        Collection<String> l3 = CollectionUtils.diff(l1, l2);
        assertTrue(l3.size() == 2);
        assertTrue(l3.contains("1"));
        assertTrue(l3.contains("3"));
        
        l3 = CollectionUtils.diff(l1, null);
        assertTrue(l3.size() == 3);
        assertTrue(l3.contains("1"));
        assertTrue(l3.contains("2"));
        assertTrue(l3.contains("3"));
        
        l3 = CollectionUtils.diff(null, null);
        assertNull(l3);     
    }
    
    @Test
    public void testIsEmpty() throws Exception {
        List<String> l = Arrays.asList(new String[]{null, null});
        assertNotNull(l);
        assertTrue(CollectionUtils.isEmpty(l));
    }
}
