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

import java.net.URI;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;


public class ResponseBuilderImplTest extends Assert {

        
    @Test
    public void testOkBuild() {
      
        checkBuild(Response.ok().build(),
                          200, null, new MetadataMap<String, Object>());
        
    }
    
    @Test
    public void testCreatedNoEntity() throws Exception {
        
        MetadataMap<String, Object> m = new MetadataMap<String, Object>();
        m.putSingle("Location", "http://foo");
        
        checkBuild(Response.created(new URI("http://foo")).build(),
                   201, null, m);
        
        
    }
    
    private void checkBuild(Response r, int status, Object entity, 
                            MetadataMap<String, Object> meta) {
        ResponseImpl ri = (ResponseImpl)r;
        assertEquals("Wrong status", ri.getStatus(), status);
        assertSame("Wrong entity", ri.getEntity(), entity);
        assertEquals("Wrong meta", ri.getMetadata(), meta);
    }
    
}
