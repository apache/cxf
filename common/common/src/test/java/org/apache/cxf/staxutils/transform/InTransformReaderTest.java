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
package org.apache.cxf.staxutils.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;
import org.junit.Test;

public class InTransformReaderTest extends Assert {
    @Test
    public void testReadWithDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream("<test xmlns=\"http://bar\"/>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar}test", "test2"),
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue("<test2 xmlns=\"\"/>".equals(value));        
    }
    
    @Test
    public void testReadWithParentDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
            "<test xmlns=\"http://bar\"><ns:subtest xmlns:ns=\"http://bar1\"/></test>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar1}subtest", "subtest"),
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ps1:test xmlns:ps1=\"http://bar\"><subtest xmlns=\"\"/></ps1:test>",
                     value);        
    }
    
    @Test
    public void testTransformAndAppend() throws Exception {
        InputStream is = new ByteArrayInputStream("<test/>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("test", "test2"),
                                       Collections.singletonMap("test", "wrapper"), 
                                       false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        System.out.println(value);
        assertTrue("<wrapper><test2/></wrapper>".equals(value));        
    }
    
    
    @Test
    public void testReadWithSameNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<test xmlns=\"http://bar\" xmlns:ns1=\"http://foo\">" 
                + "<ns1:subtest>Hello</ns1:subtest></test>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://bar}test", "test2");
        inMap.put("{http://foo}*", "{http://foo}*");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue(("<test2 xmlns=\"\" xmlns:ps1=\"http://foo\">" 
                + "<ps1:subtest>Hello</ps1:subtest></test2>").equals(value));        
    }
    
    @Test
    public void testReadWithComplexRequestSameNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/element}*");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        
        assertTrue(("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Header><ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ps1=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ps1:CallerCorrelationId>SomeComplexValue</ps1:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType></ns2:SoapHeaderIn></soap:Header><soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ps1=\"http://cxf.apache.org/transform/header/element\" " 
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\"/></soap:Body></soap:Envelope>").
                equals(value.trim()));
    }
    
    @Test
    public void testReadWithComplexRequestMultipleNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue(("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Header><ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ps1=\"http://cxf.apache.org/transform/header/otherelement\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ps2=\"http://cxf.apache.org/transform/othertest\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ps1:CallerCorrelationId>SomeComplexValue</ps1:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType></ns2:SoapHeaderIn></soap:Header><soap:Body>"
                + "<ps2:TransformTestRequest xmlns:ps2=\"http://cxf.apache.org/transform/othertest\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ps1=\"http://cxf.apache.org/transform/header/otherelement\" " 
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\"/></soap:Body></soap:Envelope>").
                equals(value.trim()));
    }
    @Test
    public void testReadWithComplexTransformationNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        inMap.put("{http://schemas.xmlsoap.org/soap/envelope/}Envelope", 
                "{http://schemas.xmlsoap.org/soap/envelope/}TheEnvelope");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue(("<ps1:TheEnvelope xmlns:ps1=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Header xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ps2=\"http://cxf.apache.org/transform/header/otherelement\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ps3=\"http://cxf.apache.org/transform/othertest\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ps2:CallerCorrelationId>SomeComplexValue</ps2:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType></ns2:SoapHeaderIn></soap:Header>"
                + "<soap:Body xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<ps3:TransformTestRequest xmlns:ps3=\"http://cxf.apache.org/transform/othertest\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ps2=\"http://cxf.apache.org/transform/header/otherelement\" " 
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\"/></soap:Body></ps1:TheEnvelope>").
                equals(value.trim()));
    }
    
    @Test
    public void testReadPartialWithComplexRequestSameNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/element}*");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        Document doc = StaxUtils.read(filteredReader);
        
        assertNotNull(doc);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(bos);
        StaxUtils.copy(doc, writer);
        writer.flush();

        String value = bos.toString();
        
        Document docTransformed = DOMUtils.readXml(new StringReader(value));
        Element env = docTransformed.getDocumentElement();
        assertEquals("http://schemas.xmlsoap.org/soap/envelope/", env.getNamespaceURI());
        assertEquals("soap", env.getPrefix());
        
        Element header = getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Header", "soap");
        
        Element customHeader = getElement(header, "http://cxf.apache.org/transform/header", 
                                          "SoapHeaderIn", "ns2");
        NamedNodeMap map = customHeader.getAttributes();
        assertEquals(4, map.getLength());
        validateAttribute(map, "xmlns", "http://cxf.apache.org/transform/test");
        validateAttribute(map, "xmlns:ns2", "http://cxf.apache.org/transform/header");
        validateAttribute(map, "xmlns:ns4", "http://cxf.apache.org/transform/fault");
        validateAttribute(map, "xmlns:ps1", "http://cxf.apache.org/transform/header/element");
        
        Element mode = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", "OperationalMode", "ns2");
        assertEquals("SIMULATION1", mode.getTextContent());
        Element headerType = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", 
                       "SomeComplexHeaderType", "ns2");
        
        Element correlationId = 
            getElement(headerType, "http://cxf.apache.org/transform/header/element", 
                       "CallerCorrelationId", "ps1");
        assertEquals("SomeComplexValue", correlationId.getTextContent());
        getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Body", "soap");
    }
    
    @Test
    public void testPartialReadWithComplexRequestMultipleNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, false);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        Document doc = StaxUtils.read(filteredReader);
        
        assertNotNull(doc);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(bos);
        StaxUtils.copy(doc, writer);
        writer.flush();

        String value = bos.toString();
        
        Document docTransformed = DOMUtils.readXml(new StringReader(value));
        Element env = docTransformed.getDocumentElement();
        assertEquals("http://schemas.xmlsoap.org/soap/envelope/", env.getNamespaceURI());
        assertEquals("soap", env.getPrefix());
        
        Element header = getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Header", "soap");
        
        Element customHeader = getElement(header, "http://cxf.apache.org/transform/header", 
                                          "SoapHeaderIn", "ns2");
        NamedNodeMap map = customHeader.getAttributes();
        assertEquals(4, map.getLength());
        validateAttribute(map, "xmlns:ps2", "http://cxf.apache.org/transform/othertest");
        validateAttribute(map, "xmlns:ns2", "http://cxf.apache.org/transform/header");
        validateAttribute(map, "xmlns:ns4", "http://cxf.apache.org/transform/fault");
        validateAttribute(map, "xmlns:ps1", "http://cxf.apache.org/transform/header/otherelement");
        
        Element mode = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", "OperationalMode", "ns2");
        assertEquals("SIMULATION1", mode.getTextContent());
        Element headerType = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", 
                       "SomeComplexHeaderType", "ns2");
        
        Element correlationId = 
            getElement(headerType, "http://cxf.apache.org/transform/header/otherelement", 
                       "CallerCorrelationId", "ps1");
        assertEquals("SomeComplexValue", correlationId.getTextContent());
        getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Body", "soap");
        
    }
    
    @Test
    public void testPartialReadWithComplexTransformationNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                ("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" 
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns:ns4=\"http://cxf.apache.org/transform/fault\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns=\"http://cxf.apache.org/transform/test\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType>"
                + "</ns2:SoapHeaderIn>"
                + "</soap:Header>"
                + "<soap:Body>"
                + "<TransformTestRequest xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\" />"
                + "</soap:Body></soap:Envelope>").getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        inMap.put("{http://schemas.xmlsoap.org/soap/envelope/}Envelope", 
                "{http://schemas.xmlsoap.org/soap/envelope/}TheEnvelope");
        
        // set the block original reader flag to true
        reader = new InTransformReader(reader, 
                                       inMap,
                                       null, true);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        Document doc = StaxUtils.read(filteredReader);
        
        assertNotNull(doc);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(bos);
        StaxUtils.copy(doc, writer);
        writer.flush();
        
        String value = bos.toString();
        
        Document docTransformed = DOMUtils.readXml(new StringReader(value));
        Element env = docTransformed.getDocumentElement();
        assertEquals("http://schemas.xmlsoap.org/soap/envelope/", env.getNamespaceURI());
        assertEquals("ps1", env.getPrefix());
        assertEquals("TheEnvelope", env.getLocalName());
        
        Element header = getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Header", "soap");
        
        Element customHeader = getElement(header, "http://cxf.apache.org/transform/header", 
                                          "SoapHeaderIn", "ns2");
        NamedNodeMap map = customHeader.getAttributes();
        assertEquals(4, map.getLength());
        validateAttribute(map, "xmlns:ps3", "http://cxf.apache.org/transform/othertest");
        validateAttribute(map, "xmlns:ns2", "http://cxf.apache.org/transform/header");
        validateAttribute(map, "xmlns:ns4", "http://cxf.apache.org/transform/fault");
        validateAttribute(map, "xmlns:ps2", "http://cxf.apache.org/transform/header/otherelement");
        
        Element mode = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", "OperationalMode", "ns2");
        assertEquals("SIMULATION1", mode.getTextContent());
        Element headerType = 
            getElement(customHeader, "http://cxf.apache.org/transform/header", 
                       "SomeComplexHeaderType", "ns2");
        
        Element correlationId = 
            getElement(headerType, "http://cxf.apache.org/transform/header/otherelement", 
                       "CallerCorrelationId", "ps2");
        assertEquals("SomeComplexValue", correlationId.getTextContent());
        getElement(env, "http://schemas.xmlsoap.org/soap/envelope/", "Body", "soap");
        
    }
    
    
    private Element getElement(Element parent, String namespace, String name, String prefix) {
        NodeList list = parent.getElementsByTagNameNS(namespace, name);
        assertEquals(1, list.getLength());
        Element el = (Element)list.item(0);
        assertEquals(prefix, el.getPrefix());
        return el;
    }
    
    private void validateAttribute(NamedNodeMap map, String name, String value) {
        Node attr = map.getNamedItem(name);
        assertEquals(attr.getTextContent(), value);
    }
}
