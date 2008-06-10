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

package org.apache.cxf.service.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SchemaInfoTest extends Assert {
    
    private SchemaInfo schemaInfo;
    
    @Before
    public void setUp() throws Exception {
        schemaInfo = new SchemaInfo("http://apache.org/hello_world_soap_http/types");
    }
  
    @Test
    public void testConstructor() throws Exception {
        assertNull(schemaInfo.getElement());
        assertEquals(schemaInfo.getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http/types");
    }
    
    @Test
    public void testNamespaceURI() throws Exception {
        schemaInfo.setNamespaceURI("http://apache.org/hello_world_soap_http/types1");
        assertEquals(schemaInfo.getNamespaceURI(), "http://apache.org/hello_world_soap_http/types1");
    }
    
}
