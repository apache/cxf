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

public class ServiceUtilsTest {
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
        Assert.assertEquals("http://ws.example.com/", tns);
    }
    
    @Test
    public void testGetSchemaValidationTypeBoolean() {
        setupSchemaValidationValue(null);
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("");
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue(Boolean.FALSE);
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("false");
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("FALSE");
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("fAlse");
        Assert.assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue(Boolean.TRUE);
        Assert.assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("true");
        Assert.assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("TRUE");
        Assert.assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
        
        setupSchemaValidationValue("tRue");
        Assert.assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
    }
    
    @Test
    public void testGetSchemaValidationType() {
        for (SchemaValidationType type : SchemaValidationType.values()) {
            setupSchemaValidationValue(type.name());
            Assert.assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
            
            setupSchemaValidationValue(type.name().toLowerCase());
            Assert.assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
            
            setupSchemaValidationValue(StringUtils.capitalize(type.name()));
            Assert.assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
        }
    }
    
    private void setupSchemaValidationValue(Object value) {
        control.reset();
        msg.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED);
        EasyMock.expectLastCall().andReturn(value);
        control.replay();
    }
}
