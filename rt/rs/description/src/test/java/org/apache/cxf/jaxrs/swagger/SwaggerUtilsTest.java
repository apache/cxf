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

import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;

import org.junit.Assert;
import org.junit.Test;

public class SwaggerUtilsTest extends Assert {

    @Test
    public void testConvertSwagger12ToUserResource() {
        UserResource ur = SwaggerUtils.getUserResource("/swagger12.json");
        assertNotNull(ur);
        assertEquals("/hello", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("helloSubject", op.getName());
        assertEquals("/{subject}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals(1, op.getParameters().size());
        Parameter param = op.getParameters().get(0);
        assertEquals("subject", param.getName());
        assertEquals(ParameterType.PATH, param.getType());
        assertEquals(String.class, param.getJavaType());
    }
    @Test
    public void testConvertSwagger20ToUserResource() {
        UserResource ur = SwaggerUtils.getUserResource("/swagger20.json");
        assertNotNull(ur);
        assertEquals("/base", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("postOp", op.getName());
        assertEquals("/somepath", op.getPath());
        assertEquals("POST", op.getVerb());
        assertEquals("application/x-www-form-urlencoded", op.getConsumes());
        assertEquals("application/json", op.getProduces());
        
        assertEquals(3, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("userName", param1.getName());
        assertEquals(ParameterType.FORM, param1.getType());
        assertEquals(String.class, param1.getJavaType());
        Parameter param2 = op.getParameters().get(1);
        assertEquals("password", param2.getName());
        assertEquals(ParameterType.FORM, param2.getType());
        assertEquals(String.class, param2.getJavaType());
        Parameter param3 = op.getParameters().get(2);
        assertEquals("type", param3.getName());
        assertEquals(ParameterType.MATRIX, param3.getType());
        assertEquals(String.class, param3.getJavaType());
    }
}
