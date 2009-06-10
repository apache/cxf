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

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.Before;
import org.junit.Test;

public class MustUnderstandInterceptorTest extends TestBase {

    private static final QName RESERVATION = new QName("http://travelcompany.example.org/reservation",
                                                       "reservation");
    private static final QName PASSENGER = new QName("http://mycompany.example.com/employees", "passenger");

    private MustUnderstandInterceptor mui;
    private DummySoapInterceptor dsi;
    private ReadHeadersInterceptor rhi;
    private StartBodyInterceptor sbi;

    @Before
    public void setUp() throws Exception {

        super.setUp();
        
        Bus bus = BusFactory.getDefaultBus();

        rhi = new ReadHeadersInterceptor(bus, "phase1");
        chain.add(rhi);
        
        sbi = new StartBodyInterceptor("phase1.5");
        chain.add(sbi);
        
        mui = new MustUnderstandInterceptor("phase2");
        chain.add(mui);

        dsi = new DummySoapInterceptor("phase3");
        chain.add(dsi);
    }

    @Test
    public void testHandleMessageSucc() throws Exception {
        prepareSoapMessage("test-soap-header.xml");
        dsi.getUnderstoodHeaders().add(RESERVATION);
        dsi.getUnderstoodHeaders().add(PASSENGER);

        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        assertEquals("DummaySoapInterceptor getRoles has been called!", true, dsi.isCalledGetRoles());
        assertEquals("DummaySoapInterceptor getUnderstood has been called!", true, dsi
            .isCalledGetUnderstood());
    }

    @Test
    public void testHandleMessageFail() throws Exception {
        prepareSoapMessage("test-soap-header.xml");

        dsi.getUnderstoodHeaders().add(RESERVATION);

        soapMessage.getInterceptorChain().doIntercept(soapMessage);

        assertEquals("DummaySoapInterceptor getRoles has been called!", true, dsi.isCalledGetRoles());
        assertEquals("DummaySoapInterceptor getUnderstood has been called!", true, dsi
            .isCalledGetUnderstood());

        SoapFault ie = (SoapFault)soapMessage.getContent(Exception.class);
        if (ie == null) {
            fail("InBound Exception Missing! Exception should be Can't understands QNames: " + PASSENGER);
        } else {
            assertEquals(soapMessage.getVersion().getMustUnderstand(), ie.getFaultCode());
            assertTrue(ie.getMessage().toString().contains(PASSENGER.toString()));
        }
    }

    @Test
    public void testHandleMessageWithSoapHeader11Param() throws Exception {
        prepareSoapMessage("test-soap-header.xml");
        dsi.getUnderstoodHeaders().add(RESERVATION);
        
        ServiceInfo serviceInfo = getMockedServiceModel(getClass().getResource("test-soap-header.wsdl")
            .toString());

        BindingInfo binding = serviceInfo.getBinding(new QName("http://org.apache.cxf/headers",
                                                               "headerTesterSOAPBinding"));
        BindingOperationInfo bop = binding.getOperation(new QName("http://org.apache.cxf/headers",
                                                                  "inHeader"));
        soapMessage.getExchange().put(BindingOperationInfo.class, bop);

        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        assertEquals("DummaySoapInterceptor getRoles has been called!", true, dsi.isCalledGetRoles());
        assertEquals("DummaySoapInterceptor getUnderstood has been called!", true, dsi
            .isCalledGetUnderstood());
    }

    @Test
    public void testHandleMessageWithSoapHeader12Param() throws Exception {
        prepareSoapMessage("test-soap-12-header.xml");
        dsi.getUnderstoodHeaders().add(RESERVATION);
        ServiceInfo serviceInfo = getMockedServiceModel(getClass().getResource("test-soap-12-header.wsdl")
            .toString());

        BindingInfo binding = serviceInfo.getBinding(new QName("http://org.apache.cxf/headers",
                                                               "headerTesterSOAPBinding"));
        BindingOperationInfo bop = binding.getOperation(new QName("http://org.apache.cxf/headers",
                                                                  "inHeader"));
        soapMessage.getExchange().put(BindingOperationInfo.class, bop);

        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        assertEquals("DummaySoapInterceptor getRoles has been called!", true, dsi.isCalledGetRoles());
        assertEquals("DummaySoapInterceptor getUnderstood has been called!", true, dsi
            .isCalledGetUnderstood());
    }
    
    private void prepareSoapMessage(String payloadFileName) throws Exception {

        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        ByteArrayDataSource bads = new ByteArrayDataSource(this.getClass()
            .getResourceAsStream(payloadFileName), "Application/xop+xml");
        String cid = AttachmentUtil.createContentID("http://cxf.apache.org");
        soapMessage.setContent(Attachment.class, new AttachmentImpl(cid, new DataHandler(bads)));
        soapMessage.setContent(XMLStreamReader.class, XMLInputFactory.newInstance()
            .createXMLStreamReader(bads.getInputStream()));

    }

    private class DummySoapInterceptor extends AbstractSoapInterceptor {

        private boolean calledGetRoles;
        private boolean calledGetUnderstood;

        private Set<URI> roles = new HashSet<URI>();
        private Set<QName> understood = new HashSet<QName>();

        public DummySoapInterceptor() {
            super("");
        }
        public DummySoapInterceptor(String phase) {
            super(phase);
        }

        public void handleMessage(SoapMessage messageParam) {
        }

        public Set<URI> getRoles() {
            calledGetRoles = true;
            if (roles.size() == 0) {
                try {
                    roles.add(new URI("http://www.w3.org/2003/05/soap-envelope/role/next"));
                    roles.add(new URI("http://www.w3.org/2003/05/soap-envelope/role/none"));
                    roles.add(new URI("http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"));
                } catch (Exception e) {
                    return null;
                }
            }
            return roles;
        }

        public Set<QName> getUnderstoodHeaders() {
            calledGetUnderstood = true;
            return understood;
        }

        public boolean isCalledGetRoles() {
            return calledGetRoles;
        }

        public boolean isCalledGetUnderstood() {
            return calledGetUnderstood;
        }

    }

}
