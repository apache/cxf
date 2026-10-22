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

package org.apache.cxf.systest.ws.rm.sec;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.systest.ws.rm.GreeterImpl;
import org.apache.cxf.systest.ws.rm.MessageLossSimulator;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the WS-RM retransmission of messages whose attachments are encrypted or signed with WS-Security.
 * The security processing replaces the attachments with secured ones whose content can only be read once,
 * so each retransmission must secure the original attachments again. Each test is run with the DOM and
 * the StAX WS-Security interceptors on the client.
 */
public class WSRMWithSecuredAttachmentsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(WSRMWithSecuredAttachmentsTest.class);

    static final byte[] ATTACHMENT = "attachment content of CXF-6955".getBytes(StandardCharsets.UTF_8);
    /** The content of the attachments received by the server, after the security processing */
    static final BlockingQueue<byte[]> RECEIVED = new LinkedBlockingQueue<>();
    /** The number of the next requests with attachments the server rejects */
    static final AtomicInteger REJECTIONS = new AtomicInteger();

    private static final String RM_CONFIG = "/org/apache/cxf/systest/ws/rm/simple.xml";
    private static final String PERSISTENT_CONFIG = "/org/apache/cxf/systest/ws/rm/persistent.xml";
    private static final String DB_NAME = "wsrm-sec-att-client";
    /** Each test uses its own in-memory database, as an in-memory database lives as long as the JVM */
    private static final AtomicInteger DB_COUNT = new AtomicInteger();

    private Bus bus;

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            Bus bus = new SpringBusFactory().createBus(RM_CONFIG);
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            publish("encrypt", "Encrypt");
            publish("sign", "Signature");
            publish("none", null);
            publish("sign-encrypt", "Signature Encrypt");
            publish("encrypt-sign", "Encrypt Signature");
        }

        private static void publish(String path, String action) {
            EndpointImpl ep = (EndpointImpl)Endpoint.create(new GreeterImpl());
            ep.getInInterceptors().add(new Rejector());
            if (action != null) {
                Map<String, Object> props = new HashMap<>();
                props.put("action", action);
                props.put("decryptionPropFile", "bob.properties");
                props.put("signatureVerificationPropFile", "bob.properties");
                props.put("passwordCallbackClass", UTPasswordCallback.class.getName());
                ep.getInInterceptors().add(new AttachmentsWSS4JInInterceptor(props));
            }
            ep.getInInterceptors().add(new AttachmentRecorder());
            ep.publish("http://localhost:" + PORT + "/" + path);
        }
    }

    /**
     * Only processes the messages with attachments, as the RM protocol messages are not secured by the
     * client (its security interceptors are only on the application client).
     */
    static class AttachmentsWSS4JInInterceptor extends WSS4JInInterceptor {
        AttachmentsWSS4JInInterceptor(Map<String, Object> props) {
            super(props);
        }

        @Override
        public void handleMessage(SoapMessage message) {
            if (isMultipart(message)) {
                super.handleMessage(message);
            }
        }
    }

    /**
     * Only secures the messages with attachments, as the StAX interceptor fails on messages without the
     * attachments to secure, like the RM protocol messages of a recovery, which use the endpoint interceptors.
     */
    static class AttachmentsWSS4JStaxOutInterceptor extends WSS4JStaxOutInterceptor {
        AttachmentsWSS4JStaxOutInterceptor(Map<String, Object> props) {
            super(props);
        }

        @Override
        public void handleMessage(SoapMessage message) {
            if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                super.handleMessage(message);
            }
        }
    }

    /** Rejects the next REJECTIONS requests with attachments, so that the client retransmits them again */
    static class Rejector extends AbstractPhaseInterceptor<Message> {
        Rejector() {
            super(Phase.RECEIVE);
        }

        public void handleMessage(Message message) {
            if (isMultipart(message) && REJECTIONS.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                throw new Fault(new IllegalStateException("simulated failure"));
            }
        }
    }

    /** Records the content of the attachments received after the security processing */
    static class AttachmentRecorder extends AbstractPhaseInterceptor<Message> {
        AttachmentRecorder() {
            super(Phase.PRE_INVOKE);
        }

        public void handleMessage(Message message) {
            if (message.getAttachments() == null) {
                return;
            }
            for (Attachment a : message.getAttachments()) {
                try (InputStream in = a.getDataHandler().getInputStream()) {
                    RECEIVED.add(in.readAllBytes());
                } catch (Exception e) {
                    RECEIVED.add(("failed to read the attachment: " + e).getBytes(StandardCharsets.UTF_8));
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
            BindingOperationInfo boi = message.getExchange().getBindingOperationInfo();
            if (boi == null || !"greetMeOneWay".equals(boi.getName().getLocalPart())) {
                return;
            }
            List<Attachment> attachments = new ArrayList<>();
            attachments.add(new AttachmentImpl("attachment-1@cxf.apache.org",
                new DataHandler(new ByteArrayDataSource(ATTACHMENT, "text/plain"))));
            message.setAttachments(attachments);
        }
    }

    static boolean isMultipart(Message message) {
        String contentType = (String)message.get(Message.CONTENT_TYPE);
        return contentType != null && contentType.startsWith("multipart/related");
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void clearReceived() {
        RECEIVED.clear();
        REJECTIONS.set(0);
    }

    @After
    public void shutdownBus() {
        REJECTIONS.set(0);
        if (bus != null) {
            bus.shutdown(true);
            bus = null;
        }
    }

    @Test
    public void testPlainAttachmentRetransmitted() throws Exception {
        sendAndAssertReceived("none", null, false, true);
    }

    @Test
    public void testEncryptedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("encrypt", "Encrypt", false, false);
    }

    @Test
    public void testEncryptedAttachmentRetransmitted() throws Exception {
        sendAndAssertReceived("encrypt", "Encrypt", false, true);
    }

    @Test
    public void testEncryptedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("encrypt", "Encrypt", false, true);
    }

    @Test
    public void testSignedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("sign", "Signature", false, false);
    }

    @Test
    public void testSignedAttachmentRetransmitted() throws Exception {
        sendAndAssertReceived("sign", "Signature", false, true);
    }

    @Test
    public void testSignedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("sign", "Signature", false, true);
    }

    /** The message is persisted with its attachment while it is sent */
    @Test
    public void testEncryptedAttachmentWithPersistentStore() throws Exception {
        createPersistentBus(DB_NAME + "-" + DB_COUNT.getAndIncrement(), 2000L, false);
        sendAndAssertReceived("encrypt", "Encrypt", false);
    }

    @Test
    public void testSignedAttachmentWithPersistentStore() throws Exception {
        createPersistentBus(DB_NAME + "-" + DB_COUNT.getAndIncrement(), 2000L, false);
        sendAndAssertReceived("sign", "Signature", false);
    }

    @Test
    public void testEncryptedAttachmentRetransmittedAfterRecovery() throws Exception {
        sendRecoverAndAssertReceived("encrypt", "Encrypt", false);
    }

    @Test
    public void testSignedAttachmentRetransmittedAfterRecovery() throws Exception {
        sendRecoverAndAssertReceived("sign", "Signature", false);
    }

    /** The first retransmission of the recovered message is rejected, so it is retransmitted again */
    @Test
    public void testEncryptedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("encrypt", "Encrypt", false);
    }

    @Test
    public void testSignedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("sign", "Signature", false);
    }

    @Test
    public void testStaxEncryptedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("encrypt", "Encrypt", true, false);
    }

    @Test
    public void testStaxEncryptedAttachmentRetransmitted() throws Exception {
        sendAndAssertReceived("encrypt", "Encrypt", true, true);
    }

    @Test
    public void testStaxEncryptedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("encrypt", "Encrypt", true, true);
    }

    @Test
    public void testStaxSignedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("sign", "Signature", true, false);
    }

    @Test
    public void testStaxSignedAttachmentRetransmitted() throws Exception {
        sendAndAssertReceived("sign", "Signature", true, true);
    }

    @Test
    public void testStaxSignedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("sign", "Signature", true, true);
    }

    @Test
    public void testStaxEncryptedAttachmentWithPersistentStore() throws Exception {
        createPersistentBus(DB_NAME + "-" + DB_COUNT.getAndIncrement(), 2000L, false);
        sendAndAssertReceived("encrypt", "Encrypt", true);
    }

    @Test
    public void testStaxSignedAttachmentWithPersistentStore() throws Exception {
        createPersistentBus(DB_NAME + "-" + DB_COUNT.getAndIncrement(), 2000L, false);
        sendAndAssertReceived("sign", "Signature", true);
    }

    @Test
    public void testStaxEncryptedAttachmentRetransmittedAfterRecovery() throws Exception {
        sendRecoverAndAssertReceived("encrypt", "Encrypt", true);
    }

    @Test
    public void testStaxSignedAttachmentRetransmittedAfterRecovery() throws Exception {
        sendRecoverAndAssertReceived("sign", "Signature", true);
    }

    @Test
    public void testStaxEncryptedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("encrypt", "Encrypt", true);
    }

    @Test
    public void testStaxSignedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("sign", "Signature", true);
    }

    @Test
    public void testSignedThenEncryptedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("sign-encrypt", "Signature Encrypt", false, false);
    }

    @Test
    public void testSignedThenEncryptedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("sign-encrypt", "Signature Encrypt", false, true);
    }

    @Test
    public void testSignedThenEncryptedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("sign-encrypt", "Signature Encrypt", false);
    }

    @Test
    public void testEncryptedThenSignedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("encrypt-sign", "Encrypt Signature", false, false);
    }

    @Test
    public void testEncryptedThenSignedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("encrypt-sign", "Encrypt Signature", false, true);
    }

    @Test
    public void testEncryptedThenSignedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("encrypt-sign", "Encrypt Signature", false);
    }

    @Test
    public void testStaxSignedThenEncryptedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("sign-encrypt", "Signature Encrypt", true, false);
    }

    @Test
    public void testStaxSignedThenEncryptedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("sign-encrypt", "Signature Encrypt", true, true);
    }

    @Test
    public void testStaxSignedThenEncryptedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("sign-encrypt", "Signature Encrypt", true);
    }

    @Test
    public void testStaxEncryptedThenSignedAttachmentWithoutLoss() throws Exception {
        sendAndAssertReceived("encrypt-sign", "Encrypt Signature", true, false);
    }

    @Test
    public void testStaxEncryptedThenSignedAttachmentRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        sendAndAssertReceived("encrypt-sign", "Encrypt Signature", true, true);
    }

    @Test
    public void testStaxEncryptedThenSignedAttachmentRetransmittedTwiceAfterRecovery() throws Exception {
        REJECTIONS.set(1);
        sendRecoverAndAssertReceived("encrypt-sign", "Encrypt Signature", true);
    }

    /** Sends a message with an attachment, whose first transmission is lost if loseFirst is set */
    private void sendAndAssertReceived(String path, String action, boolean stax, boolean loseFirst)
        throws Exception {
        bus = new SpringBusFactory().createBus(RM_CONFIG);
        initBus(2000L, loseFirst);
        sendAndAssertReceived(path, action, stax);
    }

    private void sendAndAssertReceived(String path, String action, boolean stax) throws Exception {
        Greeter greeter = createGreeter(path);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(new AttachmentAdder());
        if (action != null) {
            client.getOutInterceptors().add(createSecurityInterceptor(action, stax));
        }

        greeter.greetMeOneWay("CXF-6955");

        assertAttachmentReceived(15);
    }

    /**
     * Sends a message with an attachment whose transmission is lost, then restarts the client, which
     * recovers the message from the persistent store and retransmits it.
     */
    private void sendRecoverAndAssertReceived(String path, String action, boolean stax) throws Exception {
        String dbName = DB_NAME + "-" + DB_COUNT.getAndIncrement();

        // the first client loses the message, which is persisted with its attachment
        createPersistentBus(dbName, 60000L, true);
        Greeter greeter = createGreeter(path);
        Client client = ClientProxy.getClient(greeter);
        client.getOutInterceptors().add(new AttachmentAdder());
        // on the endpoint, as the chain of a recovered message is built from the interceptors of the endpoint
        client.getEndpoint().getOutInterceptors().add(createRecoverySecurityInterceptor(action, stax));
        greeter.greetMeOneWay("CXF-6955");
        assertTrue("the message must not be received before the restart", RECEIVED.isEmpty());
        bus.shutdown(true);
        bus = null;

        // the second client recovers the message from the store and retransmits it
        createPersistentBus(dbName, 2000L, false);
        // Force recovered attachments onto disk, where closing a reader must not delete the retry data.
        bus.setProperty(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 1);
        greeter = createGreeter(path);
        ClientProxy.getClient(greeter).getEndpoint().getOutInterceptors()
            .add(createRecoverySecurityInterceptor(action, stax));

        assertAttachmentReceived(20);
    }

    private void createPersistentBus(String dbName, long retransmissionInterval, boolean lose) {
        System.setProperty("db.name", dbName);
        try {
            bus = new SpringBusFactory().createBus(PERSISTENT_CONFIG);
        } finally {
            System.clearProperty("db.name");
        }
        initBus(retransmissionInterval, lose);
    }

    private void initBus(long retransmissionInterval, boolean lose) {
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        bus.getExtension(RMManager.class).getConfiguration().setBaseRetransmissionInterval(retransmissionInterval);
        if (lose) {
            MessageLossSimulator loser = new MessageLossSimulator();
            loser.setMode(-1);
            bus.getOutInterceptors().add(loser);
        }
    }

    private static Greeter createGreeter(String path) {
        Greeter greeter = new GreeterService().getGreeterPort();
        // also updates the address of the endpoint, to which recovered messages are retransmitted
        TestUtil.setAddress(greeter, "http://localhost:" + PORT + "/" + path);
        return greeter;
    }

    private static Interceptor<? extends Message> createSecurityInterceptor(String action, boolean stax) {
        Map<String, Object> props = createSecurityProperties(action);
        return stax ? new WSS4JStaxOutInterceptor(props) : new WSS4JOutInterceptor(props);
    }

    private static Interceptor<? extends Message> createRecoverySecurityInterceptor(String action, boolean stax) {
        return stax ? new AttachmentsWSS4JStaxOutInterceptor(createSecurityProperties(action))
            : createSecurityInterceptor(action, false);
    }

    static Map<String, Object> createSecurityProperties(String action) {
        Map<String, Object> props = new HashMap<>();
        props.put("action", action);
        props.put("encryptionUser", "bob");
        props.put("encryptionPropFile", "bob.properties");
        props.put("encryptionParts", "{Content}cid:Attachments;");
        props.put("signatureUser", "alice");
        props.put("signaturePropFile", "alice.properties");
        props.put("signatureParts", "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;{Content}cid:Attachments;");
        props.put("signatureKeyIdentifier", "DirectReference");
        props.put("passwordCallbackClass", UTPasswordCallback.class.getName());
        return props;
    }

    private static void assertAttachmentReceived(int timeoutSeconds) throws InterruptedException {
        byte[] received = RECEIVED.poll(timeoutSeconds, TimeUnit.SECONDS);
        assertNotNull("no attachment was received", received);
        assertArrayEquals("received \"" + new String(received, StandardCharsets.UTF_8) + "\"", ATTACHMENT, received);
        assertTrue("exactly one attachment is expected", RECEIVED.isEmpty());
    }
}
