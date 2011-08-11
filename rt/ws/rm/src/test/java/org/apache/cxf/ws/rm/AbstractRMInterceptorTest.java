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

package org.apache.cxf.ws.rm;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class AbstractRMInterceptorTest extends Assert {

    private IMocksControl control;
    

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() {
        control.verify();
    }

    @Test
    public void testAccessors() {
        RMInterceptor interceptor = new RMInterceptor();
        assertEquals(Phase.PRE_LOGICAL, interceptor.getPhase());
        Bus bus = control.createMock(Bus.class);
        RMManager busMgr = control.createMock(RMManager.class);
        EasyMock.expect(bus.getExtension(RMManager.class)).andReturn(busMgr);
        RMManager mgr = control.createMock(RMManager.class);
        
        control.replay();
        assertNull(interceptor.getBus());
        interceptor.setBus(bus);
        assertSame(bus, interceptor.getBus());
        assertSame(busMgr, interceptor.getManager());
        interceptor.setManager(mgr);
        assertSame(mgr, interceptor.getManager());
    }
    
    @Test
    public void testHandleMessageSequenceFaultNoBinding() {
        RMInterceptor interceptor = new RMInterceptor();
        Message message = control.createMock(Message.class);
        SequenceFault sf = control.createMock(SequenceFault.class);
        interceptor.setSequenceFault(sf);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(ex);
        Endpoint e = control.createMock(Endpoint.class);
        EasyMock.expect(ex.get(Endpoint.class)).andReturn(e);
        EasyMock.expect(e.getBinding()).andReturn(null);
        control.replay();
        try {
            interceptor.handleMessage(message);
            fail("Expected Fault not thrown.");
        } catch (Fault f) {
            assertSame(sf, f.getCause());            
        }
    }
    
    @Test
    public void testHandleMessageSequenceFault() {
        RMInterceptor interceptor = new RMInterceptor();
        Message message = control.createMock(Message.class);
        SequenceFault sf = control.createMock(SequenceFault.class);
        interceptor.setSequenceFault(sf);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(ex);
        Endpoint e = control.createMock(Endpoint.class);
        EasyMock.expect(ex.get(Endpoint.class)).andReturn(e);
        Binding b = control.createMock(Binding.class);
        EasyMock.expect(e.getBinding()).andReturn(b);        
        RMManager mgr = control.createMock(RMManager.class);
        interceptor.setManager(mgr);
        BindingFaultFactory bff = control.createMock(BindingFaultFactory.class);
        EasyMock.expect(mgr.getBindingFaultFactory(b)).andReturn(bff);
        Fault fault = control.createMock(Fault.class);
        EasyMock.expect(bff.createFault(sf)).andReturn(fault);
        EasyMock.expect(bff.toString(fault)).andReturn("f");
        control.replay();
        try {
            interceptor.handleMessage(message);
            fail("Expected Fault not thrown.");
        } catch (Fault f) {
            assertSame(f, fault);            
        }
    }
    
    @Test
    public void testHandleMessageRMException() {
        RMInterceptor interceptor = new RMInterceptor();
        Message message = control.createMock(Message.class);
        RMException rme = control.createMock(RMException.class);      
        interceptor.setRMException(rme);
        control.replay();
        try {
            interceptor.handleMessage(message);
            fail("Expected Fault not thrown.");
        } catch (Fault f) {
            assertSame(rme, f.getCause());            
        }
    }
    
    @Test
    public void testAssertReliability() {
        RMInterceptor interceptor = new RMInterceptor();
        Message message = control.createMock(Message.class);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        AssertionInfoMap aim = control.createMock(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = new ArrayList<AssertionInfo>();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim).times(2);
        PolicyAssertion a = control.createMock(PolicyAssertion.class);        
        AssertionInfo ai = new AssertionInfo(a);
        EasyMock.expectLastCall();
        control.replay();
        interceptor.assertReliability(message);
        assertTrue(!ai.isAsserted());
        aim.put(RMConstants.getRMAssertionQName(), ais);
        interceptor.assertReliability(message);
        assertTrue(!ai.isAsserted());
        ais.add(ai);
        interceptor.assertReliability(message);   
        
    }

    class RMInterceptor extends AbstractRMInterceptor {

        private SequenceFault sequenceFault;
        private RMException rmException;
        
        void setSequenceFault(SequenceFault sf) {
            sequenceFault = sf;
        }
        
        void setRMException(RMException rme) {
            rmException = rme;
        }
        
        @Override
        protected void handle(Message msg) throws SequenceFault, RMException {
            if (null != sequenceFault) { 
                throw sequenceFault;
            } else if (null != rmException) {
                throw rmException;
            }
        }     
    }
}
