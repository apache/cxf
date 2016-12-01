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
package org.apache.cxf.jaxrs.swagger;

import java.util.Map;

import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;

import org.junit.Assert;
import org.junit.Test;

public class SwaggerUtilsTest extends Assert {

    @Test
    public void testConvertSwaggerToUserApp() {
        UserApplication ap = SwaggerUtils.getUserApplication("/swagger20.json");
        assertNotNull(ap);
        assertEquals("/services/helloservice", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(2, map.size());
        
        UserResource ur = map.get("sayHello");
        assertNotNull(ur);
        assertEquals("/sayHello", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("sayHello", op.getName());
        assertEquals("/{a}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals("text/plain", op.getProduces());
        
        assertEquals(1, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("a", param1.getName());
        assertEquals(ParameterType.PATH, param1.getType());
        assertEquals(String.class, param1.getJavaType());
        
        UserResource ur2 = map.get("sayHello2");
        assertNotNull(ur2);
        assertEquals("/sayHello2", ur2.getPath());
        assertEquals(1, ur2.getOperations().size());
        UserOperation op2 = ur2.getOperations().get(0);
        assertEquals("sayHello", op2.getName());
        assertEquals("/{a}", op2.getPath());
        assertEquals("GET", op2.getVerb());
        assertEquals("text/plain", op2.getProduces());
        
        assertEquals(1, op2.getParameters().size());
        Parameter param2 = op.getParameters().get(0);
        assertEquals("a", param2.getName());
        assertEquals(ParameterType.PATH, param2.getType());
        assertEquals(String.class, param2.getJavaType());
        
    }
}
