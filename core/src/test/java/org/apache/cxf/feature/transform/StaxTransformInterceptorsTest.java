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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.exc.WstxUnexpectedCharException;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.interceptor.transform.TransformInInterceptor;
import org.apache.cxf.interceptor.transform.TransformOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;




/* Provides Stax-based transformation of incoming message.
 */
public class StaxTransformInterceptorsTest {

    /* this message is UTF-8 and states so in the header. Regular case. */
    private static final String MESSAGE_FILE = "message_utf-8.xml";
    private static final String MESSAGE_FILE_UNANNOUNCED = "message_utf-8_unannounced.xml";

    private static final String MESSAGE_FILE_LATIN1_EXPLICIT = "message_iso-8859-1.xml";
    private static final String MESSAGE_FILE_LATIN1_UNANNOUNCED = "message_iso-8859-1_unannounced.xml";

    private InputStream messageInStmUtf8;
    private Message messageUtf8;

    private InputStream messageInStmUtf8Unannounced;
    private Message messageUtf8Unannounced;

    private InputStream messageInStmLatin1Explicit;
    private Message messageLatin1Explicit;
    private Message messageUtf8InHeader;
    private Message messageUtf16beInHeader;

    private InputStream messageInStmLatin1Unannounced;
    private Message messageLatin1Unannounced;

    private InputStream messageInStmLatin1InHeader;
    private Message messageLatin1InHeader;

    private TransformInInterceptor inInterceptor;
    private TransformOutInterceptor outInterceptor;

    @Before
    public void setUp() throws TransformerConfigurationException {
        messageInStmUtf8 = ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE, this.getClass());
        if (messageInStmUtf8 == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE);
        }
        messageUtf8 = new MessageImpl();

        messageInStmUtf8Unannounced = ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE_UNANNOUNCED, this.getClass());
        if (messageInStmUtf8Unannounced == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE_UNANNOUNCED);
        }
        messageUtf8Unannounced = new MessageImpl();

        messageInStmLatin1Explicit =
                ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE_LATIN1_EXPLICIT, this.getClass());
        if (messageInStmLatin1Explicit == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE_LATIN1_EXPLICIT);
        }
        messageLatin1Explicit = new MessageImpl();

        messageInStmLatin1Unannounced =
                ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE_LATIN1_UNANNOUNCED, this.getClass());
        if (messageInStmLatin1Unannounced == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE_LATIN1_UNANNOUNCED);
        }
        messageLatin1Unannounced = new MessageImpl();

        messageInStmLatin1InHeader =
                ClassLoaderUtils.getResourceAsStream(MESSAGE_FILE_LATIN1_UNANNOUNCED, this.getClass());
        if (messageInStmLatin1InHeader == null) {
            throw new IllegalArgumentException("Cannot load message from path: " + MESSAGE_FILE_LATIN1_UNANNOUNCED);
        }
        messageLatin1InHeader = new MessageImpl();
        messageLatin1InHeader.put(Message.ENCODING, StandardCharsets.ISO_8859_1.name());

        messageUtf8InHeader = new MessageImpl();
        messageUtf8InHeader.put(Message.ENCODING, StandardCharsets.UTF_8.name());

        messageUtf16beInHeader = new MessageImpl();
        messageUtf16beInHeader.put(Message.ENCODING, StandardCharsets.UTF_16BE.name());

        Map<String, String> staxTransforms = new HashMap<String, String>();
        staxTransforms.put("{http://customerservice.example.com/}getCustomersByName",
                "{http://customerservice.example.com/}getCustomersByName1");

        inInterceptor = new TransformInInterceptor();
        inInterceptor.setInTransformElements(staxTransforms);

        outInterceptor = new TransformOutInterceptor();
        outInterceptor.setOutTransformElements(staxTransforms);
    }

    private void inStreamTest(Message message, InputStream messageIS) throws Exception {
        message.setContent(InputStream.class, messageIS);
        inInterceptor.handleMessage(message);
        XMLStreamReader transformedXReader = message.getContent(XMLStreamReader.class);
        Document doc = StaxUtils.read(transformedXReader);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void inStreamTestUtf8Regular() throws Exception {
        /* regular case */
        inStreamTest(messageUtf8, messageInStmUtf8);
    }

    @Test
    public void inStreamTestUtf8Unannounced() throws Exception {
        /* correct case as UTF-8 usage is implied */
        inStreamTest(messageUtf8Unannounced, messageInStmUtf8Unannounced);
    }

    @Test(expected = WstxIOException.class)
    public void inStreamTestLatin1Explicit() throws Exception {
        /* the header encoding (or its lack, interpreted as UTF-8) trumps the encoding declaration within the XML
        payload */
        inStreamTest(messageLatin1Explicit, messageInStmLatin1Explicit);
    }

    @Test(expected = WstxIOException.class)
    public void inStreamTestLatin1PayloadBogusUtf8Header() throws Exception {
        /* the header encoding trumps the encoding declaration within the XML payload */
        inStreamTest(messageUtf8InHeader, messageInStmLatin1Explicit);
    }

    @Test(expected = WstxUnexpectedCharException.class)
    public void inStreamTestLatin1PayloadBogusUtf16beHeader() throws Exception {
        /* the header encoding trumps the encoding declaration within the XML payload */
        inStreamTest(messageUtf16beInHeader, messageInStmLatin1Explicit);
    }

    @Test(expected = WstxUnexpectedCharException.class)
    public void inStreamTestUtf8PayloadBogusUtf16beHeader() throws Exception {
        /* the header encoding trumps the encoding declaration within the XML payload */
        inStreamTest(messageUtf16beInHeader, messageInStmUtf8);
    }

    @Test(expected = WstxIOException.class) /* we expect an unannounced Latin1 document to fail */
    public void inStreamTestLatin1Implicit() throws Exception {
        /* failure expected as an XML payload which isn't UTF-8 and doesn't announce its encoding and where
        the message header itself lacks encoding info cannot be safely decoded */
        inStreamTest(messageLatin1Unannounced, messageInStmLatin1Unannounced);
    }

    @Test
    public void inStreamTestLatin1InHeader() throws Exception {
        /* correct case as even though the XML document lacks encoding information, the Message metadata provides it */
        inStreamTest(messageLatin1InHeader, messageInStmLatin1InHeader);
    }


    private void inXMLStreamTest(Message message,
                                 String messageEncoding, InputStream messageIS) throws XMLStreamException {
        XMLStreamReader xReader = StaxUtils.createXMLStreamReader(messageIS, messageEncoding);
        message.setContent(XMLStreamReader.class, xReader);
        message.setContent(InputStream.class, messageIS);
        inInterceptor.handleMessage(message);
        XMLStreamReader transformedXReader = message.getContent(XMLStreamReader.class);
        Document doc = StaxUtils.read(transformedXReader);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }


    @Test
    public void inXMLStreamTest() throws XMLStreamException {
        inXMLStreamTest(messageUtf8, StandardCharsets.UTF_8.name(), messageInStmUtf8);
    }

    @Test
    public void inXMLStreamTestUtf8Unannounced() throws XMLStreamException {
        inXMLStreamTest(messageUtf8Unannounced, StandardCharsets.UTF_8.name(), messageInStmUtf8Unannounced);
    }

    @Test
    public void inXMLStreamTestLatin1InHeader() throws XMLStreamException {
        inXMLStreamTest(messageLatin1InHeader, StandardCharsets.ISO_8859_1.name(), messageInStmLatin1InHeader);
    }

    @Test
    public void inXMLStreamTestLatin1Explicit() throws XMLStreamException {
        inXMLStreamTest(messageLatin1Explicit, StandardCharsets.ISO_8859_1.name(), messageInStmLatin1Explicit);
    }

    private void outStreamTest(Message message, String encoding, InputStream messageIS) throws Exception {
        CachedOutputStream cos = new CachedOutputStream();
        cos.holdTempFile();
        message.setContent(OutputStream.class, cos);
        outInterceptor.handleMessage(message);

        XMLStreamWriter tXWriter = message.getContent(XMLStreamWriter.class);
        StaxUtils.copy(new StreamSource(messageIS), tXWriter);
        tXWriter.close();
        cos.releaseTempFileHold();
        Document doc = StaxUtils.read(cos.getInputStream(), encoding);
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    @Test
    public void outStreamTest() throws Exception {
        outStreamTest(messageUtf8, StandardCharsets.UTF_8.name(), messageInStmUtf8);
    }

    @Test
    public void outStreamTestLatin1Explicit() throws Exception {
        /* as soon as the payload says encoding=latin1, this is fine */
        outStreamTest(messageLatin1Explicit, StandardCharsets.ISO_8859_1.name(), messageInStmLatin1Explicit);
    }

    @Test
    public void outStreamTestLatin1InHeaderExplicit() throws Exception {
        /* Note that we DO NOT test/send XML without specifying the encoding within the header */
        outStreamTest(messageLatin1InHeader, StandardCharsets.ISO_8859_1.name(), messageInStmLatin1Explicit);
    }

    @Test(expected = WstxIOException.class)
    public void outStreamTestLatin1InHeaderUnannounced() throws Exception {
        /* Note that we DO NOT test/send XML without specifying the encoding within the header */
        outStreamTest(messageLatin1InHeader, StandardCharsets.ISO_8859_1.name(), messageInStmLatin1InHeader);
    }


    @Test
    public void outXMLStreamTest() throws XMLStreamException, SAXException, IOException, ParserConfigurationException {
        CachedWriter cWriter = new CachedWriter();
        cWriter.holdTempFile();
        XMLStreamWriter xWriter = StaxUtils.createXMLStreamWriter(cWriter);
        messageUtf8.setContent(XMLStreamWriter.class, xWriter);
        outInterceptor.handleMessage(messageUtf8);
        XMLStreamWriter tXWriter = messageUtf8.getContent(XMLStreamWriter.class);
        StaxUtils.copy(new StreamSource(messageInStmUtf8), tXWriter);
        tXWriter.close();
        cWriter.releaseTempFileHold();
        Document doc = StaxUtils.read(cWriter.getReader());
        Assert.assertTrue("Message was not transformed", checkTransformedXML(doc));
    }

    private boolean checkTransformedXML(Document doc) {
        NodeList list = doc.getDocumentElement()
                .getElementsByTagNameNS("http://customerservice.example.com/", "getCustomersByName1");
        return list.getLength() == 1;
    }
}
