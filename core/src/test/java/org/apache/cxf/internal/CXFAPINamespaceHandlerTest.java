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

package org.apache.cxf.internal;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class CXFAPINamespaceHandlerTest {
    @Test
    public void testGetSchemaLocation() {
        CXFAPINamespaceHandler handler = new CXFAPINamespaceHandler();

        assertNotNull(handler.getSchemaLocation("http://cxf.apache.org/configuration/beans"));
        assertNotNull(handler.getSchemaLocation("http://cxf.apache.org/configuration/parameterized-types"));
        assertNotNull(handler.getSchemaLocation("http://cxf.apache.org/configuration/security"));
        assertNotNull(handler.getSchemaLocation("http://schemas.xmlsoap.org/wsdl/"));
        assertNotNull(handler.getSchemaLocation("http://www.w3.org/2005/08/addressing"));
        assertNotNull(handler.getSchemaLocation("http://schemas.xmlsoap.org/ws/2004/08/addressing"));
        assertNotNull(handler.getSchemaLocation("http://cxf.apache.org/blueprint/core"));
    }

}