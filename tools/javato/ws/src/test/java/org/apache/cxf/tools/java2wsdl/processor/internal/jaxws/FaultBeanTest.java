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


import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;
import org.junit.Assert;
import org.junit.Test;

public class FaultBeanTest extends Assert {

    @Test
    public void testTransform() throws Exception {
        Class faultClass = Class.forName("org.apache.cxf.tools.fortest.cxf523.DBServiceFault");
        FaultBean bean = new FaultBean();
        WrapperBeanClass beanClass = bean.transform(faultClass, "org.apache.cxf.tools.fortest.cxf523.jaxws");

        assertNotNull(beanClass);
        assertEquals("DBServiceFaultBean", beanClass.getName());
        assertEquals("org.apache.cxf.tools.fortest.cxf523.jaxws", beanClass.getPackageName());

        assertEquals(1, beanClass.getFields().size());

        JavaField field = beanClass.getFields().get(0);
        assertEquals("message", field.getName());
        assertEquals("java.lang.String", field.getType());

        QName qname = beanClass.getElementName();
        assertEquals("DBServiceFault", qname.getLocalPart());
        assertEquals("http://cxf523.fortest.tools.cxf.apache.org/", qname.getNamespaceURI());
    }
}
