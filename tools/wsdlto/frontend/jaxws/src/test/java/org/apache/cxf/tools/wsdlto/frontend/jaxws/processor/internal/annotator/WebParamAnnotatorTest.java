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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator;

import java.util.List;

import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;

import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WebParamAnnotatorTest extends Assert {

    JavaMethod method;
    JavaParameter parameter;

    @Before
    public void setUp() {
        method = new JavaMethod();
        parameter = new JavaParameter();
        parameter.setMethod(method);
    }

    private void init(JavaMethod m, JavaParameter param, SOAPBinding.Style style, boolean wrapper) {
        m.setSoapStyle(style);
        m.setWrapperStyle(wrapper);

        param.setName("x");
        param.setTargetNamespace("http://apache.org/cxf");
        param.setQName(new QName("http://apache.org/cxf", "x"));
        param.setPartName("y");
    }
    
    @Test
    public void testAnnotateDOCWrapped() throws Exception {
        init(method, parameter, SOAPBinding.Style.DOCUMENT, true);
        parameter.annotate(new WebParamAnnotator());

        JAnnotation annotation = parameter.getAnnotation("WebParam");
        assertEquals("@WebParam(name = \"x\", targetNamespace = \"http://apache.org/cxf\")", 
                         annotation.toString());
        List<JAnnotationElement> elements = annotation.getElements();
        assertEquals(2, elements.size());
        assertEquals("http://apache.org/cxf", elements.get(1).getValue());
        assertEquals("x", elements.get(0).getValue());
        // XXX - order that attributes are appended to the string
        //       differs with the ibmjdk...
        //assertEquals("@WebParam(targetNamespace = \"http://apache.org/cxf\", name = \"x\")",
        //             annotation.toString());
    }

    @Test
    public void testAnnotateDOCBare() throws Exception {
        init(method, parameter, SOAPBinding.Style.DOCUMENT, false);

        parameter.annotate(new WebParamAnnotator());

        JAnnotation annotation = parameter.getAnnotation("WebParam");
        assertEquals("@WebParam(partName = \"y\", name = \"x\", " 
                     + "targetNamespace = \"http://apache.org/cxf\")", 
                         annotation.toString());
        List<JAnnotationElement> elements = annotation.getElements();
        assertEquals(3, elements.size());
        // XXX - order that attributes are appended to the string
        //       differs with the ibmjdk...
        //assertEquals(
        //"@WebParam(targetNamespace = \"http://apache.org/cxf\", partName = \"y\", name = \"x\")",
        //             annotation.toString());
    }

    @Test
    public void testAnnotateRPC() throws Exception {
        init(method, parameter, SOAPBinding.Style.RPC, true);
        parameter.annotate(new WebParamAnnotator());
        JAnnotation annotation = parameter.getAnnotation("WebParam");
        assertEquals(2, annotation.getElements().size());
        assertEquals("@WebParam(partName = \"y\", name = \"y\")",
                     annotation.toString());
    }
}
