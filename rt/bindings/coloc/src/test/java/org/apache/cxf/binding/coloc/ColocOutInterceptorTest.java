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
package org.apache.cxf.binding.coloc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ColocOutInterceptorTest {
    private static final String COLOCATED = Message.class.getName() + ".COLOCATED";
    private ColocOutInterceptor colocOut;
    private Message msg;
    private Exchange ex;

    @Before
    public void setUp() throws Exception {
        colocOut = new ColocOutInterceptor();
        msg = new MessageImpl();
        ex = new ExchangeImpl();
        msg.setExchange(ex);
    }

    @After
    public void tearDown() throws Exception {
        colocOut.setBus(null);
        BusFactory.setDefaultBus(null);
    }

    @Test
    public void testColocOutPhase() throws Exception {
        assertEquals(Phase.POST_LOGICAL, colocOut.getPhase());
    }

    @Test
    public void testColocOutInvalidBus() throws Exception {
        try {
            colocOut.handleMessage(msg);
            fail("Should have thrown a fault");
        } catch (Fault f) {
            assertEquals("Bus not created or not set as default bus.",
                         f.getMessage());
        }
    }

    @Test
    public void testColocOutInvalidServiceRegistry() throws Exception {

        setupBus();
        try {
            colocOut.handleMessage(msg);
            fail("Should have thrown a fault");
        } catch (Fault f) {
            assertEquals("Server Registry not registered with bus.",
                         f.getMessage());
        }
    }

    @Test
    public void testColocOutInvalidEndpoint() throws Exception {

        Bus bus = setupBus();
        ServerRegistry sr = mock(ServerRegistry.class);
        when(bus.getExtension(ServerRegistry.class)).thenReturn(sr);

        try {
            colocOut.handleMessage(msg);
            fail("Should have thrown a fault");
        } catch (Fault f) {
            assertEquals("Consumer Endpoint not found in exchange.",
                         f.getMessage());
        }
    }

    @Test
    public void testColocOutInvalidOperation() throws Exception {

        Bus bus = setupBus();
        ServerRegistry sr = mock(ServerRegistry.class);
        when(bus.getExtension(ServerRegistry.class)).thenReturn(sr);

        Endpoint ep = mock(Endpoint.class);
        ex.put(Endpoint.class, ep);

        try {
            colocOut.handleMessage(msg);
            fail("Should have thrown a fault");
        } catch (Fault f) {
            assertEquals("Operation not found in exchange.",
                         f.getMessage());
        }
    }

    @Test
    public void testColocOutIsColocated() throws Exception {
        verifyIsColocatedWithNullList();
        verifyIsColocatedWithEmptyList();
        verifyIsColocatedWithDifferentService();
        verifyIsColocatedWithDifferentEndpoint();
        verifyIsColocatedWithDifferentOperation();
        verifyIsColocatedWithSameOperation();
        verifyIsColocatedWithCompatibleOperation();
    }

    @Test
    public void testColocOutIsColocatedPropertySet() throws Exception {
        colocOut = new TestColocOutInterceptor1();

        Bus bus = setupBus();
        ServerRegistry sr = mock(ServerRegistry.class);
        when(bus.getExtension(ServerRegistry.class)).thenReturn(sr);

        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        ex.put(Endpoint.class, sep);
        QName op = new QName("E", "F");
        QName intf = new QName("G", "H");
        BindingInfo sbi = mock(BindingInfo.class);
        ServiceInfo ssi = new ServiceInfo();
        InterfaceInfo sii = new InterfaceInfo(ssi, intf);
        sii.addOperation(op);
        OperationInfo soi = sii.getOperation(op);
        ServiceInfo rsi = new ServiceInfo();
        InterfaceInfo rii = new InterfaceInfo(rsi, intf);
        rii.addOperation(op);
        OperationInfo roi = rii.getOperation(op);
        BindingOperationInfo sboi = mock(BindingOperationInfo.class);
        BindingOperationInfo rboi = mock(BindingOperationInfo.class);

        ex.put(BindingOperationInfo.class, sboi);
        //Local var
        Service ses = mock(Service.class);
        EndpointInfo sei = mock(EndpointInfo.class);

        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);
        BindingInfo rbi = mock(BindingInfo.class);
        EndpointInfo rei = mock(EndpointInfo.class);

        when(sr.getServers()).thenReturn(list);
        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(rep.getEndpointInfo()).thenReturn(rei);
        when(ses.getName()).thenReturn(new QName("A", "B"));
        when(res.getName()).thenReturn(new QName("A", "B"));
        when(rei.getName()).thenReturn(new QName("C", "D"));
        when(sei.getName()).thenReturn(new QName("C", "D"));
        when(rei.getBinding()).thenReturn(rbi);

        when(sboi.getName()).thenReturn(op);
        when(sboi.getOperationInfo()).thenReturn(soi);
        when(rboi.getName()).thenReturn(op);
        when(rboi.getOperationInfo()).thenReturn(roi);
        when(rbi.getOperation(op)).thenReturn(rboi);

        InterceptorChain chain = mock(InterceptorChain.class);
        msg.setInterceptorChain(chain);
        when(sboi.getBinding()).thenReturn(sbi);
        when(sbi.getInterface()).thenReturn(sii);

        colocOut.handleMessage(msg);
        assertTrue("COLOCATED property should be set", (Boolean)msg.get(COLOCATED));
        assertEquals("Message.WSDL_OPERATION property should be set",
                     op, msg.get(Message.WSDL_OPERATION));
        assertEquals("Message.WSDL_INTERFACE property should be set",
                     intf, msg.get(Message.WSDL_INTERFACE));
    }

    @Test
    public void testInvokeInboundChain() {
        //Reset Exchange on msg
        msg.setExchange(null);
        Bus bus = setupBus();
        colocOut.setBus(bus);
        PhaseManager pm = new PhaseManagerImpl();
        when(bus.getExtension(PhaseManager.class)).thenReturn(pm);

        Endpoint ep = mock(Endpoint.class);
        Binding bd = mock(Binding.class);
        Service srv = mock(Service.class);
        ex.setInMessage(msg);
        ex.put(Bus.class, bus);
        ex.put(Endpoint.class, ep);
        ex.put(Service.class, srv);

        when(ep.getBinding()).thenReturn(bd);
        when(bd.createMessage()).thenReturn(new MessageImpl());
        when(ep.getInInterceptors())
            .thenReturn(new ArrayList<Interceptor<? extends Message>>());
        when(ep.getService()).thenReturn(srv);
        when(srv.getInInterceptors())
            .thenReturn(new ArrayList<Interceptor<? extends Message>>());
        when(bus.getInInterceptors())
            .thenReturn(new ArrayList<Interceptor<? extends Message>>());

        colocOut.invokeInboundChain(ex, ep);
        Message inMsg = ex.getInMessage();
        assertNotSame(msg, inMsg);
        assertTrue("Requestor role should be set to true.",
                   (Boolean)inMsg.get(Message.REQUESTOR_ROLE));
        assertTrue("Inbound Message should be set to true.",
                   (Boolean)inMsg.get(Message.INBOUND_MESSAGE));
        assertNotNull("Inbound Message should have interceptor chain set.",
                      inMsg.getInterceptorChain());
        assertTrue("Client Invoke state should be FINISHED", (Boolean)ex.get(ClientImpl.FINISHED));
        
        verify(bus, times(2)).getExtension(PhaseManager.class);
        verify(ep, atLeastOnce()).getInInterceptors();
        verify(ep, atLeastOnce()).getService();
        verify(srv, atLeastOnce()).getInInterceptors();
        verify(bus, atLeastOnce()).getInInterceptors();

    }

    private void verifyIsColocatedWithNullList() {
        Server val = colocOut.isColocated(null, null, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
    }

    private void verifyIsColocatedWithEmptyList() {
        List<Server> list = new ArrayList<>();
        //Local var
        Endpoint sep = mock(Endpoint.class);
        Service ses = mock(Service.class);
        EndpointInfo sei = mock(EndpointInfo.class);

        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);

        Server val = colocOut.isColocated(list, sep, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
    }

    private void verifyIsColocatedWithDifferentService() {
        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        //Local var
        Service ses = mock(Service.class);

        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);

        when(sep.getService()).thenReturn(ses);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(ses.getName()).thenReturn(new QName("A", "C"));
        when(res.getName()).thenReturn(new QName("A", "B"));

        Server val = colocOut.isColocated(list, sep, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
    }

    private void verifyIsColocatedWithDifferentEndpoint() {
        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        BindingOperationInfo sboi = mock(BindingOperationInfo.class);
        //Local var
        Service ses = mock(Service.class);
        EndpointInfo sei = mock(EndpointInfo.class);

        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);
        EndpointInfo rei = mock(EndpointInfo.class);

        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(rep.getEndpointInfo()).thenReturn(rei);
        when(ses.getName()).thenReturn(new QName("A", "B"));
        when(res.getName()).thenReturn(new QName("A", "B"));
        when(rei.getName()).thenReturn(new QName("C", "D"));
        when(sei.getName()).thenReturn(new QName("C", "E"));

        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Is not a colocated call",
                     null,
                     val);
    }

    private void verifyIsColocatedWithDifferentOperation() {
        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        BindingOperationInfo sboi = mock(BindingOperationInfo.class);
        //Local var
        Service ses = mock(Service.class);
        ServiceInfo ssi = mock(ServiceInfo.class);
        EndpointInfo sei = mock(EndpointInfo.class);
        TestBindingInfo rbi = new TestBindingInfo(ssi, "testBinding");
        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);
        EndpointInfo rei = mock(EndpointInfo.class);

        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(rep.getEndpointInfo()).thenReturn(rei);
        when(ses.getName()).thenReturn(new QName("A", "B"));
        when(res.getName()).thenReturn(new QName("A", "B"));
        when(rei.getName()).thenReturn(new QName("C", "D"));
        when(sei.getName()).thenReturn(new QName("C", "D"));
        when(rei.getBinding()).thenReturn(rbi);
        when(sboi.getName()).thenReturn(new QName("E", "F"));
        //Causes ConcurrentModification intermittently
        //QName op = new QName("E", "F");
        //when(rbi.getOperation(op).thenReturn(null);

        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        assertEquals("BindingOperation.getOperation was not called",
                     1, rbi.getOpCount());
    }

    private void verifyIsColocatedWithSameOperation() {
        colocOut = new TestColocOutInterceptor1();
        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        BindingOperationInfo sboi = mock(BindingOperationInfo.class);

        //Local var
        Service ses = mock(Service.class);
        EndpointInfo sei = mock(EndpointInfo.class);
        BindingInfo rbi = mock(BindingInfo.class);
        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);
        EndpointInfo rei = mock(EndpointInfo.class);
        BindingOperationInfo rboi = mock(BindingOperationInfo.class);

        QName op = new QName("E", "F");
        QName intf = new QName("G", "H");
        QName inmi = new QName("M", "in");
        QName outmi = new QName("M", "out");
        ServiceInfo ssi = new ServiceInfo();
        InterfaceInfo sii = new InterfaceInfo(ssi, intf);
        sii.addOperation(op);
        OperationInfo soi = sii.getOperation(op);
        MessageInfo mii = new MessageInfo(soi, MessageInfo.Type.INPUT, inmi);
        MessageInfo mio = new MessageInfo(soi, MessageInfo.Type.OUTPUT, outmi);
        soi.setInput("in", mii);
        soi.setOutput("out", mio);

        ServiceInfo rsi = new ServiceInfo();
        InterfaceInfo rii = new InterfaceInfo(rsi, intf);
        rii.addOperation(op);
        OperationInfo roi = rii.getOperation(op);
        roi.setInput("in", mii);
        roi.setOutput("out", mio);

        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(rep.getEndpointInfo()).thenReturn(rei);
        when(ses.getName()).thenReturn(new QName("A", "B"));
        when(res.getName()).thenReturn(new QName("A", "B"));
        when(rei.getName()).thenReturn(new QName("C", "D"));
        when(sei.getName()).thenReturn(new QName("C", "D"));
        when(rei.getBinding()).thenReturn(rbi);

        when(sboi.getName()).thenReturn(op);
        when(sboi.getOperationInfo()).thenReturn(soi);
        when(rboi.getName()).thenReturn(op);
        when(rboi.getOperationInfo()).thenReturn(roi);
        when(rbi.getOperation(op)).thenReturn(rboi);

        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Expecting a colocated call",
                     s1,
                     val);
    }

    private void verifyIsColocatedWithCompatibleOperation() {
        colocOut = new TestColocOutInterceptor1();
        //Funtion Param
        Server s1 = mock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = mock(Endpoint.class);
        BindingOperationInfo sboi = mock(BindingOperationInfo.class);

        //Local var
        Service ses = mock(Service.class);
        EndpointInfo sei = mock(EndpointInfo.class);
        BindingInfo rbi = mock(BindingInfo.class);
        Endpoint rep = mock(Endpoint.class);
        Service res = mock(Service.class);
        EndpointInfo rei = mock(EndpointInfo.class);
        BindingOperationInfo rboi = mock(BindingOperationInfo.class);

        QName op = new QName("E", "F");
        QName intf = new QName("G", "H");
        QName inmi = new QName("M", "in");
        QName outmi = new QName("M", "out");
        ServiceInfo ssi = new ServiceInfo();
        InterfaceInfo sii = new InterfaceInfo(ssi, intf);
        sii.addOperation(op);
        OperationInfo soi = sii.getOperation(op);
        MessageInfo mii = new MessageInfo(soi, MessageInfo.Type.INPUT, inmi);
        MessagePartInfo mpi = mii.addMessagePart("parameters");
        mpi.setTypeClass(Source.class);
        MessageInfo mio = new MessageInfo(soi, MessageInfo.Type.OUTPUT, outmi);
        mpi = mio.addMessagePart("parameters");
        mpi.setTypeClass(Source.class);
        soi.setInput("in", mii);
        soi.setOutput("out", mio);

        ServiceInfo rsi = new ServiceInfo();
        InterfaceInfo rii = new InterfaceInfo(rsi, intf);
        rii.addOperation(op);
        OperationInfo roi = rii.getOperation(op);
        mii = new MessageInfo(roi, MessageInfo.Type.INPUT, inmi);
        mpi = mii.addMessagePart("parameters");
        mpi.setTypeClass(Object.class);
        mio = new MessageInfo(roi, MessageInfo.Type.OUTPUT, outmi);
        mpi = mio.addMessagePart("parameters");
        mpi.setTypeClass(Object.class);
        roi.setInput("in", mii);
        roi.setOutput("out", mio);

        when(sep.getService()).thenReturn(ses);
        when(sep.getEndpointInfo()).thenReturn(sei);
        when(s1.getEndpoint()).thenReturn(rep);
        when(rep.getService()).thenReturn(res);
        when(rep.getEndpointInfo()).thenReturn(rei);
        when(ses.getName()).thenReturn(new QName("A", "B"));
        when(res.getName()).thenReturn(new QName("A", "B"));
        when(rei.getName()).thenReturn(new QName("C", "D"));
        when(sei.getName()).thenReturn(new QName("C", "D"));
        when(rei.getBinding()).thenReturn(rbi);

        when(sboi.getName()).thenReturn(op);
        when(sboi.getOperationInfo()).thenReturn(soi);
        when(rboi.getName()).thenReturn(op);
        when(rboi.getOperationInfo()).thenReturn(roi);
        when(rbi.getOperation(op)).thenReturn(rboi);

        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Expecting a colocated call",
                     s1,
                     val);
    }

    private Bus setupBus() {
        Bus bus = mock(Bus.class);
        BusFactory.setDefaultBus(bus);
        return bus;
    }

    class TestColocOutInterceptor1 extends ColocOutInterceptor {
        public void invokeColocObserver(Message outMsg, Endpoint inboundEndpoint) {
            //No Op
        }

        public void invokeInboundChain(Exchange exchange, Endpoint ep) {
            //No Op
        }

    }

    class TestBindingInfo extends BindingInfo {
        private int opCount;
        TestBindingInfo(ServiceInfo si, String bindingId) {
            super(si, bindingId);
        }

        public int getOpCount() {
            return opCount;
        }

        public BindingOperationInfo getOperation(QName opName) {
            BindingOperationInfo boi = super.getOperation(opName);
            ++opCount;
            return boi;
        }

    }
}
