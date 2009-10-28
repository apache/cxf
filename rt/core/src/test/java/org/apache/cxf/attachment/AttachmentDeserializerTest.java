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
import java.util.Iterator;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
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
}