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
import java.util.List;

import org.apache.cxf.jaxws.JaxwsServiceBuilder;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.fortest.withannotation.doc.GreeterArray;
import org.apache.cxf.tools.fortest.xmllist.AddNumbersPortType;
import org.junit.Assert;
import org.junit.Test;

public class RequestWrapperTest extends Assert {
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
    public void testBuildRequestFields() {
        // Test String[]
        Class testingClass = GreeterArray.class;
        OperationInfo opInfo = getOperation(testingClass, "sayStringArray");
        assertNotNull(opInfo);
        
        RequestWrapper requestWrapper = new RequestWrapper();

        MessageInfo message = opInfo.getUnwrappedOperation().getInput();
        Method method = (Method)opInfo.getProperty("operation.method");

        List<JavaField> fields = requestWrapper.buildFields(method, message);
        assertEquals(1, fields.size());
        JavaField field = fields.get(0);
        assertEquals("arg0", field.getName());
        assertEquals("String[]", field.getType());

        // Test int[]

        opInfo = getOperation(testingClass, "sayIntArray");
        assertNotNull(opInfo);

        message = opInfo.getUnwrappedOperation().getInput();
        method = (Method) opInfo.getProperty("operation.method");

        fields = requestWrapper.buildFields(method, message);
        assertEquals(1, fields.size());
        field = fields.get(0);
        assertEquals("arg0", field.getName());
        assertEquals("int[]", field.getType());

        // Test TestDataBean[]
        
        opInfo = getOperation(testingClass, "sayTestDataBeanArray");
        assertNotNull(opInfo);

        message = opInfo.getUnwrappedOperation().getInput();
        method = (Method) opInfo.getProperty("operation.method");

        fields = requestWrapper.buildFields(method, message);
        assertEquals(1, fields.size());
        field = fields.get(0);
        assertEquals("arg0", field.getName());
        assertEquals("org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean[]", field.getType());
    }

    @Test
    public void testNoAnnotationNoClass() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.classnoanno.docwrapped";
        Class testingClass = Class.forName(pkgName + ".Stock");        

        OperationInfo opInfo = getOperation(testingClass, "getPrice");
        Wrapper wrapper = new RequestWrapper();
        wrapper.setOperationInfo(opInfo);

        assertTrue(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName + ".jaxws", wrapper.getJavaClass().getPackageName());
        assertEquals("GetPrice", wrapper.getJavaClass().getName());

        JavaClass jClass = wrapper.buildWrapperBeanClass();
        assertNotNull(jClass);
        List<JavaField> jFields = jClass.getFields();

        assertEquals(1, jFields.size());
        assertEquals("arg0", jFields.get(0).getName());
        assertEquals("java.lang.String", jFields.get(0).getClassName());
        
        List<JavaMethod> jMethods = jClass.getMethods();
        assertEquals(2, jMethods.size());

        JavaMethod jMethod = jMethods.get(0);
        assertEquals("getArg0", jMethod.getName());
        assertTrue(jMethod.getParameterListWithoutAnnotation().isEmpty());

        jMethod = jMethods.get(1);
        assertEquals("setArg0", jMethod.getName());
        assertEquals(1, jMethod.getParameterListWithoutAnnotation().size());
        assertEquals("java.lang.String newArg0", jMethod.getParameterListWithoutAnnotation().get(0));
    }

    @Test
    public void testWithAnnotationNoClass() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.withannotation.doc";
        Class testingClass = Class.forName(pkgName + ".Stock");

        OperationInfo opInfo = getOperation(testingClass, "getPrice");
        Wrapper wrapper = new RequestWrapper();
        wrapper.setOperationInfo(opInfo);

        assertFalse(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName + ".jaxws", wrapper.getJavaClass().getPackageName());
        assertEquals("GetPrice", wrapper.getJavaClass().getName());
    }

    @Test
    public void testWithAnnotationWithClass() throws Exception {
        String pkgName = "org.apache.cxf.tools.fortest.withannotation.doc";
        Class testingClass = Class.forName(pkgName + ".Greeter");

        OperationInfo opInfo = getOperation(testingClass, "sayHi");

        Wrapper wrapper = new RequestWrapper();
        wrapper.setOperationInfo(opInfo);

        assertFalse(wrapper.isWrapperAbsent());
        assertTrue(wrapper.isToDifferentPackage());
        assertFalse(wrapper.isWrapperBeanClassNotExist());
        assertEquals(pkgName, wrapper.getJavaClass().getPackageName());
        assertEquals("SayHi", wrapper.getJavaClass().getName());
    }
    
    @Test
    public void testCXF1752() throws Exception {
        OperationInfo opInfo = getOperation(AddNumbersPortType.class, "testCXF1752");
        RequestWrapper wrapper = new RequestWrapper();
        wrapper.setOperationInfo(opInfo);
        
        wrapper.buildWrapperBeanClass();
        List<JavaField> fields = wrapper.getJavaClass().getFields();
        assertEquals(6, fields.size());
        assertEquals("java.util.List<java.lang.Long>", fields.get(0).getClassName());
        assertEquals("byte[]", fields.get(2).getClassName());
        assertEquals("org.apache.cxf.tools.fortest.xmllist.AddNumbersPortType.UserObject[]",
                     fields.get(3).getClassName());
        assertEquals("java.util.List<org.apache.cxf.tools.fortest.xmllist.AddNumbersPortType.UserObject>",
                     fields.get(4).getClassName());
    }
}
