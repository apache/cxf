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
package org.apache.cxf.attachment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AttachmentSerializerDeserializerTest {

    @Test
    public void testMessageWriteXopOn1() throws Exception {
        doTestMessageSerde(true, "text/xml");
    }

    @Test
    public void testMessageWriteXopOn2() throws Exception {
        doTestMessageSerde(true, "application/soap+xml; action=\"urn:foo\"");
    }

    @Test
    public void testMessageWriteXopOff1() throws Exception {
        doTestMessageSerde(false, "text/xml");
    }

    @Test
    public void testMessageWriteXopOff2() throws Exception {
        doTestMessageSerde(false, "application/soap+xml; action=\"urn:foo\"");
    }

    private void doTestMessageSerde(boolean xop, String soapContentType) throws Exception {
        MessageImpl in = new MessageImpl();

        Collection<Attachment> atts = new ArrayList<>();
        AttachmentImpl a = new AttachmentImpl("test.xml");

        InputStream is = getClass().getResourceAsStream("my.wav");
        ByteArrayDataSource ds = new ByteArrayDataSource(is, "application/octet-stream");
        a.setDataHandler(new DataHandler(ds));

        atts.add(a);

        in.setAttachments(atts);

        // Set the SOAP content type
        in.put(Message.CONTENT_TYPE, soapContentType);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.setContent(OutputStream.class, out);

        AttachmentSerializer serializer = new AttachmentSerializer(in);
        if (!xop) {
            // default is "on"
            serializer.setXop(xop);
        }

        serializer.writeProlog();
        out.write("<soap:Body/>".getBytes());

        serializer.writeAttachments();
        out.flush();

        doTestMessageRead(in, "test.xml");
        is.close();
    }

    private void doTestMessageRead(Message in, String contentId) 
            throws IOException, MessagingException {

        final MessageImpl msg = new MessageImpl();
        msg.put(Message.CONTENT_TYPE, "multipart/related;");
        
        final ByteArrayOutputStream out = (ByteArrayOutputStream)in.getContent(OutputStream.class);
        try (ByteArrayInputStream content = new ByteArrayInputStream(out.toByteArray())) {
            msg.setContent(InputStream.class, content);
            
            AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
            deserializer.initializeAttachments();
    
            Collection<Attachment> atts = msg.getAttachments();
            assertNotNull(atts);
    
            Iterator<Attachment> itr = atts.iterator();
            assertTrue(itr.hasNext());
    
            Attachment a = itr.next();
            assertNotNull(a);
    
            assertEquals("binary", a.getHeader("Content-Transfer-Encoding"));
            assertEquals(contentId, a.getId());
    
            // check the cached output stream
            InputStream attBody = msg.getContent(InputStream.class);
            try (ByteArrayOutputStream attOut = new ByteArrayOutputStream()) {
                IOUtils.copy(attBody, attOut);
                assertEquals("<soap:Body/>", attOut.toString());
            }
        }
    }
    
    @Test
    public void testMessageMTOM() throws Exception {
        doTestMessageMTOM("test.xml", "<test.xml>", "test.xml");
    }

    @Test
    public void testMessageMTOMCid() throws Exception {
        doTestMessageMTOM("cid:http%3A%2F%2Fcxf.apache.org%2F", "<http://cxf.apache.org/>", "http://cxf.apache.org/");
    }

    @Test
    public void testMessageMTOMUrlDecoded() throws Exception {
        doTestMessageMTOM("test+me.xml", "<test%2Bme.xml>", "test+me.xml");
    }

    private void doTestMessageMTOM(String contentId, String expectedEncocedContentId, 
            String expectedDecocedContentId) throws Exception {
        MessageImpl msg = new MessageImpl();

        Collection<Attachment> atts = new ArrayList<>();
        AttachmentImpl a = new AttachmentImpl(contentId);

        InputStream is = getClass().getResourceAsStream("my.wav");
        ByteArrayDataSource ds = new ByteArrayDataSource(is, "application/octet-stream");
        a.setDataHandler(new DataHandler(ds));

        atts.add(a);

        msg.setAttachments(atts);

        // Set the SOAP content type
        msg.put(Message.CONTENT_TYPE, "application/soap+xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.setContent(OutputStream.class, out);

        AttachmentSerializer serializer = new AttachmentSerializer(msg);

        serializer.writeProlog();

        String ct = (String) msg.get(Message.CONTENT_TYPE);
        assertTrue(ct.indexOf("multipart/related;") == 0);
        assertTrue(ct.indexOf("start=\"<root.message@cxf.apache.org>\"") > -1);
        assertTrue(ct.indexOf("start-info=\"application/soap+xml\"") > -1);

        out.write("<soap:Body/>".getBytes());

        serializer.writeAttachments();
        out.flush();

        doTestMessageRead(msg, expectedDecocedContentId);
    }
}