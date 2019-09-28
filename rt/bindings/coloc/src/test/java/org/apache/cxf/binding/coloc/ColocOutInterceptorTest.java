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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ColocOutInterceptorTest {
    private static final String COLOCATED = Message.class.getName() + ".COLOCATED";
    private IMocksControl control = EasyMock.createNiceControl();
    private ColocOutInterceptor colocOut;
    private Message msg;
    private Exchange ex;

    public ColocOutInterceptorTest() {
        control.makeThreadSafe(true);
    }

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
        ServerRegistry sr = control.createMock(ServerRegistry.class);
        EasyMock.expect(bus.getExtension(ServerRegistry.class)).andReturn(sr);

        control.replay();
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
        ServerRegistry sr = control.createMock(ServerRegistry.class);
        EasyMock.expect(bus.getExtension(ServerRegistry.class)).andReturn(sr);

        Endpoint ep = control.createMock(Endpoint.class);
        ex.put(Endpoint.class, ep);

        control.replay();
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
        ServerRegistry sr = control.createMock(ServerRegistry.class);
        EasyMock.expect(bus.getExtension(ServerRegistry.class)).andReturn(sr);

        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        ex.put(Endpoint.class, sep);
        QName op = new QName("E", "F");
        QName intf = new QName("G", "H");
        BindingInfo sbi = control.createMock(BindingInfo.class);
        ServiceInfo ssi = new ServiceInfo();
        InterfaceInfo sii = new InterfaceInfo(ssi, intf);
        sii.addOperation(op);
        OperationInfo soi = sii.getOperation(op);
        ServiceInfo rsi = new ServiceInfo();
        InterfaceInfo rii = new InterfaceInfo(rsi, intf);
        rii.addOperation(op);
        OperationInfo roi = rii.getOperation(op);
        BindingOperationInfo sboi = control.createMock(BindingOperationInfo.class);
        BindingOperationInfo rboi = control.createMock(BindingOperationInfo.class);

        ex.put(BindingOperationInfo.class, sboi);
        //Local var
        Service ses = control.createMock(Service.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);

        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);
        BindingInfo rbi = control.createMock(BindingInfo.class);
        EndpointInfo rei = control.createMock(EndpointInfo.class);

        EasyMock.expect(sr.getServers()).andReturn(list);
        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(rep.getEndpointInfo()).andReturn(rei);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(rei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(sei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(rei.getBinding()).andReturn(rbi);

        EasyMock.expect(sboi.getName()).andReturn(op).anyTimes();
        EasyMock.expect(sboi.getOperationInfo()).andReturn(soi);
        EasyMock.expect(rboi.getName()).andReturn(op).anyTimes();
        EasyMock.expect(rboi.getOperationInfo()).andReturn(roi);
        EasyMock.expect(rbi.getOperation(op)).andReturn(rboi);

        InterceptorChain chain = control.createMock(InterceptorChain.class);
        msg.setInterceptorChain(chain);
        EasyMock.expect(sboi.getBinding()).andReturn(sbi);
        EasyMock.expect(sbi.getInterface()).andReturn(sii);

        control.replay();
        colocOut.handleMessage(msg);
        assertTrue("COLOCATED property should be set", (Boolean)msg.get(COLOCATED));
        assertEquals("Message.WSDL_OPERATION property should be set",
                     op, msg.get(Message.WSDL_OPERATION));
        assertEquals("Message.WSDL_INTERFACE property should be set",
                     intf, msg.get(Message.WSDL_INTERFACE));

        control.verify();
    }

    @Test
    public void testInvokeInboundChain() {
        //Reset Exchange on msg
        msg.setExchange(null);
        Bus bus = setupBus();
        colocOut.setBus(bus);
        PhaseManager pm = new PhaseManagerImpl();
        EasyMock.expect(bus.getExtension(PhaseManager.class)).andReturn(pm).times(2);

        Endpoint ep = control.createMock(Endpoint.class);
        Binding bd = control.createMock(Binding.class);
        Service srv = control.createMock(Service.class);
        ex.setInMessage(msg);
        ex.put(Bus.class, bus);
        ex.put(Endpoint.class, ep);
        ex.put(Service.class, srv);

        EasyMock.expect(ep.getBinding()).andReturn(bd);
        EasyMock.expect(bd.createMessage()).andReturn(new MessageImpl());
        EasyMock.expect(ep.getInInterceptors())
            .andReturn(new ArrayList<Interceptor<? extends Message>>()).atLeastOnce();
        EasyMock.expect(ep.getService()).andReturn(srv).atLeastOnce();
        EasyMock.expect(srv.getInInterceptors())
            .andReturn(new ArrayList<Interceptor<? extends Message>>()).atLeastOnce();
        EasyMock.expect(bus.getInInterceptors())
            .andReturn(new ArrayList<Interceptor<? extends Message>>()).atLeastOnce();

        control.replay();
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
        control.verify();
    }

    private void verifyIsColocatedWithNullList() {
        Server val = colocOut.isColocated(null, null, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        control.reset();
    }

    private void verifyIsColocatedWithEmptyList() {
        List<Server> list = new ArrayList<>();
        //Local var
        Endpoint sep = control.createMock(Endpoint.class);
        Service ses = control.createMock(Service.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);

        control.replay();
        Server val = colocOut.isColocated(list, sep, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        control.reset();
    }

    private void verifyIsColocatedWithDifferentService() {
        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        //Local var
        Service ses = control.createMock(Service.class);

        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "C"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));

        control.replay();
        Server val = colocOut.isColocated(list, sep, null);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        control.reset();
    }

    private void verifyIsColocatedWithDifferentEndpoint() {
        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        BindingOperationInfo sboi = control.createMock(BindingOperationInfo.class);
        //Local var
        Service ses = control.createMock(Service.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);

        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);
        EndpointInfo rei = control.createMock(EndpointInfo.class);

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(rep.getEndpointInfo()).andReturn(rei);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(rei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(sei.getName()).andReturn(new QName("C", "E"));

        control.replay();
        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        control.reset();
    }

    private void verifyIsColocatedWithDifferentOperation() {
        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        BindingOperationInfo sboi = control.createMock(BindingOperationInfo.class);
        //Local var
        Service ses = control.createMock(Service.class);
        ServiceInfo ssi = control.createMock(ServiceInfo.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);
        TestBindingInfo rbi = new TestBindingInfo(ssi, "testBinding");
        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);
        EndpointInfo rei = control.createMock(EndpointInfo.class);

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(rep.getEndpointInfo()).andReturn(rei);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(rei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(sei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(rei.getBinding()).andReturn(rbi);
        EasyMock.expect(sboi.getName()).andReturn(new QName("E", "F"));
        //Causes ConcurrentModification intermittently
        //QName op = new QName("E", "F");
        //EasyMock.expect(rbi.getOperation(op).andReturn(null);

        control.replay();
        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Is not a colocated call",
                     null,
                     val);
        assertEquals("BindingOperation.getOperation was not called",
                     1, rbi.getOpCount());
        control.reset();
    }

    private void verifyIsColocatedWithSameOperation() {
        colocOut = new TestColocOutInterceptor1();
        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        BindingOperationInfo sboi = control.createMock(BindingOperationInfo.class);

        //Local var
        Service ses = control.createMock(Service.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);
        BindingInfo rbi = control.createMock(BindingInfo.class);
        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);
        EndpointInfo rei = control.createMock(EndpointInfo.class);
        BindingOperationInfo rboi = control.createMock(BindingOperationInfo.class);

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

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(rep.getEndpointInfo()).andReturn(rei);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(rei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(sei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(rei.getBinding()).andReturn(rbi);

        EasyMock.expect(sboi.getName()).andReturn(op);
        EasyMock.expect(sboi.getOperationInfo()).andReturn(soi);
        EasyMock.expect(rboi.getName()).andReturn(op);
        EasyMock.expect(rboi.getOperationInfo()).andReturn(roi);
        EasyMock.expect(rbi.getOperation(op)).andReturn(rboi);

        control.replay();
        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Expecting a colocated call",
                     s1,
                     val);
        control.reset();
    }

    private void verifyIsColocatedWithCompatibleOperation() {
        colocOut = new TestColocOutInterceptor1();
        //Funtion Param
        Server s1 = control.createMock(Server.class);
        List<Server> list = new ArrayList<>();
        list.add(s1);
        Endpoint sep = control.createMock(Endpoint.class);
        BindingOperationInfo sboi = control.createMock(BindingOperationInfo.class);

        //Local var
        Service ses = control.createMock(Service.class);
        EndpointInfo sei = control.createMock(EndpointInfo.class);
        BindingInfo rbi = control.createMock(BindingInfo.class);
        Endpoint rep = control.createMock(Endpoint.class);
        Service res = control.createMock(Service.class);
        EndpointInfo rei = control.createMock(EndpointInfo.class);
        BindingOperationInfo rboi = control.createMock(BindingOperationInfo.class);

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

        EasyMock.expect(sep.getService()).andReturn(ses);
        EasyMock.expect(sep.getEndpointInfo()).andReturn(sei);
        EasyMock.expect(s1.getEndpoint()).andReturn(rep);
        EasyMock.expect(rep.getService()).andReturn(res);
        EasyMock.expect(rep.getEndpointInfo()).andReturn(rei);
        EasyMock.expect(ses.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(res.getName()).andReturn(new QName("A", "B"));
        EasyMock.expect(rei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(sei.getName()).andReturn(new QName("C", "D"));
        EasyMock.expect(rei.getBinding()).andReturn(rbi);

        EasyMock.expect(sboi.getName()).andReturn(op);
        EasyMock.expect(sboi.getOperationInfo()).andReturn(soi);
        EasyMock.expect(rboi.getName()).andReturn(op);
        EasyMock.expect(rboi.getOperationInfo()).andReturn(roi);
        EasyMock.expect(rbi.getOperation(op)).andReturn(rboi);

        control.replay();
        Server val = colocOut.isColocated(list, sep, sboi);
        assertEquals("Expecting a colocated call",
                     s1,
                     val);
        control.reset();
    }

    private Bus setupBus() {
        Bus bus = control.createMock(Bus.class);
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
