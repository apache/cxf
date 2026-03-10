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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

import jakarta.activation.DataSource;
import jakarta.activation.URLDataSource;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.XMLMessage;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AttachmentDeserializerTest {

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
        assertFalse(m.find());
        m = Pattern.compile("^--(\\S*)$", Pattern.MULTILINE).matcher(message);
        assertTrue(m.find());

        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
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
        List<String> cidlist = new ArrayList<>();
        cidlist.add("xfire_logo.jpg");
        cidlist.add("xfire_logo2.jpg");

        for (Iterator<Attachment> it = msg.getAttachments().iterator(); it.hasNext();) {
            Attachment a = it.next();
            assertTrue(cidlist.remove(a.getId()));
            it.remove();
        }
        assertEquals(0, cidlist.size());
        assertEquals(0, msg.getAttachments().size());
        is.close();
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(attBody, out);
            assertTrue(out.toString().startsWith("<env:Envelope"));
        }

        // try streaming a character off the wire
        assertEquals(255, attIs.read());
        assertEquals(216, (char)attIs.read());

//        Attachment invalid = atts.get("INVALID");
//        assertNull(invalid.getDataHandler().getInputStream());
//
//        assertTrue(attIs instanceof ByteArrayInputStream);
        is.close();
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(attBody, out);
            assertTrue(out.toString().startsWith("<env:Envelope"));
        }

        // try streaming a character off the wire
        assertEquals(255, attIs.read());
        assertEquals(216, attIs.read());

//        Attachment invalid = atts.get("INVALID");
//        assertNull(invalid.getDataHandler().getInputStream());
//
//        assertTrue(attIs instanceof ByteArrayInputStream);
        is.close();
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(attBody, out);
            assertTrue(out.toString().startsWith("<?xml"));
        }

        // try streaming a character off the wire
        assertTrue(attIs.read() == 'f');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'b');
        assertTrue(attIs.read() == 'a');
        assertTrue(attIs.read() == 'r');
        assertTrue(attIs.read() == -1);

        is.close();
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(attBody, out);
            assertTrue(out.toString().startsWith("<?xml"));
        }

        // try streaming a character off the wire
        assertTrue(attIs.read() == 'f');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'o');
        assertTrue(attIs.read() == 'b');
        assertTrue(attIs.read() == 'a');
        assertTrue(attIs.read() == 'r');
        assertTrue(attIs.read() == -1);

        assertFalse(itr.hasNext());
        is.close();
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

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(attIs, out);
            assertTrue(out.size() > 1000);
        }
        is.close();
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
        StringBuilder buf = new StringBuilder(512);
        buf.append("------=_Part_0_2180223.1203118300920\n");
        buf.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\n");
        buf.append("Content-Transfer-Encoding: 8bit\n");
        buf.append("Content-ID: <soap.xml@xfire.codehaus.org>\n");
        buf.append('\n');
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

        inputStreamWithoutAttachments.close();
        rawInputStream.close();
    }

    @Test
    public void imitateAttachmentInInterceptorForMessageWithMissingBoundary() throws Exception {
        String contentType = "multipart/mixed;boundary=abc123";
        String data = "--abc123\r\n\r\n<Document></Document>\r\n\r\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes());

        Message message = new XMLMessage(new MessageImpl());
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
        assertEquals(0, message.getAttachments().size());

        inputStream.close();
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
                + "------=_Part_1--").getBytes(StandardCharsets.UTF_8);
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

        String s = IOUtils.toString(message.getContent(InputStream.class));
        assertEquals("JJJJ", s.trim());
        int count = 1;
        for (Attachment a : message.getAttachments()) {
            s = IOUtils.toString(a.getDataHandler().getInputStream());
            assertEquals("ABCD" + count++, s);
        }

        in.close();
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
            byte[] bts = new byte[1024];

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
            ins.close();
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
        byte[] bts = new byte[1024];
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

        ins.close();
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
        byte[] bts = new byte[1024];
        InputStream ins = ds.getInputStream();
        int count = 0;
        int x = ins.read(bts, 500, 200);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 500, 200);
        }
        assertEquals(500, count);
        assertEquals(-1, ins.read(new byte[1000], 500, 500));

        ins.close();

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
        ins.close();
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
        byte[] bts = new byte[1024];
        InputStream ins = ds.getInputStream();
        int count = 0;
        int x = ins.read(bts, 100, 600);
        while (x != -1) {
            count += x;
            x = ins.read(bts, 100, 600);
        }
        assertEquals(500, count);
        assertEquals(-1, ins.read(new byte[1000], 100, 600));
        ins.close();

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
        ins.close();
    }

    @Test
    public void testManyAttachments() throws Exception {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");

        // Add many attachments
        IntStream.range(0, 100000).forEach(i -> {
            sb.append("------=_Part_34950_1098328613.1263781527359\n")
                .append("Content-Type: text/xml\n")
                .append("Content-Transfer-Encoding: binary\n")
                .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
                .append('\n')
                .append("<message>\n")
                .append("------=_Part_34950_1098328613.1263781527359--\n");
        });

        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertThrows("Failure expected on too many attachments", RuntimeException.class, 
            () -> msg.getAttachments().size());
    }

    @Test
    public void testChangingMaxAttachmentCount() throws Exception {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");

        // Add many attachments
        IntStream.range(0, 40).forEach(i -> {
            sb.append("------=_Part_34950_1098328613.1263781527359\n")
                .append("Content-Type: text/xml\n")
                .append("Content-Transfer-Encoding: binary\n")
                .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
                .append('\n')
                .append("<message>\n")
                .append("------=_Part_34950_1098328613.1263781527359--\n");
        });

        msg = new MessageImpl();
        msg.put(AttachmentDeserializer.ATTACHMENT_MAX_COUNT, "30");
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertThrows("Failure expected on too many attachments", RuntimeException.class,
            () -> msg.getAttachments().size());

        // Now we'll allow it
        msg = new MessageImpl();
        msg.put(AttachmentDeserializer.ATTACHMENT_MAX_COUNT, "60");
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertEquals(40, msg.getAttachments().size());
    }

    @Test
    public void testInvalidContentDispositionFilename() throws Exception {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");

        sb.append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
            .append("Content-Disposition: attachment; filename=../../../../../../../../etc/passwd\n")
            .append('\n')
            .append("<message>\n")
            .append("------=_Part_34950_1098328613.1263781527359--\n");

        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertEquals(1, msg.getAttachments().size());
        Attachment attachment = msg.getAttachments().iterator().next();
        AttachmentDataSource dataSource = (AttachmentDataSource)attachment.getDataHandler().getDataSource();
        assertEquals("passwd", dataSource.getName());
    }

    @Test
    public void testDefaultContentTypeIfNotSet() throws Exception {
        StringBuilder sb = new StringBuilder(1000);

        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");
        
        sb.append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
            .append('\n')
            .append("<message>\n")
            .append("------=_Part_34950_1098328613.1263781527359--\n");
            
        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");

        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertEquals(1, msg.getAttachments().size());
        Attachment attachment = msg.getAttachments().iterator().next();
        AttachmentDataSource dataSource = (AttachmentDataSource)attachment.getDataHandler().getDataSource();
        assertEquals("application/octet-stream", dataSource.getContentType());
    }

    @Test
    public void testContentTypeIfNotSet() throws Exception {
        StringBuilder sb = new StringBuilder(1000);

        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");
        
        sb.append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
            .append('\n')
            .append("<message>\n")
            .append("------=_Part_34950_1098328613.1263781527359--\n");
            
        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");
        msg.put(AttachmentUtil.ATTACHMENT_CONTENT_TYPE, "text/plain");

        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertEquals(1, msg.getAttachments().size());
        Attachment attachment = msg.getAttachments().iterator().next();
        AttachmentDataSource dataSource = (AttachmentDataSource)attachment.getDataHandler().getDataSource();
        assertEquals("text/plain", dataSource.getContentType());
    }

    @Test
    public void testContentType() throws Exception {
        StringBuilder sb = new StringBuilder(1000);

        sb.append("SomeHeader: foo\n")
            .append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <318731183421.1263781527359.IBM.WEBSERVICES@auhpap02>\n")
            .append('\n')
            .append("<envelope/>\n");
        
        sb.append("------=_Part_34950_1098328613.1263781527359\n")
            .append("Content-Transfer-Encoding: binary\n")
            .append("Content-Id: <b86a5f2d-e7af-4e5e-b71a-9f6f2307cab0>\n")
            .append("Content-Type: text/xml; charset=UTF-8\n")
            .append('\n')
            .append("<message>\n")
            .append("------=_Part_34950_1098328613.1263781527359--\n");
            
        msg = new MessageImpl();
        msg.setContent(InputStream.class, new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        msg.put(Message.CONTENT_TYPE, "multipart/related");

        AttachmentDeserializer ad = new AttachmentDeserializer(msg);
        ad.initializeAttachments();

        // Force it to load the attachments
        assertEquals(1, msg.getAttachments().size());
        Attachment attachment = msg.getAttachments().iterator().next();
        AttachmentDataSource dataSource = (AttachmentDataSource)attachment.getDataHandler().getDataSource();
        assertEquals("text/xml; charset=UTF-8", dataSource.getContentType());
    }

    @Test
    public void testCXF8706() {
        final DataSource ds = AttachmentUtil
            .getAttachmentDataSource("cid:http://image.com/1.gif", Collections.emptyList());
        assertThat(ds, instanceOf(LazyDataSource.class));
    }
    
    @Test
    public void testCXF8706followUrl() {
        System.setProperty(AttachmentUtil.ATTACHMENT_XOP_FOLLOW_URLS_PROPERTY, "true");
        try {
            final DataSource ds = AttachmentUtil
                .getAttachmentDataSource("cid:http://image.com/1.gif", Collections.emptyList());
            assertThat(ds, instanceOf(URLDataSource.class));
        } finally {
            System.clearProperty(AttachmentUtil.ATTACHMENT_XOP_FOLLOW_URLS_PROPERTY);
        }
    }
}
