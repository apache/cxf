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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.staxutils.StaxUtils;
import org.junit.Assert;
import org.junit.Test;


public class OutTransformWriterTest extends Assert {

    @Test
    public void testDefaultNamespace() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}*", "{http://testbeans.com/v2}*");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "http://testbeans.com/v2");
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String expected = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<testBean xmlns=\"http://testbeans.com/v2\"><bean/></testBean>";
        assertEquals(expected, os.toString());
    }
    
    @Test
    public void testNamespaceConversion() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}testBean", "{http://testbeans.com/v2}testBean");
        outMap.put("{http://testbeans.com}bean", "{http://testbeans.com/v3}bean");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "");
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String xmlPI = "<?xml version='1.0' encoding='UTF-8'?>";
        String start = "<ps1:testBean xmlns:ps1=\"http://testbeans.com/v2\"";
        String expected1 = xmlPI + start
            + " xmlns:ps2=\"http://testbeans.com/v3\"><ps2:bean/></ps1:testBean>";
        String expected2 = xmlPI + start
            + "><ps2:bean xmlns:ps2=\"http://testbeans.com/v3\"/></ps1:testBean>";
        String out = os.toString();
        assertTrue("Output \"" + out + "\" does not match expected values",
                expected1.equals(out) || expected2.equals(out));
        
    }
    
    @Test
    public void testNamespaceConversionAndDefaultNS() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, "UTF-8");
        
        Map<String, String> outMap = new HashMap<String, String>();
        outMap.put("{http://testbeans.com}testBean", "{http://testbeans.com/v2}testBean");
        outMap.put("{http://testbeans.com}bean", "{http://testbeans.com/v3}bean");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "http://testbeans.com/v2");
        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        m.marshal(new TestBean(), transformWriter);
        
        String xmlPI = "<?xml version='1.0' encoding='UTF-8'?>";
        String start = "<testBean xmlns=\"http://testbeans.com/v2\"";
        String expected1 = xmlPI + start
            + " xmlns:ps2=\"http://testbeans.com/v3\"><ps2:bean/></testBean>";
        String expected2 = xmlPI + start
            + "><ps2:bean xmlns:ps2=\"http://testbeans.com/v3\"/></testBean>";
        String out = os.toString();
        assertTrue("Output \"" + out + "\" does not match expected values",
                expected1.equals(out) || expected2.equals(out));
    }
 
    @XmlRootElement(name = "testBean", namespace = "http://testbeans.com")
    public static class TestBean {
        private TestBean2 bean = new TestBean2();

        @XmlElement(name = "bean", namespace = "http://testbeans.com")
        public void setBean(TestBean2 bean) {
            this.bean = bean;
        }

        public TestBean2 getBean() {
            return bean;
        }
    }
    
    private static class TestBean2 {
    }
    
    // additional test cases
    @Test
    public void testReplaceSimpleElement() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<ns:test xmlns:ns=\"http://bar\"><ns:a>1</ns:a></ns:test>".getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = 
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, "UTF-8"), 
                                   null, Collections.singletonMap("{http://bar}a", "{http://bar}a=1 2 3"),
                                   null, null, false, null);
        StaxUtils.copy(new StreamSource(is), writer);
        writer.flush();

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));

        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ns:test xmlns:ns=\"http://bar\"><ns:a>1 2 3</ns:a></ns:test>", value);        
    }

    @Test
    public void testReadWithComplexRequestSameNamespace() throws Exception {
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/element}*");
        
        XMLStreamReader reader = 
            TransformTestUtils.createOutTransformedStreamReader("../resources/complexReqIn1.xml", 
                                                                inMap, null, null, null, false, null);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                OutTransformWriter.class.getResourceAsStream("../resources/complexReq1.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, true);
    }
    
    @Test
    public void testReadWithComplexRequestMultipleNamespace() throws Exception {
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        
        XMLStreamReader reader = 
            TransformTestUtils.createOutTransformedStreamReader("../resources/complexReqIn2.xml", 
                                                                inMap, null, null, null, false, null);
        
        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq2.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, true);
    }
    
    @Test
    public void testReadWithComplexTransformationNamespace() throws Exception {
        Map<String, String> inMap = new HashMap<String, String>();
        inMap.put("{http://cxf.apache.org/transform/header/element}*", 
                "{http://cxf.apache.org/transform/header/otherelement}*");
        inMap.put("{http://cxf.apache.org/transform/test}*", 
                "{http://cxf.apache.org/transform/othertest}*");
        inMap.put("{http://schemas.xmlsoap.org/soap/envelope/}Envelope", 
                "{http://schemas.xmlsoap.org/soap/envelope/}TheEnvelope");
        
        // set the block original reader flag to true
        XMLStreamReader reader = 
            TransformTestUtils.createOutTransformedStreamReader("../resources/complexReqIn3.xml", 
                                                                inMap, null, null, null, false, null);

        XMLStreamReader reader2 = 
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq3.xml"));        
        TransformTestUtils.verifyReaders(reader2, reader, true, false);
    }

    @Test
    public void testReadWithReplaceAppend() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("requestValue",
                              "{http://cxf.apache.org/hello_world_soap_http/types}requestType");
        
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("requestValue",
                           "{http://cxf.apache.org/hello_world_soap_http/types}greetMe");
        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn1.xml", 
                                                     "../resources/greetMeReq.xml",
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
        
        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn2.xml", 
                                                     "../resources/greetMeReq.xml",
                                  transformElements, appendElements, dropElements, 
                                  transformAttributes, null);
    }

    @Test
    public void testReadWithChangeNamespaces() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("*",
                              "{http://cxf.apache.org/hello_world_soap_http/types}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn3.xml", 
                                                     "../resources/greetMeReq.xml",
                                  transformElements, null, null, null, null);
    }

    @Test
    public void testReadWithDeleteAttributes() throws Exception {
        Map<String, String> transformAttributes = new HashMap<String, String>();
        transformAttributes.put("{http://www.w3.org/2001/XMLSchema-instance}type",
                                "");

        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn4.xml", 
                                                     "../resources/greetMeReq.xml",
                                  null, null, null, transformAttributes, null);
    }
    
    @Test
    public void testReadWithAppendPreInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType=ASIN");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn1.xml", 
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPreInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType=ASIN");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn1nospace.xml", 
                                                     "../resources/amazon.xml",
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
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn2.xml", 
                                                     "../resources/AddRequest2.xml",
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
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn2nospace.xml", 
                                                     "../resources/AddRequest2.xml",
                                  transformElements, appendElements, null, null, null);
    }

    @Test
    public void testReadWithAppendPostInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId=0486411214");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn2.xml", 
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId=0486411214");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn2nospace.xml", 
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostWrap1() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn1.xml", 
                                                     "../resources/AddRequest.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostWrap2() throws Exception {
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn1nospace.xml", 
                                                     "../resources/AddRequest.xml",
                                  null, appendElements, null, null, null);
        
    }

    @Test
    public void testReadWithAppendPostWrapReplaceDrop() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("payload",
                              "{http://www.w3.org/2003/05/soap-envelope}Envelope");
        transformElements.put("params",
                              "{http://apache.org/cxf/calculator/types}add");
        transformElements.put("i1",
                              "{http://apache.org/cxf/calculator/types}arg0");
        transformElements.put("i2",
                              "{http://apache.org/cxf/calculator/types}arg1");
        transformElements.put("i3",
                              "");
        Map<String, String> appendElements = new HashMap<String, String>();
        appendElements.put("payload/",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        List<String> dropElements = new ArrayList<String>();
        dropElements.add("param");

        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn3.xml", 
                                                     "../resources/AddRequest3.xml",
                                  transformElements, appendElements, dropElements, null, null);
        
    }
    
    @Test
    public void testOldSTSTransform() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("{http://docs.oasis-open.org/ws-sx/ws-trust/200512}*",
                              "{http://schemas.xmlsoap.org/ws/2005/02/trust}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/wstrustReqSTRCIn1.xml", 
                                                     "../resources/wstrustReqSTRC.xml",
                                  transformElements, null, null, null, null);
    }

    @Test
    public void testPreservePrefixBindings() throws Exception {
        Map<String, String> transformElements = new HashMap<String, String>();
        transformElements.put("{urn:abc}*",
                              "{urn:a}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/multiNSIn1.xml", 
                                                     "../resources/multiNS.xml",
                                  transformElements, null, null, null, null);
    }

    @Test
    public void testReplaceDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<test xmlns=\"http://bar\"><a>1</a></test>".getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = 
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, "UTF-8"), 
                                   null, null, null, null, false, "");
        StaxUtils.copy(new StreamSource(is), writer);
        writer.flush();

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));

        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ps1:test xmlns:ps1=\"http://bar\"><ps1:a>1</ps1:a></ps1:test>", value);        
    }

}
