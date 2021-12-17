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

package org.apache.cxf.binding.soap;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReadHeaderInterceptorTest extends TestBase {

    private ReadHeadersInterceptor rhi;
    private StaxInInterceptor staxIntc = new StaxInInterceptor();
    private StartBodyInterceptor sbi;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        rhi = new ReadHeadersInterceptor(BusFactory.getDefaultBus(), "phase1");
        chain.add(rhi);
        sbi = new StartBodyInterceptor("phase1");
        chain.add(sbi);
        chain.add(new CheckFaultInterceptor("phase2"));
    }

    @Test
    public void testBadHttpVerb() throws Exception {
        prepareSoapMessage("test-soap-header.xml");
        soapMessage.put(Message.HTTP_REQUEST_METHOD, "OPTIONS");
        ReadHeadersInterceptor r = new ReadHeadersInterceptor(BusFactory.getDefaultBus());
        try {
            r.handleMessage(soapMessage);
            fail("Did not throw exception");
        } catch (Fault f) {
            assertEquals(405, f.getStatusCode());
        }
    }

    @Test
    public void testBadSOAPEnvelopeNamespace() throws Exception {
        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        InputStream in = getClass().getResourceAsStream("test-bad-env.xml");
        assertNotNull(in);
        ByteArrayDataSource bads = new ByteArrayDataSource(in, "test/xml");
        soapMessage.setContent(InputStream.class, bads.getInputStream());

        ReadHeadersInterceptor r = new ReadHeadersInterceptor(BusFactory.getDefaultBus());
        try {
            r.handleMessage(soapMessage);
            fail("Did not throw exception");
        } catch (SoapFault f) {
            assertEquals(Soap11.getInstance().getVersionMismatch(), f.getFaultCode());
        }
    }


    @Test
    public void testBadSOAPEnvelopeName() throws Exception {
        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        InputStream in = getClass().getResourceAsStream("test-bad-envname.xml");
        assertNotNull(in);
        ByteArrayDataSource bads = new ByteArrayDataSource(in, "test/xml");
        soapMessage.setContent(InputStream.class, bads.getInputStream());

        ReadHeadersInterceptor r = new ReadHeadersInterceptor(BusFactory.getDefaultBus());
        try {
            r.handleMessage(soapMessage);
            fail("Did not throw exception");
        } catch (SoapFault f) {
            assertEquals(Soap11.getInstance().getSender(), f.getFaultCode());
        }
    }

    @Test
    public void testNoClosingEnvTage() throws Exception {
        assertTrue(testNoClosingEnvTag(Boolean.TRUE));
    }

    @Test
    public void testNoClosingEnvTagValidationTypeBoth() throws Exception {
        assertTrue(testNoClosingEnvTag(SchemaValidationType.BOTH));
    }

    @Test
    public void testNoClosingEnvTagValidationTypeIn() throws Exception {
        assertTrue(testNoClosingEnvTag(SchemaValidationType.IN));
    }

    @Test
    public void testNoClosingEnvTagValidationTypeOut() throws Exception {
        assertFalse(testNoClosingEnvTag(SchemaValidationType.OUT));
    }

    @Test
    public void testNoClosingEnvTagValidationTypeNone() throws Exception {
        assertFalse(testNoClosingEnvTag(SchemaValidationType.NONE));
    }

    @Test
    public void testNoClosingEnvTagValidationTypeFalse() throws Exception {
        assertFalse(testNoClosingEnvTag(Boolean.FALSE));
    }

    private boolean testNoClosingEnvTag(Object validationType) {
        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        InputStream in = getClass().getResourceAsStream("test-no-endenv.xml");
        assertNotNull(in);

        soapMessage.put(Message.SCHEMA_VALIDATION_ENABLED, validationType);
        soapMessage.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(in));

        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        return soapMessage.getContent(Exception.class) != null;
    }

    @Test
    public void testHandleHeader() {
        try {
            prepareSoapMessage("test-soap-header.xml");
        } catch (IOException ioe) {
            fail("Failed in creating soap message");
        }

        staxIntc.handleMessage(soapMessage);
        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        // check the xmlReader should be placed on the first entry of the body element
        XMLStreamReader xmlReader = soapMessage.getContent(XMLStreamReader.class);
        assertEquals("check the first entry of body", "itinerary", xmlReader.getLocalName());

        List<Header> eleHeaders = soapMessage.getHeaders();

        List<Element> headerChilds = new ArrayList<>();
        Iterator<Header> iter = eleHeaders.iterator();
        while (iter.hasNext()) {
            Header hdr = iter.next();

            if (hdr.getObject() instanceof Element) {
                headerChilds.add((Element) hdr.getObject());
            }
        }
        assertEquals(2, headerChilds.size());
        for (int i = 0; i < headerChilds.size(); i++) {
            Element ele = headerChilds.get(i);
            if ("reservation".equals(ele.getLocalName())) {
                Element reservation = ele;
                List<Element> reservationChilds = new ArrayList<>();
                Element elem = DOMUtils.getFirstElement(reservation);
                while (elem != null) {
                    reservationChilds.add(elem);
                    elem = DOMUtils.getNextElement(elem);
                }
                assertEquals(2, reservationChilds.size());
                assertEquals("reference", reservationChilds.get(0).getLocalName());
                assertEquals("uuid:093a2da1-q345-739r-ba5d-pqff98fe8j7d", reservationChilds.get(0)
                    .getTextContent());
                assertEquals("dateAndTime", reservationChilds.get(1).getLocalName());
                assertEquals("2001-11-29T13:20:00.000-05:00", reservationChilds.get(1)
                    .getTextContent());

            }
            if ("passenger".equals(ele.getLocalName())) {
                Element passenger = ele;
                assertNotNull(passenger);
                Element child = DOMUtils.getFirstElement(passenger);
                assertNotNull("passenger should have a child element", child);
                assertEquals("name", child.getLocalName());
                assertEquals("Bob", child.getTextContent());
            }

        }
    }

    private void prepareSoapMessage(String message) throws IOException {

        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        ByteArrayDataSource bads = new ByteArrayDataSource(this.getClass().getResourceAsStream(message),
                                                           "Application/xop+xml");
        String cid = AttachmentUtil.createContentID("http://cxf.apache.org");
        soapMessage.setContent(Attachment.class, new AttachmentImpl(cid, new DataHandler(bads)));
        soapMessage.setContent(InputStream.class, bads.getInputStream());

    }
}