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

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyOperator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EndpointPolicyImplTest {
    final class TestEndpointPolicy extends EndpointPolicyImpl {
        @Override
        protected EndpointPolicyImpl createEndpointPolicy() {
            return new TestEndpointPolicy();
        }

        @Override
        void finalizeConfig(Message m) {
        }
    };

    private List<Interceptor<? extends Message>> createMockInterceptorList() {
        @SuppressWarnings("unchecked")
        Interceptor<? extends Message> i = mock(Interceptor.class);
        Interceptor<? extends Message> m = i;
        List<Interceptor<? extends Message>> a = new ArrayList<>();
        a.add(m);
        return a;
    }

    @Test
    public void testAccessors() {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();
        Message m = new MessageImpl();
        assertNull(epi.getPolicy());
        assertNull(epi.getChosenAlternative());
        assertNull(epi.getInterceptors(m));
        assertNull(epi.getFaultInterceptors(m));

        Policy p = mock(Policy.class);
        Assertion a = mock(Assertion.class);
        List<Assertion> la = Collections.singletonList(a);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();

        epi.setPolicy(p);
        assertSame(p, epi.getPolicy());
        epi.setChosenAlternative(la);
        assertSame(la, epi.getChosenAlternative());
        epi.setInterceptors(li);
        assertSame(li, epi.getInterceptors(m));
        epi.setFaultInterceptors(li);
        assertSame(li, epi.getFaultInterceptors(m));
        epi.setVocabulary(la);
        assertSame(la, epi.getVocabulary(m));
        epi.setFaultVocabulary(la);
        assertSame(la, epi.getFaultVocabulary(m));
    }

    @Test
    public void testInitialize() throws NoSuchMethodException {
        Message m = new MessageImpl();

        EndpointInfo ei = mock(EndpointInfo.class);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        ServiceInfo si = mock(ServiceInfo.class);
        when(ei.getService()).thenReturn(si);
        when(engine.getAggregatedServicePolicy(si, m)).thenReturn(new Policy());
        when(engine.getAggregatedEndpointPolicy(ei, m)).thenReturn(new Policy());

        EndpointPolicyImpl epi = spy(new EndpointPolicyImpl(ei, engine, false, null));

        epi.initializePolicy(m);
        epi.checkExactlyOnes();
        epi.chooseAlternative(m);

        epi.initialize(m);
    }

    @Test
    public void testInitializePolicy() {
        EndpointInfo ei = mock(EndpointInfo.class);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        ServiceInfo si = mock(ServiceInfo.class);
        when(ei.getService()).thenReturn(si);
        Policy sp = mock(Policy.class);
        when(engine.getAggregatedServicePolicy(si, null)).thenReturn(sp);
        Policy ep = mock(Policy.class);
        when(engine.getAggregatedEndpointPolicy(ei, null)).thenReturn(ep);
        Policy merged = mock(Policy.class);
        when(sp.merge(ep)).thenReturn(merged);
        when(merged.normalize(null, true)).thenReturn(merged);

        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, true, null);
        epi.initializePolicy(null);
        assertSame(merged, epi.getPolicy());
    }

    @Test
    public void testChooseAlternative() {
        Policy policy = new Policy();

        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        Assertor assertor = mock(Assertor.class);
        AlternativeSelector selector = mock(AlternativeSelector.class);

        Message m = new MessageImpl();
        EndpointPolicyImpl epi = new EndpointPolicyImpl(null, engine, true, assertor);
        epi.setPolicy(policy);

        when(engine.isEnabled()).thenReturn(true);
        when(engine.getAlternativeSelector()).thenReturn(selector);
        when(selector.selectAlternative(policy, engine, assertor, null, m)).thenReturn(null);

        try {
            epi.chooseAlternative(m);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }

        when(engine.isEnabled()).thenReturn(true);
        when(engine.getAlternativeSelector()).thenReturn(selector);
        Collection<Assertion> alternative = new ArrayList<>();
        when(selector.selectAlternative(policy, engine, assertor, null, m)).thenReturn(alternative);

        epi.chooseAlternative(m);
        Collection<Assertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);

        when(engine.isEnabled()).thenReturn(false);
        when(engine.getAlternativeSelector()).thenReturn(null);

        try {
            epi.chooseAlternative(m);
        } catch (Exception ex) {
            // no NPE expected
            fail("No Exception expected: " + ex);
        }
        choice = epi.getChosenAlternative();
        assertTrue("not an empty list", choice != null && choice.isEmpty());
    }

    private MessageImpl createMessage() {
        MessageImpl m = new MessageImpl();
        Exchange ex = new ExchangeImpl();
        m.setExchange(ex);
        return m;
    }
    @Test
    public void testUpdatePolicy() {

        EndpointPolicyImpl epi = new TestEndpointPolicy();

        Policy p1 = new Policy();
        QName aqn1 = new QName("http://x.y.z", "a");
        PolicyAssertion a1 = mockAssertion(aqn1, true);
        p1.addAssertion(a1);

        Policy p2 = new Policy();
        QName aqn2 = new QName("http://x.y.z", "b");
        PolicyAssertion a2 = mockAssertion(aqn2, true);
        p2.addAssertion(a2);

        epi.setPolicy(p1.normalize(null, true));

        Policy ep = epi.updatePolicy(p2, createMessage()).getPolicy();

        List<ExactlyOne> pops = CastUtils.cast(ep.getPolicyComponents(), ExactlyOne.class);
        assertEquals("New policy must have 1 top level policy operator", 1, pops.size());
        List<All> alts = CastUtils.cast(pops.get(0).getPolicyComponents(), All.class);
        assertEquals("2 alternatives should be available", 2, alts.size());

        List<PolicyAssertion> assertions1 = CastUtils
            .cast(alts.get(0).getAssertions(), PolicyAssertion.class);
        assertEquals("1 assertion should be available", 1, assertions1.size());

        List<PolicyAssertion> assertions2 = CastUtils
            .cast(alts.get(1).getAssertions(), PolicyAssertion.class);
        assertEquals("1 assertion should be available", 1, assertions2.size());

        QName n1 = assertions1.get(0).getName();
        QName n2 = assertions2.get(0).getName();
        assertTrue("Policy was not merged",
                   n1.equals(aqn1) && n2.equals(aqn2) || n1.equals(aqn2) && n2.equals(aqn1));
        
        verify(a1, times(1)).getName();
        verify(a1, times(2)).getType();
        verify(a1, times(1)).normalize();
        verify(a2, times(1)).getName();
        verify(a2, times(2)).getType();
        verify(a2, times(1)).normalize();
    }

    @Test
    public void testUpdatePolicyWithEmptyPolicy() {

        doTestUpdateWithEmptyPolicy(new Policy());
    }

    @Test
    public void testUpdatePolicyWithEmptyAll() {

        Policy emptyPolicy = new Policy();
        emptyPolicy.addPolicyComponent(new All());
        emptyPolicy.addPolicyComponent(new All());
        doTestUpdateWithEmptyPolicy(emptyPolicy);
    }

    @Test
    public void testUpdatePolicyWithEmptyExactlyOneAndAll() {

        Policy emptyPolicy = new Policy();
        PolicyOperator exactlyOne = new ExactlyOne();
        exactlyOne.addPolicyComponent(new All());
        exactlyOne.addPolicyComponent(new All());
        emptyPolicy.addPolicyComponent(exactlyOne);
        emptyPolicy.addPolicyComponent(new All());
        emptyPolicy.addPolicyComponent(new All());
        doTestUpdateWithEmptyPolicy(emptyPolicy);
    }

    private void doTestUpdateWithEmptyPolicy(Policy emptyPolicy) {
        Policy p1 = new Policy();
        QName aqn1 = new QName("http://x.y.z", "a");
        PolicyAssertion a = mockAssertion(aqn1, true);
        p1.addAssertion(a);

        EndpointPolicyImpl epi = new TestEndpointPolicy();

        epi.setPolicy(p1.normalize(true));

        Policy ep = epi.updatePolicy(emptyPolicy, createMessage()).getPolicy();

        List<ExactlyOne> pops = CastUtils.cast(ep.getPolicyComponents(), ExactlyOne.class);
        assertEquals("New policy must have 1 top level policy operator", 1, pops.size());
        List<All> alts = CastUtils.cast(pops.get(0).getPolicyComponents(), All.class);
        assertEquals("1 alternatives should be available", 1, alts.size());

        List<PolicyAssertion> assertions1 = CastUtils
            .cast(alts.get(0).getAssertions(), PolicyAssertion.class);
        assertEquals("1 assertion should be available", 1, assertions1.size());

        QName n1 = assertions1.get(0).getName();
        assertEquals("Policy was not merged", n1, aqn1);

        verify(a, times(1)).getName();
        verify(a, times(2)).getType();
        verify(a, times(1)).normalize();
    }

    private PolicyAssertion mockAssertion(QName name, boolean normalize) {
        PolicyAssertion a = mock(PolicyAssertion.class);
        when(a.getName()).thenReturn(name);
        if (normalize) {
            when(a.getType()).thenReturn(Constants.TYPE_ASSERTION);
            when(a.normalize()).thenReturn(a);
        }
        return a;
    }

    @Test
    public void testInitialiseInterceptorsServer() {
        doTestInitializeInterceptors(false);
    }

    @Test
    public void testInitialiseInterceptorsClient() {
        doTestInitializeInterceptors(true);
    }

    private void doTestInitializeInterceptors(boolean requestor) {

        EndpointInfo ei = mock(EndpointInfo.class);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);

        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, requestor, null);
        Collection<Assertion> v = new ArrayList<>();
        Collection<Assertion> fv = new ArrayList<>();
        QName aqn = new QName("http://x.y.z", "a");
        PolicyAssertion a1 = mockAssertion(aqn, false);
        PolicyAssertion a2 = mockAssertion(aqn, false);
        v.add(a1);
        v.add(a2);
        fv.addAll(v);
        epi.setVocabulary(v);
        epi.setChosenAlternative(v);
        epi.setFaultVocabulary(fv);

        PolicyInterceptorProviderRegistry reg = mock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);

        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        Interceptor<? extends Message> api = li.get(0);
        when(reg.getInInterceptorsForAssertion(aqn)).thenReturn(li);
        if (requestor) {
            when(reg.getInFaultInterceptorsForAssertion(aqn)).thenReturn(li);
        }

        Message m = new MessageImpl();
        epi.initializeInterceptors(m);
        assertEquals(1, epi.getInterceptors(m).size());
        assertSame(api, epi.getInterceptors(m).get(0));
        if (requestor) {
            assertEquals(1, epi.getFaultInterceptors(m).size());
            assertSame(api, epi.getFaultInterceptors(m).get(0));
        } else {
            assertNull(epi.getFaultInterceptors(m));
        }
        
        verify(a1, times(requestor ? 2 : 1)).getName();
        verify(a2, times(requestor ? 2 : 1)).getName();
    }

    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine,
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = mock(Bus.class);
        when(engine.getBus()).thenReturn(bus);
        when(bus.getExtension(PolicyInterceptorProviderRegistry.class)).thenReturn(reg);
    }

}