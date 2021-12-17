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

package org.apache.cxf.ws.rm.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PersistenceUtilsTest {

    private static final String MULTIPART_TYPE = "multipart/related; type=\"text/xml\";"
        + " boundary=\"uuid:74b6a245-2e17-40eb-a86c-308664e18460\"; start=\"<root."
        + "message@cxf.apache.org>\"; start-info=\"application/soap+xml\"";

    private static final String SOAP_PART = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<data/></soap:Envelope>";

    @Test
    public void testSerialiseDeserialiseAcknowledgement() {
        SequenceAcknowledgement ack = new SequenceAcknowledgement();
        AcknowledgementRange range = new AcknowledgementRange();
        range.setLower(Long.valueOf(1));
        range.setUpper(Long.valueOf(10));
        ack.getAcknowledgementRange().add(range);
        PersistenceUtils utils = PersistenceUtils.getInstance();
        InputStream is = utils.serialiseAcknowledgment(ack);
        SequenceAcknowledgement refAck = utils.deserialiseAcknowledgment(is);
        assertEquals(refAck.getAcknowledgementRange().size(), refAck.getAcknowledgementRange().size());
        AcknowledgementRange refRange = refAck.getAcknowledgementRange().get(0);
        assertEquals(range.getLower(), refRange.getLower());
        assertEquals(range.getUpper(), refRange.getUpper());
    }

    @Test
    public void testEncodeRMContent() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(SOAP_PART.getBytes());

        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, "text/xml");
        // update rmmessage
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, bis);

        assertStartsWith(rmmsg.getContent().getInputStream(), "<soap:");
        assertNotNull(rmmsg.getContentType());
        assertTrue(rmmsg.getContentType().startsWith("text/xml"));
    }

    @Test
    public void testEncodeRMContentWithAttachments() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(SOAP_PART.getBytes());

        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, "text/xml");
        // add attachments
        addAttachment(messageImpl);
        // update rmmessage
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, bis);

        assertStartsWith(rmmsg.getContent().getInputStream(), "\r\n--uuid:");
        assertNotNull(rmmsg.getContentType());
        assertTrue(rmmsg.getContentType().startsWith("multipart/related"));
    }

    @Test
    public void testEncodeDecodeRMContent() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(SOAP_PART.getBytes());
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, "text/xml");
        // add attachments
        addAttachment(messageImpl);
        // serialize
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, bis);

        Message messageImplRestored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, messageImplRestored);
        assertEquals(1, messageImplRestored.getAttachments().size());
        CachedOutputStream cos = (CachedOutputStream)messageImplRestored.get(RMMessageConstants.SAVED_CONTENT);
        assertStartsWith(cos.getInputStream(), SOAP_PART);
    }

    @Test
    public void testDecodeRMContentWithAttachment() throws Exception {
        InputStream is = getClass().getResourceAsStream("SerializedRMMessage.txt");
        CachedOutputStream cos = new CachedOutputStream();
        IOUtils.copyAndCloseInput(is, cos);
        cos.flush();
        RMMessage msg = new RMMessage();
        msg.setContent(cos);
        msg.setContentType(MULTIPART_TYPE);
        Message messageImpl = new MessageImpl();
        PersistenceUtils.decodeRMContent(msg, messageImpl);

        assertEquals(1, messageImpl.getAttachments().size());
        CachedOutputStream cos1 = (CachedOutputStream)messageImpl
            .get(RMMessageConstants.SAVED_CONTENT);
        assertStartsWith(cos1.getInputStream(), "<soap:Envelope");
    }

    private static void addAttachment(Message msg) throws IOException {
        Collection<Attachment> attachments = new ArrayList<>();
        DataHandler dh = new DataHandler(new ByteArrayDataSource("hello world!", "text/plain"));
        Attachment a = new AttachmentImpl("test.xml", dh);
        attachments.add(a);
        msg.setAttachments(attachments);
    }

    // just read the beginning of the input and compare it against the specified string
    private static boolean assertStartsWith(InputStream in, String starting) {
        assertNotNull(in);
        byte[] buf = new byte[starting.length()];
        try {
            in.read(buf, 0, buf.length);
            assertEquals(starting, new String(buf, StandardCharsets.UTF_8));
            in.close();
            return true;
        } catch (IOException e) {
            // ignore
        }
        return false;
    }
}