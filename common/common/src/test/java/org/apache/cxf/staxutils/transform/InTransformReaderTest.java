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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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
                                        null, null, null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue("<test2 xmlns=\"\"/>".equals(value));        
    }
    
    @Test
    public void testReplaceSimpleElement() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<ns:test xmlns:ns=\"http://bar\"><ns:a>1</ns:a></ns:test>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                        null,
                                        Collections.singletonMap("{http://bar}a", "{http://bar}a:1 2 3"),
                                        null, 
                                        null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ns:test xmlns:ns=\"http://bar\"><ns:a>1 2 3</ns:a></ns:test>", value);        
    }
    
    @Test
    public void testTransformAndReplaceSimpleElement() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<ns:test xmlns:ns=\"http://bar\"><ns:a>1</ns:a></ns:test>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar}*", "{http://foo}*"),
                                       Collections.singletonMap("{http://bar}a", "{http://bar}a:1 2 3"),
                                       null, 
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals(
                "<ps1:test xmlns:ps1=\"http://foo\"><ps1:a>1 2 3</ps1:a></ps1:test>", value);        
    }
    
    @Test
    public void testReadWithParentDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
            "<test xmlns=\"http://bar\"><ns:subtest xmlns:ns=\"http://bar1\"/></test>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                        Collections.singletonMap("{http://bar1}subtest", "subtest"), 
                                        null, null, null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ps1:test xmlns:ps1=\"http://bar\"><subtest xmlns=\"\"/></ps1:test>",
                     value);        
    }
    @Test
    public void testReadWithComplexRequestSameNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn1.xml"));        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/element}*");
        
        reader = new InTransformReader(reader, 
                                       inMap, null, null,
                                       null, false);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq1.xml"));
        verifyReaders(reader, reader2, true);
    }
    
    @Test
    public void testReadWithComplexRequestMultipleNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn2.xml"));        
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        
        reader = new InTransformReader(reader, 
                                        inMap, null, null,
                                        null, false);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq2.xml"));        
        verifyReaders(reader, reader2, true);
    }
    
    @Test
    public void testReadWithComplexTransformationNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn3.xml"));        
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        inMap.put("{http://schemas.xmlsoap.org/soap/envelope/}Envelope", 
                "{http://schemas.xmlsoap.org/soap/envelope/}TheEnvelope");
        
        // set the block original reader flag to true
        reader = new InTransformReader(reader, 
                                       inMap, null, null,
                                       null, true);
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq3.xml"));        
        verifyReaders(reader, reader2, true);
    }

    @Test
    public void testReadPartialWithComplexRequestSameNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn1.xml"));        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/element}*");
        
        reader = new InTransformReader(reader, 
                                       inMap, null, null,
                                       null, false);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq1partial.xml"));        
        verifyReaders(filteredReader, reader2, false);
    }
    
    @Test
    public void testPartialReadWithComplexRequestMultipleNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn2.xml"));        
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        
        reader = new InTransformReader(reader, 
                                        inMap, null, null,
                                        null, false);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq2partial.xml"));        
        verifyReaders(filteredReader, reader2, false);
    }
    
    @Test
    public void testPartialReadWithComplexTransformationNamespace() throws Exception {
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream("../resources/complexReqIn3.xml"));        
        
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        inMap.put("{http://schemas.xmlsoap.org/soap/envelope/}Envelope", 
                "{http://schemas.xmlsoap.org/soap/envelope/}TheEnvelope");
        
        // set the block original reader flag to true
        reader = new InTransformReader(reader, 
                                       inMap, null, null,
                                       null, true);
        
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                  InTransformReader.class.getResourceAsStream("../resources/complexReq3partial.xml"));        
        verifyReaders(filteredReader, reader2, false);
    }
    
    
    @Test
    public void testReadWithReplaceAppend() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("requestValue",
                              "{http://cxf.apache.org/hello_world_soap_http/types}requestType");
        
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("requestValue",
                           "{http://cxf.apache.org/hello_world_soap_http/types}greetMe");

        transformStreamAndCompare("../resources/greetMeReqIn1.xml", "../resources/greetMeReq.xml",
                                  transformElements, appendElements, null, null, null);
    }

    @Test
    public void testReadWithReplaceAppendDelete() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("requestValue",
                              "{http://cxf.apache.org/hello_world_soap_http/types}requestType");
        transformElements.put("{http://cxf.apache.org/hello_world_soap_http/types}requestDate",
                              "");
        
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("requestValue",
                           "{http://cxf.apache.org/hello_world_soap_http/types}greetMe");
        
        List<String> dropElements = new ArrayList<String>();
        dropElements.add("value");
        
        Map<String, String> transformAttributes = new HashMap<String, String>();
        transformAttributes.put("num", "");
        transformAttributes.put("nombre", "{http://cxf.apache.org/hello_world_soap_http/types}name");
        
        transformStreamAndCompare("../resources/greetMeReqIn2.xml", "../resources/greetMeReq.xml",
                                  transformElements, appendElements, dropElements, 
                                  transformAttributes, null);
    }

    @Test
    public void testReadWithChangeNamespaces() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("*",
                              "{http://cxf.apache.org/hello_world_soap_http/types}*");

        transformStreamAndCompare("../resources/greetMeReqIn3.xml", "../resources/greetMeReq.xml",
                                  transformElements, null, null, null, null);
    }

    @Test
    public void testReadWithDeleteAttributes() throws Exception {
        Map<String, String> transformAttributes = new HashMap<String, String>();
        transformAttributes.put("{http://www.w3.org/2001/XMLSchema-instance}type",
                                "");

        transformStreamAndCompare("../resources/greetMeReqIn4.xml", "../resources/greetMeReq.xml",
                                  null, null, null, transformAttributes, null);
    }
    
    @Test
    public void testReadWithAppendPreInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType:ASIN");
        transformStreamAndCompare("../resources/amazonIn1.xml", "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPreInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType:ASIN");
        transformStreamAndCompare("../resources/amazonIn1nospace.xml", "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPreWrap1() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("payload",
                              "{http://www.w3.org/2003/05/soap-envelope}Envelope");
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://apache.org/cxf/calculator/types}add",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        transformStreamAndCompare("../resources/AddRequestIn2.xml", "../resources/AddRequest.xml",
                                  transformElements, appendElements, null, null, null);
    }

    @Test
    public void testReadWithAppendPreWrap2() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("payload",
                              "{http://www.w3.org/2003/05/soap-envelope}Envelope");
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://apache.org/cxf/calculator/types}add",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        transformStreamAndCompare("../resources/AddRequestIn2nospace.xml", "../resources/AddRequest.xml",
                                  transformElements, appendElements, null, null, null);
    }

    @Test
    public void testReadWithAppendPostInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType:ASIN");
        transformStreamAndCompare("../resources/amazonIn1.xml", "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType:ASIN");
        transformStreamAndCompare("../resources/amazonIn1nospace.xml", "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostWrap1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        transformStreamAndCompare("../resources/AddRequestIn1.xml", "../resources/AddRequest.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostWrap2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        transformStreamAndCompare("../resources/AddRequestIn1nospace.xml", "../resources/AddRequest.xml",
                                  null, appendElements, null, null, null);
        
    }

    // test utilities methods 
    
    private void transformStreamAndCompare(String inname, String outname, 
                                           Map<String, String> transformElements,
                                           Map<String, String> appendElements,
                                           List<String> dropElements,
                                           Map<String, String> transformAttributes,
                                           Map<String, String> appendAttributes) 
        throws XMLStreamException {
        
        XMLStreamReader reader = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream(inname));

        reader = new InTransformReader(reader,
                                        transformElements, appendElements, dropElements, 
                                        transformAttributes, false);

        XMLStreamReader teacher = 
            StaxUtils.createXMLStreamReader(
                      InTransformReader.class.getResourceAsStream(outname));
        
        verifyReaders(reader, teacher, false);
    }

    /**
     * Verifies the two stream events are equivalent and throws an assertion 
     * exception at the first mismatch.
     * @param reader
     * @param teacher
     * @param eec
     * @throws XMLStreamException
     */
    private void verifyReaders(XMLStreamReader reader, XMLStreamReader teacher, 
                               boolean eec) throws XMLStreamException {
        // compare the elements and attributes while ignoring comments, line breaks, etc
        for (;;) {
            int revent = getNextEvent(reader);
            int tevent = getNextEvent(teacher);
            
            if (revent == -1 && tevent == -1) {
                break;
            }
            assertEquals(revent, tevent);

            switch (revent) {
            case XMLStreamConstants.START_ELEMENT:
                assertEquals(teacher.getName(), reader.getName());
                verifyAttributes(reader, teacher);
                break;
            case XMLStreamConstants.END_ELEMENT:
                if (eec) {
                    // perform end-element-check
                    assertEquals(teacher.getName(), reader.getName());
                }
                break;
            case XMLStreamConstants.CHARACTERS:
                assertEquals(teacher.getText(), reader.getText());
                break;
            default:
            }
        }
    }

    private void verifyAttributes(XMLStreamReader reader, XMLStreamReader teacher) {
        int acount = teacher.getAttributeCount();
        assertEquals(acount, reader.getAttributeCount());
        Map<QName, String> attributesMap = new HashMap<QName, String>();
        // temporarily store all the attributes
        for (int i = 0; i < acount; i++) {
            attributesMap.put(reader.getAttributeName(i), reader.getAttributeValue(i));
        }
        // compares each attribute
        for (int i = 0; i < acount; i++) {
            String avalue = attributesMap.remove(teacher.getAttributeName(i));
            assertEquals(avalue, teacher.getAttributeValue(i));
        }
        // attributes must be exhausted
        assertTrue(attributesMap.isEmpty());
    }

    /**
     * Returns the next relevant reader event.
     *  
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private int getNextEvent(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int e = reader.next();
            if (e == XMLStreamConstants.END_DOCUMENT) {
                return e;
            }
            if (e == XMLStreamConstants.START_ELEMENT || e == XMLStreamConstants.END_ELEMENT) {
                return e;
            } else if (e == XMLStreamConstants.CHARACTERS) {
                String text = reader.getText();
                if (text.trim().length() == 0) {
                    continue;
                }
                return e;
            }
        }
        return -1;
    }
}
