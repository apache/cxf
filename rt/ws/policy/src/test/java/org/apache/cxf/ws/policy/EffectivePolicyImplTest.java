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
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 *
 */
public class EffectivePolicyImplTest {
    private Message msg = new MessageImpl();

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
        EffectivePolicyImpl effectivePolicy = new EffectivePolicyImpl();
        assertNull(effectivePolicy.getPolicy());
        assertNull(effectivePolicy.getChosenAlternative());
        assertNull(effectivePolicy.getInterceptors());

        Policy p = mock(Policy.class);
        Assertion a = mock(Assertion.class);
        List<Assertion> la = Collections.singletonList(a);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        effectivePolicy.setPolicy(p);
        assertSame(p, effectivePolicy.getPolicy());
        effectivePolicy.setChosenAlternative(la);
        assertSame(la, effectivePolicy.getChosenAlternative());
        effectivePolicy.setInterceptors(li);
        assertSame(li, effectivePolicy.getInterceptors());
    }

    @Test
    public void testInitialiseFromEndpointPolicy() throws NoSuchMethodException {
        EffectivePolicyImpl effectivePolicy = spy(new EffectivePolicyImpl());
        EndpointPolicyImpl endpointPolicy = mock(EndpointPolicyImpl.class);
        Policy p = mock(Policy.class);
        when(endpointPolicy.getPolicy()).thenReturn(p);
        Collection<Assertion> chosenAlternative = new ArrayList<>();
        when(endpointPolicy.getChosenAlternative()).thenReturn(chosenAlternative);
        PolicyEngineImpl pe = new PolicyEngineImpl();
        effectivePolicy.initialiseInterceptors(pe, false, msg);
        effectivePolicy.initialise(endpointPolicy, pe, false, null);
    }

    @Test
    public void testInitialise() throws NoSuchMethodException {
        Bus bus = mock(Bus.class);
        EffectivePolicyImpl effectivePolicy = spy(new EffectivePolicyImpl());
        EndpointInfo ei = mock(EndpointInfo.class);
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        PolicyEngineImpl pe = new PolicyEngineImpl(bus);
        Assertor a = mock(Assertor.class);
        boolean requestor = true;

        when(effectivePolicy.initialisePolicy(ei, boi, pe, requestor, requestor, a, null)).thenReturn(a);
        effectivePolicy.chooseAlternative(pe, a, null);
        effectivePolicy.initialiseInterceptors(pe, false, msg);

        effectivePolicy.initialise(ei, boi, pe, a, requestor, requestor, null);
    }

    @Test
    public void testInitialiseFault() throws NoSuchMethodException {
        Bus bus = mock(Bus.class);
        EffectivePolicyImpl effectivePolicy = spy(new EffectivePolicyImpl());
        EndpointInfo ei = mock(EndpointInfo.class);
        BindingFaultInfo bfi = mock(BindingFaultInfo.class);
        PolicyEngineImpl pe = new PolicyEngineImpl(bus);
        Assertor a = mock(Assertor.class);

        effectivePolicy.initialisePolicy(ei, null, bfi, pe, null);
        effectivePolicy.chooseAlternative(pe, a, null);
        effectivePolicy.initialiseInterceptors(pe, false, msg);

        effectivePolicy.initialise(ei, null, bfi, pe, a, null);
    }

    @Test
    public void testInitialiseClientPolicy() {
        doTestInitialisePolicy(true);
    }

    @Test
    public void testInitialiseServerPolicy() {
        doTestInitialisePolicy(false);
    }

    private void doTestInitialisePolicy(boolean requestor) {
        EndpointInfo ei = mock(EndpointInfo.class);
        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        BindingMessageInfo bmi = mock(BindingMessageInfo.class);
        if (requestor) {
            when(boi.getInput()).thenReturn(bmi);
        } else {
            when(boi.getOutput()).thenReturn(bmi);
        }

        EndpointPolicy effectivePolicy = mock(EndpointPolicy.class);
        if (requestor) {
            when(engine.getClientEndpointPolicy(ei, (Conduit)null, null)).thenReturn(effectivePolicy);
        } else {
            when(engine.getServerEndpointPolicy(ei, (Destination)null, null)).thenReturn(effectivePolicy);
        }
        Policy ep = mock(Policy.class);
        when(effectivePolicy.getPolicy()).thenReturn(ep);
        Policy op = mock(Policy.class);
        when(engine.getAggregatedOperationPolicy(boi, null)).thenReturn(op);
        Policy merged = mock(Policy.class);
        when(ep.merge(op)).thenReturn(merged);
        Policy mp = mock(Policy.class);
        when(engine.getAggregatedMessagePolicy(bmi, null)).thenReturn(mp);
        when(merged.merge(mp)).thenReturn(merged);
        when(merged.normalize(null, true)).thenReturn(merged);

        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        epi.initialisePolicy(ei, boi, engine, requestor, requestor, null, null);
        assertSame(merged, epi.getPolicy());
    }

    @Test
    public void testInitialiseServerFaultPolicy() {
        Message m = new MessageImpl();
        EndpointInfo ei = mock(EndpointInfo.class);
        BindingFaultInfo bfi = mock(BindingFaultInfo.class);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);

        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        when(bfi.getBindingOperation()).thenReturn(boi);
        EndpointPolicy endpointPolicy = mock(EndpointPolicy.class);
        when(engine.getServerEndpointPolicy(ei, (Destination)null, m)).thenReturn(endpointPolicy);
        Policy ep = mock(Policy.class);
        when(endpointPolicy.getPolicy()).thenReturn(ep);
        Policy op = mock(Policy.class);
        when(engine.getAggregatedOperationPolicy(boi, m)).thenReturn(op);
        Policy merged = mock(Policy.class);
        when(ep.merge(op)).thenReturn(merged);
        Policy fp = mock(Policy.class);
        when(engine.getAggregatedFaultPolicy(bfi, m)).thenReturn(fp);
        when(merged.merge(fp)).thenReturn(merged);
        when(merged.normalize(null, true)).thenReturn(merged);

        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        epi.initialisePolicy(ei, boi, bfi, engine, m);
        assertSame(merged, epi.getPolicy());
    }

    @Test
    public void testChooseAlternative() {
        Message m = new MessageImpl();
        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        Policy policy = new Policy();
        epi.setPolicy(policy);
        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        Assertor assertor = mock(Assertor.class);
        AlternativeSelector selector = mock(AlternativeSelector.class);
        when(engine.getAlternativeSelector()).thenReturn(selector);
        when(selector.selectAlternative(policy, engine, assertor, null, m)).thenReturn(null);

        try {
            epi.chooseAlternative(engine, assertor, m);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }

        when(engine.getAlternativeSelector()).thenReturn(selector);
        Collection<Assertion> alternative = new ArrayList<>();
        when(selector.selectAlternative(policy, engine, assertor, null, m)).thenReturn(alternative);

        epi.chooseAlternative(engine, assertor, m);
        Collection<Assertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);
    }

    @Test
    public void testInitialiseOutInterceptors() {
        testInitialiseInterceptors(false, false);
    }

    @Test
    public void testInitialiseInInterceptors() {
        testInitialiseInterceptors(true, false);
    }

    @Test
    public void testInitialiseOutFaultInterceptors() {
        testInitialiseInterceptors(false, true);
    }

    @Test
    public void testInitialiseInFaultInterceptors() {
        testInitialiseInterceptors(true, true);
    }

    private void testInitialiseInterceptors(boolean useIn, boolean fault) {
        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        List<Assertion> alternative = new ArrayList<>();
        epi.setChosenAlternative(alternative);

        PolicyEngineImpl engine = mock(PolicyEngineImpl.class);
        PolicyInterceptorProviderRegistry reg = mock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);

        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());

        setupPolicyInterceptorProviderRegistry(engine, reg);
        List<Interceptor<? extends Message>> il = new ArrayList<>();
        setupRegistryInterceptors(useIn, fault, reg, null, il);
        PolicyAssertion a = mock(PolicyAssertion.class);
        alternative.add(a);

        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());

        setupPolicyInterceptorProviderRegistry(engine, reg);
        QName qn = new QName("http://x.y.z", "a");
        when(a.getName()).thenReturn(qn);
        il = new ArrayList<>();
        setupRegistryInterceptors(useIn, fault, reg, qn, il);

        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());

        setupPolicyInterceptorProviderRegistry(engine, reg);
        when(a.getName()).thenReturn(qn);
        @SuppressWarnings("unchecked")
        Interceptor<Message> pi = mock(Interceptor.class);
        il = new ArrayList<>();
        il.add(pi);
        setupRegistryInterceptors(useIn, fault, reg, qn, il);

        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(1, epi.getInterceptors().size());
        assertSame(pi, epi.getInterceptors().get(0));
    }

    private void setupRegistryInterceptors(boolean useIn, boolean fault,
                                           PolicyInterceptorProviderRegistry reg, QName qn,
                                           List<Interceptor<? extends Message>> m) {
        if (useIn && !fault) {
            when(reg.getInInterceptorsForAssertion(qn))
                .thenReturn(m);
        } else if (!useIn && !fault) {
            when(reg.getOutInterceptorsForAssertion(qn))
                .thenReturn(m);
        } else if (useIn && fault) {
            when(reg.getInFaultInterceptorsForAssertion(qn))
                .thenReturn(m);
        } else if (!useIn && fault) {
            when(reg.getOutFaultInterceptorsForAssertion(qn))
                .thenReturn(m);
        }
    }

    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine,
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = mock(Bus.class);
        when(engine.getBus()).thenReturn(bus);
        when(bus.getExtension(PolicyInterceptorProviderRegistry.class)).thenReturn(reg);
    }

}