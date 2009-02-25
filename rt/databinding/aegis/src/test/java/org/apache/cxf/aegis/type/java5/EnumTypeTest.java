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

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.java5.CurrencyService.Currency;
import org.apache.cxf.aegis.xml.jdom.JDOMReader;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.common.util.SOAPConstants;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;
import org.junit.Before;
import org.junit.Test;

public class EnumTypeTest extends AbstractAegisTest {
    private DefaultTypeMapping tm;

    private enum smallEnum {
        VALUE1, VALUE2;

        @Override
        public String toString() {
            return name() + "*";
        }
        
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();

        tm = new DefaultTypeMapping();
        Java5TypeCreator creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);
    }
    
    @Test
    public void testType() throws Exception {
        EnumType type = new EnumType();
        type.setTypeClass(smallEnum.class);
        type.setSchemaType(new QName("urn:test", "test"));

        tm.register(type);

        Element root = new Element("root");
        JDOMWriter writer = new JDOMWriter(root);

        type.writeObject(smallEnum.VALUE1, writer, getContext());

        assertEquals("VALUE1", root.getValue());

        JDOMReader reader = new JDOMReader(root);
        Object value = type.readObject(reader, getContext());

        assertEquals(smallEnum.VALUE1, value);
    }

    @Test
    public void testAutoCreation() throws Exception {
        Type type = tm.getTypeCreator().createType(smallEnum.class);

        assertTrue(type instanceof EnumType);
    }

    @Test
    public void testTypeAttributeOnEnum() throws Exception {
        Type type = tm.getTypeCreator().createType(TestEnum.class);

        assertEquals("urn:xfire:foo", type.getSchemaType().getNamespaceURI());

        assertTrue(type instanceof EnumType);
    }

    @Test
    public void testXFireTypeAttributeOnEnum() throws Exception {
        Type type = tm.getTypeCreator().createType(XFireTestEnum.class);

        assertEquals("urn:xfire:foo", type.getSchemaType().getNamespaceURI());

        assertTrue(type instanceof EnumType);
    }

    @Test
    public void testJaxbTypeAttributeOnEnum() throws Exception {
        Type type = tm.getTypeCreator().createType(JaxbTestEnum.class);

        assertEquals("urn:xfire:foo", type.getSchemaType().getNamespaceURI());

        assertTrue(type instanceof EnumType);
    }

    @Test
    public void testWSDL() throws Exception {
        EnumType type = new EnumType();
        type.setTypeClass(smallEnum.class);
        type.setSchemaType(new QName("urn:test", "test"));

        Element root = new Element("root");
        org.jdom.Document wsdl = new org.jdom.Document(root);
        type.writeSchema(root);

        Document doc = new DOMOutputter().output(wsdl);
        addNamespace("xsd", SOAPConstants.XSD);
        assertValid("//xsd:simpleType[@name='test']/xsd:restriction[@base='xsd:string']", doc);
        assertValid("//xsd:restriction[@base='xsd:string']/xsd:enumeration[@value='VALUE1']", doc);
        assertValid("//xsd:restriction[@base='xsd:string']/xsd:enumeration[@value='VALUE2']", doc);
    }

    @Test
    public void testCurrencyService() throws Exception {
        createService(CurrencyService.class);

        Document wsdl = getWSDLDocument("CurrencyService");

        assertValid("//xsd:element[@name='inputCurrency'][@minOccurs='0']", wsdl);
        assertValid("//xsd:simpleType[@name='Currency']/xsd:restriction[@base='xsd:string']", wsdl);
        assertValid("//xsd:restriction[@base='xsd:string']/xsd:enumeration[@value='USD']", wsdl);
        assertValid("//xsd:restriction[@base='xsd:string']/xsd:enumeration[@value='EURO']", wsdl);
        assertValid("//xsd:restriction[@base='xsd:string']/xsd:enumeration[@value='POUNDS']", wsdl);
    }

    @Test
    public void testNillable() throws Exception {
        Type type = tm.getTypeCreator().createType(EnumBean.class);

        tm.register(type);

        Element root = new Element("root");
        JDOMWriter writer = new JDOMWriter(root);

        type.writeObject(new EnumBean(), writer, getContext());

        JDOMReader reader = new JDOMReader(root);
        Object value = type.readObject(reader, getContext());

        assertTrue(value instanceof EnumBean);
        EnumBean bean = (EnumBean)value;
        assertNull(bean.getCurrency());
    }

    public static class EnumBean {
        private Currency currency;

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public Currency[] getSomeCurrencies() {
            return new Currency[] {Currency.EURO, null};
        }

        public void setSomeCurrencies(Currency[] currencies) {

        }
    }
}
