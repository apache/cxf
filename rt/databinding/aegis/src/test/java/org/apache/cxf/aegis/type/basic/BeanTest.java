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
package org.apache.cxf.aegis.type.basic;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.util.jdom.StaxBuilder;
import org.apache.cxf.aegis.xml.jdom.JDOMReader;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.common.util.SOAPConstants;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.Test;

public class BeanTest extends AbstractAegisTest {
    TypeMapping mapping;
    private AegisContext context;

    public void setUp() throws Exception {
        super.setUp();
    
        addNamespace("b", "urn:Bean");
        addNamespace("bz", "urn:beanz");
        addNamespace("a", "urn:anotherns");
        addNamespace("xsi", SOAPConstants.XSI_NS);

        context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();
    }

    @Test
    public void testBean() throws Exception {
        BeanType type = new BeanType();
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        // Test reading
        ElementReader reader = new ElementReader(getResourceAsStream("bean1.xml"));

        SimpleBean bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("bleh", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());

        reader.getXMLStreamReader().close();

        // Test reading with extra elements
        reader = new ElementReader(getResourceAsStream("bean2.xml"));
        bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("bleh", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());
        reader.getXMLStreamReader().close();

        // test <bleh/> element
        reader = new ElementReader(getResourceAsStream("bean7.xml"));
        bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());
        reader.getXMLStreamReader().close();

        bean.setBleh("bleh");

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertValid("/b:root/b:bleh[text()='bleh']", element);
        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }
    
    @Test
    public void testBeanWithXsiType() throws Exception {
        BeanType type = new BeanType();
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        // Test reading
        ElementReader reader = new ElementReader(getResourceAsStream("bean9.xml"));
        Context ctx = getContext();
        ctx.getGlobalContext().setReadXsiTypes(false);

        SimpleBean bean = (SimpleBean)type.readObject(reader, ctx);
        assertEquals("bleh", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());

        reader.getXMLStreamReader().close();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertValid("/b:root/b:bleh[text()='bleh']", element);
        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }

    @Test
    public void testUnmappedProperty() throws Exception {
        String ns = "urn:Bean";
        BeanTypeInfo info = new BeanTypeInfo(SimpleBean.class, ns, false);

        QName name = new QName(ns, "howdycustom");
        info.mapElement("howdy", name);
        info.setTypeMapping(mapping);

        assertEquals("howdy", info.getPropertyDescriptorFromMappedName(name).getName());

        BeanType type = new BeanType(info);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        ElementReader reader = new ElementReader(getResourceAsStream("bean3.xml"));

        SimpleBean bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("howdy", bean.getHowdy());
        assertNull(bean.getBleh());

        reader.getXMLStreamReader().close();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertInvalid("/b:root/b:bleh", element);
        assertValid("/b:root/b:howdycustom[text()='howdy']", element);
    }

    @Test
    public void testAttributeMap() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(SimpleBean.class, "urn:Bean");
        info.mapAttribute("howdy", new QName("urn:Bean", "howdy"));
        info.mapAttribute("bleh", new QName("urn:Bean", "bleh"));
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        ElementReader reader = new ElementReader(getResourceAsStream("bean4.xml"));

        SimpleBean bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("bleh", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());

        reader.getXMLStreamReader().close();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertValid("/b:root[@b:bleh='bleh']", element);
        assertValid("/b:root[@b:howdy='howdy']", element);

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        assertValid("//xsd:complexType[@name='bean']/xsd:attribute[@name='howdy']", schema);
        assertValid("//xsd:complexType[@name='bean']/xsd:attribute[@name='bleh']", schema);
    }

    @Test
    public void testAttributeMapDifferentNS() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(SimpleBean.class, "urn:Bean");
        info.mapAttribute("howdy", new QName("urn:Bean2", "howdy"));
        info.mapAttribute("bleh", new QName("urn:Bean2", "bleh"));
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        ElementReader reader = new ElementReader(getResourceAsStream("bean8.xml"));

        SimpleBean bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals("bleh", bean.getBleh());
        assertEquals("howdy", bean.getHowdy());

        reader.getXMLStreamReader().close();

        // Test writing

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ElementWriter writer = new ElementWriter(bos, "root", "urn:Bean");
        type.writeObject(bean, writer, getContext());
        writer.close();
        writer.flush();

        bos.close();
        
        StaxBuilder builder = new StaxBuilder();
        Document doc = builder.build(new ByteArrayInputStream(bos.toByteArray()));
        Element element = doc.getRootElement();

        addNamespace("b2", "urn:Bean2");
        assertValid("/b:root[@b2:bleh='bleh']", element);
        assertValid("/b:root[@b2:howdy='howdy']", element);
    }

    @Test
    public void testNullProperties() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(SimpleBean.class, "urn:Bean");
        info.setTypeMapping(mapping);
        info.mapAttribute("howdy", new QName("urn:Bean", "howdy"));
        info.mapElement("bleh", new QName("urn:Bean", "bleh"));

        BeanType type = new BeanType(info);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        SimpleBean bean = new SimpleBean();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertInvalid("/b:root[@b:howdy]", element);
        assertValid("/b:root/b:bleh[@xsi:nil='true']", element);

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        assertValid("//xsd:complexType[@name='bean']/xsd:attribute[@name='howdy']", schema);
        assertValid("//xsd:complexType[@name='bean']/xsd:sequence/xsd:element[@name='bleh']", schema);
    }
    
    @Test
    public void testNillableInt() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(IntBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(IntBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        assertValid("//xsd:complexType[@name='bean']/xsd:sequence/xsd:element[@name='int1']"
                    + "[@nillable='true'][@minOccurs='0']",
                    schema);
        assertValid("//xsd:complexType[@name='bean']/xsd:sequence/xsd:element[@name='int2'][@minOccurs='0']",
                    schema);
        assertInvalid("//xsd:complexType[@name='bean']/xsd:sequence"
                        + "/xsd:element[@name='int2'][@nillable='true']",
                      schema);
    }
    @Test
    public void testNillableIntMinOccurs1() throws Exception {
        context = new AegisContext();

        TypeCreationOptions config = new TypeCreationOptions();
        config.setDefaultMinOccurs(1);
        config.setDefaultNillable(false);
        context.setTypeCreationOptions(config);
        context.initialize();
        mapping = context.getTypeMapping();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(IntBean.class);
        type.setTypeClass(IntBean.class);
        type.setTypeMapping(mapping);

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        assertValid("//xsd:complexType[@name='IntBean']/xsd:sequence/xsd:element[@name='int1']", schema);
        assertInvalid(
                      "//xsd:complexType[@name='IntBean']/xsd:sequence/xsd:element[@name='int1'][@minOccurs]",
                      schema);
        assertInvalid("//xsd:complexType[@name='IntBean']/xsd:sequence/xsd:element[@name='int1'][@nillable]",
                      schema);
    }
    
    @Test
    public void testCharMappings() throws Exception {
        context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(SimpleBean.class);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        NodeList typeAttrNode = 
            assertValid("//xsd:complexType[@name='SimpleBean']/xsd:sequence/xsd:element[@name='character']"
                       + "/@type", 
                       schema); 
        assertEquals(1, typeAttrNode.getLength());
        Attr typeAttr = (Attr)typeAttrNode.item(0);
        String typeQnameString = typeAttr.getValue();
        String[] pieces = typeQnameString.split(":");
        assertEquals(CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME.getLocalPart(),
                     pieces[1]);
    }
    
    @Test
    public void testByteMappings() throws Exception {
        context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(SimpleBean.class);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);

        Element types = new Element("types", "xsd", SOAPConstants.XSD);
        Element schema = new Element("schema", "xsd", SOAPConstants.XSD);
        types.addContent(schema);

        new Document(types);

        type.writeSchema(schema);

        NodeList typeAttrNode = 
            assertValid("//xsd:complexType[@name='SimpleBean']/xsd:sequence/xsd:element[@name='littleByte']"
                       + "/@type", 
                       schema); 
        assertEquals(1, typeAttrNode.getLength());
        Attr typeAttr = (Attr)typeAttrNode.item(0);
        String typeQnameString = typeAttr.getValue();
        String[] pieces = typeQnameString.split(":");
        assertEquals("xsd", pieces[0]);
        assertEquals("byte", pieces[1]);
        
        typeAttrNode = 
            assertValid("//xsd:complexType[@name='SimpleBean']/xsd:sequence/xsd:element[@name='bigByte']"
                       + "/@type", 
                       schema); 
        assertEquals(1, typeAttrNode.getLength());
        typeAttr = (Attr)typeAttrNode.item(0);
        typeQnameString = typeAttr.getValue();
        pieces = typeQnameString.split(":");
        assertEquals("xsd", pieces[0]);
        assertEquals("byte", pieces[1]);
        
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        SimpleBean bean = new SimpleBean();
        bean.setBigByte(new Byte((byte)0xfe));
        bean.setLittleByte((byte)0xfd);
        type.writeObject(bean, new JDOMWriter(element), getContext());
        Byte bb = new Byte((byte)0xfe);
        String bbs = bb.toString();
        assertValid("/b:root/bz:bigByte[text()='" + bbs + "']", element);
        
        // Test reading
        ElementReader reader = new ElementReader(getResourceAsStream("byteBeans.xml"));
        bean = (SimpleBean)type.readObject(reader, getContext());
        assertEquals(-5, bean.getLittleByte());
        assertEquals(25, bean.getBigByte().byteValue());

        reader.getXMLStreamReader().close();

    }
    
    @Test
    public void testNullNonNillableWithDate() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(DateBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(DateBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        DateBean bean = new DateBean();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        // Make sure the date doesn't have an element. Its non nillable so it
        // just
        // shouldn't be there.
        assertInvalid("/b:root/b:date", element);
        assertValid("/b:root", element);
    }
    @Test
    public void testExtendedBean() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(ExtendedBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(ExtendedBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        assertEquals(8, pds.length);

        ExtendedBean bean = new ExtendedBean();
        bean.setHowdy("howdy");

        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }
    @Test
    public void testByteBean() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(ByteBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(ByteBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        QName name = new QName("urn:Bean", "data");
        Type dataType = type.getTypeInfo().getType(name);
        assertNotNull(dataType);

        assertTrue(type.getTypeInfo().isNillable(name));

        ByteBean bean = new ByteBean();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        type.writeObject(bean, new JDOMWriter(element), getContext());

        // Make sure the date doesn't have an element. Its non nillable so it
        // just
        // shouldn't be there.

        addNamespace("xsi", SOAPConstants.XSI_NS);
        assertValid("/b:root/b:data[@xsi:nil='true']", element);

        bean = (ByteBean)type.readObject(new JDOMReader(element), getContext());
        assertNotNull(bean);
        assertNull(bean.getData());
    }
    @Test
    public void testGetSetRequired() throws Exception {
        BeanType type = new BeanType();
        type.setTypeClass(GoodBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean"));

        assertTrue(type.getTypeInfo().getElements().hasNext());

        type = new BeanType();
        type.setTypeClass(BadBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean"));

        assertFalse(type.getTypeInfo().getElements().hasNext());

        type = new BeanType();
        type.setTypeClass(BadBean2.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean2"));

        assertFalse(type.getTypeInfo().getElements().hasNext());
    }

    public static class DateBean {
        private Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    public static class IntBean {
        private Integer int1;
        private int int2;

        public Integer getInt1() {
            return int1;
        }

        public void setInt1(Integer int1) {
            this.int1 = int1;
        }

        public int getInt2() {
            return int2;
        }

        public void setInt2(int int2) {
            this.int2 = int2;
        }
    }

    public static class ByteBean {
        private byte[] data;

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    // This class only has a read property, no write
    public static class GoodBean {
        private String string;

        public String getString() {
            return string;
        }
    }

    public static class BadBean {
        public String delete() {
            return null;
        }
    }

    public static class BadBean2 {
        public void setString(String string) {
        }
    }

    public static class ExtendedBean extends SimpleBean {
        private String howdy;

        public String getHowdy() {
            return howdy;
        }

        public void setHowdy(String howdy) {
            this.howdy = howdy;
        }
    }
}
