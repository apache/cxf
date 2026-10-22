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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentSerializer;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheSizeExceededException;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
    public void testEncodeDecodeRMContentRestoresRootContentType() throws Exception {
        String soap12 = "application/soap+xml; action=\"urn:test\"";
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        // AttachmentOutInterceptor has already replaced the content type with the multipart one
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        messageImpl.put(RMMessageConstants.SAVED_CONTENT_TYPE, soap12);
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        assertTrue(rmmsg.getContentType().startsWith("multipart/related"));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        assertEquals(soap12, restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        assertEquals(soap12, restored.get(Message.CONTENT_TYPE));
    }

    @Test
    public void testEncodeRMContentNoAttachmentsUsesSavedContentType() throws Exception {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        messageImpl.put(RMMessageConstants.SAVED_CONTENT_TYPE, "text/xml");
        messageImpl.setAttachments(new ArrayList<>());
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        assertEquals("text/xml", rmmsg.getContentType());
        assertStartsWith(rmmsg.getContent().getInputStream(), "<soap:");

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
        assertEquals("text/xml", restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
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
        assertNotNull("SAVED_CONTENT_TYPE should be set after decode",
                       messageImpl.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        String restored = (String) messageImpl.get(Message.CONTENT_TYPE);
        assertFalse("Content-Type should not be multipart after decode",
                     restored.startsWith("multipart/"));
    }

    @Test
    public void testRecoveredAttachmentOnDiskCanBeReadRepeatedly() throws Exception {
        checkRecoveredAttachmentOnDisk(3);
    }

    @Test
    public void testRecoveredAttachmentOnDiskDiscardedWithoutReading() throws Exception {
        checkRecoveredAttachmentOnDisk(0);
    }

    @Test
    public void testRecoveredAttachmentCachesReleasedOnFailure() throws Exception {
        Message source = new MessageImpl();
        source.put(Message.CONTENT_TYPE, "text/xml");
        addAttachment(source);
        source.getAttachments().add(new AttachmentImpl("large.txt",
            new DataHandler(new ByteArrayDataSource("too large for the configured attachment limit", "text/plain"))));
        RMMessage persisted = new RMMessage();
        PersistenceUtils.encodeRMContent(persisted, source,
            new ByteArrayInputStream(SOAP_PART.getBytes(StandardCharsets.UTF_8)));

        File directory = temporaryFolder.newFolder();
        Message restored = new MessageImpl();
        restored.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 1);
        restored.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, directory);
        restored.put(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, 16L);
        IOException error = assertThrows(IOException.class,
            () -> PersistenceUtils.decodeRMContent(persisted, restored));
        assertTrue(error.getCause() instanceof CacheSizeExceededException);
        assertEquals("partial recovery must release the attachment caches", 0, directory.list().length);
    }

    @Test
    public void testTooManyRecoveredAttachmentsReportedAsIOException() throws Exception {
        Message source = new MessageImpl();
        source.put(Message.CONTENT_TYPE, "text/xml");
        Collection<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < AttachmentDeserializer.DEFAULT_ATTACHMENT_MAX_COUNT + 2; i++) {
            attachments.add(new AttachmentImpl("attachment-" + i,
                new DataHandler(new ByteArrayDataSource("attachment content", "text/plain"))));
        }
        source.setAttachments(attachments);
        RMMessage persisted = new RMMessage();
        PersistenceUtils.encodeRMContent(persisted, source,
            new ByteArrayInputStream(SOAP_PART.getBytes(StandardCharsets.UTF_8)));

        File directory = temporaryFolder.newFolder();
        Message restored = new MessageImpl();
        restored.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 1);
        restored.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, directory);
        IOException error = assertThrows(IOException.class,
            () -> PersistenceUtils.decodeRMContent(persisted, restored));
        assertTrue(error.getCause() instanceof RuntimeException);
        assertTrue(error.getCause().getCause() instanceof IOException);
        assertEquals("partial recovery must release the attachment caches", 0, directory.list().length);
    }

    private void checkRecoveredAttachmentOnDisk(int reads) throws Exception {
        File directory = temporaryFolder.newFolder();
        Message restored = new MessageImpl();
        restored.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 1);
        restored.put(AttachmentDeserializer.ATTACHMENT_DIRECTORY, directory);
        PersistenceUtils.decodeRMContent(encodeOutbound("text/xml", false), restored);
        Closeable cleanup = (Closeable)restored.get(RMMessageConstants.ATTACHMENTS_CLOSEABLE);
        try {
            assertTrue("the attachment must spill to disk", directory.list().length > 0);
            Attachment attachment = restored.getAttachments().iterator().next();
            for (int i = 0; i < reads; i++) {
                try (InputStream in = attachment.getDataHandler().getInputStream()) {
                    assertEquals("hello world!", IOUtils.toString(in));
                }
            }
        } finally {
            cleanup.close();
        }
        assertEquals("attachment cache files must be deleted on cleanup", 0, directory.list().length);
    }

    @Test
    public void testEncodeDecodeInboundPathViaPartHeaders() throws Exception {
        // Simulates the server (destination) side: inbound message has no SAVED_CONTENT_TYPE
        // but AttachmentDeserializer has set ATTACHMENT_PART_HEADERS with the root Content-Type
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        Map<String, List<String>> partHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        partHeaders.put("Content-Type", List.of("text/xml; charset=UTF-8"));
        messageImpl.put(AttachmentDeserializer.ATTACHMENT_PART_HEADERS, partHeaders);
        addAttachment(messageImpl);

        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        assertTrue("Persisted content should be multipart",
                    rmmsg.getContentType().startsWith("multipart/related"));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        String restoredCt = (String) restored.get(RMMessageConstants.SAVED_CONTENT_TYPE);
        assertNotNull("SAVED_CONTENT_TYPE should be set on decode", restoredCt);
        assertFalse("Restored type should not be multipart", restoredCt.startsWith("multipart/"));
        assertFalse("Charset should be stripped from restored type",
                     restoredCt.toLowerCase().contains("charset"));
        assertEquals("text/xml", restoredCt);
    }

    @Test
    public void testEncodeDecodeInboundSoap12WithActionAndCharset() throws Exception {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        Map<String, List<String>> partHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        partHeaders.put("Content-Type", List.of("application/soap+xml; charset=UTF-8; action=\"urn:test\""));
        messageImpl.put(AttachmentDeserializer.ATTACHMENT_PART_HEADERS, partHeaders);
        addAttachment(messageImpl);

        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        String restoredCt = (String) restored.get(RMMessageConstants.SAVED_CONTENT_TYPE);
        assertEquals("application/soap+xml; action=\"urn:test\"", restoredCt);
    }

    @Test
    public void testNoRootContentTypeDoesNotNestMultipart() throws Exception {
        // neither SAVED_CONTENT_TYPE nor the part headers are available, only a multipart type without
        // the type of its root part
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, "multipart/related; boundary=\"uuid:1\"");
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        assertEquals("text/xml", restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
    }

    @Test
    public void testRootPartEncodingIsPreserved() throws Exception {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        messageImpl.put(RMMessageConstants.SAVED_CONTENT_TYPE, "text/xml");
        messageImpl.put(Message.ENCODING, "ISO-8859-1");
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));

        String stored = IOUtils.toString(rmmsg.getContent().getInputStream());
        assertTrue(stored, stored.contains("charset=ISO-8859-1"));
        assertFalse(stored, stored.contains("charset=UTF-8"));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("ISO-8859-1", restored.get(Message.ENCODING));
        assertEquals("text/xml", restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
    }

    @Test
    public void testEncodeDecodeMtomRestoresXopPackage() throws Exception {
        RMMessage rmmsg = encodeOutbound("text/xml", true);
        assertTrue(rmmsg.getContentType(), rmmsg.getContentType().contains("type=\"application/xop+xml\""));
        String stored = IOUtils.toString(rmmsg.getContent().getInputStream());
        assertTrue(stored, stored.contains("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\""));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        assertEquals("text/xml", restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testEncodeDecodeMtomSoap12WithAction() throws Exception {
        String soap12 = "application/soap+xml; action=\"urn:test\"";
        RMMessage rmmsg = encodeOutbound(soap12, true);

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(soap12, restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testEncodeDecodeSwaDoesNotEnableMtom() throws Exception {
        RMMessage rmmsg = encodeOutbound("text/xml", false);
        assertFalse(rmmsg.getContentType(), rmmsg.getContentType().contains("application/xop+xml"));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("text/xml", restored.get(RMMessageConstants.SAVED_CONTENT_TYPE));
        assertNull(restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testEncodeDecodeInboundXopRootPart() throws Exception {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        Map<String, List<String>> partHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        partHeaders.put("Content-Type", List.of("application/xop+xml; charset=UTF-8; type=\"text/xml\""));
        messageImpl.put(AttachmentDeserializer.ATTACHMENT_PART_HEADERS, partHeaders);
        addAttachment(messageImpl);

        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testMtomContentTypeIsNotOutbound() throws Exception {
        // a stored message is only an XOP package if it was sent with MTOM, not if it was received with it
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        messageImpl.put(RMMessageConstants.SAVED_CONTENT_TYPE, "text/xml");
        messageImpl.put(Message.MTOM_ENABLED, Boolean.TRUE);
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        assertFalse(rmmsg.getContentType(), rmmsg.getContentType().contains("application/xop+xml"));
    }

    @Test
    public void testEncodeDecodeOutboundMtomWithoutSavedContentType() throws Exception {
        // the SOAP type is taken from the start-info of the multipart type written by AttachmentOutInterceptor
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, "multipart/related; type=\"application/xop+xml\";"
            + " boundary=\"uuid:1\"; start=\"<root.message@cxf.apache.org>\";"
            + " start-info=\"application/soap+xml; action=\\\"urn:test\\\"\"");
        messageImpl.put(Message.MTOM_ENABLED, Boolean.TRUE);
        setOutbound(messageImpl);
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        String stored = IOUtils.toString(rmmsg.getContent().getInputStream());
        assertTrue(stored, stored.contains("Content-Type: application/xop+xml; charset=UTF-8;"
            + " type=\"application/soap+xml; action=\\\"urn:test\\\"\""));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("application/soap+xml; action=\"urn:test\"", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testDecodeInboundXopRootWithCharsetInType() throws Exception {
        // the charset in the quoted type parameter is not a parameter of the XOP root part type
        Message restored = encodeDecodeInbound("application/xop+xml; type=\"application/soap+xml; charset=utf-8\"");
        assertEquals("application/soap+xml", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testDecodeInboundXopRootWithEscapedAction() throws Exception {
        Message restored = encodeDecodeInbound(
            "application/xop+xml; charset=UTF-8; type=\"application/soap+xml; action=\\\"urn:test\\\"\"");
        assertEquals("application/soap+xml; action=\"urn:test\"", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testInboundRootPartWithSeveralContentTypes() throws Exception {
        Message restored = encodeDecodeInbound("text/xml; charset=UTF-8", "application/soap+xml");
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
    }

    /** Decodes a stored message whose root part has the multipart type of the message as its type */
    @Test
    public void testDecodeSoap12MessageWithMultipartRootType() throws Exception {
        RMMessage rmmsg = encodeWithMultipartRootType("multipart/related; type=\"application/soap+xml\";"
            + " boundary=\"uuid:1\"; start=\"<root.message@cxf.apache.org>\";"
            + " start-info=\"application/soap+xml; action=\\\"urn:test\\\"\"");

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        assertEquals("application/soap+xml; action=\"urn:test\"", restored.get(Message.CONTENT_TYPE));
        assertNull(restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testDecodeSoap12MtomMessageWithMultipartRootType() throws Exception {
        RMMessage rmmsg = encodeWithMultipartRootType("multipart/related; type=\"application/xop+xml\";"
            + " boundary=\"uuid:1\"; start=\"<root.message@cxf.apache.org>\";"
            + " start-info=\"application/soap+xml; action=\\\"urn:test\\\"\"");

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("application/soap+xml; action=\"urn:test\"", restored.get(Message.CONTENT_TYPE));
        assertEquals(Boolean.TRUE, restored.get(Message.MTOM_ENABLED));
    }

    @Test
    public void testDecodeSoap11MessageWithMultipartRootType() throws Exception {
        RMMessage rmmsg = encodeWithMultipartRootType("multipart/related; type=\"text/xml\"; boundary=\"uuid:1\";"
            + " start=\"<root.message@cxf.apache.org>\"; start-info=\"text/xml\"");

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals("text/xml", restored.get(Message.CONTENT_TYPE));
        assertNull(restored.get(Message.MTOM_ENABLED));
    }

    /** Encodes and decodes an inbound message whose root part has the given Content-Type headers */
    private static Message encodeDecodeInbound(String... rootTypes) throws IOException {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        Map<String, List<String>> partHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        partHeaders.put("Content-Type", List.of(rootTypes));
        messageImpl.put(AttachmentDeserializer.ATTACHMENT_PART_HEADERS, partHeaders);
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));

        Message restored = new MessageImpl();
        PersistenceUtils.decodeRMContent(rmmsg, restored);
        assertEquals(1, restored.getAttachments().size());
        return restored;
    }

    /** Encodes a message with an attachment, with the given multipart type as the type of its root part */
    private static RMMessage encodeWithMultipartRootType(String multipartType) throws IOException {
        CachedOutputStream cos = new CachedOutputStream();
        Message msg = new MessageImpl();
        msg.setContent(OutputStream.class, cos);
        addAttachment(msg);
        msg.put(Message.CONTENT_TYPE, multipartType);
        AttachmentSerializer serializer = new AttachmentSerializer(msg);
        serializer.setXop(false);
        serializer.writeProlog();
        cos.write(SOAP_PART.getBytes(StandardCharsets.UTF_8));
        cos.flush();
        serializer.writeAttachments();
        RMMessage rmmsg = new RMMessage();
        rmmsg.setContentType((String)msg.get(Message.CONTENT_TYPE));
        rmmsg.setContent(cos);
        return rmmsg;
    }

    private static void setOutbound(Message msg) {
        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(msg);
    }

    /** Encodes a captured outbound message with an attachment, written with or without MTOM */
    private static RMMessage encodeOutbound(String soapContentType, boolean mtom) throws IOException {
        RMMessage rmmsg = new RMMessage();
        Message messageImpl = new MessageImpl();
        messageImpl.put(Message.CONTENT_TYPE, MULTIPART_TYPE);
        messageImpl.put(RMMessageConstants.SAVED_CONTENT_TYPE, soapContentType);
        if (mtom) {
            messageImpl.put(Message.MTOM_ENABLED, Boolean.TRUE);
        }
        setOutbound(messageImpl);
        addAttachment(messageImpl);
        PersistenceUtils.encodeRMContent(rmmsg, messageImpl, new ByteArrayInputStream(SOAP_PART.getBytes()));
        return rmmsg;
    }

    private static void addAttachment(Message msg) throws IOException {
        Collection<Attachment> attachments = new ArrayList<>();
        DataHandler dh = new DataHandler(new ByteArrayDataSource("hello world!", "text/plain"));
        Attachment a = new AttachmentImpl("test.xml", dh);
        attachments.add(a);
        msg.setAttachments(attachments);
    }

    // just read the beginning of the input and compare it against the specified string
    @SuppressWarnings("PMD.UnusedReturnValue")
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
