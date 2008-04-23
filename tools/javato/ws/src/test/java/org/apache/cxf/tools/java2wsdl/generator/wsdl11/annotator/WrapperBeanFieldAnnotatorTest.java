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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11.annotator;


import java.util.List;

import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.common.model.JavaField;
import org.junit.Assert;
import org.junit.Test;

public class WrapperBeanFieldAnnotatorTest extends Assert {

    @Test
    public void testAnnotate() {
        JavaClass clz = new JavaClass();
        clz.setFullClassName("org.apache.cxf.tools.fortest.withannotation.doc.jaxws.SayHi");

        JavaField reqField = new JavaField("array",
                                           "String[]",
                                           "http://doc.withannotation.fortest.tools.cxf.apache.org/");

        reqField.setOwner(clz);
        List<JAnnotation> annotation = reqField.getAnnotations();
        assertEquals(0, annotation.size());
        
        reqField.annotate(new WrapperBeanFieldAnnotator());
        annotation = reqField.getAnnotations();

        String expectedNamespace = "http://doc.withannotation.fortest.tools.cxf.apache.org/";
        assertEquals("@XmlElement(name = \"array\", namespace = \"" + expectedNamespace + "\")",
                     annotation.get(0).toString());

        clz.setFullClassName("org.apache.cxf.tools.fortest.withannotation.doc.jaxws.SayHiResponse");
        JavaField resField = new JavaField("return",
                                           "String[]",
                                           "http://doc.withannotation.fortest.tools.cxf.apache.org/");
        resField.setOwner(clz);
        resField.annotate(new WrapperBeanFieldAnnotator());
        annotation = resField.getAnnotations();
        assertEquals("@XmlElement(name = \"return\", namespace = \"" + expectedNamespace + "\")",
                     annotation.get(0).toString());
    }
}
