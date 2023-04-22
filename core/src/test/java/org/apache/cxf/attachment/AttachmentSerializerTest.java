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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AttachmentSerializerTest {

    @Test
    public void testMessageWriteXopOn1() throws Exception {
        doTestMessageWrite(true, "text/xml");
    }

    @Test
    public void testMessageWriteXopOn2() throws Exception {
        doTestMessageWrite(true, "application/soap+xml; action=\"urn:foo\"");
    }

    @Test
    public void testMessageWriteXopOff1() throws Exception {
        doTestMessageWrite(false, "text/xml");
    }

    @Test
    public void testMessageWriteXopOff2() throws Exception {
        doTestMessageWrite(false, "application/soap+xml; action=\"urn:foo\"");
    }

    private void doTestMessageWrite(boolean xop, String soapContentType) throws Exception {
        MessageImpl msg = new MessageImpl();

        Collection<Attachment> atts = new ArrayList<>();
        AttachmentImpl a = new AttachmentImpl("test.xml");

        InputStream is = getClass().getResourceAsStream("my.wav");
        ByteArrayDataSource ds = new ByteArrayDataSource(is, "application/octet-stream");
        a.setDataHandler(new DataHandler(ds));

        atts.add(a);

        msg.setAttachments(atts);

        // Set the SOAP content type
        msg.put(Message.CONTENT_TYPE, soapContentType);
        final String soapCtType;
        final String soapCtParams;
        final String soapCtParamsEscaped;
        int p = soapContentType.indexOf(';');
        if (p != -1) {
            soapCtParams = soapContentType.substring(p);
            soapCtParamsEscaped = escapeQuotes(soapCtParams);
            soapCtType = soapContentType.substring(0, p);
        } else {
            soapCtParams = "";
            soapCtParamsEscaped = "";
            soapCtType = soapContentType;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.setContent(OutputStream.class, out);

        AttachmentSerializer serializer = new AttachmentSerializer(msg);
        if (!xop) {
            // default is "on"
            serializer.setXop(xop);
        }

        serializer.writeProlog();

        // we expect the following rules at the package header level
        // - the package header must have media type multipart/related.
        // - the start-info property must be present for mtom but otherwise optional. its
        //   value must contain the content type associated with the root content's xml serialization,
        //   including its parameters as appropriate.
        // - the action property should not appear directly in the package header level
        // - the type property must contain the media type type/subtype of the root content part.
        //   namely application/xop+xml for mtom but otherwise text/xml or application/soap+xml
        //   depending on the soap version 1.1 or 1.2, respectively.
        String ct = (String) msg.get(Message.CONTENT_TYPE);
        assertTrue(ct.indexOf("multipart/related;") == 0);
        assertTrue(ct.indexOf("start=\"<root.message@cxf.apache.org>\"") > -1);
        assertTrue(ct.indexOf("start-info=\"" + soapCtType + soapCtParamsEscaped + "\"") > -1);
        assertTrue(ct.indexOf("action=\"") == -1);
        if (xop) {
            assertTrue(ct.indexOf("type=\"application/xop+xml\"") > -1);
        } else {
            assertTrue(ct.indexOf("type=\"" + soapCtType + "\"") > -1);
        }
        out.write("<soap:Body/>".getBytes());

        serializer.writeAttachments();

        out.flush();

        DataSource source = new ByteArrayDataSource(new ByteArrayInputStream(out.toByteArray()), ct);
        MimeMultipart mpart = new MimeMultipart(source);
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage inMsg = new MimeMessage(session);
        inMsg.setContent(mpart);
        inMsg.addHeaderLine("Content-Type: " + ct);

        MimeMultipart multipart = (MimeMultipart) inMsg.getContent();

        MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(0);
        // we expect the following rules at the root content level
        // - the envelope header must have type application/xop+xml for mtom but otherwise the content's
        //   serialization type.
        // - the type property must be present for mtom and it must contain the content's serialization type
        //   including its parameters if appropriate.
        // - the action must appear if it was present in the original message (i.e., for soap 1.2)
        if (xop) {
            assertEquals("application/xop+xml; charset=UTF-8; type=\"" + soapCtType + soapCtParamsEscaped + "\"",
                part.getHeader("Content-Type")[0]);
        } else {
            assertEquals(soapCtType + "; charset=UTF-8" + soapCtParams,
                         part.getHeader("Content-Type")[0]);
        }

        assertEquals("binary", part.getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("<root.message@cxf.apache.org>", part.getHeader("Content-ID")[0]);

        InputStream in = part.getDataHandler().getInputStream();
        ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
        IOUtils.copy(in, bodyOut);
        out.close();
        in.close();

        assertEquals("<soap:Body/>", bodyOut.toString());

        MimeBodyPart part2 = (MimeBodyPart) multipart.getBodyPart(1);
        assertEquals("application/octet-stream", part2.getHeader("Content-Type")[0]);
        assertEquals("binary", part2.getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("<test.xml>", part2.getHeader("Content-ID")[0]);

    }
    
    @Test
    public void testMessageMTOM() throws Exception {
        doTestMessageMTOM("test.xml", "<test.xml>");
    }

    @Test
    public void testMessageMTOMCid() throws Exception {
        doTestMessageMTOM("cid:http%3A%2F%2Fcxf.apache.org%2F", "<http://cxf.apache.org/>");
    }

    @Test
    public void testMessageMTOMCidEncoded() throws Exception {
        doTestMessageMTOM("cid:cxf@[2001%3A0db8%3A11a3%3A09d7%3A1f34%3A8a2e%3A07a0%3A765d]",
            "<cxf@[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]>");
    }

    @Test
    public void testMessageMTOMUrlDecoded() throws Exception {
        doTestMessageMTOM("test+me.xml", "<test%2Bme.xml>");
    }
    
    @Test
    public void testMessageMTOMUrlDecodedCid() throws Exception {
        doTestMessageMTOM("cid:test+me.xml", "<test+me.xml>");
    }

    private void doTestMessageMTOM(String contentId, String expectedContentId) throws Exception {
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
        DataSource source = new ByteArrayDataSource(new ByteArrayInputStream(out.toByteArray()), ct);
        MimeMultipart mpart = new MimeMultipart(source);
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage inMsg = new MimeMessage(session);
        inMsg.setContent(mpart);

        inMsg.addHeaderLine("Content-Type: " + ct);

        MimeMultipart multipart = (MimeMultipart) inMsg.getContent();

        MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(0);
        assertEquals("application/xop+xml; charset=UTF-8; type=\"application/soap+xml\"",
                     part.getHeader("Content-Type")[0]);
        assertEquals("binary", part.getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("<root.message@cxf.apache.org>", part.getHeader("Content-ID")[0]);

        InputStream in = part.getDataHandler().getInputStream();
        ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
        IOUtils.copy(in, bodyOut);
        out.close();
        in.close();

        assertEquals("<soap:Body/>", bodyOut.toString());

        MimeBodyPart part2 = (MimeBodyPart) multipart.getBodyPart(1);
        assertEquals("application/octet-stream", part2.getHeader("Content-Type")[0]);
        assertEquals("binary", part2.getHeader("Content-Transfer-Encoding")[0]);
        assertEquals(expectedContentId, part2.getHeader("Content-ID")[0]);

    }

    private static String escapeQuotes(String s) {
        return s.indexOf('"') != 0 ? s.replace("\"", "\\\"") : s;
    }
}