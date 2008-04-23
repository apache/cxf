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
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.headers.coloc.types.FaultDetailT;
import org.apache.headers.coloc.types.InHeaderT;
import org.apache.headers.coloc.types.OutHeaderT;
import org.apache.headers.rpc_lit.PingMeFault;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ColocUtilTest extends Assert {
    private IMocksControl control = EasyMock.createNiceControl();
    private Bus bus;

    @Before
    public void setUp() throws Exception {
        bus = control.createMock(Bus.class);
        BusFactory.setDefaultBus(bus);
    }

    @After
    public void tearDown() throws Exception {
        BusFactory.setDefaultBus(null);
    }

    @Test
    public void testSetColocInPhases() throws Exception {
        PhaseManagerImpl phaseMgr = new PhaseManagerImpl();
        SortedSet<Phase> list = phaseMgr.getInPhases();
        int size1 = list.size();
        ColocUtil.setPhases(list, Phase.USER_LOGICAL, Phase.INVOKE);

        assertNotSame("The list size should not be same",
                      size1, list.size());
        assertEquals("Expecting Phase.USER_LOGICAL",
                     list.first().getName(),
                     Phase.USER_LOGICAL);
        assertEquals("Expecting Phase.POST_INVOKE",
                     list.last().getName(),
                     Phase.INVOKE);
    }

    @Test
    public void testSetColocOutPhases() throws Exception {
        PhaseManagerImpl phaseMgr = new PhaseManagerImpl();

        SortedSet<Phase> list = phaseMgr.getOutPhases();
        int size1 = list.size();
        ColocUtil.setPhases(list, Phase.SETUP, Phase.POST_LOGICAL);

        assertNotSame("The list size should not be same",
                      size1, list.size());
        assertEquals("Expecting Phase.SETUP",
                     list.first().getName(),
                     Phase.SETUP);
        assertEquals("Expecting Phase.POST_LOGICAL",
                     list.last().getName(),
                     Phase.POST_LOGICAL);

    }
    
    @Test
    public void testGetOutInterceptorChain() throws Exception {
        PhaseManagerImpl phaseMgr = new PhaseManagerImpl();
        SortedSet<Phase> list = phaseMgr.getInPhases();
        ColocUtil.setPhases(list, Phase.SETUP, Phase.POST_LOGICAL);
        
        Endpoint ep = control.createMock(Endpoint.class);
        Service srv = control.createMock(Service.class);
        Exchange ex = new ExchangeImpl();
        
        ex.put(Bus.class, bus);
        ex.put(Endpoint.class, ep);
        ex.put(Service.class, srv);
        
        EasyMock.expect(ep.getOutInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        EasyMock.expect(ep.getService()).andReturn(srv).atLeastOnce();
        EasyMock.expect(srv.getOutInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        EasyMock.expect(bus.getOutInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        
        control.replay();
        InterceptorChain chain = ColocUtil.getOutInterceptorChain(ex, list);
        control.verify();
        assertNotNull("Should have chain instance", chain);
        Iterator<Interceptor<? extends Message>> iter = chain.iterator();
        assertEquals("Should not have interceptors in chain",
                     false,
                     iter.hasNext());
    }

    @Test
    public void testGetInInterceptorChain() throws Exception {
        PhaseManagerImpl phaseMgr = new PhaseManagerImpl();
        SortedSet<Phase> list = phaseMgr.getInPhases();
        ColocUtil.setPhases(list, Phase.SETUP, Phase.POST_LOGICAL);
        
        Endpoint ep = control.createMock(Endpoint.class);
        Service srv = control.createMock(Service.class);
        Exchange ex = new ExchangeImpl();
        
        ex.put(Bus.class, bus);
        ex.put(Endpoint.class, ep);
        ex.put(Service.class, srv);
        
        EasyMock.expect(bus.getExtension(PhaseManager.class)).andReturn(phaseMgr);
        EasyMock.expect(ep.getInInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        EasyMock.expect(ep.getService()).andReturn(srv).atLeastOnce();
        EasyMock.expect(srv.getInInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        EasyMock.expect(bus.getInInterceptors()).andReturn(new ArrayList<Interceptor>()).atLeastOnce();
        
        control.replay();
        InterceptorChain chain = ColocUtil.getInInterceptorChain(ex, list);
        control.verify();
        assertNotNull("Should have chain instance", chain);
        Iterator<Interceptor<? extends Message>> iter = chain.iterator();
        assertEquals("Should not have interceptors in chain",
                     false,
                     iter.hasNext());
        assertNotNull("OutFaultObserver should be set", chain.getFaultObserver());
    }
    
    @Test
    public void testIsSameFaultInfo() {
        OperationInfo oi = control.createMock(OperationInfo.class);
        
        boolean match = ColocUtil.isSameFaultInfo(null, null);
        assertEquals("Should return true", true, match);
        List<FaultInfo> fil1 = new ArrayList<FaultInfo>();
        match = ColocUtil.isSameFaultInfo(fil1, null);
        assertEquals("Should not find a match", false, match);
        match = ColocUtil.isSameFaultInfo(null, fil1);
        assertEquals("Should not find a match", false, match);
        
        List<FaultInfo> fil2 = new ArrayList<FaultInfo>();
        match = ColocUtil.isSameFaultInfo(fil1, fil2);
        
        QName fn1 = new QName("A", "B");
        QName fn2 = new QName("C", "D");
        
        FaultInfo fi1 = new FaultInfo(fn1, null, oi);
        fi1.setProperty(Class.class.getName(), PingMeFault.class);
        fil1.add(fi1);
        FaultInfo fi2 = new FaultInfo(fn2, null, oi);
        fi2.setProperty(Class.class.getName(), FaultDetailT.class);
        match = ColocUtil.isSameFaultInfo(fil1, fil2);
        assertEquals("Should not find a match", false, match);

        FaultInfo fi3 = new FaultInfo(fn2, null, oi);
        fi3.setProperty(Class.class.getName(), PingMeFault.class);
        fil2.add(fi3);
        match = ColocUtil.isSameFaultInfo(fil1, fil2);
        assertEquals("Should find a match", true, match);
    }
    
    @Test
    public void testIsSameMessageInfo() {
        OperationInfo oi = control.createMock(OperationInfo.class);
        boolean match = ColocUtil.isSameMessageInfo(null, null);
        assertEquals("Should return true", true, match);
        QName mn1 = new QName("A", "B");
        QName mn2 = new QName("C", "D");

        MessageInfo mi1 = new MessageInfo(oi, MessageInfo.Type.INPUT, mn1);
        MessageInfo mi2 = new MessageInfo(oi, MessageInfo.Type.INPUT, mn2);
        match = ColocUtil.isSameMessageInfo(mi1, null);
        assertEquals("Should not find a match", false, match);
        match = ColocUtil.isSameMessageInfo(null, mi2);
        assertEquals("Should not find a match", false, match);
        
        MessagePartInfo mpi = new MessagePartInfo(new QName("", "B"), null);
        mpi.setTypeClass(InHeaderT.class);
        mi1.addMessagePart(mpi);
        
        mpi = new MessagePartInfo(new QName("", "D"), null);
        mpi.setTypeClass(OutHeaderT.class);
        mi2.addMessagePart(mpi);
        
        match = ColocUtil.isSameMessageInfo(mi1, mi2);
        assertEquals("Should not find a match", false, match);
        
        mpi.setTypeClass(InHeaderT.class);
        match = ColocUtil.isSameMessageInfo(mi1, mi2);
        assertEquals("Should find a match", true, match);
    }
    
}
