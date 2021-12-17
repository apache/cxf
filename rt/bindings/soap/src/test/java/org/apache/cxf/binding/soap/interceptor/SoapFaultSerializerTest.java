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

package org.apache.cxf.binding.soap.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPPart;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultOutInterceptor.Soap11FaultOutInterceptorInternal;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor.Soap12FaultOutInterceptorInternal;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor.SAAJPreInInterceptor;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SoapFaultSerializerTest {
    private void assertValid(String xpathExpression, Document doc) {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        namespaces.put("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap/");
        namespaces.put("soap", "http://schemas.xmlsoap.org/soap/");
        namespaces.put("soap12env", "http://www.w3.org/2003/05/soap-envelope");
        namespaces.put("xml", "http://www.w3.org/XML/1998/namespace");
        XPathUtils xpu = new XPathUtils(namespaces);
        if (!xpu.isExist(xpathExpression, doc, XPathConstants.NODE)) {
            fail("Failed to select any nodes for expression:\n" + xpathExpression
                 + " from document:\n" + StaxUtils.toString(doc));
        }
    }

    @Test
    public void testSoap11Out() throws Exception {
        String faultString = "Hadrian caused this Fault!";
        SoapFault fault = new SoapFault(faultString, Soap11.getInstance().getSender());

        SoapMessage m = new SoapMessage(new MessageImpl());
        m.setExchange(new ExchangeImpl());
        m.setContent(Exception.class, fault);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        writer.writeStartDocument();
        writer.writeStartElement("Body");

        m.setContent(XMLStreamWriter.class, writer);

        Soap11FaultOutInterceptorInternal.INSTANCE.handleMessage(m);

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();

        Document faultDoc = StaxUtils.read(new ByteArrayInputStream(out.toByteArray()));
        assertValid("//s:Fault/faultcode[text()='ns1:Client']", faultDoc);
        assertValid("//s:Fault/faultstring[text()='" + faultString + "']", faultDoc);

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(out.toByteArray()));
        m.setContent(XMLStreamReader.class, reader);

        reader.nextTag();

        Soap11FaultInInterceptor inInterceptor = new Soap11FaultInInterceptor();
        inInterceptor.handleMessage(m);

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(fault.getMessage(), fault2.getMessage());
        assertEquals(Soap11.getInstance().getSender(), fault2.getFaultCode());
    }

    @Test
    public void testSoap12Out() throws Exception {
        String faultString = "Hadrian caused this Fault!";
        SoapFault fault = new SoapFault(faultString, Soap12.getInstance().getSender());

        QName qname = new QName("http://cxf.apache.org/soap/fault", "invalidsoap", "cxffaultcode");
        fault.setSubCode(qname);

        SoapMessage m = new SoapMessage(new MessageImpl());
        m.setVersion(Soap12.getInstance());

        m.setContent(Exception.class, fault);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        writer.writeStartDocument();
        writer.writeStartElement("Body");

        m.setContent(XMLStreamWriter.class, writer);

        Soap12FaultOutInterceptorInternal.INSTANCE.handleMessage(m);

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();

        Document faultDoc = StaxUtils.read(new ByteArrayInputStream(out.toByteArray()));

        assertValid("//soap12env:Fault/soap12env:Code/soap12env:Value[text()='ns1:Sender']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Code/soap12env:Subcode/"
                    + "soap12env:Value[text()='cxffaultcode:invalidsoap']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Reason/soap12env:Text[@xml:lang='en']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Reason/soap12env:Text[text()='" + faultString + "']",
                    faultDoc);

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(out.toByteArray()));
        m.setContent(XMLStreamReader.class, reader);

        reader.nextTag();

        Soap12FaultInInterceptor inInterceptor = new Soap12FaultInInterceptor();
        inInterceptor.handleMessage(m);

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(Soap12.getInstance().getSender(), fault2.getFaultCode());
        assertEquals(fault.getMessage(), fault2.getMessage());
        assertEquals(fault.getSubCode(), fault2.getSubCode());
    }

    @Test
    public void testSoap12WithMultipleSubCodesOut() throws Exception {
        String faultString = "Hadrian caused this Fault!";
        SoapFault fault = new SoapFault(faultString, Soap12.getInstance().getSender());

        fault.addSubCode(new QName("http://cxf.apache.org/soap/fault", "invalidsoap"));
        fault.addSubCode(new QName("http://cxf.apache.org/soap/fault2", "invalidsoap2", "cxffaultcode2"));

        SoapMessage m = new SoapMessage(new MessageImpl());
        m.setVersion(Soap12.getInstance());

        m.setContent(Exception.class, fault);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        writer.writeStartDocument();
        writer.writeStartElement("Body");

        m.setContent(XMLStreamWriter.class, writer);

        Soap12FaultOutInterceptorInternal.INSTANCE.handleMessage(m);

        writer.writeEndElement();
        writer.writeEndDocument();
        writer.close();

        Document faultDoc = StaxUtils.read(new ByteArrayInputStream(out.toByteArray()));

        assertValid("//soap12env:Fault/soap12env:Code/soap12env:Value[text()='ns1:Sender']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Code/soap12env:Subcode/"
                    + "soap12env:Value[text()='ns2:invalidsoap']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Code/soap12env:Subcode/soap12env:Subcode/"
                    + "soap12env:Value[text()='cxffaultcode2:invalidsoap2']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Reason/soap12env:Text[@xml:lang='en']",
                    faultDoc);
        assertValid("//soap12env:Fault/soap12env:Reason/soap12env:Text[text()='" + faultString + "']",
                    faultDoc);

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(out.toByteArray()));
        m.setContent(XMLStreamReader.class, reader);

        reader.nextTag();

        Soap12FaultInInterceptor inInterceptor = new Soap12FaultInInterceptor();
        inInterceptor.handleMessage(m);

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(Soap12.getInstance().getSender(), fault2.getFaultCode());
        assertEquals(fault.getMessage(), fault2.getMessage());
        assertEquals(fault.getSubCodes(), fault2.getSubCodes());
    }
    @Test
    public void testFaultToSoapFault() throws Exception {
        Exception ex = new Exception();
        Fault fault = new Fault(ex, Fault.FAULT_CODE_CLIENT);
        verifyFaultToSoapFault(fault, null, true, Soap11.getInstance());
        verifyFaultToSoapFault(fault, null, true, Soap12.getInstance());

        fault = new Fault(ex, Fault.FAULT_CODE_SERVER);
        verifyFaultToSoapFault(fault, null, false, Soap11.getInstance());
        verifyFaultToSoapFault(fault, null, false, Soap12.getInstance());

        fault.setMessage("fault-one");
        verifyFaultToSoapFault(fault, "fault-one", false, Soap11.getInstance());

        ex = new Exception("fault-two");
        fault = new Fault(ex, Fault.FAULT_CODE_CLIENT);
        verifyFaultToSoapFault(fault, "fault-two", true, Soap11.getInstance());

        fault = new Fault(ex, new QName("http://cxf.apache.org", "myFaultCode"));
        SoapFault f = verifyFaultToSoapFault(fault, "fault-two", false, Soap12.getInstance());
        assertEquals("myFaultCode", f.getSubCodes().get(0).getLocalPart());

    }

    private SoapFault verifyFaultToSoapFault(Fault fault, String msg, boolean sender, SoapVersion v) {
        SoapFault sf = SoapFault.createFault(fault, v);
        assertEquals(sender ? v.getSender() : v.getReceiver(), sf.getFaultCode());
        assertEquals(msg, sf.getMessage());
        return sf;
    }

    @Test
    public void testCXF1864() throws Exception {

        SoapMessage m = new SoapMessage(new MessageImpl());
        m.setVersion(Soap12.getInstance());


        XMLStreamReader reader = StaxUtils.createXMLStreamReader(this.getClass()
                                                                 .getResourceAsStream("cxf1864.xml"));
        m.setContent(XMLStreamReader.class, reader);

        reader.nextTag();

        Soap12FaultInInterceptor inInterceptor = new Soap12FaultInInterceptor();
        inInterceptor.handleMessage(m);

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(Soap12.getInstance().getReceiver(), fault2.getFaultCode());
    }

    @Test
    public void testCXF4181() throws Exception {
        //Try WITH SAAJ
        SoapMessage m = new SoapMessage(new MessageImpl());
        m.put(Message.HTTP_REQUEST_METHOD, "POST");
        m.setVersion(Soap12.getInstance());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(this.getClass()
                                                                 .getResourceAsStream("cxf4181.xml"));

        m.setContent(XMLStreamReader.class, reader);

        new SAAJPreInInterceptor().handleMessage(m);
        new ReadHeadersInterceptor(null).handleMessage(m);
        new StartBodyInterceptor().handleMessage(m);
        new SAAJInInterceptor().handleMessage(m);
        new Soap12FaultInInterceptor().handleMessage(m);

        Node nd = m.getContent(Node.class);

        SOAPPart part = (SOAPPart)nd;
        assertEquals("S", part.getEnvelope().getPrefix());
        assertEquals("S2", part.getEnvelope().getHeader().getPrefix());
        assertEquals("S3", part.getEnvelope().getBody().getPrefix());
        SOAPFault fault = part.getEnvelope().getBody().getFault();
        assertEquals("S", fault.getPrefix());

        assertEquals("Authentication Failure", fault.getFaultString());

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);

        assertEquals(Soap12.getInstance().getSender(), fault2.getFaultCode());
        assertEquals(new QName("http://schemas.xmlsoap.org/ws/2005/02/trust", "FailedAuthentication"),
                     fault2.getSubCode());

        Element el = part.getEnvelope().getBody();
        nd = el.getFirstChild();
        int count = 0;
        while (nd != null) {
            if (nd instanceof Element) {
                count++;
            }
            nd = nd.getNextSibling();
        }
        assertEquals(1, count);


        //Try WITHOUT SAAJ
        m = new SoapMessage(new MessageImpl());
        m.setVersion(Soap12.getInstance());
        reader = StaxUtils.createXMLStreamReader(this.getClass()
                                                 .getResourceAsStream("cxf4181.xml"));

        m.setContent(XMLStreamReader.class, reader);
        m.put(Message.HTTP_REQUEST_METHOD, "POST");

        new ReadHeadersInterceptor(null).handleMessage(m);
        new StartBodyInterceptor().handleMessage(m);
        new Soap12FaultInInterceptor().handleMessage(m);

        //nd = m.getContent(Node.class);

        fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);

        assertEquals(Soap12.getInstance().getSender(), fault2.getFaultCode());
        assertEquals(new QName("http://schemas.xmlsoap.org/ws/2005/02/trust", "FailedAuthentication"),
             fault2.getSubCode());
    }


    @Test
    public void testCXF5493() throws Exception {

        SoapMessage m = new SoapMessage(new MessageImpl());
        m.setVersion(Soap11.getInstance());


        XMLStreamReader reader = StaxUtils.createXMLStreamReader(this.getClass()
                                                                 .getResourceAsStream("cxf5493.xml"));
        m.setContent(XMLStreamReader.class, reader);
        reader.nextTag(); //env
        reader.nextTag(); //body
        reader.nextTag(); //fault

        Soap11FaultInInterceptor inInterceptor = new Soap11FaultInInterceptor();
        inInterceptor.handleMessage(m);

        SoapFault fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(Soap11.getInstance().getReceiver(), fault2.getFaultCode());
        assertEquals("some text containing a xml tag <xml-tag>", fault2.getMessage());



        m = new SoapMessage(new MessageImpl());
        m.put(Message.HTTP_REQUEST_METHOD, "POST");
        m.setVersion(Soap11.getInstance());
        reader = StaxUtils.createXMLStreamReader(this.getClass().getResourceAsStream("cxf5493.xml"));

        m.setContent(XMLStreamReader.class, reader);

        new SAAJPreInInterceptor().handleMessage(m);
        new ReadHeadersInterceptor(null).handleMessage(m);
        new StartBodyInterceptor().handleMessage(m);
        new SAAJInInterceptor().handleMessage(m);
        new Soap11FaultInInterceptor().handleMessage(m);

        fault2 = (SoapFault)m.getContent(Exception.class);
        assertNotNull(fault2);
        assertEquals(Soap11.getInstance().getReceiver(), fault2.getFaultCode());
        assertEquals("some text containing a xml tag <xml-tag>", fault2.getMessage());
    }
}