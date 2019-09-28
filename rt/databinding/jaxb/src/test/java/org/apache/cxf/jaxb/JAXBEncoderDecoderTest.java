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
import java.util.List;
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
import javax.xml.ws.ResponseWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb_form.ObjectWithQualifiedElementElement;
import org.apache.cxf.jaxb_misc.Base64WithDefaultValueType;
import org.apache.cxf.jaxb_misc.ObjectFactory;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.StringStruct;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JAXBEncoderDecoderTest
 */
public class JAXBEncoderDecoderTest {
    public static final QName  SOAP_ENV =
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
    public static final QName  SOAP_BODY =
            new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

    RequestWrapper wrapperAnnotation;
    JAXBContext context;
    Schema schema;
    Map<String, String> mapField;
    String[] arrayField;
    List<String> listField;

    @Before
    public void setUp() throws Exception {
        context = JAXBContext.newInstance(new Class[] {
            GreetMe.class,
            GreetMeResponse.class,
            StringStruct.class,
            ObjectWithQualifiedElementElement.class
        });
        Method method = getMethod("greetMe");
        wrapperAnnotation = method.getAnnotation(RequestWrapper.class);

        InputStream is = getClass().getResourceAsStream("resources/StringStruct.xsd");
        StreamSource schemaSource = new StreamSource(is);
        assertNotNull(schemaSource);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = factory.newSchema(schemaSource);
        assertNotNull(schema);
    }
    private Method getMethod(String methodName) {
        Method[] declMethods = this.getClass().getDeclaredMethods();
        for (Method method : declMethods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    @RequestWrapper(localName = "greetMe",
        targetNamespace = "http://apache.org/hello_world_soap_http/types",
        className = "org.apache.hello_world_soap_http.types.GreetMe")
    @ResponseWrapper(localName = "greetMeResponse",
        targetNamespace = "http://apache.org/hello_world_soap_http/types",
        className = "org.apache.hello_world_soap_http.types.GreetMeResponse")
    public java.lang.String greetMe(
        java.lang.String requestType
    ) {
        return "Hello " + requestType;
    }


    private Type getFieldType(String name) throws Exception {
        return this.getClass()
            .getDeclaredField(name)
            .getGenericType();
    }

    @Test
    public void testCXF3611() throws Exception {
        Map<String, String> foo = new HashMap<>();

        assertTrue(JAXBSchemaInitializer.isArray(getFieldType("arrayField")));
        assertTrue(JAXBSchemaInitializer.isArray(getFieldType("listField")));

        assertFalse(JAXBSchemaInitializer.isArray(foo.getClass()));
        assertFalse(JAXBSchemaInitializer.isArray(getFieldType("mapField")));
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
        assertTrue(xmlResult.contains(":string2>cord</ns"));
    }

    @Test
    public void testCustomNamespaces() throws Exception {
        Map<String, String> mapper = new HashMap<>();
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
        JAXBUtils.setNamespaceMapper(mapper, m);
        JAXBEncoderDecoder.marshall(m, testObject, part, writer);
        writer.flush();
        writer.close();
        String xmlResult = stringWriter.toString();
        // the following is a bit of a crock, but, to tell the truth, this test case most exists
        // so that it could be examined inside the debugger to see how JAXB works.
        assertTrue(xmlResult.contains("Gallia:string2"));
    }

    @Test
    public void testMarshallIntoStaxStreamWriter() throws Exception {
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
        FixNamespacesXMLStreamWriter writer = new FixNamespacesXMLStreamWriter(opFactory.createXMLStreamWriter(baos));

        assertNull(writer.getMarshaller());

        Marshaller m = context.createMarshaller();
        JAXBEncoderDecoder.marshall(m, obj, part, writer);
        assertEquals(m, writer.getMarshaller());
        writer.flush();
        writer.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLInputFactory ipFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = ipFactory.createXMLEventReader(bais);

        Unmarshaller um = context.createUnmarshaller();
        Object val = um.unmarshal(reader, GreetMe.class);
        assertTrue(val instanceof JAXBElement);
        val = ((JAXBElement<?>)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(),
                     ((GreetMe)val).getRequestType());
    }

    @Test
    public void testMarshallIntoStaxEventWriter() throws Exception {
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
        FixNamespacesXMLEventWriter writer = new FixNamespacesXMLEventWriter(opFactory.createXMLEventWriter(baos));
        assertNull(writer.getMarshaller());

        //STARTDOCUMENT/ENDDOCUMENT is not required
        //writer.add(eFactory.createStartDocument("utf-8", "1.0"));
        Marshaller m = context.createMarshaller();
        JAXBEncoderDecoder.marshall(m, obj, part, writer);
        assertEquals(m, writer.getMarshaller());
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
        val = ((JAXBElement<?>)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(),
                     ((GreetMe)val).getRequestType());
    }

    @Test
    public void testUnmarshallFromStaxStreamReader() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(is);

        QName[] tags = {SOAP_ENV, SOAP_BODY};
        StaxStreamFilter filter = new StaxStreamFilter(tags);
        FixNamespacesXMLStreamReader filteredReader = new FixNamespacesXMLStreamReader(
                factory.createFilteredReader(reader, filter));

        assertNull(filteredReader.getUnmarshaller());

        //Remove START_DOCUMENT & START_ELEMENT pertaining to Envelope and Body Tags.

        part.setTypeClass(GreetMe.class);
        Unmarshaller um = context.createUnmarshaller();
        Object val = JAXBEncoderDecoder.unmarshall(um, filteredReader, part, true);
        assertEquals(um, filteredReader.getUnmarshaller());
        assertNotNull(val);
        assertTrue(val instanceof GreetMe);
        assertEquals("TestSOAPInputPMessage",
                     ((GreetMe)val).getRequestType());

        is.close();
    }

    @Test
    public void testUnmarshallFromStaxEventReader() throws Exception {
        QName elName = new QName(wrapperAnnotation.targetNamespace(),
                                 wrapperAnnotation.localName());
        MessagePartInfo part = new MessagePartInfo(elName, null);

        InputStream is = getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");
        XMLInputFactory factory = XMLInputFactory.newInstance();
        FixNamespacesXMLEventReader reader = new FixNamespacesXMLEventReader(factory.createXMLEventReader(is));

        assertNull(reader.getUnmarshaller());

        part.setTypeClass(GreetMe.class);
        Unmarshaller um = context.createUnmarshaller();
        Object val = JAXBEncoderDecoder.unmarshall(um, reader, part, true);
        assertEquals(um, reader.getUnmarshaller());
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


        Document doc = DOMUtils.getEmptyDocument();
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

        doc = DOMUtils.getEmptyDocument();
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

        Document doc = DOMUtils.getEmptyDocument();
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
    public void testMarshallExceptionWithOrder() throws Exception {
        Document doc = DOMUtils.getEmptyDocument();
        Element elNode = doc.createElementNS("http://cxf.apache.org",  "ExceptionRoot");

        OrderException exception = new OrderException("Mymessage");
        exception.setAValue("avalue");
        exception.setDetail("detail");
        exception.setInfo1("info1");
        exception.setInfo2("info2");
        exception.setIntVal(10000);

        QName elName = new QName("http://cxf.apache.org", "OrderException");
        ServiceInfo serviceInfo = new ServiceInfo();
        InterfaceInfo interfaceInfo = new InterfaceInfo(serviceInfo, null);
        OperationInfo op = interfaceInfo.addOperation(new QName("http://cxf.apache.org", "operation"));
        MessageInfo message = new MessageInfo(op, null, null);
        MessagePartInfo part = new MessagePartInfo(elName, message);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(OrderException.class);

        //just need a simple generic context to handle the exceptions internal primitives
        JAXBContext exceptionContext = JAXBContext.newInstance(new Class[] {
            String.class,
        });
        JAXBEncoderDecoder.marshallException(exceptionContext.createMarshaller(), exception, part, elNode);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        StaxUtils.writeTo(elNode, bout);
        int a = bout.toString().lastIndexOf("aValue");
        int b = bout.toString().lastIndexOf("detail");
        int c = bout.toString().lastIndexOf("info1");
        int d = bout.toString().lastIndexOf("info2");
        int e = bout.toString().lastIndexOf("intVal");
        assertTrue(a < b);
        assertTrue(b < c);
        assertTrue(c < d);
        assertTrue(d < e);
        assertTrue(bout.toString().indexOf("transientValue") < 0);
        assertTrue(bout.toString(), bout.toString().indexOf("mappedField=\"MappedField\"") > 0);
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
        val = ((JAXBElement<?>)val).getValue();
        assertTrue(val instanceof GreetMe);
        assertEquals(obj.getRequestType(),
                     ((GreetMe)val).getRequestType());
    }

    @Test
    public void testGetClassFromType() throws Exception {
        Method testByte = getMethod("testByte");
        Type[] genericParameterTypes = testByte.getGenericParameterTypes();
        Class<?>[] paramTypes = testByte.getParameterTypes();

        int idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertEquals(cls, paramTypes[idx]);
            idx++;
        }

        Method testBase64Binary = getMethod("testBase64Binary");
        genericParameterTypes = testBase64Binary.getGenericParameterTypes();
        paramTypes = testBase64Binary.getParameterTypes();

        idx = 0;
        for (Type t : genericParameterTypes) {
            Class<?> cls = JAXBEncoderDecoder.getClassFromType(t);
            assertEquals(cls, paramTypes[idx]);
            idx++;
        }
    }

    @Test
    public void testDefaultValueConverter() throws Exception {
        Base64WithDefaultValueType testData = (new ObjectFactory()).createBase64WithDefaultValueType();
        byte[] checkValue = testData.getAttributeWithDefaultValue();
        assertNotNull(checkValue);
    }


    @RequestWrapper(localName = "testByte",
        targetNamespace = "http://apache.org/type_test/doc",
        className = "org.apache.type_test.doc.TestByte")
    @ResponseWrapper(localName = "testByteResponse",
        targetNamespace = "http://apache.org/type_test/doc",
        className = "org.apache.type_test.doc.TestByteResponse")
    public byte testByte(
        byte x,
        javax.xml.ws.Holder<java.lang.Byte> y,
        javax.xml.ws.Holder<java.lang.Byte> z) {
        return 24;
    }

    @RequestWrapper(localName = "testBase64Binary",
        targetNamespace = "http://apache.org/type_test/doc",
        className = "org.apache.type_test.doc.TestBase64Binary")
    @ResponseWrapper(localName = "testBase64BinaryResponse",
        targetNamespace = "http://apache.org/type_test/doc",
        className = "org.apache.type_test.doc.TestBase64BinaryResponse")
    public byte[] testBase64Binary(
        byte[] x,
        javax.xml.ws.Holder<byte[]> y,
        javax.xml.ws.Holder<byte[]> z) {
        return null;
    }


}
