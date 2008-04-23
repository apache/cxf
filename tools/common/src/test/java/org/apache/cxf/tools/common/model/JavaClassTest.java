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

package org.apache.cxf.tools.common.model;

import org.junit.Assert;
import org.junit.Test;

public class JavaClassTest extends Assert {
    @Test
    public void testGetterSetter() throws Exception {
        JavaField field = new JavaField("arg0",
                                        "org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean",
                                        "http://doc.withannotation.fortest.tools.cxf.apache.org/");
        JavaClass clz = new JavaClass();
        clz.setFullClassName("org.apache.cxf.tools.fortest.withannotation.doc.jaxws.EchoDataBean");
        JavaMethod getter = clz.appendGetter(field);
        assertEquals("getArg0", getter.getName());
        assertEquals("org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean",
                     getter.getReturn().getClassName());
        assertEquals("arg0", getter.getReturn().getName());

        JavaMethod setter = clz.appendSetter(field);
        assertEquals("setArg0", setter.getName());
        assertEquals("void", setter.getReturn().getClassName());
        assertEquals("arg0", getter.getReturn().getName());
        assertEquals("org.apache.cxf.tools.fortest.withannotation.doc.TestDataBean",
                     setter.getParameters().get(0).getClassName());
    }

    @Test
    public void testGetterSetterStringArray() {
        JavaField field = new JavaField("array",
                                        "String[]",
                                        "http://doc.withannotation.fortest.tools.cxf.apache.org/");

        JavaClass clz = new JavaClass();
        clz.setFullClassName("org.apache.cxf.tools.fortest.withannotation.doc.jaxws.SayHi");
        JavaMethod getter = clz.appendGetter(field);
        assertEquals("getArray", getter.getName());
        assertEquals("String[]",
                     getter.getReturn().getClassName());
        assertEquals("array", getter.getReturn().getName());
        assertEquals("return this.array;", getter.getJavaCodeBlock().getExpressions().get(0).toString());

        JavaMethod setter = clz.appendSetter(field);
        assertEquals("setArray", setter.getName());
        assertEquals("void", setter.getReturn().getClassName());
        assertEquals("array", getter.getReturn().getName());
        assertEquals("String[]",
                     setter.getParameters().get(0).getClassName());
        assertEquals("this.array = newArray;", setter.getJavaCodeBlock().getExpressions().get(0).toString());

        
        field = new JavaField("return",
                              "String[]",
                              "http://doc.withannotation.fortest.tools.cxf.apache.org/");
        clz = new JavaClass();
        clz.setFullClassName("org.apache.cxf.tools.fortest.withannotation.doc.jaxws.SayHiResponse");
        getter = clz.appendGetter(field);
        assertEquals("getReturn", getter.getName());
        assertEquals("String[]",
                     getter.getReturn().getClassName());
        assertEquals("_return", getter.getReturn().getName());

        setter = clz.appendSetter(field);
        assertEquals("setReturn", setter.getName());
        assertEquals("void", setter.getReturn().getClassName());
        assertEquals("_return", getter.getReturn().getName());
        assertEquals("String[]",
                     setter.getParameters().get(0).getClassName());
    }
}
