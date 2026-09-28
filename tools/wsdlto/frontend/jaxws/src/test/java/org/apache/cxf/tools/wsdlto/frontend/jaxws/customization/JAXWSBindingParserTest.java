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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.io.StringReader;

import org.w3c.dom.Element;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXWSBindingParserTest {

    private static JAXWSBinding parse(String body) throws Exception {
        String xml = "<jaxws:bindings xmlns:jaxws=\"" + ToolConstants.NS_JAXWS_BINDINGS + "\">"
            + "<part name=\"p\"/>" + body + "</jaxws:bindings>";
        Element el = StaxUtils.read(new StringReader(xml)).getDocumentElement();
        JAXWSBinding binding = new JAXWSBinding();
        new JAXWSBindingParser(null).parseElement(binding, el);
        return binding;
    }

    private static void assertRejected(String body) throws Exception {
        try {
            parse(body);
            fail("Expected ToolException for " + body);
        } catch (ToolException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("not a valid Java name"));
        }
    }

    @Test
    public void testValidNames() throws Exception {
        JAXWSBinding binding = parse("<jaxws:package name=\"org.mypkg\"/>"
            + "<jaxws:method name=\"myGreetMe\"/>"
            + "<jaxws:class name=\"org.mypkg.MyGreeter\"/>"
            + "<jaxws:parameter part=\"part\" name=\"num1\"/>");
        assertEquals("org.mypkg", binding.getPackage());
        assertEquals("myGreetMe", binding.getMethodName());
        assertEquals("org.mypkg.MyGreeter", binding.getJaxwsClass().getClassName());
        assertEquals("num1", binding.getJaxwsParas().get(0).getName());
    }

    @Test
    public void testInvalidMethodName() throws Exception {
        assertRejected("<jaxws:method name=\"x(){} public void pwn(){} void y\"/>");
        assertRejected("<jaxws:method name=\"class\"/>");
        assertRejected("<jaxws:method name=\"a.b\"/>");
    }

    @Test
    public void testInvalidClassName() throws Exception {
        assertRejected("<jaxws:class name=\"Foo { static { System.exit(1); } } class Bar\"/>");
    }

    @Test
    public void testInvalidPackageName() throws Exception {
        assertRejected("<jaxws:package name=\"com.foo; import evil.*\"/>");
    }

    @Test
    public void testInvalidParameterName() throws Exception {
        assertRejected("<jaxws:parameter part=\"part\" name=\"a, int b\"/>");
    }
}
