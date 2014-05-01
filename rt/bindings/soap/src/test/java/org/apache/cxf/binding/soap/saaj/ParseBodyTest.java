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

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.staxutils.StaxUtils;
import org.junit.Assert;
import org.junit.Test;

public class ParseBodyTest extends Assert {
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

        xmlReader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(data.getBytes("utf-8")));
        
        //reader should be on the start element for the 
        assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.next());
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
}
