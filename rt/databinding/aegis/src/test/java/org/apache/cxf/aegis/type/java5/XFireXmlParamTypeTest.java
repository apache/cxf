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
package org.apache.cxf.aegis.type.java5;

import java.lang.reflect.Method;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.Configuration;
import org.apache.cxf.aegis.type.DefaultTypeCreator;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.Type;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class XFireXmlParamTypeTest extends AbstractAegisTest {
    private DefaultTypeMapping tm;
    private Java5TypeCreator creator;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        tm = new DefaultTypeMapping();
        creator = new Java5TypeCreator();
        creator.setNextCreator(new DefaultTypeCreator());
        creator.setConfiguration(new Configuration());
        tm.setTypeCreator(creator);
    }

    @Test
    public void testType() throws Exception {
        Method m = CustomTypeService.class.getMethod("doFoo", new Class[] {String.class});

        Type type = creator.createType(m, 0);
        tm.register(type);
        assertTrue(type instanceof CustomStringType);
        assertEquals(new QName("urn:xfire:foo", "custom"), type.getSchemaType());

        type = creator.createType(m, -1);
        tm.register(type);
        assertTrue(type instanceof CustomStringType);
        assertEquals(new QName("urn:xfire:foo", "custom"), type.getSchemaType());
    }

    @Test
    public void testMapServiceWSDL() throws Exception {
        createService(CustomTypeService.class, new CustomTypeService(), null);

        Document wsdl = getWSDLDocument("CustomTypeService");
        assertValid("//xsd:element[@name='s'][@type='ns0:custom']", wsdl);
    }

    public class CustomTypeService {

        @org.codehaus.xfire.aegis.type.java5.XmlReturnType(type = CustomStringType.class, 
                                                           namespace = "urn:xfire:foo", name = "custom")
        public String doFoo(
                            @org.codehaus.xfire.aegis.type.java5.XmlParamType(type = CustomStringType.class,
                                          namespace = "urn:xfire:foo", name = "custom")
                            String s) {
            return null;
        }
    }
}