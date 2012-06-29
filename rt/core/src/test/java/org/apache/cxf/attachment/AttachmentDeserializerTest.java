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
import java.io.PushbackInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataSource;
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
        m.close();
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
    @Test
    public void testDoesntReturnZero() throws Exception {
        String contentType = "multipart/mixed;boundary=----=_Part_1";
        byte[] messageBytes = (
                  "------=_Part_1\n\n"
                + "JJJJ\n"
                + "------=_Part_1"
                + "\n\nContent-Transfer-Encoding: binary\n\n"
                + "ABCD1\r\n"
                + "------=_Part_1"
                + "\n\nContent-Transfer-Encoding: binary\n\n"
                + "ABCD2\r\n"
                + "------=_Part_1"
                + "\n\nContent-Transfer-Encoding: binary\n\n"
                + "ABCD3\r\n"
                + "------=_Part_1--").getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(messageBytes) {
            public int read(byte[] b, int off, int len) {
                return super.read(b, off, len >= 2 ? 2 : len); 
            }
        };
        
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, in);
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/mixed"));

        ad.initializeAttachments();
        
        String s = getString(message.getContent(InputStream.class));
        assertEquals("JJJJ", s.trim());
        int count = 1;
        for (Attachment a : message.getAttachments()) {
            s = getString(a.getDataHandler().getInputStream());
            assertEquals("ABCD" + count++, s);
        }
    }
    
    private String getString(InputStream ins) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);
        byte b[] = new byte[100];
        int i = ins.read(b);
        while (i > 0) {
            bout.write(b, 0 , i);
            i = ins.read(b);
        }
        if (i == 0) {
            throw new IOException("Should not be 0");
        }
        return bout.toString();
    }
    
    @Test
    public void testCXF3383() throws Exception {
        String contentType = "multipart/related; type=\"application/xop+xml\";"
            + " boundary=\"uuid:7a555f51-c9bb-4bd4-9929-706899e2f793\"; start=" 
            + "\"<root.message@cxf.apache.org>\"; start-info=\"text/xml\"";
        
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, getClass().getResourceAsStream("cxf3383.data"));
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/related"));
        
        ad.initializeAttachments();
        
        
        for (int x = 1; x < 50; x++) {
            String cid = "1882f79d-e20a-4b36-a222-7a75518cf395-" + x + "@cxf.apache.org";
            DataSource ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
            byte bts[] = new byte[1024];
            
            InputStream ins = ds.getInputStream();
            int count = 0;
            int sz = ins.read(bts, 0, bts.length);
            while (sz != -1) {
                count += sz;
                // We do not expect the data to fill up the buffer:
                assertTrue(count < bts.length);
                sz = ins.read(bts, count, bts.length - count);
            }
            assertEquals(x + 1, count);
        }
    }

    
    @Test
    public void testCXF3582() throws Exception {
        String contentType = "multipart/related; type=\"application/xop+xml\"; "
            + "boundary=\"uuid:906fa67b-85f9-4ef5-8e3d-52416022d463\"; "
            + "start=\"<root.message@cxf.apache.org>\"; start-info=\"text/xml\"";
            
            
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, getClass().getResourceAsStream("cxf3582.data"));
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/related"));
        
        ad.initializeAttachments();
        
        String cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-1@apache.org";
        DataSource ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        byte bts[] = new byte[1024];
        InputStream ins = ds.getInputStream();
        int count = ins.read(bts, 0, bts.length);
        assertEquals(500, count);
        assertEquals(-1, ins.read(new byte[1000], 500, 500));

        cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-2@apache.org";
        ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        bts = new byte[1024];
        ins = ds.getInputStream();
        count = ins.read(bts, 0, bts.length);
        assertEquals(1024, count);
        assertEquals(225, ins.read(new byte[1000], 500, 500));
        assertEquals(-1, ins.read(new byte[1000], 500, 500));
    }

    @Test
    public void testCXF3582b() throws Exception {
        String contentType = "multipart/related; type=\"application/xop+xml\"; "
            + "boundary=\"uuid:906fa67b-85f9-4ef5-8e3d-52416022d463\"; "
            + "start=\"<root.message@cxf.apache.org>\"; start-info=\"text/xml\"";
            
            
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, getClass().getResourceAsStream("cxf3582.data"));
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/related"));
        
        ad.initializeAttachments();
        
        String cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-1@apache.org";
        DataSource ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        byte bts[] = new byte[1024];
        InputStream ins = ds.getInputStream();
        int count = 0;
        int x = ins.read(bts, 500, 200);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 500, 200);
        }
        assertEquals(500, count);
        assertEquals(-1, ins.read(new byte[1000], 500, 500));

        cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-2@apache.org";
        ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        bts = new byte[1024];
        ins = ds.getInputStream();
        count = 0;
        x = ins.read(bts, 500, 200);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 500, 200);
        }
        assertEquals(1249, count);
        assertEquals(-1, ins.read(new byte[1000], 500, 500));
    }
    @Test
    public void testCXF3582c() throws Exception {
        String contentType = "multipart/related; type=\"application/xop+xml\"; "
            + "boundary=\"uuid:906fa67b-85f9-4ef5-8e3d-52416022d463\"; "
            + "start=\"<root.message@cxf.apache.org>\"; start-info=\"text/xml\"";
            
            
        Message message = new MessageImpl();
        message.put(Message.CONTENT_TYPE, contentType);
        message.setContent(InputStream.class, getClass().getResourceAsStream("cxf3582.data"));
        message.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, System
                .getProperty("java.io.tmpdir"));
        message.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String
                .valueOf(AttachmentDeserializer.THRESHOLD));


        AttachmentDeserializer ad 
            = new AttachmentDeserializer(message, 
                                         Collections.singletonList("multipart/related"));
        
        ad.initializeAttachments();
        
        String cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-1@apache.org";
        DataSource ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        byte bts[] = new byte[1024];
        InputStream ins = ds.getInputStream();
        int count = 0;
        int x = ins.read(bts, 100, 600);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 100, 600);
        }
        assertEquals(500, count);
        assertEquals(-1, ins.read(new byte[1000], 100, 600));

        cid = "1a66bb35-67fc-4e89-9f33-48af417bf9fe-2@apache.org";
        ds = AttachmentUtil.getAttachmentDataSource(cid, message.getAttachments());
        bts = new byte[1024];
        ins = ds.getInputStream();
        count = 0;
        x = ins.read(bts, 100, 600);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 100, 600);
        }
        assertEquals(1249, count);
        assertEquals(-1, ins.read(new byte[1000], 100, 600));
    }
}

