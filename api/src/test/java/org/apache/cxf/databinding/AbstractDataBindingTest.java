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

package org.apache.cxf.databinding;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class AbstractDataBindingTest extends Assert {
    @Test(expected = IllegalArgumentException.class)
    public void testNamespaceMapCheckDuplicates() {
        Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("urn:hello.there", "ht");
        testMap.put("urn:high.temperature", "ht");
        AbstractDataBinding.checkNamespaceMap(testMap);
    }
    
    @Test
    public void testNamespaceMapOK() {
        Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("urn:hello.there", "ht");
        testMap.put("urn:high.temperature", "warm");
        AbstractDataBinding.checkNamespaceMap(testMap);
    }

}
