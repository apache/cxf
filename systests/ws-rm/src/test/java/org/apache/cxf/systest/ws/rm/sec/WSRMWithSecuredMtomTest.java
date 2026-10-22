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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.activation.DataHandler;
import javax.jws.Oneway;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.rm.MessageLossSimulator;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Exercises attachments created by JAXB marshalling, rather than pre-existing SwA attachments. */
@RunWith(Parameterized.class)
public class WSRMWithSecuredMtomTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(WSRMWithSecuredMtomTest.class);

    private static final String NAMESPACE = "urn:cxf:rm:secured-mtom";
    private static final String RM_CONFIG = "/org/apache/cxf/systest/ws/rm/simple.xml";
    private static final String PERSISTENT_CONFIG = "/org/apache/cxf/systest/ws/rm/persistent.xml";
    private static final byte[] DATA = "MTOM attachment content of CXF-6955".getBytes(StandardCharsets.UTF_8);
    private static final BlockingQueue<byte[]> RECEIVED = new LinkedBlockingQueue<>();
    private static final BlockingQueue<String> CONTENT_TYPES = new LinkedBlockingQueue<>();
    private static final AtomicInteger REJECTIONS = new AtomicInteger();
    private static final AtomicInteger DATABASES = new AtomicInteger();

    private final boolean stax;
    private final String action;
    private Bus bus;

    public WSRMWithSecuredMtomTest(boolean stax, String action) {
        this.stax = stax;
        this.action = action;
    }

    @Parameterized.Parameters(name = "stax={0}, action={1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {false, "Encrypt"}, {true, "Encrypt"},
            {false, "Signature"}, {true, "Signature"},
            {false, "Signature Encrypt"}, {true, "Signature Encrypt"},
            {false, "Encrypt Signature"}, {true, "Encrypt Signature"}
        });
    }

    @XmlRootElement(name = "upload", namespace = NAMESPACE)
    public static class Upload {
        private DataHandler data;

        @XmlMimeType("application/octet-stream")
        public DataHandler getData() {
            return data;
        }

        public void setData(DataHandler data) {
            this.data = data;
        }
    }

    @WebService(targetNamespace = NAMESPACE)
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public interface Receiver {
        @Oneway
        void upload(@WebParam(name = "upload", targetNamespace = NAMESPACE) Upload upload);
    }

    @WebService(endpointInterface = "org.apache.cxf.systest.ws.rm.sec.WSRMWithSecuredMtomTest$Receiver",
                targetNamespace = NAMESPACE)
    public static class ReceiverImpl implements Receiver {
        public void upload(Upload upload) {
            try (InputStream in = upload.getData().getInputStream()) {
                RECEIVED.add(in.readAllBytes());
            } catch (Exception e) {
                throw new Fault(e);
            }
        }
    }

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            Bus serverBus = new SpringBusFactory().createBus(RM_CONFIG);
            BusFactory.setDefaultBus(serverBus);
            setBus(serverBus);
            for (String action : new String[] {"Encrypt", "Signature", "Signature Encrypt", "Encrypt Signature"}) {
                EndpointImpl endpoint = new EndpointImpl(serverBus, new ReceiverImpl());
                endpoint.getProperties().put(Message.MTOM_ENABLED, Boolean.TRUE);
                endpoint.getInInterceptors().add(new AbstractPhaseInterceptor<Message>(Phase.RECEIVE) {
                    public void handleMessage(Message message) {
                        String type = (String)message.get(Message.CONTENT_TYPE);
                        if (type != null && type.startsWith("multipart/")) {
                            if (REJECTIONS.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                                throw new Fault(new IllegalStateException("simulated failure"));
                            }
                            CONTENT_TYPES.add(type);
                        }
                    }
                });
                Map<String, Object> props = new HashMap<>();
                props.put("action", action);
                props.put("decryptionPropFile", "bob.properties");
                props.put("signatureVerificationPropFile", "bob.properties");
                props.put("passwordCallbackClass", UTPasswordCallback.class.getName());
                props.put(ConfigurationConstants.EXPAND_XOP_INCLUDE, "false");
                endpoint.getInInterceptors().add(new WSS4JInInterceptor(props) {
                    @Override
                    public void handleMessage(SoapMessage message) {
                        String type = (String)message.get(Message.CONTENT_TYPE);
                        if (type != null && type.startsWith("multipart/")) {
                            super.handleMessage(message);
                        }
                    }
                });
                endpoint.publish(address(action));
            }
        }
    }

    @BeforeClass
    public static void startServer() throws Exception {
        assertTrue("server did not launch", launchServer(Server.class, true));
    }

    @Before
    public void clearReceived() {
        RECEIVED.clear();
        CONTENT_TYPES.clear();
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
    public void testWithoutLoss() throws Exception {
        createBus(null, false, 2000L);
        send(createClient(false));
        assertReceived();
    }

    @Test
    public void testRetransmittedTwice() throws Exception {
        REJECTIONS.set(1);
        createBus(null, true, 2000L);
        send(createClient(false));
        assertReceived();
    }

    @Test
    public void testPersistentStoreWithoutLoss() throws Exception {
        createBus(database(), false, 2000L);
        send(createClient(false));
        assertReceived();
    }

    @Test
    public void testRetransmittedTwiceAfterRecovery() throws Exception {
        String database = database();
        createBus(database, true, 60000L);
        send(createClient(true));
        assertTrue("the initial transmission must be lost", RECEIVED.isEmpty());
        shutdownBus();

        REJECTIONS.set(1);
        createBus(database, false, 2000L);
        bus.setProperty(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, 1);
        createClient(true);
        assertReceived();
    }

    private static String database() {
        return "wsrm-sec-mtom-" + DATABASES.getAndIncrement();
    }

    private static String address(String action) {
        return "http://localhost:" + PORT + "/" + action.replace(' ', '-');
    }

    private void createBus(String database, boolean loseFirst, long interval) {
        if (database == null) {
            bus = new SpringBusFactory().createBus(RM_CONFIG);
        } else {
            System.setProperty("db.name", database);
            try {
                bus = new SpringBusFactory().createBus(PERSISTENT_CONFIG);
            } finally {
                System.clearProperty("db.name");
            }
        }
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        bus.getExtension(RMManager.class).getConfiguration().setBaseRetransmissionInterval(interval);
        if (loseFirst) {
            MessageLossSimulator loser = new MessageLossSimulator();
            loser.setMode(-1);
            bus.getOutInterceptors().add(loser);
        }
    }

    private Receiver createClient(boolean recovery) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(Receiver.class);
        factory.setAddress(address(action));
        Receiver client = factory.create(Receiver.class);
        ((javax.xml.ws.soap.SOAPBinding)((BindingProvider)client).getBinding()).setMTOMEnabled(true);
        Map<String, Object> props = WSRMWithSecuredAttachmentsTest.createSecurityProperties(action);
        // Keep the XOP reference intact and secure its attachment explicitly.
        props.put(ConfigurationConstants.EXPAND_XOP_INCLUDE, "false");
        props.put(ConfigurationConstants.STORE_BYTES_IN_ATTACHMENT, "false");
        // Focus on attachment signing: streaming Body signing finalizes external references before
        // JAXB has created the MTOM attachments. The SwA tests also sign the Body.
        props.put(ConfigurationConstants.SIGNATURE_PARTS, "{Content}cid:Attachments;");
        Interceptor<? extends Message> security;
        if (stax) {
            WSS4JStaxOutInterceptor interceptor = recovery ? new WSS4JStaxOutInterceptor(props) {
                @Override
                public void handleMessage(SoapMessage message) {
                    if (isApplicationMessage(message)) {
                        super.handleMessage(message);
                    }
                }
            } : new WSS4JStaxOutInterceptor(props);
            interceptor.setAllowMTOM(true);
            security = interceptor;
        } else {
            security = recovery ? new WSS4JOutInterceptor(props) {
                @Override
                public void handleMessage(SoapMessage message) {
                    if (isApplicationMessage(message)) {
                        super.handleMessage(message);
                    }
                }
            } : new WSS4JOutInterceptor(props);
        }
        if (recovery) {
            ClientProxy.getClient(client).getEndpoint().getOutInterceptors().add(security);
        } else {
            ClientProxy.getClient(client).getOutInterceptors().add(security);
        }
        return client;
    }

    private static boolean isApplicationMessage(SoapMessage message) {
        AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, true);
        return maps != null && maps.getAction() != null
            && !RMContextUtils.isRMProtocolMessage(maps.getAction().getValue());
    }

    private static void send(Receiver client) {
        Upload upload = new Upload();
        upload.setData(new DataHandler(new ByteArrayDataSource(DATA, "application/octet-stream")));
        client.upload(upload);
    }

    private static void assertReceived() throws Exception {
        byte[] received = RECEIVED.poll(20, TimeUnit.SECONDS);
        assertNotNull("no MTOM payload was received", received);
        assertArrayEquals(DATA, received);
        String type = CONTENT_TYPES.poll(1, TimeUnit.SECONDS);
        assertNotNull("the request must be multipart", type);
        assertTrue("MTOM must remain enabled: " + type, type.contains("application/xop+xml"));
        assertTrue("exactly one payload is expected", RECEIVED.isEmpty());
    }
}
