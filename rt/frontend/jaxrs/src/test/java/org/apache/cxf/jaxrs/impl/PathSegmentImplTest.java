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

package org.apache.cxf.jaxrs.impl;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.junit.Assert;
import org.junit.Test;

public class PathSegmentImplTest extends Assert {
    
    @Test
    public void testPlainPathSegment() { 
        PathSegment ps = new PathSegmentImpl("bar");
        assertEquals("bar", ps.getPath());
        assertEquals(0, ps.getMatrixParameters().size());
    }
    
    @Test
    public void testPathSegmentWithMatrixParams() { 
        PathSegment ps = new PathSegmentImpl("bar;a=1;a=2;b=3%202", false);
        assertEquals("bar", ps.getPath());
        MultivaluedMap<String, String> params = ps.getMatrixParameters();
        assertEquals(2, params.size());
        assertEquals(2, params.get("a").size());
        assertEquals("1", params.get("a").get(0));
        assertEquals("2", params.get("a").get(1));
        assertEquals("3%202", params.getFirst("b"));
    }
    
    @Test
    public void testPathSegmentWithDecodedMatrixParams() { 
        PathSegment ps = new PathSegmentImpl("bar%20foo;a=1%202");
        assertEquals("bar foo", ps.getPath());
        MultivaluedMap<String, String> params = ps.getMatrixParameters();
        assertEquals(1, params.size());
        assertEquals(1, params.get("a").size());
        assertEquals("1 2", params.get("a").get(0));
    }
}
