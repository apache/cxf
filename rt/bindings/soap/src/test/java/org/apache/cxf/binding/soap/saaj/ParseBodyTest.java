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
package org.apache.cxf.binding.soap.saaj;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParseBodyTest {
    static final String[] DATA = {
        "<SOAP-ENV:Body xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "    <foo>\n        <bar/>\n    </foo>\n</SOAP-ENV:Body>",
        "<SOAP-ENV:Body xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<foo>\n        <bar/>\n    </foo>\n</SOAP-ENV:Body>",
        "<SOAP-ENV:Body xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<foo>\n        <bar/>\n    </foo></SOAP-ENV:Body>"};

    private XMLStreamReader xmlReader;
    private MessageFactory factory;
    private SOAPMessage soapMessage;

    private void prepare(int n) throws Exception {
        String data = DATA[n];
        //System.out.println("Original[" + n + "]: " + data);

        xmlReader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(data.getBytes()));

        //reader should be on the start element for the
        assertEquals(XMLStreamConstants.START_ELEMENT, xmlReader.next());
        assertEquals("Body", xmlReader.getLocalName());

        factory = MessageFactory.newInstance();
        soapMessage = factory.createMessage();
    }

    @Test
    public void testUsingStaxUtilsCopyWithSAAJWriterData0() throws Exception {
        testUsingStaxUtilsCopyWithSAAJWriter(0);
    }

    @Test
    public void testUsingStaxUtilsCopyWithSAAJWriterData1() throws Exception {
        testUsingStaxUtilsCopyWithSAAJWriter(1);
    }

    @Test
    public void testUsingStaxUtilsCopyWithSAAJWriterData2() throws Exception {
        testUsingStaxUtilsCopyWithSAAJWriter(2);
    }

    @Test
    public void testReadSOAPFault() throws Exception {
        InputStream inStream = getClass().getResourceAsStream("soap12-fault.xml");
        Document doc = StaxUtils.read(inStream);

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        SOAPMessage saajMsg = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        SAAJStreamWriter writer = new SAAJStreamWriter(part);
        StaxUtils.copy(doc, writer);
        //Source s = new StaxSource(StaxUtils.createXMLStreamReader(doc));
        //part.setContent(s);
        saajMsg.saveChanges();

        msg.setContent(SOAPMessage.class, saajMsg);
        doc = part;

        // System.out.println("OUTPUT: " + StaxUtils.toString(doc));

        byte[] docbytes = getMessageBytes(doc);

        // System.out.println("OUTPUT: " + new String(docbytes));
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(docbytes));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());
        StaxUtils.read(db, reader, false);

    }

    private void testUsingStaxUtilsCopyWithSAAJWriter(int n) throws Exception {
        prepare(n);
        StaxUtils.copy(xmlReader, new SAAJStreamWriter(soapMessage.getSOAPPart(), soapMessage.getSOAPPart()
            .getEnvelope().getBody()), true, true);

        DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
        xmlReader = StaxUtils.createXMLStreamReader(bodySource);

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        StaxUtils.copy(xmlReader, buf);
        String result = buf.toString();
        //System.out.println("UsingStaxUtilsCopyWithSAAJWriter: " + result);
        assertEquals(DATA[n], result);
    }

    private byte[] getMessageBytes(Document doc) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XMLStreamWriter byteArrayWriter = StaxUtils.createXMLStreamWriter(outputStream);

        StaxUtils.writeDocument(doc, byteArrayWriter, false);

        byteArrayWriter.flush();
        return outputStream.toByteArray();
    }
}