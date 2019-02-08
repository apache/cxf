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

package org.apache.cxf.feature.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/* Provides XSLT transformation of incoming message.
 * Interceptor breaks streaming (can be fixed in further versions when XSLT engine supports XML stream)
 */
public class XSLTInterceptorsTest {

    private static final String TRANSFORMATION_XSL = "transformation.xsl";
    private static final String MESSAGE_FILE = "message.xml";

    private InputStream messageIS;
    private Message message;
    private XSLTInInterceptor inInterceptor;
    private XSLTOutInterceptor outInterceptor;

    @Before
    public void setUp() throws TransformerConfigurationException {
        messageIS = ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE, this.getClass());
        if (messageIS == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE);
        }
        message = new MessageImpl();
        inInterceptor = new XSLTInInterceptor(TRANSFORMATION_XSL);
        outInterceptor = new XSLTOutInterceptor(TRANSFORMATION_XSL);
    }

    @Test
    public void inStreamTest() throws Exception {
        message.setContent(InputStream.class, messageIS);
        inInterceptor.handleMessage(message);
        InputStream transformedIS = message.getContent(InputStream.class);
        Document doc = StaxUtils.read(transformedIS);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void inXMLStreamTest() throws XMLStreamException {
        XMLStreamReader xReader = StaxUtils.createXMLStreamReader(messageIS);
        message.setContent(XMLStreamReader.class, xReader);
        inInterceptor.handleMessage(message);
        XMLStreamReader transformedXReader = message.getContent(XMLStreamReader.class);
        Document doc = StaxUtils.read(transformedXReader);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void inReaderTest() throws Exception {
        Reader reader = new InputStreamReader(messageIS);
        message.setContent(Reader.class, reader);
        inInterceptor.handleMessage(message);
        Reader transformedReader = message.getContent(Reader.class);
        Document doc = StaxUtils.read(transformedReader);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void outStreamTest() throws Exception {
        CachedOutputStream cos = new CachedOutputStream();
        cos.holdTempFile();
        message.setContent(OutputStream.class, cos);
        outInterceptor.handleMessage(message);
        OutputStream os = message.getContent(OutputStream.class);
        IOUtils.copy(messageIS, os);
        os.close();
        cos.releaseTempFileHold();
        Document doc = StaxUtils.read(cos.getInputStream());
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void outXMLStreamTest() throws XMLStreamException, SAXException, IOException, ParserConfigurationException {
        CachedWriter cWriter = new CachedWriter();
        cWriter.holdTempFile();
        XMLStreamWriter xWriter = StaxUtils.createXMLStreamWriter(cWriter);
        message.setContent(XMLStreamWriter.class, xWriter);
        outInterceptor.handleMessage(message);
        XMLStreamWriter tXWriter = message.getContent(XMLStreamWriter.class);
        StaxUtils.copy(new StreamSource(messageIS), tXWriter);
        tXWriter.close();
        cWriter.releaseTempFileHold();
        Document doc = StaxUtils.read(cWriter.getReader());
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void outWriterStreamTest() throws Exception {
        CachedWriter cWriter = new CachedWriter();
        message.setContent(Writer.class, cWriter);
        outInterceptor.handleMessage(message);
        Writer tWriter = message.getContent(Writer.class);
        IOUtils.copy(new InputStreamReader(messageIS), tWriter, IOUtils.DEFAULT_BUFFER_SIZE);
        tWriter.close();
        Document doc = StaxUtils.read(cWriter.getReader());
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    private boolean checkTransformedXML(Document doc) {
        NodeList list = doc.getDocumentElement()
            .getElementsByTagNameNS("http://customerservice.example.com/", "getCustomersByName1");
        return list.getLength() == 1;
    }
}
