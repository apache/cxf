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
import javax.xml.namespace.QName;


import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;
import org.junit.Assert;
import org.junit.Test;

public class WrapperBeanAnnotatorTest extends Assert {

    @Test
    public void testAnnotate() {
        String pkgName = "org.apache.cxf.tools.fortest.withannotation.doc.jaxws";
        WrapperBeanClass clz = new WrapperBeanClass();
        clz.setFullClassName(pkgName + ".SayHi");
        clz.setElementName(new QName("http://doc.withannotation.fortest.tools.cxf.apache.org/", "sayHi"));

        clz.annotate(new WrapperBeanAnnotator());
        List<JAnnotation> annotations = clz.getAnnotations();
        
        String expectedNamespace = "http://doc.withannotation.fortest.tools.cxf.apache.org/";
                
        JAnnotation rootElementAnnotation = annotations.get(0);
        assertEquals("@XmlRootElement(name = \"sayHi\", " 
                     + "namespace = \"" + expectedNamespace + "\")", 
                     rootElementAnnotation.toString());
        
        JAnnotation xmlTypeAnnotation = annotations.get(2);
        assertEquals("@XmlType(name = \"sayHi\", "
                     + "namespace = \"" + expectedNamespace + "\")", 
                     xmlTypeAnnotation.toString());
        
        JAnnotation accessorTypeAnnotation = annotations.get(1);
        assertEquals("@XmlAccessorType(XmlAccessType.FIELD)", 
                     accessorTypeAnnotation.toString());
        
        WrapperBeanClass resWrapperClass = new WrapperBeanClass();
        resWrapperClass.setFullClassName(pkgName + ".SayHiResponse");
        resWrapperClass.setElementName(new QName(expectedNamespace,
                                     "sayHiResponse"));
        
        resWrapperClass.annotate(new WrapperBeanAnnotator());
        annotations = resWrapperClass.getAnnotations();
        
        rootElementAnnotation = annotations.get(0);
        assertEquals("@XmlRootElement(name = \"sayHiResponse\", " 
                     + "namespace = \"" + expectedNamespace + "\")", 
                     rootElementAnnotation.toString());
        
        accessorTypeAnnotation = annotations.get(1);
        assertEquals("@XmlAccessorType(XmlAccessType.FIELD)", 
                     accessorTypeAnnotation.toString());
        
        xmlTypeAnnotation = annotations.get(2);
        assertEquals("@XmlType(name = \"sayHiResponse\", "
                     + "namespace = \"" + expectedNamespace + "\")",
                     xmlTypeAnnotation.toString());
    }
}
