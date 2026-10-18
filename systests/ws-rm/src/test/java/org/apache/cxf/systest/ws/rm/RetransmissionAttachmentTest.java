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

package org.apache.cxf.systest.ws.rm;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the retransmission of messages with attachments, which are written by the
 * AttachmentOutInterceptor again for each retransmission.
 */
public class RetransmissionAttachmentTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(RetransmissionAttachmentTest.class);

    static final byte[] ATTACHMENT = "attachment content".getBytes(StandardCharsets.UTF_8);
    /** The Content-Type and the attachments of the greetMeOneWay requests received by the server */
    static final BlockingQueue<String> RECEIVED_CONTENT_TYPES = new LinkedBlockingQueue<>();
    static final BlockingQueue<byte[]> RECEIVED_ATTACHMENTS = new LinkedBlockingQueue<>();

    private static final String CONFIG = "/org/apache/cxf/systest/ws/rm/simple.xml";
    private static final String PERSISTENT_CONFIG = "/org/apache/cxf/systest/ws/rm/persistent.xml";
    private static final String DB_NAME = "rat-client";
    /** Each test uses its own in-memory database, as an in-memory database lives as long as the JVM */
    private static final AtomicInteger DB_COUNT = new AtomicInteger();

    private Bus bus;

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            Bus bus = new SpringBusFactory().createBus(CONFIG);
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            EndpointImpl ep = (EndpointImpl)Endpoint.create(new GreeterImpl());
            ep.getInInterceptors().add(new RequestRecorder());
            ep.publish("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        }
    }

    static class RequestRecorder extends AbstractPhaseInterceptor<Message> {
        RequestRecorder() {
            super(Phase.PRE_INVOKE);
        }

        public void handleMessage(Message message) {
            if (!isGreetMeOneWay(message)) {
                return;
            }
            RECEIVED_CONTENT_TYPES.add((String)message.get(Message.CONTENT_TYPE));
            if (message.getAttachments() != null) {
                for (Attachment a : message.getAttachments()) {
                    try (InputStream in = a.getDataHandler().getInputStream()) {
                        RECEIVED_ATTACHMENTS.add(in.readAllBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /** Adds a SwA attachment to the greetMeOneWay requests */
    static class AttachmentAdder extends AbstractPhaseInterceptor<Message> {
        AttachmentAdder() {
            super(Phase.PRE_LOGICAL);
        }

        public void handleMessage(Message message) {
            if (isGreetMeOneWay(message)) {
                List<Attachment> attachments = new ArrayList<>();
                attachments.add(new AttachmentImpl("attachment-1@cxf.apache.org",
                    new DataHandler(new ByteArrayDataSource(ATTACHMENT, "text/plain"))));
                message.setAttachments(attachments);
            }
        }
    }

    static boolean isGreetMeOneWay(Message message) {
        BindingOperationInfo boi = message.getExchange().getBindingOperationInfo();
        return boi != null && "greetMeOneWay".equals(boi.getName().getLocalPart());
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void clearReceived() {
        RECEIVED_CONTENT_TYPES.clear();
        RECEIVED_ATTACHMENTS.clear();
    }

    @After
    public void shutdownBus() {
        if (bus != null) {
            bus.shutdown(true);
            bus = null;
        }
    }

    @Test
    public void testAttachmentRetransmitted() throws Exception {
        Greeter greeter = createGreeter();
        ClientProxy.getClient(greeter).getOutInterceptors().add(new AttachmentAdder());

        greeter.greetMeOneWay("attachment");

        String contentType = RECEIVED_CONTENT_TYPES.poll(10, TimeUnit.SECONDS);
        assertNotNull("the retransmitted message was not received", contentType);
        assertTrue(contentType, contentType.startsWith("multipart/related; type=\"text/xml\";"));
        byte[] attachment = RECEIVED_ATTACHMENTS.poll(1, TimeUnit.SECONDS);
        assertNotNull("no attachment was received", attachment);
        assertArrayEquals(ATTACHMENT, attachment);
    }

    @Test
    public void testMtomRetransmitted() throws Exception {
        Greeter greeter = createGreeter();
        ((SOAPBinding)((BindingProvider)greeter).getBinding()).setMTOMEnabled(true);

        greeter.greetMeOneWay("mtom");

        String contentType = RECEIVED_CONTENT_TYPES.poll(10, TimeUnit.SECONDS);
        assertNotNull("the retransmitted message was not received", contentType);
        assertTrue(contentType, contentType.contains("start-info=\"text/xml\""));
    }

    /**
     * The first transmission is lost and the client is restarted before the message is retransmitted,
     * so the message is retransmitted after having been recovered from the persistent store.
     */
    @Test
    public void testAttachmentRetransmittedAfterRecovery() throws Exception {
        String persisted = retransmitAfterRecovery(false, "multipart/related; type=\"text/xml\";");
        // the content type of the root part must be the one of the SOAP message, not the multipart one
        assertTrue(persisted, persisted.contains("Content-Type: text/xml"));
        assertFalse(persisted, persisted.contains("multipart/related"));
    }

    /**
     * Same as testAttachmentRetransmittedAfterRecovery with MTOM, the recovered message must be
     * retransmitted as an XOP package again.
     */
    @Test
    public void testMtomAttachmentRetransmittedAfterRecovery() throws Exception {
        String persisted = retransmitAfterRecovery(true, "multipart/related; type=\"application/xop+xml\";");
        assertTrue(persisted,
            persisted.contains("Content-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\""));
        assertFalse(persisted, persisted.contains("multipart/related"));
    }

    /**
     * Sends a message with an attachment which is lost and persisted, then restarts the client, which
     * recovers and retransmits the message. Returns the persisted message.
     */
    private String retransmitAfterRecovery(boolean mtom, String expectedContentTypePrefix) throws Exception {
        String dbName = DB_NAME + "-" + DB_COUNT.getAndIncrement();

        // the first client loses the message, which is persisted with its attachment
        Greeter greeter = createPersistentGreeter(dbName, 60000L, true);
        ClientProxy.getClient(greeter).getOutInterceptors().add(new AttachmentAdder());
        ((SOAPBinding)((BindingProvider)greeter).getBinding()).setMTOMEnabled(mtom);
        greeter.greetMeOneWay("attachment");

        RMStore store = bus.getExtension(RMManager.class).getStore();
        assertNotNull(store);
        String id = RMUtils.getEndpointIdentifier(ClientProxy.getClient(greeter).getEndpoint());
        Collection<SourceSequence> sequences = store.getSourceSequences(id);
        assertEquals(1, sequences.size());
        Collection<RMMessage> messages = store.getMessages(sequences.iterator().next().getIdentifier(), true);
        assertEquals(1, messages.size());
        String persistedType = messages.iterator().next().getContentType();
        assertTrue(persistedType, persistedType.startsWith("multipart/related"));
        String persisted;
        try (InputStream in = messages.iterator().next().getContent().getInputStream()) {
            persisted = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue("the message must not be received before the restart", RECEIVED_CONTENT_TYPES.isEmpty());

        bus.shutdown(true);
        bus = null;

        // the second client recovers the message from the store and retransmits it
        createPersistentGreeter(dbName, 2000L, false);

        String contentType = RECEIVED_CONTENT_TYPES.poll(20, TimeUnit.SECONDS);
        assertNotNull("the recovered message was not retransmitted", contentType);
        assertTrue(contentType, contentType.startsWith(expectedContentTypePrefix));
        byte[] attachment = RECEIVED_ATTACHMENTS.poll(1, TimeUnit.SECONDS);
        assertNotNull("no attachment was received", attachment);
        assertArrayEquals(ATTACHMENT, attachment);
        assertTrue("exactly one attachment is expected", RECEIVED_ATTACHMENTS.isEmpty());
        return persisted;
    }

    /**
     * Creates a client whose first transmission of each application message is lost, so that
     * the server only receives the retransmission.
     */
    private Greeter createGreeter() throws Exception {
        bus = new SpringBusFactory().createBus(CONFIG);
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        bus.getExtension(RMManager.class).getConfiguration().setBaseRetransmissionInterval(2000L);
        MessageLossSimulator loser = new MessageLossSimulator();
        loser.setMode(-1);
        bus.getOutInterceptors().add(loser);

        Greeter greeter = new GreeterService().getGreeterPort();
        updateAddressPort(greeter, PORT);
        return greeter;
    }

    /**
     * Creates a client using the persistent store. The messages are recovered from the store when the
     * client is created, so the retransmission interval is set before.
     */
    private Greeter createPersistentGreeter(String dbName, long retransmissionInterval, boolean lose)
        throws Exception {
        System.setProperty("db.name", dbName);
        try {
            bus = new SpringBusFactory().createBus(PERSISTENT_CONFIG);
        } finally {
            System.clearProperty("db.name");
        }
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        bus.getExtension(RMManager.class).getConfiguration().setBaseRetransmissionInterval(retransmissionInterval);
        if (lose) {
            MessageLossSimulator loser = new MessageLossSimulator();
            loser.setMode(-1);
            bus.getOutInterceptors().add(loser);
        }
        Greeter greeter = new GreeterService().getGreeterPort();
        updateAddressPort(greeter, PORT);
        return greeter;
    }
}
