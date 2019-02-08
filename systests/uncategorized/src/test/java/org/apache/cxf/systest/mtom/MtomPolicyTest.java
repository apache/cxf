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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.cxf.ws.policy.selector.FirstAlternativeSelector;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MtomPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = TestUtil.getPortNumber(MtomPolicyTest.class);
    public static final String PORT2 = TestUtil.getPortNumber(MtomPolicyTest.class, 2);

    static TestUtilities testUtilities = new TestUtilities(MtomPolicyTest.class);

    @BeforeClass
    public static void createTheBus() throws Exception {
        createStaticBus();
        testUtilities.setBus(getStaticBus());
    }


    @Test
    public void testRequiredMtom() throws Exception {
        String address = "http://localhost:" + PORT + "/EchoService";
        setupServer(true, address);

        sendMtomMessage(address);

        Node res = testUtilities.invoke(address, "http://schemas.xmlsoap.org/soap/http", "nonmtom.xml");

        NodeList list = testUtilities.assertValid("//faultstring", res);
        String text = list.item(0).getTextContent();
        assertTrue(text.contains("These policy alternatives can not be satisfied: "));
        assertTrue(text.contains("{http://schemas.xmlsoap.org/ws/2004/09/policy/optimizedmimeserialization}"
                    + "OptimizedMimeSerialization"));
    }

    @Test
    public void testOptionalMtom() throws Exception {
        String address = "http://localhost:" + PORT2 + "/EchoService";
        setupServer(false, address);

        sendMtomMessage(address);

        Node res = testUtilities.invoke(address, "http://schemas.xmlsoap.org/soap/http", "nonmtom.xml");

        testUtilities.assertNoFault(res);
    }

    public void setupServer(boolean mtomRequired, String address) throws Exception {
        getStaticBus().getExtension(PolicyEngine.class).setAlternativeSelector(
            new FirstAlternativeSelector());
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new EchoService());
        sf.setBus(getStaticBus());
        sf.setAddress(address);

        WSPolicyFeature policyFeature = new WSPolicyFeature();
        List<Element> policyElements = new ArrayList<>();
        if (mtomRequired) {
            policyElements.add(StaxUtils.read(
                getClass().getResourceAsStream("mtom-policy.xml"))
                           .getDocumentElement());
        } else {
            policyElements.add(StaxUtils.read(
                getClass().getResourceAsStream("mtom-policy-optional.xml"))
                           .getDocumentElement());
        }
        policyFeature.setPolicyElements(policyElements);

        sf.getFeatures().add(policyFeature);

        sf.create();
    }

    private void sendMtomMessage(String a) throws Exception {
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/wsdl/http");
        ei.setAddress(a);

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


}
