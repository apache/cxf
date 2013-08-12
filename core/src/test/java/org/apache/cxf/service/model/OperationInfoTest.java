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

public class OperationInfoTest extends Assert {

    private OperationInfo operationInfo;
    
    @Before
    public void setUp() throws Exception {
        operationInfo = new OperationInfo(null, new QName("urn:test:ns", "operationTest"));
    }
    
    @Test
    public void testName() throws Exception {
        assertNull(operationInfo.getInterface());
        assertEquals("operationTest", operationInfo.getName().getLocalPart());
        operationInfo.setName(new QName("urn:test:ns", "operationTest2"));
        assertEquals("operationTest2", operationInfo.getName().getLocalPart());
        try {
            operationInfo.setName(null);
            fail("should catch IllegalArgumentException since name is null");
        } catch (NullPointerException e) {
            assertEquals(e.getMessage(), "Operation Name cannot be null.");
        }
    }
    
    @Test
    public void testInput() throws Exception {
        assertFalse(operationInfo.hasInput());
        MessageInfo inputMessage = operationInfo.createMessage(new QName(
            "http://apache.org/hello_world_soap_http", "testInputMessage"),
            MessageInfo.Type.INPUT);
        operationInfo.setInput("input", inputMessage);
        assertTrue(operationInfo.hasInput());
        inputMessage = operationInfo.getInput();
        assertEquals("testInputMessage", inputMessage.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_soap_http",
                     inputMessage.getName().getNamespaceURI());
        assertEquals(operationInfo.getInputName(), "input");
    }
    
    @Test
    public void testOutput() throws Exception {
        assertFalse(operationInfo.hasOutput());
        MessageInfo outputMessage = operationInfo.createMessage(new QName(
            "http://apache.org/hello_world_soap_http", "testOutputMessage"),
            MessageInfo.Type.OUTPUT);
        operationInfo.setOutput("output", outputMessage);
        assertTrue(operationInfo.hasOutput());
        outputMessage = operationInfo.getOutput();
        assertEquals("testOutputMessage", outputMessage.getName().getLocalPart());
        assertEquals("http://apache.org/hello_world_soap_http",
                     outputMessage.getName().getNamespaceURI());
        assertEquals(operationInfo.getOutputName(), "output");
    }
    
    @Test
    public void testOneWay() throws Exception {
        assertFalse(operationInfo.isOneWay());
        MessageInfo inputMessage = operationInfo.createMessage(new QName(
            "http://apache.org/hello_world_soap_http", "testInputMessage"),
            MessageInfo.Type.INPUT);
        operationInfo.setInput("input", inputMessage);
        assertTrue(operationInfo.isOneWay());
    }
    
    @Test
    public void testFault() throws Exception {
        assertEquals(operationInfo.getFaults().size(), 0);
        QName faultName = new QName("urn:test:ns", "fault");
        operationInfo.addFault(faultName, new QName(
            "http://apache.org/hello_world_soap_http", "faultMessage"));
        assertEquals(operationInfo.getFaults().size(), 1);
        FaultInfo fault = operationInfo.getFault(faultName);
        assertNotNull(fault);
        assertEquals(fault.getFaultName().getLocalPart(), "fault");
        assertEquals(fault.getName().getLocalPart(), "faultMessage");
        assertEquals(fault.getName().getNamespaceURI(), 
                     "http://apache.org/hello_world_soap_http");
        operationInfo.removeFault(faultName);
        assertEquals(operationInfo.getFaults().size(), 0);
        try {
            operationInfo.addFault(null, null);
            fail("should get NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("Fault Name cannot be null.", e.getMessage());
        }
        try {
            operationInfo.addFault(faultName, null);
            operationInfo.addFault(faultName, null);
            fail("should get IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), 
                "A fault with name [{urn:test:ns}fault] already exists in this operation");
        }
    }
    
    
}
