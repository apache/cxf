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
package org.apache.cxf.jibx;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import org.junit.Before;
import org.junit.Test;

public class PrimitiveTypesTest extends AbstractJibxTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createService(PrimitiveTypesService.class, new PrimitiveTypesService(), "PrimitiveTypesService",
                      new QName("urn:PrimitiveTypesService", "PrimitiveTypesService"));
    }

    @Test
    public void testInt() throws Exception {
        doTestType("testInt", "24", "In:24");
    }

    @Test
    public void testInteger() throws Exception {
        doTestType("testInteger", "24", "In:24");
    }

    @Test
    public void testFloat() throws Exception {
        doTestType("testFloat", "3.14", "In:3.14");
    }

    @Test
    public void testFloatPrimitive() throws Exception {
        doTestType("testFloatPrim", "3.14", "In:3.14");
    }

    @Test
    public void testBoolean() throws Exception {
        doTestType("testBoolean", "false", "In:false");
    }

    @Test
    public void testBooleanPrimitive() throws Exception {
        doTestType("testBooleanPrim", "true", "In:true");

    }

    private void doTestType(String operation, String data, String expected) throws Exception {
        String req = "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/'>" + "<env:Header/>"
                     + "<env:Body xmlns='urn:PrimitiveTypesService' xmlns:x='http://example.com'>" + "   <"
                     + operation + ">" + "      <arg0>" + data + "</arg0>" + "   </" + operation + ">"
                     + "</env:Body>" + "</env:Envelope>";
        Node nd = invoke("PrimitiveTypesService", req.getBytes());
        addNamespace("t", "urn:PrimitiveTypesService");
        assertValid("//t:return[text()='" + expected + "']", nd);
        assertValid("//t:return1[text()='" + data + "']", nd);
    }

}
