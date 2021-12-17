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

import java.util.Arrays;

import jakarta.jws.WebParam;
import jakarta.jws.WebParam.Mode;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.FaultAction;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAnnotationTest {
    @Test
    public void testList() throws Exception {
        JAnnotation annotation = new JAnnotation(XmlSeeAlso.class);
        annotation.addElement(new JAnnotationElement(null,
                                                            Arrays.asList(new Class[]{XmlSeeAlso.class})));
        assertEquals("@XmlSeeAlso({XmlSeeAlso.class})", annotation.toString());
        assertEquals("jakarta.xml.bind.annotation.XmlSeeAlso", annotation.getImports().iterator().next());
    }

    @Test
    public void testSimpleForm() {
        JAnnotation annotation = new JAnnotation(WebService.class);
        assertEquals("@WebService", annotation.toString());
    }

    @Test
    public void testStringForm() {
        JAnnotation annotation = new JAnnotation(WebService.class);
        annotation.addElement(new JAnnotationElement("name", "AddNumbersPortType"));
        annotation.addElement(new JAnnotationElement("targetNamespace", "http://example.com/"));
        assertEquals("@WebService(name = \"AddNumbersPortType\", targetNamespace = \"http://example.com/\")",
                     annotation.toString());
    }

    @Test
    public void testEnum() {
        JAnnotation annotation = new JAnnotation(SOAPBinding.class);
        annotation.addElement(new JAnnotationElement("parameterStyle",
                                                            SOAPBinding.ParameterStyle.BARE));
        assertEquals("@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)", annotation.toString());
    }

    @Test
    public void testCombination() {
        JAnnotation annotation = new JAnnotation(Action.class);
        annotation.addElement(new JAnnotationElement("input", "3in"));
        annotation.addElement(new JAnnotationElement("output", "3out"));


        JAnnotation faultAction = new JAnnotation(FaultAction.class);
        faultAction.addElement(new JAnnotationElement("className", A.class));
        faultAction.addElement(new JAnnotationElement("value", "3fault"));

        annotation.addElement(new JAnnotationElement("fault",
                                                            Arrays.asList(new JAnnotation[]{faultAction})));

        String expected = "@Action(input = \"3in\", output = \"3out\", "
            + "fault = {@FaultAction(className = A.class, value = \"3fault\")})";
        assertEquals(expected, annotation.toString());

        assertTrue(annotation.getImports().contains("jakarta.xml.ws.FaultAction"));
        assertTrue(annotation.getImports().contains("jakarta.xml.ws.Action"));
        assertTrue(annotation.getImports().contains("org.apache.cxf.tools.common.model.A"));
    }

    @Test
    public void testPrimitive() {
        JAnnotation annotation = new JAnnotation(WebParam.class);
        annotation.addElement(new JAnnotationElement("header", true, true));
        annotation.addElement(new JAnnotationElement("mode", Mode.INOUT));
        assertEquals("@WebParam(header = true, mode = WebParam.Mode.INOUT)", annotation.toString());
    }

    @Test
    public void testAddSame() {
        JAnnotation annotation = new JAnnotation(WebParam.class);
        annotation.addElement(new JAnnotationElement("header", true, true));
        annotation.addElement(new JAnnotationElement("header", false, true));
        annotation.addElement(new JAnnotationElement("mode", Mode.INOUT));
        annotation.addElement(new JAnnotationElement("mode", Mode.OUT));
        assertEquals("@WebParam(header = false, mode = WebParam.Mode.OUT)", annotation.toString());
    }
}