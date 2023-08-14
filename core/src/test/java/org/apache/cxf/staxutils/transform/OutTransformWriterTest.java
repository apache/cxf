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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class OutTransformWriterTest {

    @Test
    public void testDefaultNamespace() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());

        Map<String, String> outMap = new HashMap<>();
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
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();

        JAXBContext context = JAXBContext.newInstance(TestBean.class);
        Marshaller m = context.createMarshaller();
        Map<String, String> outMap = new HashMap<>();
        outMap.put("{http://testbeans.com}testBean", "{http://testbeans.com/v2}testBean");
        outMap.put("{http://testbeans.com}bean", "{http://testbeans.com/v3}bean");
        OutTransformWriter transformWriter = new OutTransformWriter(writer,
                                                                    outMap,
                                                                    Collections.<String, String>emptyMap(),
                                                                    Collections.<String>emptyList(),
                                                                    false,
                                                                    "");
        m.marshal(new TestBean(), transformWriter);

        Element el = writer.getDocument().getDocumentElement();
        assertEquals("http://testbeans.com/v2", el.getNamespaceURI());
        assertFalse(StringUtils.isEmpty(el.getPrefix()));

        Element el2 = DOMUtils.getFirstElement(el);
        assertEquals("http://testbeans.com/v3", el2.getNamespaceURI());
        assertFalse(StringUtils.isEmpty(el2.getPrefix()));

    }

    @Test
    public void testNamespaceConversionAndDefaultNS() throws Exception {
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();

        Map<String, String> outMap = new HashMap<>();
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

        Element el = writer.getDocument().getDocumentElement();
        assertEquals("http://testbeans.com/v2", el.getNamespaceURI());
        assertTrue(StringUtils.isEmpty(el.getPrefix()));

        el = DOMUtils.getFirstElement(el);
        assertEquals("http://testbeans.com/v3", el.getNamespaceURI());
        assertFalse(StringUtils.isEmpty(el.getPrefix()));
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

    private static final class TestBean2 {
    }

    // additional test cases
    @Test
    public void testReplaceSimpleElement() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<ns:test xmlns:ns=\"http://bar\"><ns:a>1</ns:a></ns:test>".getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer =
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, StandardCharsets.UTF_8.name()),
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
        Map<String, String> inMap = new HashMap<>();
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
        Map<String, String> inMap = new HashMap<>();
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
        Map<String, String> inMap = new HashMap<>();
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
    public void testReadWithComplexTransformationNamespace2() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("{http://testbeans.com/double}*",
            "{http://testbeans.com/double/v2}*");
        map.put("{http://testbeans.com}*",
            "{http://testbeans.com/v3}*");

        // the namespaces are prefixed in the input
        XMLStreamReader reader =
            TransformTestUtils.createOutTransformedStreamReader("../resources/doubleBeanIn1.xml",
                                                                map, null, null, null, false, null);
        XMLStreamReader reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/doubleBean.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, false);

        // the child elements with the default namespace that is declared in the elements
        reader =
            TransformTestUtils.createOutTransformedStreamReader("../resources/doubleBeanIn2.xml",
                                                                map, null, null, null, false, null);
        reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/doubleBean.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, false);

        // the child elements with the default namespace that is declared in their parent element
        reader =
            TransformTestUtils.createOutTransformedStreamReader("../resources/doubleBeanIn3.xml",
                                                                map, null, null, null, false, null);
        reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/doubleBean.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, false);

        // writing each child separately (as the soap header children are serialized)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer =
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, StandardCharsets.UTF_8.name()),
                                   map, null,
                                   null, null, false, null);
        boolean nsset = "ns3".equals(writer.getNamespaceContext().getPrefix("http://testbeans.com/double"));
        writer.writeStartElement("ns3", "testDoubleBean", "http://testbeans.com/double");
        if (!nsset) {
            writer.writeNamespace("ns3", "http://testbeans.com/double");
        }
        nsset = "".equals(writer.getNamespaceContext().getPrefix("http://testbeans.com"));
        writer.writeStartElement("", "bean", "http://testbeans.com");
        if (!nsset) {
            writer.writeNamespace("", "http://testbeans.com");
        }
        writer.writeEndElement();
        nsset = "".equals(writer.getNamespaceContext().getPrefix("http://testbeans.com"));
        writer.writeStartElement("", "beanNext", "http://testbeans.com");
        if (!nsset) {
            writer.writeNamespace("", "http://testbeans.com");
        }
        writer.writeEndElement();
        writer.writeEndElement();
        writer.flush();

        reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));
        reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/doubleBean.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, false);
    }

    @Test
    public void testRemoveOneNamespace() throws Exception {
        Map<String, String> inMap = new HashMap<>();
        inMap.put("{http://cxf.apache.org/vgop/serviceorder/v1_0}result", "result");
        XMLStreamReader reader =
            TransformTestUtils.createOutTransformedStreamReader("../resources/complexReqIn5.xml",
                                                                inMap, null, null, null, false, null);

        XMLStreamReader reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/complexReq5.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, true);
    }

    @Test
    public void testRemoveOneDefaultNamespace() throws Exception {
        Map<String, String> inMap = new HashMap<>();
        inMap.put("{http://cxf.apache.org/hello_world_soap_http/types2}requestType", "requestType");
        XMLStreamReader reader =
            TransformTestUtils.createOutTransformedStreamReader("../resources/greetMe2ReqIn1.xml",
                                                                inMap, null, null, null, false, null);

        XMLStreamReader reader2 =
            StaxUtils.createXMLStreamReader(
                InTransformReader.class.getResourceAsStream("../resources/greetMe2Req.xml"));
        TransformTestUtils.verifyReaders(reader2, reader, true, true);
    }

    @Test
    public void testReadWithReplaceAppend() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("requestValue",
                              "{http://cxf.apache.org/hello_world_soap_http/types}requestType");

        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("requestValue",
                           "{http://cxf.apache.org/hello_world_soap_http/types}greetMe");
        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn1.xml",
                                                     "../resources/greetMeReq.xml",
                                  transformElements, appendElements, null, null);
    }

    @Test
    public void testReadWithReplaceAppendDelete() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("requestValue",
                              "{http://cxf.apache.org/hello_world_soap_http/types}requestType");
        transformElements.put("{http://cxf.apache.org/hello_world_soap_http/types}requestDate",
                              "");

        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("requestValue",
                           "{http://cxf.apache.org/hello_world_soap_http/types}greetMe");

        List<String> dropElements = new ArrayList<>();
        dropElements.add("value");

        Map<String, String> transformAttributes = new HashMap<>();
        transformAttributes.put("num", "");
        transformAttributes.put("nombre", "{http://cxf.apache.org/hello_world_soap_http/types}name");

        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn2.xml",
                                                     "../resources/greetMeReq.xml",
                                  transformElements, appendElements, dropElements,
                                  transformAttributes);
    }

    @Test
    public void testReadWithChangeNamespaces() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("*",
                              "{http://cxf.apache.org/hello_world_soap_http/types}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn3.xml",
                                                     "../resources/greetMeReq.xml",
                                  transformElements, null, null, null);
    }

    @Test
    public void testReadWithDeleteAttributes() throws Exception {
        Map<String, String> transformAttributes = new HashMap<>();
        transformAttributes.put("{http://www.w3.org/2001/XMLSchema-instance}type",
                                "");

        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeReqIn4.xml",
                                                     "../resources/greetMeReq.xml",
                                  null, null, null, transformAttributes);
    }

    @Test
    public void testReadWithAppendPreInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType=ASIN");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn1.xml",
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPreInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}IdType=ASIN");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn1nospace.xml",
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPreWrap1() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("payload",
                              "{http://www.w3.org/2003/05/soap-envelope}Envelope");
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://apache.org/cxf/calculator/types}add",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn2.xml",
                                                     "../resources/AddRequest2.xml",
                                  transformElements, appendElements, null, null);
    }

    @Test
    public void testReadWithAppendPreWrap2() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("payload",
                              "{http://www.w3.org/2003/05/soap-envelope}Envelope");
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://apache.org/cxf/calculator/types}add",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn2nospace.xml",
                                                     "../resources/AddRequest2.xml",
                                  transformElements, appendElements, null, null);
    }

    @Test
    public void testReadWithAppendPostInclude1() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId=0486411214");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn2.xml",
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPostInclude2() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://xml.amazon.com/AWSECommerceService/2004-08-01}Request/",
                           "{http://xml.amazon.com/AWSECommerceService/2004-08-01}ItemId=0486411214");
        TransformTestUtils.transformOutStreamAndCompare("../resources/amazonIn2nospace.xml",
                                                     "../resources/amazon.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPostWrap1() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn1.xml",
                                                     "../resources/AddRequest.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPostWrap2() throws Exception {
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("{http://www.w3.org/2003/05/soap-envelope}Body/",
                           "{http://apache.org/cxf/calculator/types}add");
        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn1nospace.xml",
                                                     "../resources/AddRequest.xml",
                                  null, appendElements, null, null);

    }

    @Test
    public void testReadWithAppendPostWrapReplaceDrop() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
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
        Map<String, String> appendElements = new HashMap<>();
        appendElements.put("payload/",
                           "{http://www.w3.org/2003/05/soap-envelope}Body");
        List<String> dropElements = new ArrayList<>();
        dropElements.add("param");

        TransformTestUtils.transformOutStreamAndCompare("../resources/AddRequestIn3.xml",
                                                     "../resources/AddRequest3.xml",
                                  transformElements, appendElements, dropElements, null);

    }

    @Test
    public void testOldSTSTransform() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("{http://docs.oasis-open.org/ws-sx/ws-trust/200512}*",
                              "{http://schemas.xmlsoap.org/ws/2005/02/trust}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/wstrustReqSTRCIn1.xml",
                                                     "../resources/wstrustReqSTRC.xml",
                                  transformElements, null, null, null);
    }

    @Test
    public void testPreservePrefixBindings() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("{urn:abc}*",
                              "{urn:a}*");

        TransformTestUtils.transformOutStreamAndCompare("../resources/multiNSIn1.xml",
                                                     "../resources/multiNS.xml",
                                  transformElements, null, null, null);
    }

    @Test
    public void testReplaceDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
                "<test xmlns=\"http://bar\"><a>1</a></test>".getBytes());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer =
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, StandardCharsets.UTF_8.name()),
                                   null, null, null, null, false, "");
        StaxUtils.copy(new StreamSource(is), writer);
        writer.flush();

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ps1:test xmlns:ps1=\"http://bar\"><ps1:a>1</ps1:a></ps1:test>", value);
    }

    @Test
    public void testNamespacedAttributeDropElement() throws Exception {
        Map<String, String> transformElements = new HashMap<>();
        transformElements.put("{http://www.w3.org/2005/08/addressing}ReplyTo", "");
        TransformTestUtils.transformOutStreamAndCompare("../resources/greetMeWSAReqIn.xml",
                                                        "../resources/greetMeWSAReq.xml",
                                  transformElements, null, null, null);
    }

}