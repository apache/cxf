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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
@SuppressWarnings("PMD.UselessPureMethodCall")
public class AbstractRMInterceptorTest {
    @Test
    public void testAccessors() {
        RMInterceptor interceptor = new RMInterceptor();
        assertEquals(Phase.PRE_LOGICAL, interceptor.getPhase());
        Bus bus = mock(Bus.class);
        RMManager busMgr = mock(RMManager.class);
        when(bus.getExtension(RMManager.class)).thenReturn(busMgr);
        RMManager mgr = mock(RMManager.class);

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
        Message message = mock(Message.class);
        SequenceFault sf = mock(SequenceFault.class);
        interceptor.setSequenceFault(sf);
        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        Endpoint e = mock(Endpoint.class);
        when(ex.getEndpoint()).thenReturn(e);
        when(e.getBinding()).thenReturn(null);

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
        Message message = mock(Message.class);
        SequenceFault sf = mock(SequenceFault.class);
        interceptor.setSequenceFault(sf);
        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        Endpoint e = mock(Endpoint.class);
        when(ex.getEndpoint()).thenReturn(e);
        Binding b = mock(Binding.class);
        when(e.getBinding()).thenReturn(b);
        RMManager mgr = mock(RMManager.class);
        interceptor.setManager(mgr);
        BindingFaultFactory bff = mock(BindingFaultFactory.class);
        when(mgr.getBindingFaultFactory(b)).thenReturn(bff);
        Fault fault = mock(Fault.class);
        when(bff.createFault(sf, message)).thenReturn(fault);
        when(bff.toString(fault)).thenReturn("f");

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
        Message message = mock(Message.class);
        RMException rme = mock(RMException.class);
        interceptor.setRMException(rme);

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
        Message message = mock(Message.class);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);
        AssertionInfoMap aim = mock(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = new ArrayList<>();
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        PolicyAssertion a = mock(PolicyAssertion.class);
        AssertionInfo ai = new AssertionInfo(a);

        interceptor.assertReliability(message);
        assertFalse(ai.isAsserted());
        aim.put(RM10Constants.RMASSERTION_QNAME, ais);
        interceptor.assertReliability(message);
        assertFalse(ai.isAsserted());
        verify(message, times(2)).get(AssertionInfoMap.class);

        ais.add(ai);
        interceptor.assertReliability(message);

        verify(message, times(3)).get(AssertionInfoMap.class);
    }

    class RMInterceptor extends AbstractRMInterceptor<Message> {

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
