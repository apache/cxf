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
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that an inbound message with an attachment, which could not be delivered and is persisted
 * by the destination, is redelivered with its attachment and the SOAP content type after the server
 * has been restarted.
 */
public class ServerAttachmentRecoveryTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(ServerAttachmentRecoveryTest.class);

    private static final String SERVER_CONFIG = "/org/apache/cxf/systest/ws/rm/sync-ack-persistent-server.xml";
    private static final String CLIENT_CONFIG = "/org/apache/cxf/systest/ws/rm/simple.xml";
    private static final String DB_NAME = "sarec-server";
    /** Each test uses its own in-memory database, as an in-memory database lives as long as the JVM */
    private static final AtomicInteger DB_COUNT = new AtomicInteger();
    private static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

    private static final BlockingQueue<String> DELIVERED_CONTENT_TYPES = new LinkedBlockingQueue<>();
    private static final BlockingQueue<byte[]> DELIVERED_ATTACHMENTS = new LinkedBlockingQueue<>();

    private String dbName;
    private Bus serverBus;
    private Bus clientBus;
    private EndpointImpl endpoint;

    /** Fails the delivery or records the delivered message, depending on the state of the server. */
    static class DeliveryRecorder extends AbstractPhaseInterceptor<Message> {
        private final boolean fail;

        DeliveryRecorder(boolean fail) {
            super(Phase.PRE_INVOKE);
            this.fail = fail;
        }

        public void handleMessage(Message message) {
            if (!RetransmissionAttachmentTest.isGreetMeOneWay(message)) {
                return;
            }
            if (fail) {
                throw new RuntimeException("intentional delivery failure");
            }
            DELIVERED_CONTENT_TYPES.add((String)message.get(Message.CONTENT_TYPE));
            if (message.getAttachments() != null) {
                for (Attachment a : message.getAttachments()) {
                    try (InputStream in = a.getDataHandler().getInputStream()) {
                        DELIVERED_ATTACHMENTS.add(in.readAllBytes());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Before
    public void clearDelivered() throws Exception {
        dbName = DB_NAME + "-" + DB_COUNT.getAndIncrement();
        DELIVERED_CONTENT_TYPES.clear();
        DELIVERED_ATTACHMENTS.clear();
    }

    @After
    public void shutdownBuses() {
        stopServer();
        if (clientBus != null) {
            clientBus.shutdown(true);
            clientBus = null;
        }
    }

    @Test
    public void testAttachmentRedeliveredAfterServerRestart() throws Exception {
        startServer(true);
        Greeter greeter = createGreeter();
        ClientProxy.getClient(greeter).getOutInterceptors().add(new RetransmissionAttachmentTest.AttachmentAdder());
        greeter.greetMeOneWay("attachment");

        // the message is persisted by the destination although it could not be delivered
        RMManager manager = serverBus.getExtension(RMManager.class);
        RMStore store = manager.getStore();
        assertNotNull(store);
        String id = RMUtils.getEndpointIdentifier(endpoint.getServer().getEndpoint());
        RMMessage persisted = awaitPersistedMessage(store, id);
        assertTrue(persisted.getContentType(), persisted.getContentType().startsWith("multipart/related"));
        String content;
        try (InputStream in = persisted.getContent().getInputStream()) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // the content type of the root part is the one of the SOAP message and not a nested multipart type
        assertTrue(content, content.contains("Content-Type: text/xml"));
        assertFalse(content, content.contains("multipart/related"));
        assertTrue("the message must not be delivered before the restart", DELIVERED_CONTENT_TYPES.isEmpty());

        // restart the server, which recovers the message from the store and redelivers it
        stopServer();
        DELIVERED_CONTENT_TYPES.clear();
        DELIVERED_ATTACHMENTS.clear();
        startServer(false);

        String contentType = DELIVERED_CONTENT_TYPES.poll(30, TimeUnit.SECONDS);
        assertNotNull("the recovered message was not redelivered", contentType);
        assertFalse(contentType, contentType.startsWith("multipart/"));
        assertTrue(contentType, contentType.startsWith("text/xml"));
        byte[] attachment = DELIVERED_ATTACHMENTS.poll(1, TimeUnit.SECONDS);
        assertNotNull("the attachment was lost", attachment);
        assertArrayEquals(RetransmissionAttachmentTest.ATTACHMENT, attachment);
        assertTrue("exactly one attachment is expected", DELIVERED_ATTACHMENTS.isEmpty());
    }

    private RMMessage awaitPersistedMessage(RMStore store, String endpointId) throws Exception {
        for (int i = 0; i < 100; i++) {
            Collection<DestinationSequence> sequences = store.getDestinationSequences(endpointId);
            if (sequences.size() == 1) {
                Collection<RMMessage> messages = store.getMessages(sequences.iterator().next().getIdentifier(), false);
                if (messages.size() == 1) {
                    return messages.iterator().next();
                }
            }
            Thread.sleep(100);
        }
        fail("the message was not persisted by the destination");
        return null;
    }

    private void startServer(boolean fail) {
        System.setProperty("db.name", dbName);
        try {
            serverBus = new SpringBusFactory().createBus(SERVER_CONFIG);
        } finally {
            System.clearProperty("db.name");
        }
        RetryPolicyType retryPolicy = new RetryPolicyType();
        retryPolicy.setMaxRetries(-1);
        serverBus.getExtension(RMManager.class).getDestinationPolicy().setRetryPolicy(retryPolicy);
        serverBus.getExtension(RMManager.class).getConfiguration().setAcknowledgementInterval(0L);

        endpoint = new EndpointImpl(serverBus, new GreeterImpl());
        endpoint.getInInterceptors().add(new DeliveryRecorder(fail));
        endpoint.publish(ADDRESS);
    }

    private void stopServer() {
        if (endpoint != null) {
            endpoint.stop();
            endpoint = null;
        }
        if (serverBus != null) {
            serverBus.shutdown(true);
            serverBus = null;
        }
    }

    private Greeter createGreeter() throws Exception {
        clientBus = new SpringBusFactory().createBus(CLIENT_CONFIG);
        BusFactory.setDefaultBus(clientBus);
        BusFactory.setThreadDefaultBus(clientBus);
        // the client must not retransmit, only the server redelivers
        clientBus.getExtension(RMManager.class).getConfiguration().setBaseRetransmissionInterval(60000L);
        Greeter greeter = new GreeterService().getGreeterPort();
        updateAddressPort(greeter, PORT);
        return greeter;
    }
}
