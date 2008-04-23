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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper;

import javax.wsdl.OperationType;
import javax.xml.namespace.QName;

import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.junit.Assert;
import org.junit.Test;

public class MethodMapperTest extends Assert {

    private OperationInfo getOperation() {
        OperationInfo operation = new OperationInfo();
        operation.setName(new QName("urn:test:ns", "OperationTest"));
        return operation;
    }
    
    @Test
    public void testMap() throws Exception {
        JavaMethod method = new MethodMapper().map(getOperation());
        assertNotNull(method);

        assertEquals(javax.jws.soap.SOAPBinding.Style.DOCUMENT, method.getSoapStyle());
        assertEquals("operationTest", method.getName());
        assertEquals("OperationTest", method.getOperationName());
        assertEquals(OperationType.REQUEST_RESPONSE, method.getStyle());
        assertFalse(method.isWrapperStyle());
        assertFalse(method.isOneWay());
    }

    @Test
    public void testMapOneWayOperation() throws Exception {
        OperationInfo operation = getOperation();

        MessageInfo inputMessage = operation.createMessage(new QName("urn:test:ns", "testInputMessage"),
                                                           MessageInfo.Type.INPUT);
        operation.setInput("input", inputMessage);

        JavaMethod method = new MethodMapper().map(operation);
        assertNotNull(method);
        assertTrue(method.isOneWay());
    }

    @Test
    public void testMapWrappedOperation() throws Exception {
        OperationInfo operation = getOperation();
        operation.setUnwrappedOperation(operation);

        JavaMethod method = new MethodMapper().map(operation);
        assertNotNull(method);
        
        assertTrue(method.isWrapperStyle());
    }
}
