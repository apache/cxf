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


package org.apache.cxf.helpers;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceUtilsTest extends Assert {
    private IMocksControl control;
    private  Message msg;
    
    @Before 
    public void setUp() {
        control = EasyMock.createNiceControl();
        msg = control.createMock(Message.class);
    }
    
    @Test
    public void testmakeNamespaceFromClassName() throws Exception {
        String tns = ServiceUtils.makeNamespaceFromClassName("com.example.ws.Test", "http");
        assertEquals("http://ws.example.com/", tns);
    }
    
    @Test
    public void testIsSchemaValidationEnabled() {
        setupSchemaValidationValue(SchemaValidationType.NONE);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));
        
        setupSchemaValidationValue(SchemaValidationType.IN);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.IN);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.IN);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.IN);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));
        
        setupSchemaValidationValue(SchemaValidationType.OUT);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));
        
        setupSchemaValidationValue(SchemaValidationType.BOTH);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));
    }
    
    @Test
    public void testGetSchemaValidationTypeBoolean() {
        setupSchemaValidationValue(null);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("");
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue(Boolean.FALSE);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("false");
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("FALSE");
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("fAlse");
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue(Boolean.TRUE);
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("true");
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("TRUE");
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("tRue");
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
    }
    
    @Test
    public void testGetSchemaValidationType() {
        for (SchemaValidationType type : SchemaValidationType.values()) {
            setupSchemaValidationValue(type.name());
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
            
            setupSchemaValidationValue(type.name().toLowerCase());
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
            
            setupSchemaValidationValue(StringUtils.capitalize(type.name()));
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
        }
    }
    
    private void setupSchemaValidationValue(Object value) {
        control.reset();
        msg.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED);
        EasyMock.expectLastCall().andReturn(value);
        control.replay();
    }
}
