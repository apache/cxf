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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.lang.reflect.Method;

import org.apache.cxf.jaxws.JaxwsServiceBuilder;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.fortest.withannotation.doc.GreeterArray;
import org.junit.Assert;
import org.junit.Test;

public class ResponseWrapperTest extends Assert {
    JaxwsServiceBuilder builder = new JaxwsServiceBuilder();

    private OperationInfo getOperation(Class clz, String opName) {
        builder.setServiceClass(clz);
        ServiceInfo serviceInfo = builder.createService();

        for (OperationInfo op : serviceInfo.getInterface().getOperations()) {
            if (op.getUnwrappedOperation() != null
                && op.hasInput() && opName.equals(op.getName().getLocalPart())) {
                return op;
            }
        }
        return null;        
    }
    
    @Test
    public void testBuildFields() {
        // Test String[]
        Class testingClass = GreeterArray.class;
        OperationInfo opInfo = getOperation(testingClass, "sayStringArray");
        assertNotNull(opInfo);
        
        ResponseWrapper responseWrapper = new ResponseWrapper();

        MessageInfo message = opInfo.getUnwrappedOperation().getOutput();
        Method method = (Method)opInfo.getProperty("operation.method");

        JavaField field  = responseWrapper.buildFields(method, message).get(0);
        assertEquals("_return", field.getParaName());
        assertEquals("String[]", field.getType());

        // Test int[]

        opInfo = getOperation(testingClass, "sayIntArray");
        assertNotNull(opInfo);

        message = opInfo.getUnwrappedOperation().getOutput();
        method = (Method) opInfo.getProperty("operation.method");

        field = responseWrapper.buildFields(method, message).get(0);
        assertEquals("_return", field.getParaName());
        assertEquals("int[]", field.getType());

        // Test TestDataBean[]
        
        opInfo = getOperation(testingClass, "sayTestDataBeanArray");
        assertNotNull(opInfo);

        message = opInfo.getUnwrappedOperation().getOutput();
        method = (Method) opInfo.getProperty("operation.method");

        field = responseWrapper.buildFields(method, message).get(0);
        assertEquals("_return", field.getParaName());
        assertEquals("org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean[]", field.getType());
    }

    @Test
    public void testWithAnnotationWithClass() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.withannotation.doc";
        Class testingClass = Class.forName(pkgName + ".Greeter");

        OperationInfo opInfo = getOperation(testingClass, "sayHi");
        
        Wrapper wrapper = new ResponseWrapper();
        wrapper.setOperationInfo(opInfo);
        assertEquals(pkgName, wrapper.getJavaClass().getPackageName());
        assertEquals("SayHiResponse", wrapper.getJavaClass().getName());
        
    }
}
