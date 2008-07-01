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

import org.junit.Assert;
import org.junit.Test;


public class ResponseImplTest extends Assert {
    
    @Test
    public void testResourceImpl() {
        String entity = "bar";
        ResponseImpl ri = new ResponseImpl(200, entity);
        assertEquals("Wrong status", ri.getStatus(), 200);
        assertSame("Wrong entity", entity, ri.getEntity());
        
        MetadataMap<String, Object> meta = new MetadataMap<String, Object>();
        ri.addMetadata(meta);
        ri.getMetadata();
        assertSame("Wrong metadata", meta, ri.getMetadata());
    }

}
