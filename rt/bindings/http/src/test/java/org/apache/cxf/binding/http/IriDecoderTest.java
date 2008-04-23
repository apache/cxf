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
package org.apache.cxf.binding.http;

import java.util.List;

import org.apache.cxf.binding.http.IriDecoderHelper.Param;
import org.junit.Assert;
import org.junit.Test;

public class IriDecoderTest extends Assert {
    
    @Test
    public void testPaths() {
        List<Param> params = IriDecoderHelper.decodeIri("test/123.xml", "test/{id}.xml");
        assertEquals(1, params.size());
        assertEquals("id", params.get(0).getName());
        assertEquals("123", params.get(0).getValue());
        
        List<Param> p = IriDecoderHelper.decodeIri("http://host:8192/service/392/4?name=nodet", 
                "http://host:8192/service/{id}/{nb}");
        assertNotNull(p);
        //assertEquals(3, p.size());
        assertEquals(new Param("id", "392"), p.get(0));
        assertEquals(new Param("nb", "4"), p.get(1));
        assertEquals(new Param("name", "nodet"), p.get(2));
    }
}
