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
package org.apache.cxf.systest.mtom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MtomServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT1 = TestUtil.getPortNumber(MtomServerTest.class);
    public static final String PORT2 = TestUtil.getPortNumber(MtomServerTest.class, 2);

    private static final String HTTP_ID = "http://schemas.xmlsoap.org/wsdl/http/";

    private static TestUtilities testUtilities = new TestUtilities(MtomPolicyTest.class);

    @BeforeClass
    public static void createTheBus() throws Exception {
        createStaticBus();
        testUtilities.setBus(getStaticBus());
    }

    @Test
    public void testMtomRequest() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new EchoService());
        sf.setBus(getStaticBus());
        String address = "http://localhost:" + PORT1 + "/EchoService";
        sf.setAddress(address);
        Map<String, Object> props = new HashMap<>();
        props.put(Message.MTOM_ENABLED, "true");
        sf.setProperties(props);
        sf.create();

        EndpointInfo ei = new EndpointInfo(null, HTTP_ID);
        ei.setAddress(address);

        ConduitInitiatorManager conduitMgr = getStaticBus().getExtension(ConduitInitiatorManager.class);
        ConduitInitiator conduitInit = conduitMgr.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        Conduit conduit = conduitInit.getConduit(ei, getStaticBus());

        TestUtilities.TestMessageObserver obs = new TestUtilities.TestMessageObserver();
        conduit.setMessageObserver(obs);

        Message m = new MessageImpl();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";

        m.put(Message.CONTENT_TYPE, ct);
        conduit.prepare(m);

        OutputStream os = m.getContent(OutputStream.class);
        InputStream is = testUtilities.getResourceAsStream("request");
        if (is == null) {
            throw new RuntimeException("Could not find resource " + "request");
        }

        IOUtils.copy(is, os);

        os.flush();
        is.close();
        os.close();

        byte[] res = obs.getResponseStream().toByteArray();
        MessageImpl resMsg = new MessageImpl();
        resMsg.setContent(InputStream.class, new ByteArrayInputStream(res));
        resMsg.put(Message.CONTENT_TYPE, obs.getResponseContentType());
        resMsg.setExchange(new ExchangeImpl());
        AttachmentDeserializer deserializer = new AttachmentDeserializer(resMsg);
        deserializer.initializeAttachments();

        Collection<Attachment> attachments = resMsg.getAttachments();
        assertNotNull(attachments);
        assertEquals(1, attachments.size());

        Attachment inAtt = attachments.iterator().next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(inAtt.getDataHandler().getInputStream(), out);
            assertEquals(27364, out.size());
        }
    }

    @Test
    public void testURLBasedAttachment() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new EchoService());
        sf.setBus(getStaticBus());
        String address = "http://localhost:" + PORT2 + "/EchoService";
        sf.setAddress(address);
        Map<String, Object> props = new HashMap<>();
        props.put(Message.MTOM_ENABLED, "true");
        sf.setProperties(props);
        Server server = sf.create();
        server.getEndpoint().getService().getDataBinding().setMtomThreshold(0);

        servStatic(getClass().getResource("mtom-policy.xml"),
                   "http://localhost:" + PORT2 + "/policy.xsd");

        EndpointInfo ei = new EndpointInfo(null, HTTP_ID);
        ei.setAddress(address);

        ConduitInitiatorManager conduitMgr = getStaticBus().getExtension(ConduitInitiatorManager.class);
        ConduitInitiator conduitInit = conduitMgr.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        Conduit conduit = conduitInit.getConduit(ei, getStaticBus());

        TestUtilities.TestMessageObserver obs = new TestUtilities.TestMessageObserver();
        conduit.setMessageObserver(obs);

        Message m = new MessageImpl();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";

        m.put(Message.CONTENT_TYPE, ct);
        conduit.prepare(m);

        OutputStream os = m.getContent(OutputStream.class);
        InputStream is = testUtilities.getResourceAsStream("request-url-attachment");
        if (is == null) {
            throw new RuntimeException("Could not find resource " + "request");
        }
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
            IOUtils.copy(is, bout);
            String s = bout.toString(StandardCharsets.UTF_8.name());
            s = s.replaceAll(":9036/", ":" + PORT2 + "/");

            os.write(s.getBytes(StandardCharsets.UTF_8));
        }
        os.flush();
        is.close();
        os.close();

        byte[] res = obs.getResponseStream().toByteArray();
        MessageImpl resMsg = new MessageImpl();
        resMsg.setContent(InputStream.class, new ByteArrayInputStream(res));
        resMsg.put(Message.CONTENT_TYPE, obs.getResponseContentType());
        resMsg.setExchange(new ExchangeImpl());
        AttachmentDeserializer deserializer = new AttachmentDeserializer(resMsg);
        deserializer.initializeAttachments();

        Collection<Attachment> attachments = resMsg.getAttachments();
        assertNotNull(attachments);
        assertEquals(1, attachments.size());

        Attachment inAtt = attachments.iterator().next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            IOUtils.copy(inAtt.getDataHandler().getInputStream(), out);
            assertTrue("Wrong size: " + out.size()
                    + "\n" + out.toString(),
                    out.size() > 970 && out.size() < 1020);
        }
        unregisterServStatic("http://localhost:" + PORT2 + "/policy.xsd");

    }

    private void unregisterServStatic(String add) throws Exception {
        Bus bus = getStaticBus();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm
            .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(add);

        Destination d = df.getDestination(ei, bus);
        d.setMessageObserver(null);

    }

    /**
     * Serve static file
     */
    private void servStatic(final URL resource,
                                   final String add) throws Exception {
        Bus bus = getStaticBus();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm
            .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(add);

        Destination d = df.getDestination(ei, bus);
        d.setMessageObserver(new MessageObserver() {

            public void onMessage(Message message) {
                try {
                    // HTTP seems to need this right now...
                    ExchangeImpl ex = new ExchangeImpl();
                    ex.setInMessage(message);

                    Conduit backChannel = message.getDestination().getBackChannel(message);

                    MessageImpl res = new MessageImpl();
                    ex.setOutMessage(res);
                    res.put(Message.CONTENT_TYPE, "text/xml");
                    backChannel.prepare(res);

                    OutputStream out = res.getContent(OutputStream.class);
                    InputStream is = resource.openStream();
                    IOUtils.copy(is, out, 2048);

                    out.flush();

                    out.close();
                    is.close();

                    backChannel.close(res);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }
}
