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

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InterfaceInfoTest extends Assert {
    
    private InterfaceInfo interfaceInfo;

    @Before
    public void setUp() throws Exception {
        interfaceInfo = new InterfaceInfo(new ServiceInfo(), new QName(
            "http://apache.org/hello_world_soap_http", "interfaceTest"));
    }
    
    @Test
    public void testName() throws Exception {
        assertEquals(interfaceInfo.getName().getLocalPart(), "interfaceTest");
        assertEquals(interfaceInfo.getName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http");
        QName qname = new QName(
             "http://apache.org/hello_world_soap_http1", "interfaceTest1");
        interfaceInfo.setName(qname);
        assertEquals(interfaceInfo.getName().getLocalPart(), "interfaceTest1");
        assertEquals(interfaceInfo.getName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http1");
    }
 
    @Test
    public void testOperation() throws Exception {
        QName name = new QName("urn:test:ns", "sayHi");
        interfaceInfo.addOperation(name);
        assertEquals("sayHi", interfaceInfo.getOperation(name).getName().getLocalPart());
        interfaceInfo.addOperation(new QName("urn:test:ns", "greetMe"));
        assertEquals(interfaceInfo.getOperations().size(), 2);
        boolean duplicatedOperationName = false;
        try {
            interfaceInfo.addOperation(name);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), 
                "An operation with name [{urn:test:ns}sayHi] already exists in this service");
            duplicatedOperationName = true;
        }
        if (!duplicatedOperationName) {
            fail("should get IllegalArgumentException");
        }
        boolean isNull = false;
        try {
            QName qname = null;
            interfaceInfo.addOperation(qname);
        } catch (NullPointerException e) {
            isNull = true;
            assertEquals(e.getMessage(), "Operation Name cannot be null.");
        }
        if (!isNull) {
            fail("should get NullPointerException");
        }
    }
    
}
