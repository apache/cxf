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

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.cxf.ws.policy.selector.FirstAlternativeSelector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MtomPolicyTest extends AbstractCXFTest {
    
    String address = "http://localhost:9036/EchoService";
    
    @BeforeClass
    public static void setKeepAliveProperty() {
        TestUtilities.setKeepAliveSystemProperty(false);
    }
    
    @AfterClass
    public static void cleanKeepAliveProperty() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }
    
    @Test
    public void testRequiredMtom() throws Exception {
        setupServer(true);
        
        sendMtomMessage(address);
        
        Node res = invoke(address, "http://schemas.xmlsoap.org/soap/http", "nonmtom.xml");
        
        NodeList list = assertValid("//faultstring", res);
        String text = list.item(0).getTextContent();
        assertTrue(text.contains("These policy alternatives can not be satisfied: "));
        assertTrue(text.contains("{http://schemas.xmlsoap.org/ws/2004/09/policy/optimizedmimeserialization}"
                    + "OptimizedMimeSerialization"));
    }
    
    @Test
    public void testOptionalMtom() throws Exception {
        setupServer(false);
        
        sendMtomMessage(address);
        
        Node res = invoke(address, "http://schemas.xmlsoap.org/soap/http", "nonmtom.xml");
        
        assertNoFault(res);
    }
    
    public void setupServer(boolean mtomRequired) throws Exception {
        getBus().getExtension(PolicyEngine.class).setAlternativeSelector(
            new FirstAlternativeSelector());
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new EchoService());
        sf.setBus(getBus());
        sf.setAddress(address);
        
        WSPolicyFeature policyFeature = new WSPolicyFeature();
        List<Element> policyElements = new ArrayList<Element>();
        if (mtomRequired) {
            policyElements.add(DOMUtils.readXml(
                getClass().getResourceAsStream("mtom-policy.xml"))
                           .getDocumentElement());
        } else {
            policyElements.add(DOMUtils.readXml(
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

        ConduitInitiatorManager conduitMgr = getBus().getExtension(ConduitInitiatorManager.class);
        ConduitInitiator conduitInit = conduitMgr.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        Conduit conduit = conduitInit.getConduit(ei);

        TestMessageObserver obs = new TestMessageObserver();
        conduit.setMessageObserver(obs);

        Message m = new MessageImpl();
        String ct = "multipart/related; type=\"application/xop+xml\"; "
                    + "start=\"<soap.xml@xfire.codehaus.org>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";

        m.put(Message.CONTENT_TYPE, ct);
        conduit.prepare(m);

        OutputStream os = m.getContent(OutputStream.class);
        InputStream is = getResourceAsStream("request");
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(inAtt.getDataHandler().getInputStream(), out);
        out.close();
        assertEquals(37448, out.size());
    }

    @Override
    protected Bus createBus() throws BusException {
        return BusFactory.getDefaultBus();
    }

}
