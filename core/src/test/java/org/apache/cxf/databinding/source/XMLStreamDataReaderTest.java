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

package org.apache.cxf.databinding.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;

import com.ctc.wstx.msv.W3CSchemaFactory;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class XMLStreamDataReaderTest {
    private static final byte[] DUMMY_DATA = "<ns:dummy xmlns:ns='http://www.apache.org/cxf'/>".getBytes();
    private static final byte[] DUMMY_DATA_NULL_BYTE =
        "<ns:dummy xmlns:ns='http://www.apache.org/cxf&#x00;'/>".getBytes();

    @Test
    public void testCloseOriginalInputStream() throws Exception {
        XMLStreamDataReader reader = new XMLStreamDataReader();
        Message msg = new MessageImpl();

        TestInputStream in1 = new TestInputStream(DUMMY_DATA);

        msg.setContent(InputStream.class, in1);

        reader.setProperty(Message.class.getName(), msg);

        Object obj = reader.read(new QName("http://www.apache.org/cxf", "dummy"),
                                 StaxUtils.createXMLStreamReader(in1), XMLStreamReader.class);

        assertTrue(obj instanceof XMLStreamReader);

        assertFalse(in1.isClosed());
        ((XMLStreamReader)obj).close();

        assertTrue(in1.isClosed());
    }

    // Parse some XML with a null byte and check we don't return internal Woodstox package names in the error message
    @Test
    public void testParseNullByte() throws Exception {
        XMLStreamDataReader reader = new XMLStreamDataReader();
        Message msg = new MessageImpl();

        TestInputStream in1 = new TestInputStream(DUMMY_DATA_NULL_BYTE);

        msg.setContent(InputStream.class, in1);

        reader.setProperty(Message.class.getName(), msg);

        Object obj = reader.read(new QName("http://www.apache.org/cxf", "dummy"),
                StaxUtils.createXMLStreamReader(in1), XMLStreamReader.class);

        assertTrue(obj instanceof XMLStreamReader);

        try {
            reader.read((XMLStreamReader) obj);
        } catch (Fault f) {
            assertFalse(f.getMessage().contains("com.ctc.wstx"));
        }

        ((XMLStreamReader)obj).close();
    }

    @Test
    public void testValid() throws Exception {
        testValidate("resources/schema.xsd", "resources/test-valid.xml", false);
    }

    @Test
    public void testInValid() throws Exception {
        testValidate("resources/schema.xsd", "resources/test-invalid.xml", true);
    }


    private void testValidate(String schemaPath, String xmlPath, boolean exceptionExpected) throws Exception {

        //create schema
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        URL schemaURI = getClass().getResource(schemaPath);
        Document wsdl = documentBuilder.parse(schemaURI.openStream());
        String wsdlSystemId = schemaURI.toExternalForm();
        DOMSource source = new DOMSource(wsdl);
        source.setSystemId(wsdlSystemId);
        source.setSystemId(wsdlSystemId);

        XMLValidationSchema schemaw3c =
                W3CSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA).createSchema(schemaURI);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaURI);

        XMLStreamDataReader reader = new XMLStreamDataReader();
        reader.setSchema(schema);


        InputStream testIS = getClass().getResourceAsStream(xmlPath);
        Message msg = new MessageImpl();
        Exchange exchange = new ExchangeImpl();

        ServiceInfo serviceInfo =  new ServiceInfo();

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.get(XMLValidationSchema.class.getName())).thenReturn(schemaw3c);

        Service svc = new ServiceImpl(serviceInfo);

        exchange.put(Service.class, svc);
        exchange.put(Endpoint.class, endpoint);

        msg.setExchange(exchange);
        msg.setContent(InputStream.class, testIS);
        reader.setProperty(Message.class.getName(), msg);

        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        XMLStreamReader2 xmlStreamReader = (XMLStreamReader2) xmlFactory.createXMLStreamReader(testIS, "utf-8");

        MessageInfo messageInfo = new MessageInfo(null,
                MessageInfo.Type.INPUT,
                new QName("http://www.test.org/services",
                        "NullTestOperationRequest"));
        MessagePartInfo messagePartInfo  = new MessagePartInfo(new QName(
                "http://www.test.org/services", "NullTestOperationRequest"), messageInfo);
        messagePartInfo.setElement(true);
        boolean exceptionCaught = false;
        try {
            reader.read(messagePartInfo, xmlStreamReader);
        } catch (Fault fault) {
            exceptionCaught = true;
        }  catch (Exception exc) {
            fail(exc.getMessage());
        }
        assertEquals(exceptionExpected, exceptionCaught);
    }

    private static class TestInputStream extends ByteArrayInputStream {
        private boolean closed;

        TestInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
