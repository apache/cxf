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

package org.apache.cxf.jaxrs.json.basic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class JsonMapObjectReaderWriterTest extends Assert {

    @Test
    public void testWriteMap() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("a", "aValue");
        map.put("b", 123);
        map.put("c", Collections.singletonList("cValue"));
        map.put("claim", null);
        String json = new JsonMapObjectReaderWriter().toJson(map);
        assertEquals("{\"a\":\"aValue\",\"b\":123,\"c\":[\"cValue\"],\"claim\":null}", 
                     json);
    }
    @Test
    public void testReadMap() throws Exception {
        String json = "{\"a\":\"aValue\",\"b\":123,\"c\":[\"cValue\"],\"f\":null}";
        Map<String, Object> map = new JsonMapObjectReaderWriter().fromJson(json);
        assertEquals(4, map.size());
        assertEquals("aValue", map.get("a"));
        assertEquals(123L, map.get("b"));
        assertEquals(Collections.singletonList("cValue"), map.get("c"));
        assertNull(map.get("f"));
    }
}
