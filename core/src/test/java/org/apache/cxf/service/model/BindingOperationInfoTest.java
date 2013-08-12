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

public class BindingOperationInfoTest extends Assert {
    private static final String TEST_NS = "urn:test:ns";
    private BindingOperationInfo bindingOperationInfo;
    
    @Before
    public void setUp() throws Exception {
        OperationInfo operationInfo = new OperationInfo(null, new QName(TEST_NS, "operationTest"));
        MessageInfo inputMessage = operationInfo.createMessage(new QName(
            "http://apache.org/hello_world_soap_http", "testInputMessage"),
            MessageInfo.Type.INPUT);
        operationInfo.setInput("input", inputMessage);
        
        MessageInfo outputMessage = operationInfo.createMessage(new QName(
            "http://apache.org/hello_world_soap_http", "testOutputMessage"),
            MessageInfo.Type.OUTPUT);
        operationInfo.setOutput("output", outputMessage);
        operationInfo.addFault(new QName(TEST_NS, "fault"), new QName(
            "http://apache.org/hello_world_soap_http", "faultMessage"));
        bindingOperationInfo = new BindingOperationInfo(null, operationInfo);
    }
    
    @Test
    public void testName() throws Exception {
        assertEquals(bindingOperationInfo.getName(), new QName(TEST_NS, "operationTest"));
    }
    
    @Test
    public void testBinding() throws Exception {
        assertNull(bindingOperationInfo.getBinding());
    }
    
    @Test
    public void testOperation() throws Exception {
        assertEquals(bindingOperationInfo.getOperationInfo().getName(), new QName(TEST_NS, "operationTest"));
        assertTrue(bindingOperationInfo.getOperationInfo().hasInput());
        assertTrue(bindingOperationInfo.getOperationInfo().hasOutput());
        assertEquals(bindingOperationInfo.getOperationInfo().getInputName(), "input");
        assertEquals(bindingOperationInfo.getOperationInfo().getOutputName(), "output");
        assertEquals(bindingOperationInfo.getFaults().iterator().next().getFaultInfo().getFaultName(),
                     new QName(TEST_NS, "fault"));
        assertEquals(1, bindingOperationInfo.getFaults().size());
    }
    
    @Test
    public void testInputMessage() throws Exception {
        BindingMessageInfo inputMessage = bindingOperationInfo.getInput();
        assertNotNull(inputMessage);
        assertEquals(inputMessage.getMessageInfo().getName().getLocalPart(), "testInputMessage");
        assertEquals(inputMessage.getMessageInfo().getName().getNamespaceURI(), 
                     "http://apache.org/hello_world_soap_http");
    }
    
    @Test
    public void testOutputMessage() throws Exception {
        BindingMessageInfo outputMessage = bindingOperationInfo.getOutput();
        assertNotNull(outputMessage);
        assertEquals(outputMessage.getMessageInfo().getName().getLocalPart(), "testOutputMessage");
        assertEquals(outputMessage.getMessageInfo().getName().getNamespaceURI(), 
                     "http://apache.org/hello_world_soap_http");
    }
    
    @Test
    public void testFaultMessage() throws Exception {
        BindingFaultInfo faultMessage = bindingOperationInfo.getFaults().iterator().next();
        assertNotNull(faultMessage);
        assertEquals(faultMessage.getFaultInfo().getName().getLocalPart(), "faultMessage");
        assertEquals(faultMessage.getFaultInfo().getName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http");
    }
}
