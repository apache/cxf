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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Test;

public class AttachmentSerializerTest extends Assert {
    
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
        
        Collection<Attachment> atts = new ArrayList<Attachment>();
        AttachmentImpl a = new AttachmentImpl("test.xml");
        
        InputStream is = getClass().getResourceAsStream("my.wav");
        ByteArrayDataSource ds = new ByteArrayDataSource(is, "application/octet-stream");
        a.setDataHandler(new DataHandler(ds));
        
        atts.add(a);
        
        msg.setAttachments(atts);
        
        // Set the SOAP content type
        msg.put(Message.CONTENT_TYPE, soapContentType);
        String soapCtType = null;
        String soapCtParams = null;
        int p = soapContentType.indexOf(';');
        if (p != -1) {
            soapCtParams = soapContentType.substring(p);
            soapCtType = soapContentType.substring(0, p);
        } else {
            soapCtParams = "";
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

        // we expect
        // - the package header must have type multipart/related
        // - the start-info property must be present for mtom but otherwise optional
        // - the action property should not appear directly
        // - the type property must be application/xop+xml for mtom but otherwise text/xml or application/soap+xml
        String ct = (String) msg.get(Message.CONTENT_TYPE);
        System.out.println("##teset ct=" + ct);
        assertTrue(ct.indexOf("multipart/related;") == 0);
        assertTrue(ct.indexOf("start=\"<root.message@cxf.apache.org>\"") > -1);
        assertTrue(ct.indexOf("start-info=\"" + escapeQuotes(soapContentType) + "\"") > -1);
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
        // we expect
        // - the envelope header must have type application/xop+xml for mtom but otherwise t
        // - the start-info property must be present for mtom but otherwise text/xml or application/soap+xml
        // - the action must appear if it was present in the original message
        if (xop) {
            assertEquals("application/xop+xml; charset=UTF-8; type=\"" + soapCtType + "\"" + soapCtParams, 
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
        MessageImpl msg = new MessageImpl();
        
        Collection<Attachment> atts = new ArrayList<Attachment>();
        AttachmentImpl a = new AttachmentImpl("test.xml");
        
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
        assertEquals("<test.xml>", part2.getHeader("Content-ID")[0]);
        
    }

    private static String escapeQuotes(String s) {
        return s.indexOf('"') != 0 ? s.replace("\"", "\\\"") : s;    
    }
}
