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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.Service;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AnnotatedTypeTest extends AbstractAegisTest {
    private TypeMapping tm;
    private Service service;
    private AegisDatabinding databinding;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Server s = createService(AnnotatedService.class);
        service = s.getEndpoint().getService();
        databinding = (AegisDatabinding) service.getDataBinding();

        tm = databinding.getAegisContext().getTypeMapping();
    }

    @Test
    public void testTM() {
        assertTrue(tm.getTypeCreator() instanceof XMLTypeCreator);
    }

    @Test
    public void testType() {
        AnnotatedTypeInfo info = new AnnotatedTypeInfo(tm, AnnotatedBean1.class, "urn:foo",
                                                       new TypeCreationOptions());

        Iterator<QName> elements = info.getElements().iterator();
        assertTrue(elements.hasNext());
        QName element = elements.next();
        assertTrue(elements.hasNext());

        AegisType custom = info.getType(element);

        if ("bogusProperty".equals(element.getLocalPart())) {
            assertTrue(custom instanceof StringType);
        } else if ("elementProperty".equals(element.getLocalPart())) {
            assertTrue(custom instanceof CustomStringType);
        } else {
            fail("Unexpected element name: " + element.getLocalPart());
        }
        element = elements.next();
        assertFalse(elements.hasNext());

        custom = info.getType(element);

        if ("bogusProperty".equals(element.getLocalPart())) {
            assertTrue(custom instanceof StringType);
        } else if ("elementProperty".equals(element.getLocalPart())) {
            assertTrue(custom instanceof CustomStringType);
        } else {
            fail("Unexpected element name: " + element.getLocalPart());
        }

        Iterator<QName> atts = info.getAttributes().iterator();
        assertTrue(atts.hasNext());
        atts.next();
        assertFalse(atts.hasNext());

        assertTrue(info.isExtensibleElements());
        assertTrue(info.isExtensibleAttributes());
    }

    /**
     * Test if attributeProperty is correctly mapped to attProp by
     * applying the xml mapping file <className>.aegis.xml
     */
    @Test
    public void testAegisType() {
        BeanType type = (BeanType)tm.getTypeCreator().createType(AnnotatedBean3.class);

        assertEquals(0, type.getTypeInfo().getAttributes().size());

        Iterator<QName> itr = type.getTypeInfo().getElements().iterator();
        assertTrue(itr.hasNext());
        QName q = itr.next();
        assertEquals("attProp", q.getLocalPart());
    }

    @Test
    public void testExtensibilityOff() {
        BeanType type = (BeanType)tm.getTypeCreator().createType(AnnotatedBean4.class);

        assertFalse(type.getTypeInfo().isExtensibleElements());
        assertFalse(type.getTypeInfo().isExtensibleAttributes());
    }

    @Test
    public void testNillableAndMinOccurs() {
        BeanType type = (BeanType)tm.getTypeCreator().createType(AnnotatedBean4.class);
        AnnotatedTypeInfo info = (AnnotatedTypeInfo)type.getTypeInfo();
        Iterator<QName> elements = info.getElements().iterator();
        assertTrue(elements.hasNext());
        // nillable first
        QName element = elements.next();
        if ("minOccursProperty".equals(element.getLocalPart())) {
            assertEquals(1, info.getMinOccurs(element));
        } else {
            assertFalse(info.isNillable(element));
        }

        assertTrue(elements.hasNext());
        // minOccurs = 1 second
        element = elements.next();
        if ("minOccursProperty".equals(element.getLocalPart())) {
            assertEquals(1, info.getMinOccurs(element));
        } else {
            assertFalse(info.isNillable(element));
        }
    }

    @Test
    public void testWSDL() throws Exception {
        Document wsdl = getWSDLDocument("AnnotatedService");

        addNamespace("xsd", Constants.URI_2001_SCHEMA_XSD);
        assertValid(
                    "//xsd:complexType[@name='AnnotatedBean1']/xsd:sequence/xsd:"
                    + "element[@name='elementProperty']",
                    wsdl);
        assertValid("//xsd:complexType[@name='AnnotatedBean1']/xsd:attribute"
                    + "[@name='attributeProperty']",
                    wsdl);
        assertValid(
                    "//xsd:complexType[@name='AnnotatedBean1']/xsd:sequence/xsd:element"
                    + "[@name='bogusProperty']",
                    wsdl);

        assertValid(
                    "//xsd:complexType[@name='AnnotatedBean2']/xsd:sequence/xsd:element"
                    + "[@name='element'][@type='xsd:string']",
                    wsdl);
        assertValid(
                    "//xsd:complexType[@name='AnnotatedBean2']/xsd:attribute"
                    + "[@name='attribute'][@type='xsd:string']",
                    wsdl);
    }

    @Test
    public void testGetSetRequired() throws Exception {
        BeanType type = new BeanType(new AnnotatedTypeInfo(tm, BadBean.class, "urn:foo",
                                                           new TypeCreationOptions()));
        type.setSchemaType(new QName("urn:foo", "BadBean"));

        assertFalse(type.getTypeInfo().getElements().iterator().hasNext());
    }

    public static class BadBean {
        public void setString(String string) {
        }
    }
}