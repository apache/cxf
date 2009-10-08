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

import javax.ws.rs.core.EntityTag;

import org.junit.Assert;
import org.junit.Test;

public class EntityTagHeaderProviderTest extends Assert {
    
      
    @Test
    public void testFromString() {
        EntityTag tag = EntityTag.valueOf("\"\"");
        assertTrue(!tag.isWeak() && "".equals(tag.getValue()));
        tag = EntityTag.valueOf("W/");
        assertTrue(tag.isWeak() && "".equals(tag.getValue()));
        tag = EntityTag.valueOf("W/\"12345\"");
        assertTrue(tag.isWeak() && "12345".equals(tag.getValue()));
        tag = EntityTag.valueOf("\"12345\"");
        assertTrue(!tag.isWeak() && "12345".equals(tag.getValue()));
    }
    
    @Test
    public void testToString() {
        EntityTag tag = new EntityTag("");
        assertEquals("\"\"", tag.toString());
        tag = new EntityTag("", true);
        assertEquals("W/\"\"", tag.toString());
        tag = new EntityTag("bar");
        assertEquals("\"bar\"", tag.toString());
        
    }
}
