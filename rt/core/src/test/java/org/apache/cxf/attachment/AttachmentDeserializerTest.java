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
import java.io.PushbackInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.XMLMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AttachmentDeserializerTest extends Assert {
    
    private MessageImpl msg;
    
    @Before
    public void setUp() throws Exception {
        msg = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        msg.setExchange(exchange);
    }
    
    @Test
    public void testNoBoundaryInCT() throws Exception {
        //CXF-2623
        String message = "SomeHeader: foo\n" 
            + "------=_Part_34950_1098328613.1263781527359\n"
            + "Content-Type: text/xml; charset=UTF-8\n"
            + "Content-Transfer-Encoding: binary\n"
            + "Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n"
            + "\n"
            + "<envelope/>\n"
            + "------=_Part_34950_1098328613.1263781527359\n"
            + "Content-Type: text/xml\n"
            + "Content-Transfer-Encoding: binary\n"
            + "Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n"
            + "\n"
            + "<message>\n"
            + "------=_Part_34950_1098328613.1263781527359--";
        
        Matcher m = Pattern.compile("^--(\\S*)$").matcher(message);
        Assert.assertFalse(m.find());
        m = Pattern.compile("^--(\\S*)$", Pattern.MULTILINE).matcher(message);
        Assert.assertTrue(m.find());
        
        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(message.getBytes("UTF-8")));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();
        assertEquals(1, msg.getAttachments().size());
    }
    
    @Test
    public void testLazyAttachmentCollection() throws Exception {
        InputStream is = getClass().getResourceAsStream("mimedata2");
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";
        
        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);
        
        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();
        
        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);
        attBody.close();
        assertEquals(2, msg.getAttachments().size());
    }
    
    @Test
    public void testDeserializerMtom() throws Exception {
        InputStream is = getClass().getResourceAsStream("mimedata");
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";
        
        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);
        
        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();
        
        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);
        
        Collection<Attachment> atts = msg.getAttachments();
        assertNotNull(atts);
        
        Iterator<Attachment> itr = atts.iterator();
        assertTrue(itr.hasNext());
        
        Attachment a = itr.next();
        assertNotNull(a);
        
        InputStream attIs = a.getDataHandler().getInputStream();
        
        // check the cached output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(attBody, out);
        assertTrue(out.toString().startsWith("<env:Envelope"));
        
        // try streaming a character off the wire
        assertTrue(attIs.read() == '/');
        assertTrue(attIs.read() == '9');
        
//        Attachment invalid = atts.get("INVALID");
//        assertNull(invalid.getDataHandler().getInputStream());
//        
//        assertTrue(attIs instanceof ByteArrayInputStream);
    }

    @Test
    public void testDeserializerMtomWithAxis2StyleBoundaries() throws Exception {
        InputStream is = getClass().getResourceAsStream("axis2_mimedata");
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=MIMEBoundaryurn_uuid_6BC4984D5D38EB283C1177616488109";

        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);

        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();

        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);

        Collection<Attachment> atts = msg.getAttachments();
        assertNotNull(atts);

        Iterator<Attachment> itr = atts.iterator();
        assertTrue(itr.hasNext());

        Attachment a = itr.next();
        assertNotNull(a);

        InputStream attIs = a.getDataHandler().getInputStream();

        // check the cached output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(attBody, out);
        assertTrue(out.toString().startsWith("<env:Envelope"));

        // try streaming a character off the wire
        assertTrue(attIs.read() == '/');
        assertTrue(attIs.read() == '9');

//        Attachment invalid = atts.get("INVALID");
//        assertNull(invalid.getDataHandler().getInputStream());
//
//        assertTrue(attIs instanceof ByteArrayInputStream);
    }
    
    @Test
    public void testDeserializerSwA() throws Exception {
        InputStream is = getClass().getResourceAsStream("swadata");
        String ct = "multipart/related; type=\"text/xml\"; "
            + "start=\"<86048FF3556694F7DA1918466DDF8143>\";    "
            + "boundary=\"----=_Part_0_14158819.1167275505862\"";

        
        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);
        
        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();
        
        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);
        
        Collection<Attachment> atts = msg.getAttachments();
        assertNotNull(atts);
        
        Iterator<Attachment> itr = atts.iterator();
        assertTrue(itr.hasNext());
        
        Attachment a = itr.next();
        assertNotNull(a);
        
        InputStream attIs = a.getDataHandler().getInputStream();

        // check the cached output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(attBody, out);
        assertTrue(out.toString().startsWith("<?xml"));
        
        // try streaming a character off the wire
        assertTrue(attIs.read() == 'f');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'b');
        assertTrue(attIs.read() == 'a');
        assertTrue(attIs.read() == 'r');
        assertTrue(attIs.read() == -1);
    }
    
    @Test
    public void testDeserializerSwAWithoutBoundryInContentType() throws Exception {
        InputStream is = getClass().getResourceAsStream("swadata");
        String ct = "multipart/related; type=\"text/xml\"; ";

        
        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);
        
        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();
        
        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);
        
        Collection<Attachment> atts = msg.getAttachments();
        assertNotNull(atts);
        
        Iterator<Attachment> itr = atts.iterator();
        assertTrue(itr.hasNext());
        
        Attachment a = itr.next();
        assertNotNull(a);
        
        InputStream attIs = a.getDataHandler().getInputStream();
        
        // check the cached output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(attBody, out);
        assertTrue(out.toString().startsWith("<?xml"));
        
        // try streaming a character off the wire
        assertTrue(attIs.read() == 'f');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'b');
        assertTrue(attIs.read() == 'a');
        assertTrue(attIs.read() == 'r');
        assertTrue(attIs.read() == -1);
        
        assertFalse(itr.hasNext());
    }
    
    @Test
    public void testDeserializerWithCachedFile() throws Exception {
        InputStream is = getClass().getResourceAsStream("mimedata");
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";
        
        msg.put(Message.CONTENT_TYPE, ct);
        msg.setContent(InputStream.class, is);
        msg.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, "10");
        
        AttachmentDeserializer deserializer = new AttachmentDeserializer(msg);
        deserializer.initializeAttachments();
        
        InputStream attBody = msg.getContent(InputStream.class);
        assertTrue(attBody != is);
        assertTrue(attBody instanceof DelegatingInputStream);
        
        Collection<Attachment> atts = msg.getAttachments();
        assertNotNull(atts);
        
        Iterator<Attachment> itr = atts.iterator();
        assertTrue(itr.hasNext());
        
        Attachment a = itr.next();
        assertNotNull(a);
        
        InputStream attIs = a.getDataHandler().getInputStream();
        
        assertFalse(itr.hasNext());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(attIs, out);
        assertTrue(out.size() > 1000);

    }
    
    
    @Test
    public void testSmallStream() throws Exception {
        byte[] messageBytes = ("------=_Part_1\n\nJJJJ\n------=_Part_1\n\n"
            + "Content-Transfer-Encoding: binary\n\n=3D=3D=3D\n------=_Part_1\n").getBytes();
        PushbackInputStream pushbackStream = new PushbackInputStream(new ByteArrayInputStream(messageBytes),
                                                                     2048);
        pushbackStream.read(new byte[4096], 0, 4015);
        pushbackStream.unread(messageBytes);
        pushbackStream.read(new byte[72]);

        MimeBodyPartInputStream m = new MimeBodyPartInputStream(pushbackStream, "------=_Part_1".getBytes(),
                                                                2048);
        
        assertEquals(10, m.read(new byte[1000]));
        assertEquals(-1, m.read(new byte[1000]));
        assertEquals(-1, m.read(new byte[1000]));
    }
    
    @Test
    public void testCXF2542() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("------=_Part_0_2180223.1203118300920\n");
        buf.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\n");
        buf.append("Content-Transfer-Encoding: 8bit\n");
        buf.append("Content-ID: <soap.xml@xfire.codehaus.org>\n");
        buf.append("\n");
        buf.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                   + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
                   + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                   + "<soap:Body><getNextMessage xmlns=\"http://foo.bar\" /></soap:Body>"
                   + "</soap:Envelope>\n");
        buf.append("------=_Part_0_2180223.1203118300920--\n");

        InputStream rawInputStream = new ByteArrayInputStream(buf.toString().getBytes());
        MessageImpl message = new MessageImpl();
        message.setContent(InputStream.class, rawInputStream);
        message.put(Message.CONTENT_TYPE, 
                    "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml\"; boundary=\"----=_Part_0_2180223.1203118300920\"");
        new AttachmentDeserializer(message).initializeAttachments();
        InputStream inputStreamWithoutAttachments = message.getContent(InputStream.class);
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(inputStreamWithoutAttachments, new DefaultHandler());
    }
    
    @Test
    public void imitateAttachmentInInterceptorForMessageWithMissingBoundary() throws Exception {
        ByteArrayInputStream inputStream;
        String contentType = "multipart/mixed;boundary=abc123";
        String data = "--abc123\r\n\r\n<Document></Document>\r\n\r\n";

        Message message;

        inputStream = new ByteArrayInputStream(data.getBytes());

        message = new XMLMessage(new MessageImpl());
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, inputStream);
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/mixed"));

        ad.initializeAttachments();
        message.getAttachments().size();

    }

}