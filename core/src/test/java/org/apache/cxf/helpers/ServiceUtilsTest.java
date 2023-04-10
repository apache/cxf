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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceUtilsTest {
    private Message msg;

    @Before
    public void setUp() {
        msg = mock(Message.class);
    }

    @Test
    public void testmakeNamespaceFromClassName() throws Exception {
        String tns = ServiceUtils.makeNamespaceFromClassName("com.example.ws.Test", "http");
        assertEquals("http://ws.example.com/", tns);
    }


    @Test
    public void testRequestResponseTypes() {
        // lets do server side first
        setupSchemaValidationValue(SchemaValidationType.REQUEST, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));

        setupSchemaValidationValue(SchemaValidationType.REQUEST, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.RESPONSE, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.RESPONSE, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));

        // now client side
        setupSchemaValidationValue(SchemaValidationType.REQUEST, true);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));

        setupSchemaValidationValue(SchemaValidationType.REQUEST, true);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.RESPONSE, true);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.RESPONSE, true);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
    }

    @Test
    public void testIsSchemaValidationEnabled() {
        setupSchemaValidationValue(SchemaValidationType.NONE, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.NONE, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.IN, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.IN, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.IN, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.IN, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.OUT, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.OUT, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));

        setupSchemaValidationValue(SchemaValidationType.BOTH, false);
        assertFalse(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.NONE, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.BOTH, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, msg));
        setupSchemaValidationValue(SchemaValidationType.BOTH, false);
        assertTrue(ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, msg));
    }

    @Test
    public void testGetSchemaValidationTypeBoolean() {
        setupSchemaValidationValue(null, false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("", false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue(Boolean.FALSE, false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("false", false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("FALSE", false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("fAlse", false);
        assertEquals(SchemaValidationType.NONE, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue(Boolean.TRUE, false);
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("true", false);
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("TRUE", false);
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));

        setupSchemaValidationValue("tRue", false);
        assertEquals(SchemaValidationType.BOTH, ServiceUtils.getSchemaValidationType(msg));
    }

    @Test
    public void testGetSchemaValidationType() {
        for (SchemaValidationType type : SchemaValidationType.values()) {
            setupSchemaValidationValue(type.name(), false);
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));

            setupSchemaValidationValue(type.name().toLowerCase(), false);
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));

            setupSchemaValidationValue(StringUtils.capitalize(type.name()), false);
            assertEquals(type, ServiceUtils.getSchemaValidationType(msg));
        }
    }

    private void setupSchemaValidationValue(Object value, boolean isRequestor) {
        when(msg.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED)).thenReturn(value);
        when(msg.get(Message.REQUESTOR_ROLE)).thenReturn(isRequestor);
    }
}