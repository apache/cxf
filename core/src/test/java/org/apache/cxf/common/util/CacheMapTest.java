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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


/**
 * 
 */
public class CacheMapTest {

    @Test
    public void testRemove() {
        Map<Object, Object> definitions = new CacheMap<>();
        
        Object putValue = new Object();
        
        String putKey = "test";
        definitions.put(putKey, putValue);
        
        Assert.assertNull(definitions.remove(null));

        String removeKey = new String("test");
        Object removeValue = definitions.remove(removeKey);
        
        Assert.assertEquals(putKey, removeKey);
        Assert.assertEquals(putValue, removeValue);
        Assert.assertTrue(definitions.isEmpty());
        Assert.assertNull(definitions.remove(null));
    }

}
