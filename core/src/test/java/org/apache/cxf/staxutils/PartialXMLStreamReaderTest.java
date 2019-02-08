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

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PartialXMLStreamReaderTest {

    @Test
    public void testReader() throws Exception {
        String test =
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
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
                + "</soap:Body></soap:Envelope>";

        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(new StringReader(test));
        QName bodyTag = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        PartialXMLStreamReader filteredReader = new PartialXMLStreamReader(reader, bodyTag);

        Document doc = StaxUtils.read(filteredReader);

        assertNotNull(doc);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(bos);
        StaxUtils.copy(doc, writer);
        writer.flush();
        String value = bos.toString();

        assertTrue(("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Header>"
                + "<ns2:SoapHeaderIn xmlns=\"http://cxf.apache.org/transform/test\" "
                + "xmlns:ns2=\"http://cxf.apache.org/transform/header\" "
                + "xmlns:ns3=\"http://cxf.apache.org/transform/header/element\" "
                + "xmlns:ns4=\"http://cxf.apache.org/transform/fault\">"
                + "<ns2:OperationalMode>SIMULATION1</ns2:OperationalMode>"
                + "<ns2:SomeComplexHeaderType>"
                + "<ns3:CallerCorrelationId>SomeComplexValue</ns3:CallerCorrelationId>"
                + "</ns2:SomeComplexHeaderType></ns2:SoapHeaderIn></soap:Header>"
                + "<soap:Body/>"
                + "</soap:Envelope>").
                equals(value.trim()));

    }
}
