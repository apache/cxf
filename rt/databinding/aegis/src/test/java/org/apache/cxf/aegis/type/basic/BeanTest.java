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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
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

    }

    private void defaultContext() {
        context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();
    }

    @Test
    public void testBean() throws Exception {
        defaultContext();
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
        Element element = writeObjectToElement(type, bean, getContext());

        assertValid("/b:root/b:bleh[text()='bleh']", element);
        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }

    @Test
    public void testBeanWithXsiType() throws Exception {
        defaultContext();
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

        Element element = writeObjectToElement(type, bean, getContext());

        assertValid("/b:root/b:bleh[text()='bleh']", element);
        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }

    @Test
    public void testUnmappedProperty() throws Exception {
        
        defaultContext();
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

        Element element = writeObjectToElement(type, bean, getContext());

        assertInvalid("/b:root/b:bleh", element);
        assertValid("/b:root/b:howdycustom[text()='howdy']", element);
    }
    
    @Test
    public void testAttributeMap() throws Exception {
        defaultContext();
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
        Element element = writeObjectToElement(type, bean, getContext());
        assertValid("/b:root[@b:bleh='bleh']", element);
        assertValid("/b:root[@b:howdy='howdy']", element);
        
        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType stype = (XmlSchemaComplexType)schema.getTypeByName("bean");
        boolean howdy = false;
        boolean bleh = false;
        for (int x = 0; x < stype.getAttributes().getCount(); x++) {
            XmlSchemaObject o = stype.getAttributes().getItem(x);
            if (o instanceof XmlSchemaAttribute) {
                XmlSchemaAttribute a = (XmlSchemaAttribute)o;
                if ("howdy".equals(a.getName())) {
                    howdy = true;
                }
                if ("bleh".equals(a.getName())) {
                    bleh = true;
                }
            }
        }
        assertTrue(howdy);
        assertTrue(bleh);
    }

    @Test
    public void testAttributeMapDifferentNS() throws Exception {
        defaultContext();
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
        
        Document doc = DOMUtils.readXml(new ByteArrayInputStream(bos.toByteArray()));
        Element element = doc.getDocumentElement();

        addNamespace("b2", "urn:Bean2");
        assertValid("/b:root[@b2:bleh='bleh']", element);
        assertValid("/b:root[@b2:howdy='howdy']", element);
    }

    @Test
    public void testNullProperties() throws Exception {
        defaultContext();
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
        
        Element element = writeObjectToElement(type, bean, getContext());

        assertInvalid("/b:root[@b:howdy]", element);
        assertValid("/b:root/b:bleh[@xsi:nil='true']", element);

        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType stype = (XmlSchemaComplexType)schema.getTypeByName("bean");
        XmlSchemaSequence seq = (XmlSchemaSequence) stype.getParticle();
        boolean howdy = false;
        boolean bleh = false;

        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement a = (XmlSchemaElement)o;
                if ("bleh".equals(a.getName())) {
                    bleh = true;
                }
            }
        }

        for (int x = 0; x < stype.getAttributes().getCount(); x++) {
            XmlSchemaObject o = stype.getAttributes().getItem(x);
            if (o instanceof XmlSchemaAttribute) {
                XmlSchemaAttribute a = (XmlSchemaAttribute)o;
                if ("howdy".equals(a.getName())) {
                    howdy = true;
                }
            }
        }

        assertTrue(howdy);
        assertTrue(bleh);
    }
    
    @Test
    public void testNillableInt() throws Exception {
        defaultContext();
        BeanTypeInfo info = new BeanTypeInfo(IntBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(IntBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType btype = (XmlSchemaComplexType)schema.getTypeByName("bean");
        XmlSchemaSequence seq = (XmlSchemaSequence)btype.getParticle();
        boolean int1ok = false;
        boolean int2ok = false;
        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement oe = (XmlSchemaElement) o;
                if ("int1".equals(oe.getName())) {
                    int1ok = true;
                    assertTrue(oe.isNillable());
                    assertEquals(0, oe.getMinOccurs());
                } else if ("int2".equals(oe.getName())) {
                    int2ok = true;
                    assertEquals(0, oe.getMinOccurs());
                    assertFalse(oe.isNillable());
                }
            }
        }
        assertTrue(int1ok);
        assertTrue(int2ok);
    }
    
    @Test
    public void testNillableAnnotation() throws Exception {
        context = new AegisContext();
        TypeCreationOptions config = new TypeCreationOptions();
        config.setDefaultNillable(false);
        config.setDefaultMinOccurs(1);
        context.setTypeCreationOptions(config);
        context.initialize();
        mapping = context.getTypeMapping();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(BeanWithNillableItem.class);
        type.setTypeClass(BeanWithNillableItem.class);
        type.setTypeMapping(mapping);

        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType btype = (XmlSchemaComplexType)schema.getTypeByName("BeanWithNillableItem");
        XmlSchemaSequence seq = (XmlSchemaSequence)btype.getParticle();
        boolean itemFound = false;
        boolean itemNotNillableFound = false;
        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement oe = (XmlSchemaElement) o;
                if ("item".equals(oe.getName())) {
                    itemFound = true;
                    assertTrue(oe.isNillable());
                    assertEquals(0, oe.getMinOccurs());
                } else if ("itemNotNillable".equals(oe.getName())) {
                    itemNotNillableFound = true;
                    assertFalse(oe.isNillable());
                }
            }
        }
        assertTrue(itemFound);
        assertTrue(itemNotNillableFound);
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

        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType btype = (XmlSchemaComplexType)schema.getTypeByName("IntBean");
        XmlSchemaSequence seq = (XmlSchemaSequence)btype.getParticle();
        boolean int1ok = false;
        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement oe = (XmlSchemaElement) o;
                if ("int1".equals(oe.getName())) {
                    int1ok = true;
                    assertFalse(oe.isNillable());
                    assertEquals(1, oe.getMinOccurs());
                } 
            }
        }
        assertTrue(int1ok);
    }
    
    @Test
    public void testCharMappings() throws Exception {
        defaultContext();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(SimpleBean.class);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);
        
        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType btype = (XmlSchemaComplexType)schema.getTypeByName("SimpleBean");
        XmlSchemaSequence seq = (XmlSchemaSequence)btype.getParticle();
        boolean charok = false;

        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement oe = (XmlSchemaElement) o;
                if ("character".equals(oe.getName())) {
                    charok = true;
                    assertNotNull(oe.getSchemaTypeName());
                    assertTrue(oe.isNillable());
                    assertEquals(CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME, 
                                 oe.getSchemaTypeName());
                }
            }
        }
        assertTrue(charok);
    }
    
    @Test
    public void testByteMappings() throws Exception {
        defaultContext();

        BeanType type = (BeanType)mapping.getTypeCreator().createType(SimpleBean.class);
        type.setTypeClass(SimpleBean.class);
        type.setTypeMapping(mapping);

        XmlSchema schema = newXmlSchema("urn:Bean");
        type.writeSchema(schema);
        
        XmlSchemaComplexType btype = (XmlSchemaComplexType)schema.getTypeByName("SimpleBean");
        XmlSchemaSequence seq = (XmlSchemaSequence)btype.getParticle();
        boolean littleByteOk = false;
        boolean bigByteOk = false;

        for (int x = 0; x < seq.getItems().getCount(); x++) {
            XmlSchemaObject o = seq.getItems().getItem(x);
            if (o instanceof XmlSchemaElement) {
                XmlSchemaElement oe = (XmlSchemaElement) o;
                if ("littleByte".equals(oe.getName())) {
                    littleByteOk = true;
                    assertNotNull(oe.getSchemaTypeName());
                    assertEquals(XmlSchemaConstants.BYTE_QNAME, oe.getSchemaTypeName());
                } else if ("bigByte".equals(oe.getName())) {
                    bigByteOk = true;
                    assertNotNull(oe.getSchemaTypeName());
                    assertEquals(XmlSchemaConstants.BYTE_QNAME, oe.getSchemaTypeName());
                }
            }
        }
        assertTrue(littleByteOk);
        assertTrue(bigByteOk);
        
        SimpleBean bean = new SimpleBean();
        bean.setBigByte(new Byte((byte)0xfe));
        bean.setLittleByte((byte)0xfd);
        Element element = writeObjectToElement(type, bean, getContext());
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
        defaultContext();
        BeanTypeInfo info = new BeanTypeInfo(DateBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(DateBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        DateBean bean = new DateBean();

        // Test writing
        Element element = writeObjectToElement(type, bean, getContext());

        // Make sure the date doesn't have an element. Its non nillable so it
        // just
        // shouldn't be there.
        assertInvalid("/b:root/b:date", element);
        assertValid("/b:root", element);
    }
    
    @Test
    public void testExtendedBean() throws Exception {
        defaultContext();
        BeanTypeInfo info = new BeanTypeInfo(ExtendedBean.class, "urn:Bean");
        info.setTypeMapping(mapping);

        BeanType type = new BeanType(info);
        type.setTypeClass(ExtendedBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        assertEquals(9, pds.length);

        ExtendedBean bean = new ExtendedBean();
        bean.setHowdy("howdy");

        Element element = writeObjectToElement(type, bean, getContext());
        assertValid("/b:root/b:howdy[text()='howdy']", element);
    }
    
    @Test
    public void testByteBean() throws Exception {
        defaultContext();
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
        Element element = writeObjectToElement(type, bean, getContext());

        // Make sure the date doesn't have an element. Its non nillable so it
        // just
        // shouldn't be there.

        addNamespace("xsi", SOAPConstants.XSI_NS);
        assertValid("/b:root/b:data[@xsi:nil='true']", element);

        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(element);
        bean = (ByteBean)type.readObject(new ElementReader(sreader), getContext());
        assertNotNull(bean);
        assertNull(bean.getData());
    }
    @Test
    public void testGetSetRequired() throws Exception {
        defaultContext();
        BeanType type = new BeanType();
        type.setTypeClass(GoodBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean"));

        assertTrue(type.getTypeInfo().getElements().iterator().hasNext());

        type = new BeanType();
        type.setTypeClass(BadBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean"));

        assertFalse(type.getTypeInfo().getElements().iterator().hasNext());

        type = new BeanType();
        type.setTypeClass(BadBean2.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:foo", "BadBean2"));

        assertFalse(type.getTypeInfo().getElements().iterator().hasNext());
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
    
    public static class BeanWithNillableItem {
        private IntBean item;
        private Integer itemNotNillable;

        @XmlElement(nillable = true)
        public IntBean getItem() {
            return item;
        }

        public void setItem(IntBean item) {
            this.item = item;
        }

        public Integer getItemNotNillable() {
            return itemNotNillable;
        }

        public void setItemNotNillable(Integer itemNotNillable) {
            this.itemNotNillable = itemNotNillable;
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
