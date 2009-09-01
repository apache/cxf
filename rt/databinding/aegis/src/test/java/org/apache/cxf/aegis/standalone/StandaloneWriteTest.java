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

package org.apache.cxf.aegis.standalone;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.services.SimpleBean;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.test.TestUtilities;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 */
public class StandaloneWriteTest {
    private AegisContext context;
    private TestUtilities testUtilities;
    private XMLOutputFactory xmlOutputFactory;
    private XMLInputFactory xmlInputFactory;
    
    private interface ListStringInterface {
        List<String> method();
    }
    
    @Before
    public void before() {
        testUtilities = new TestUtilities(getClass());
        testUtilities.addNamespace("feline", "urn:meow");
        xmlOutputFactory = XMLOutputFactory.newInstance();
        xmlInputFactory = XMLInputFactory.newInstance();
    }
    
    @Test
    public void testTypeLookup() throws Exception {
        context = new AegisContext();
        context.initialize();
        AegisType st = context.getTypeMapping().getType(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, 
                                                             "string"));
        assertNotNull(st);
        assertEquals(st.getClass(), StringType.class);
    }
    
    @Test
    public void testBasicTypeWrite() throws Exception {
        context = new AegisContext();
        context.initialize();
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        writer.write("ball-of-yarn",
                     new QName("urn:meow", "cat-toy"),
                      false, xmlWriter, new StringType());
        xmlWriter.close();
        String xml = stringWriter.toString();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));
        reader.nextTag();
        assertEquals("urn:meow", reader.getNamespaceURI());
        assertEquals("cat-toy", reader.getLocalName());
        reader.next();
        String text = reader.getText();
        assertEquals("ball-of-yarn", text);
    }
    
    @Test
    public void testWriteCollection() throws Exception {
        context = new AegisContext();
        context.setWriteXsiTypes(true);
        context.initialize();
        List<String> strings = new ArrayList<String>();
        strings.add("cat");
        strings.add("dog");
        strings.add("hailstorm");
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        java.lang.reflect.Type listStringType 
            = ListStringInterface.class.getMethods()[0].getGenericReturnType();
        writer.write(strings, new QName("urn:borghes", "items"),
                      false, xmlWriter, listStringType);
        xmlWriter.close();
        String xml = stringWriter.toString();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(xml));
        reader.nextTag();
        assertEquals("urn:borghes", reader.getNamespaceURI());
        assertEquals("items", reader.getLocalName());
        reader.nextTag();
        assertEquals(reader.getNamespaceURI(), "urn:org.apache.cxf.aegis.types");
        assertEquals("string", reader.getLocalName());
        String text = reader.getElementText();
        assertEquals("cat", text);
        reader.nextTag();
        assertEquals(reader.getNamespaceURI(), "urn:org.apache.cxf.aegis.types");
        assertEquals("string", reader.getLocalName());
        text = reader.getElementText();
        assertEquals("dog", text);
        reader.nextTag();
        assertEquals(reader.getNamespaceURI(), "urn:org.apache.cxf.aegis.types");
        assertEquals("string", reader.getLocalName());
        text = reader.getElementText();
        assertEquals("hailstorm", text);
    }
    
    @Test
    public void testBean() throws Exception {
        context = new AegisContext();
        Set<java.lang.reflect.Type> rootClasses = new HashSet<java.lang.reflect.Type>();
        rootClasses.add(SimpleBean.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        SimpleBean sb = new SimpleBean();
        sb.setCharacter('\u4000');
        sb.setHowdy("doody");
        AegisType sbType = context.getTypeMapping().getType(sb.getClass());
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        writer.write(sb, new QName("urn:meow", "catnip"),
                          false, xmlWriter, sbType);
        xmlWriter.close();
        String xml = stringWriter.toString();
        assertTrue(xml.contains("doody"));
        
    }
}
