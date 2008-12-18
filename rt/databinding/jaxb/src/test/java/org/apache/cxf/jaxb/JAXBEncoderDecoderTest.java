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

package org.apache.cxf.jaxb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.ws.RequestWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb_form.ObjectWithQualifiedElementElement;
import org.apache.cxf.jaxb_misc.Base64WithDefaultValueType;
import org.apache.cxf.jaxb_misc.ObjectFactory;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.StringStruct;
import org.apache.type_test.doc.TypeTestPortType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JAXBEncoderDecoderTest
 * @author apaibir
 */
public class JAXBEncoderDecoderTest extends Assert {
    public static final QName  SOAP_ENV = 
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
    public static final QName  SOAP_BODY = 
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

    RequestWrapper wrapperAnnotation;
    JAXBContext context;
    Schema schema;
    
    @Before
    public void setUp() throws Exception {
        
        context = JAXBContext.newInstance(new Class[] {
            GreetMe.class,
            GreetMeResponse.class,
            StringStruct.class,
            ObjectWithQualifiedElementElement.class
        });
        Method method = TestUtil.getMethod(Greeter.class, "greetMe");
        wrapperAnnotation = method.getAnnotation(RequestWrapper.class);
        
        InputStream is =  getClass().getResourceAsStream("resources/StringStruct.xsd");
        StreamSource schemaSource = new StreamSource(is);
        assertNotNull(schemaSource);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = factory.newSchema(schemaSource);
        assertNotNull(schema);
    }

    @Test
    public void testMarshallIntoDOM() throws Exception {
        String str = new String("Hello");
        QName inCorrectElName = new QName("http://test_jaxb_marshall", "requestType");
        MessagePartInfo part = new MessagePartInfo(inCorrectElName, null);
        part.setElement(true);
        part.setElementQName(inCorrectElName);
        
        Document doc = DOMUtils.createDocument();
        Element elNode = doc.createElementNS(inCorrectElName.getNamespaceURI(), 
                                             inCorrectElName.getLocalPart());
        assertNotNull(elNode);

        Node node;
        try {
            JAXBEncoderDecoder.marshall(context.createMarshaller(), null, part, elNode);
            fail("Should have thrown a Fault");
        } catch (Fault ex) {
            //expected - not a valid object
        }

        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        part.setElementQName(elName);
        JAXBEncoderDecoder.marshall(context.createMarshaller(), obj, part, elNode);
        node = elNode.getLastChild();
        //The XML Tree Looks like
        //<GreetMe><requestType>Hello</requestType></GreetMe>
        assertEquals(Node.ELEMENT_NODE, node.getNodeType());
        Node childNode = node.getFirstChild();
        assertEquals(Node.ELEMENT_NODE, childNode.getNodeType());
        childNode = childNode.getFirstChild();
        assertEquals(Node.TEXT_NODE, childNode.getNodeType());
        assertEquals(str, childNode.getNodeValue());

        // Now test schema validation during marshaling
        StringStruct stringStruct = new StringStruct();
        // Don't initialize one of the structure members.
        //stringStruct.setArg0("hello");
        stringStruct.setArg1("world");
        // Marshal without the schema should work.
        JAXBEncoderDecoder.marshall(context.createMarshaller(), stringStruct, part,  elNode);
        try {
            Marshaller m = context.createMarshaller();
            m.setSchema(schema);
            // Marshal with the schema should get an exception.
            JAXBEncoderDecoder.marshall(m, stringStruct, part,  elNode);
            fail("Marshal with schema should have thrown a Fault");
        } catch (Fault ex) {
            //expected - not a valid object
        }
    }
    
    @Test
    public void testMarshallWithFormQualifiedElement() throws Exception {
        ObjectWithQualifiedElementElement testObject = new ObjectWithQualifiedElementElement();
        testObject.setString1("twine");
        testObject.setString2("cord");
        
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(stringWriter);
        JAXBEncoderDecoder.marshall(context.createMarshaller(), testObject, part, writer);
        writer.flush();
        writer.close();
        String xmlResult = stringWriter.toString();
        // the following is a bit of a crock, but, to tell the truth, this test case most exists
        // so that it could be examined inside the debugger to see how JAXB works.
        assertTrue(xmlResult.contains("ns3:string2"));
    }
    
    @Test
    public void testCustomNamespaces() throws Exception {
        Map<String, String> mapper = new HashMap<String, String>();
        mapper.put("http://apache.org/hello_world_soap_http/types", "Omnia");
        mapper.put("http://cxf.apache.org/jaxb_form", "Gallia");
        ObjectWithQualifiedElementElement testObject = new ObjectWithQualifiedElementElement();
        testObject.setString1("twine");
        testObject.setString2("cord");
        
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        
        StringWriter stringWriter = new StringWriter();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(stringWriter);
        Marshaller m = context.createMarshaller();
        JAXBUtils.setNamespaceWrapper(mapper, m);
        JAXBEncoderDecoder.marshall(m, testObject, part, writer);
        writer.flush();
        writer.close();
        String xmlResult = stringWriter.toString();
        // the following is a bit of a crock, but, to tell the truth, this test case most exists
        // so that it could be examined inside the debugger to see how JAXB works.
        assertTrue(xmlResult.contains("Gallia:string2"));
    }
    
    @Test
    public void testMarshallIntoStax() throws Exception {
        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
                
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(baos);

        //STARTDOCUMENT/ENDDOCUMENT is not required
        //writer.add(eFactory.createStartDocument("utf-8", "1.0"));        
        JAXBEncoderDecoder.marshall(context.createMarshaller(), obj, part, writer);
        //writer.add(eFactory.createEndDocument());
        writer.flush();
        writer.close();
        
        //System.out.println(baos.toString());
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLInputFactory ipFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = ipFactory.createXMLEventReader(bais);
        
        Unmarshaller um = context.createUnmarshaller();        
        Object val = um.unmarshal(reader, GreetMe.class);
        assertTrue(val instanceof JAXBElement);
        val = ((JAXBElement)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(), 
                     ((GreetMe)val).getRequestType());
    }

    @Test
    public void testUnmarshallFromStax() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);

        InputStream is =  getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = 
            factory.createXMLStreamReader(is);

        QName[] tags = {SOAP_ENV, SOAP_BODY};
        StaxStreamFilter filter = new StaxStreamFilter(tags);
        reader = factory.createFilteredReader(reader, filter);

        //Remove START_DOCUMENT & START_ELEMENT pertaining to Envelope and Body Tags.

        part.setTypeClass(GreetMe.class);
        Object val = JAXBEncoderDecoder.unmarshall(context.createUnmarshaller(), reader, part, true);
        assertNotNull(val);
        assertTrue(val instanceof GreetMe);
        assertEquals("TestSOAPInputPMessage", 
                     ((GreetMe)val).getRequestType());

        is.close();
    }
    
    @Test
    public void testMarshalRPCLit() throws Exception {
        QName elName = new QName("http://test_jaxb_marshall", "in");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        

        Document doc = DOMUtils.createDocument();
        Element elNode = doc.createElementNS(elName.getNamespaceURI(), 
                                             elName.getLocalPart());
        JAXBEncoderDecoder.marshall(context.createMarshaller(), 
                                    new String("TestSOAPMessage"), part,  elNode);
        
        assertEquals("TestSOAPMessage", elNode.getFirstChild().getFirstChild().getNodeValue());
    }

    
    @Test
    public void testUnMarshall() throws Exception {
        //Hello World Wsdl generated namespace
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());

        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(Class.forName(wrapperAnnotation.className()));
        

        Document doc = DOMUtils.createDocument();
        Element elNode = doc.createElementNS(elName.getNamespaceURI(), 
                                             elName.getLocalPart());
        Element rtEl = doc.createElementNS(elName.getNamespaceURI(), "requestType");
        elNode.appendChild(rtEl);
        rtEl.appendChild(doc.createTextNode("Hello Test"));

        Object obj = JAXBEncoderDecoder.unmarshall(context.createUnmarshaller(),
                         elNode, part, true);
        assertNotNull(obj);

        //Add a Node and then test
        assertEquals(GreetMe.class,  obj.getClass());
        assertEquals("Hello Test", ((GreetMe)obj).getRequestType());
        
        part.setTypeClass(String.class);
        Node n = null;
        try {
            JAXBEncoderDecoder.unmarshall(context.createUnmarshaller(), n, part, true);
            fail("Should have received a Fault");
        } catch (Fault pe) {
            //Expected Exception
        } catch (Exception ex) {
            fail("Should have received a Fault, not: " + ex);
        }
        
        // Now test schema validation during unmarshaling
        elName = new QName(wrapperAnnotation.targetNamespace(),
                           "stringStruct");
        // Create an XML Tree of
        // <StringStruct><arg1>World</arg1></StringStruct>
//         elNode = soapElFactory.createElement(elName);
//         elNode.addNamespaceDeclaration("", elName.getNamespaceURI());

        part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(Class.forName("org.apache.hello_world_soap_http.types.StringStruct"));
        
        doc = DOMUtils.createDocument();
        elNode = doc.createElementNS(elName.getNamespaceURI(), 
                                             elName.getLocalPart());
        rtEl = doc.createElementNS(elName.getNamespaceURI(), "arg1");
        elNode.appendChild(rtEl);
        rtEl.appendChild(doc.createTextNode("World"));
        
        // Should unmarshal without problems when no schema used.
        obj = JAXBEncoderDecoder.unmarshall(context.createUnmarshaller(), elNode, part, true);
        assertNotNull(obj);
        assertEquals(StringStruct.class,  obj.getClass());
        assertEquals("World", ((StringStruct)obj).getArg1());
        
        try {
            // unmarshal with schema should raise exception.
            Unmarshaller m = context.createUnmarshaller();
            m.setSchema(schema);
            obj = JAXBEncoderDecoder.unmarshall(m, elNode, part, true);
            fail("Should have thrown a Fault");
        } catch (Fault ex) {
            // expected - schema validation should fail.
        }
    }

    @Test
    public void testUnmarshallWithoutClzInfo() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());

        Document doc = DOMUtils.createDocument();
        Element elNode = doc.createElementNS(elName.getNamespaceURI(), 
                                             elName.getLocalPart());
        Element rtEl = doc.createElementNS(elName.getNamespaceURI(), "requestType");
        elNode.appendChild(rtEl);
        rtEl.appendChild(doc.createTextNode("Hello Test"));
       
        Object obj = JAXBEncoderDecoder.unmarshall(context.createUnmarshaller(), 
                                                   elNode,
                                                   null,
                                                   true);
        assertNotNull(obj);
        assertEquals(GreetMe.class,  obj.getClass());
        assertEquals("Hello Test", ((GreetMe)obj).getRequestType());
    }

    @Test
    public void testMarshallWithoutQNameInfo() throws Exception {
        GreetMe obj = new GreetMe();
        obj.setRequestType("Hello");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputFactory opFactory = XMLOutputFactory.newInstance();
        opFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        XMLEventWriter writer = opFactory.createXMLEventWriter(baos);

        //STARTDOCUMENT/ENDDOCUMENT is not required
        //writer.add(eFactory.createStartDocument("utf-8", "1.0"));        
        JAXBEncoderDecoder.marshall(context.createMarshaller(), obj, null, writer);
        //writer.add(eFactory.createEndDocument());
        writer.flush();
        writer.close();
        
        //System.out.println(baos.toString());
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLInputFactory ipFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = ipFactory.createXMLEventReader(bais);
        
        Unmarshaller um = context.createUnmarshaller();        
        Object val = um.unmarshal(reader, GreetMe.class);
        assertTrue(val instanceof JAXBElement);
        val = ((JAXBElement)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(), 
                     ((GreetMe)val).getRequestType());
    }
    
    @Test
    public void testGetClassFromType() throws Exception {
        Method testByte = TestUtil.getMethod(TypeTestPortType.class, "testByte");
        Type[] genericParameterTypes = testByte.getGenericParameterTypes();
        Class<?>[] paramTypes = testByte.getParameterTypes();
 
        int idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertTrue(cls.equals(paramTypes[idx]));
            idx++;
        }
        
        Method testBase64Binary = TestUtil.getMethod(TypeTestPortType.class, "testBase64Binary");
        genericParameterTypes = testBase64Binary.getGenericParameterTypes();
        paramTypes = testBase64Binary.getParameterTypes();
 
        idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertTrue(cls.equals(paramTypes[idx]));
            idx++;
        }
    }
 
    @Test
    public void testDefaultValueConverter() throws Exception {
        Base64WithDefaultValueType testData = (new ObjectFactory()).createBase64WithDefaultValueType();
        byte[] checkValue = testData.getAttributeWithDefaultValue();
        assertNotNull(checkValue);
    }
}

