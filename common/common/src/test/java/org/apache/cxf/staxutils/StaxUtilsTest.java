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

package org.apache.cxf.staxutils;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.helpers.XMLUtils;
import org.junit.Assert;
import org.junit.Test;

public class StaxUtilsTest extends Assert {

    @Test
    public void testFactoryCreation() {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream("./resources/amazon.xml"));
        assertTrue(reader != null);
    }

    private InputStream getTestStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void testToNextElement() {
        String soapMessage = "./resources/sayHiRpcLiteralReq.xml";
        XMLStreamReader r = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        DepthXMLStreamReader reader = new DepthXMLStreamReader(r);
        assertTrue(StaxUtils.toNextElement(reader));
        assertEquals("Envelope", reader.getLocalName());

        StaxUtils.nextEvent(reader);

        assertTrue(StaxUtils.toNextElement(reader));
        assertEquals("Body", reader.getLocalName());
    }
    
    @Test
    public void testToNextTag() throws Exception {
        String soapMessage = "./resources/headerSoapReq.xml";
        XMLStreamReader r = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        DepthXMLStreamReader reader = new DepthXMLStreamReader(r);
        reader.nextTag();
        StaxUtils.toNextTag(reader, new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"));
        assertEquals("Body", reader.getLocalName());
    }   
    
    @Test
    public void testCopy() throws Exception {
        
        // do the stream copying
        String soapMessage = "./resources/headerSoapReq.xml";     
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
        StaxUtils.copy(reader, writer);
        writer.flush();
        baos.flush();
           
        // write output to a string
        String output = baos.toString();       
        
        // re-read the input xml doc to a string
        InputStreamReader inputStreamReader = new InputStreamReader(getTestStream(soapMessage));
        StringWriter stringWriter = new StringWriter();
        char[] buffer = new char[4096];
        int n = 0;
        n = inputStreamReader.read(buffer);
        while (n > 0) {
            stringWriter.write(buffer, 0 , n);
            n = inputStreamReader.read(buffer);
        }
        String input = stringWriter.toString();
        // seach for the first begin of "<soap:Envelope" to escape the apache licenses header
        int beginIndex = input.indexOf("<soap:Envelope");
        input = input.substring(beginIndex);
        beginIndex = output.indexOf("<soap:Envelope");
        output = output.substring(beginIndex);
        
        output = output.replaceAll("\r\n", "\n");
        input = input.replaceAll("\r\n", "\n");
        
        // compare the input and output string
        assertEquals(input, output);
    }
    
    @Test
    public void testCXF2468() throws Exception {
        Document doc = XMLUtils.newDocument();
        doc.appendChild(doc.createElementNS("http://blah.org/", "blah"));
        Element foo = doc.createElementNS("http://blah.org/", "foo");
        Attr attr = doc.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:nil");
        attr.setValue("true");
        foo.setAttributeNodeNS(attr);
        doc.getDocumentElement().appendChild(foo);
        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(doc);
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.copy(sreader, swriter, true);
        swriter.flush();
        assertTrue("No xsi namespace: " + sw.toString(), sw.toString().contains("XMLSchema-instance"));
    }
    
    @Test
    public void testNonNamespaceAwareParser() throws Exception {
        String xml = "<blah xmlns=\"http://blah.org/\" xmlns:snarf=\"http://snarf.org\">"
            + "<foo snarf:blop=\"blop\">foo</foo></blah>";

        
        StringReader reader = new StringReader(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
        Source source = new DOMSource(doc);
        
        dbf.setNamespaceAware(true);
        reader = new StringReader(xml);
        Document docNs = dbf.newDocumentBuilder().parse(new InputSource(reader));
        Source sourceNs = new DOMSource(docNs);
        
        
        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(source);
        
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);

        //should not throw an exception
        StaxUtils.copy(sreader, swriter);
        swriter.flush();
        swriter.close();
        
        String output = sw.toString();
        assertTrue(output.contains("blah"));        
        assertTrue(output.contains("foo"));        
        assertTrue(output.contains("snarf"));        
        assertTrue(output.contains("blop"));        
       
        
        sreader = StaxUtils.createXMLStreamReader(sourceNs);
        sw = new StringWriter();
        swriter = StaxUtils.createXMLStreamWriter(sw);
        //should not throw an exception
        StaxUtils.copy(sreader, swriter);
        swriter.flush();
        swriter.close();
        
        output = sw.toString();
        assertTrue(output.contains("blah"));        
        assertTrue(output.contains("foo"));        
        assertTrue(output.contains("snarf"));        
        assertTrue(output.contains("blop"));        

        
        sreader = StaxUtils.createXMLStreamReader(source);
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        swriter = StaxUtils.createXMLStreamWriter(bout);
        StaxUtils.copy(sreader, swriter); 
        swriter.flush();
        swriter.close();
        
        output = bout.toString();
        assertTrue(output.contains("blah"));        
        assertTrue(output.contains("foo"));        
        assertTrue(output.contains("snarf"));        
        assertTrue(output.contains("blop"));        
    }
    
    @Test
    public void testEmptyNamespace() throws Exception {
        String testString = "<ns1:a xmlns:ns1=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2 xmlns=\"\">def</s2></ns1:a>";
        
        cycleString(testString);
        
        testString = "<a xmlns=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2 xmlns=\"\">def</s2></a>";
        cycleString(testString);

        testString = "<a xmlns=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2>def</s2></a>";
        cycleString(testString);
        
        testString = "<ns1:a xmlns:ns1=\"http://www.apache.org/\"><s1>"
            + "abc</s1><s2 xmlns=\"\">def</s2></ns1:a>";
        
        cycleString(testString);
    }
    
    private void cycleString(String s) throws Exception {
        StringReader reader = new StringReader(s);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
        String orig = XMLUtils.toString(doc.getDocumentElement());
        
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        //should not throw an exception
        StaxUtils.writeDocument(doc, swriter, false, true);
        swriter.flush();
        swriter.close();
        
        String output = sw.toString();
        assertEquals(s, output);
        
        W3CDOMStreamWriter domwriter = new W3CDOMStreamWriter();
        StaxUtils.writeDocument(doc, domwriter, false, true);
        output = XMLUtils.toString(domwriter.getDocument().getDocumentElement());
        assertEquals(orig, output);
    }
    
    @Test
    public void testRootPI() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(getTestStream("./resources/rootMaterialTest.xml"));
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.writeDocument(doc, swriter, true, false);
        swriter.flush();
        swriter.close();
        String output = sw.toString();
        assertTrue(output.contains("<?pi in='the sky'?>"));
        assertTrue(output.contains("<?e excl='gads'?>"));
    }
    
    @Test
    public void testRootPInoProlog() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(getTestStream("./resources/rootMaterialTest.xml"));
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.writeDocument(doc, swriter, false, false);
        swriter.flush();
        swriter.close();
        String output = sw.toString();
        assertFalse(output.contains("<?pi in='the sky'?>"));
        assertFalse(output.contains("<?e excl='gads'?>"));
    }
    
    @Test
    public void testDefaultPrefix() throws Exception {
        try {
            String soapMessage = "./resources/AddRequest.xml";     
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            StaxSource staxSource = new StaxSource(reader);
            StaxUtils.copy(staxSource, writer);
            writer.flush();
            baos.flush();
        } catch (XMLStreamException e) {
            fail("shouldn't catch this exception");
        }
           
        
    }
}
