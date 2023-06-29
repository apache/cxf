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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyEngineTest {

    private PolicyEngineImpl engine;
    private Message msg = new MessageImpl();

    private EndpointInfo createMockEndpointInfo() throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setName(new QName("mock", "mock"));
        return ei;
    }
    private BindingOperationInfo createMockBindingOperationInfo() throws Exception {
        OperationInfo info = new OperationInfo();
        return new BindingOperationInfo(null, info) {
            public boolean isUnwrapped() {
                return false;
            }
        };
    }

    @Test
    public void testAccessors() throws Exception {
        engine = new PolicyEngineImpl(false);
        assertNotNull(engine.getRegistry());
        assertNull(engine.getBus());
        assertNotNull(engine.getPolicyProviders());
        assertNull(engine.getAlternativeSelector());
        assertFalse(engine.isEnabled());
        Bus bus = new ExtensionManagerBus();

        engine.setBus(bus);
        List<PolicyProvider> providers = CastUtils.cast(Collections.EMPTY_LIST, PolicyProvider.class);
        engine.setPolicyProviders(providers);
        PolicyRegistry reg = mock(PolicyRegistry.class);
        engine.setRegistry(reg);
        engine.setEnabled(true);
        AlternativeSelector selector = mock(AlternativeSelector.class);
        engine.setAlternativeSelector(selector);
        assertSame(bus, engine.getBus());
        assertSame(reg, engine.getRegistry());
        assertTrue(engine.isEnabled());
        assertSame(selector, engine.getAlternativeSelector());
        assertNotNull(engine.createOutPolicyInfo());
        bus.shutdown(true);
    }

    @Test
    public void testGetEffectiveClientRequestPolicy() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        AssertingConduit conduit = mock(AssertingConduit.class);
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialise(ei, boi, engine, conduit, true, true, msg);

        assertSame(epi, engine.getEffectiveClientRequestPolicy(ei, boi, conduit, msg));
        assertSame(epi, engine.getEffectiveClientRequestPolicy(ei, boi, conduit, msg));
    }

    @Test
    public void testSetEffectiveClientRequestPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);

        engine.setEffectiveClientRequestPolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy,
                   engine.getEffectiveClientRequestPolicy(ei, boi, (Conduit)null, msg));
    }

    @Test
    public void testGetEffectiveServerResponsePolicy() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        AssertingDestination destination = mock(AssertingDestination.class);
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialise(ei, boi, engine, destination, false, false, null);

        assertSame(epi, engine.getEffectiveServerResponsePolicy(ei, boi, destination, null, msg));
        assertSame(epi, engine.getEffectiveServerResponsePolicy(ei, boi, destination, null, msg));
    }

    @Test
    public void testSetEffectiveServerResponsePolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);

        engine.setEffectiveServerResponsePolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy,
                   engine.getEffectiveServerResponsePolicy(ei, boi, (Destination)null, null, msg));
    }

    @Test
    public void testGetEffectiveServerFaultPolicy() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null);
        AssertingDestination destination = mock(AssertingDestination.class);
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialise(ei, null, bfi, engine, destination, msg);

        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, null, bfi, destination, msg));
        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, null, bfi, destination, msg));
    }

    @Test
    public void testSetEffectiveServerFaultPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null);
        EffectivePolicy epi = mock(EffectivePolicy.class);
        engine.setEffectiveServerFaultPolicy(ei, bfi, epi);
        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, null, bfi, (Destination)null, msg));
    }

    @Test
    public void testGetEffectiveServerRequestPolicyInfo() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialise(ei, boi, engine, false, true, msg);

        assertSame(epi, engine.getEffectiveServerRequestPolicy(ei, boi, msg));
        assertSame(epi, engine.getEffectiveServerRequestPolicy(ei, boi, msg));
    }

    @Test
    public void testSetEffectiveServerRequestPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);
        engine.setEffectiveServerRequestPolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy, engine.getEffectiveServerRequestPolicy(ei, boi, msg));
    }

    @Test
    public void testGetEffectiveClientResponsePolicy() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialise(ei, boi, engine, true, false, msg);

        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi, msg));
        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi, msg));
    }

    @Test
    public void testSetEffectiveClientResponsePolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy epi = mock(EffectivePolicy.class);

        engine.setEffectiveClientResponsePolicy(ei, boi, epi);
        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi, msg));
    }

    @Test
    public void testGetEffectiveClientFaultPolicy() throws Exception {
        engine = spy(new PolicyEngineImpl());
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null);
        EffectivePolicyImpl epi = mock(EffectivePolicyImpl.class);
        when(engine.createOutPolicyInfo()).thenReturn(epi);
        epi.initialisePolicy(ei, null, bfi, engine, msg);

        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, null, bfi, msg));
        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, null, bfi, msg));
    }

    @Test
    public void testSetEffectiveClientFaultPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null);
        EffectivePolicy epi = mock(EffectivePolicy.class);
        engine.setEffectiveClientFaultPolicy(ei, bfi, epi);
        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, null, bfi, msg));
    }

    @Test
    public void testGetEndpointPolicyClientSide() throws Exception {
        engine = spy(new PolicyEngineImpl(mock(Bus.class)));
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        AssertingConduit conduit = mock(AssertingConduit.class);
        EndpointPolicyImpl epi = mock(EndpointPolicyImpl.class);
        when(engine.createEndpointPolicyInfo(ei, true, conduit, msg)).thenReturn(epi);
        assertSame(epi, engine.getClientEndpointPolicy(ei, conduit, msg));
    }

    @Test
    public void testGetEndpointPolicyServerSide() throws Exception {
        engine = spy(new PolicyEngineImpl(mock(Bus.class)));
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        AssertingDestination destination = mock(AssertingDestination.class);
        EndpointPolicyImpl epi = mock(EndpointPolicyImpl.class);
        when(engine.createEndpointPolicyInfo(ei, false, destination, msg)).thenReturn(epi);
        assertSame(epi, engine.getServerEndpointPolicy(ei, destination, msg));
    }

    @Test
    public void testCreateEndpointPolicyInfo() throws Exception {
        engine = spy(new PolicyEngineImpl(mock(Bus.class)));
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        Assertor assertor = mock(Assertor.class);
        EndpointPolicyImpl epi = mock(EndpointPolicyImpl.class);
        when(engine.createEndpointPolicyInfo(ei, false, assertor, msg)).thenReturn(epi);
        assertSame(epi, engine.createEndpointPolicyInfo(ei, false, assertor, msg));
    }

    @Test
    public void testEndpointPolicyWithEqualPolicies() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        ServiceInfo si = mock(ServiceInfo.class);
        ei.setService(si);
        EndpointPolicyImpl epi = mock(EndpointPolicyImpl.class);
        engine.setServerEndpointPolicy(ei, epi);
        engine.setClientEndpointPolicy(ei, epi);

        assertSame(epi, engine.getClientEndpointPolicy(ei, (Conduit)null, msg));
        assertSame(epi, engine.getServerEndpointPolicy(ei, (Destination)null, msg));
    }


    @Test
    public void testDontAddBusInterceptors() {
        doTestAddBusInterceptors(false);
    }

    @Test
    public void testAddBusInterceptors() {
        doTestAddBusInterceptors(true);
    }

    private void doTestAddBusInterceptors(boolean enabled) {
        engine = new PolicyEngineImpl(enabled);

        Bus bus = mock(Bus.class);
        List<Interceptor<? extends Message>> out = new ArrayList<>();
        List<Interceptor<? extends Message>> in = new ArrayList<>();
        List<Interceptor<? extends Message>> inFault = new ArrayList<>();
        List<Interceptor<? extends Message>> outFault = new ArrayList<>();
        if (enabled) {
            when(bus.getOutInterceptors()).thenReturn(out);
            when(bus.getInInterceptors()).thenReturn(in);
            when(bus.getInFaultInterceptors()).thenReturn(inFault);
            when(bus.getOutFaultInterceptors()).thenReturn(outFault);
        }
        engine.setBus(bus);

        if (enabled) {
            Set<String> idsOut = getInterceptorIds(out);
            Set<String> idsIn = getInterceptorIds(in);
            Set<String> idsInFault = getInterceptorIds(inFault);
            Set<String> idsOutFault = getInterceptorIds(outFault);
            assertEquals(1, out.size());
            assertTrue(idsOut.contains(PolicyConstants.POLICY_OUT_INTERCEPTOR_ID));
            assertEquals(1, in.size());
            assertTrue(idsIn.contains(PolicyConstants.POLICY_IN_INTERCEPTOR_ID));
            assertEquals(2, inFault.size());
            assertTrue(idsInFault.contains(PolicyConstants.CLIENT_POLICY_IN_FAULT_INTERCEPTOR_ID));
            assertTrue(idsInFault.contains(PolicyVerificationInFaultInterceptor.class.getName()));
            assertEquals(1, outFault.size());
            assertTrue(idsOutFault.contains(PolicyConstants.SERVER_POLICY_OUT_FAULT_INTERCEPTOR_ID));
        } else {
            assertEquals(0, out.size());
            assertEquals(0, in.size());
            assertEquals(0, inFault.size());
            assertEquals(0, outFault.size());
        }
        if (enabled) {
            assertNotNull(engine.getAlternativeSelector());
        }
    }

    @Test
    public void testGetAggregatedServicePolicy() {
        engine = new PolicyEngineImpl();
        ServiceInfo si = mock(ServiceInfo.class);

        Policy p = engine.getAggregatedServicePolicy(si, null);
        assertTrue(p.isEmpty());

        PolicyProvider provider1 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = mock(Policy.class);
        when(provider1.getEffectivePolicy(si, null)).thenReturn(p1);

        assertSame(p1, engine.getAggregatedServicePolicy(si, null));

        PolicyProvider provider2 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = mock(Policy.class);
        Policy p3 = mock(Policy.class);
        when(provider1.getEffectivePolicy(si, null)).thenReturn(p1);
        when(provider2.getEffectivePolicy(si, null)).thenReturn(p2);
        when(p1.merge(p2)).thenReturn(p3);

        assertSame(p3, engine.getAggregatedServicePolicy(si, null));
    }

    @Test
    public void testGetAggregatedEndpointPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();

        Policy p = engine.getAggregatedEndpointPolicy(ei, null);
        assertTrue(p.isEmpty());

        PolicyProvider provider1 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = mock(Policy.class);
        when(provider1.getEffectivePolicy(ei, null)).thenReturn(p1);

        assertSame(p1, engine.getAggregatedEndpointPolicy(ei, null));

        PolicyProvider provider2 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = mock(Policy.class);
        Policy p3 = mock(Policy.class);
        when(provider1.getEffectivePolicy(ei, null)).thenReturn(p1);
        when(provider2.getEffectivePolicy(ei, null)).thenReturn(p2);
        when(p1.merge(p2)).thenReturn(p3);

        assertSame(p3, engine.getAggregatedEndpointPolicy(ei, null));
    }

    @Test
    public void testGetAggregatedOperationPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        BindingOperationInfo boi = createMockBindingOperationInfo();

        Policy p = engine.getAggregatedOperationPolicy(boi, null);
        assertTrue(p.isEmpty());

        PolicyProvider provider1 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = mock(Policy.class);
        when(provider1.getEffectivePolicy(boi, null)).thenReturn(p1);

        assertSame(p1, engine.getAggregatedOperationPolicy(boi, null));

        PolicyProvider provider2 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = mock(Policy.class);
        Policy p3 = mock(Policy.class);
        when(provider1.getEffectivePolicy(boi, null)).thenReturn(p1);
        when(provider2.getEffectivePolicy(boi, null)).thenReturn(p2);
        when(p1.merge(p2)).thenReturn(p3);

        assertSame(p3, engine.getAggregatedOperationPolicy(boi, null));
    }

    @Test
    public void testGetAggregatedMessagePolicy() {
        engine = new PolicyEngineImpl();
        BindingMessageInfo bmi = mock(BindingMessageInfo.class);

        Policy p = engine.getAggregatedMessagePolicy(bmi, null);
        assertTrue(p.isEmpty());

        PolicyProvider provider1 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = mock(Policy.class);
        when(provider1.getEffectivePolicy(bmi, null)).thenReturn(p1);

        assertSame(p1, engine.getAggregatedMessagePolicy(bmi, null));

        PolicyProvider provider2 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = mock(Policy.class);
        Policy p3 = mock(Policy.class);
        when(provider1.getEffectivePolicy(bmi, null)).thenReturn(p1);
        when(provider2.getEffectivePolicy(bmi, null)).thenReturn(p2);
        when(p1.merge(p2)).thenReturn(p3);

        assertSame(p3, engine.getAggregatedMessagePolicy(bmi, null));
    }

    @Test
    public void testGetAggregatedFaultPolicy() {
        engine = new PolicyEngineImpl();
        BindingFaultInfo bfi = mock(BindingFaultInfo.class);

        Policy p = engine.getAggregatedFaultPolicy(bfi, null);
        assertTrue(p.isEmpty());

        PolicyProvider provider1 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = mock(Policy.class);
        when(provider1.getEffectivePolicy(bfi, null)).thenReturn(p1);

        assertSame(p1, engine.getAggregatedFaultPolicy(bfi, null));

        PolicyProvider provider2 = mock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = mock(Policy.class);
        Policy p3 = mock(Policy.class);
        when(provider1.getEffectivePolicy(bfi, null)).thenReturn(p1);
        when(provider2.getEffectivePolicy(bfi, null)).thenReturn(p2);
        when(p1.merge(p2)).thenReturn(p3);

        assertSame(p3, engine.getAggregatedFaultPolicy(bfi, null));
    }

    @Test
    public void testGetAssertions() throws NoSuchMethodException {
        engine = spy(new PolicyEngineImpl());
        PolicyAssertion a = mock(PolicyAssertion.class);
        when(a.getType()).thenReturn(Constants.TYPE_ASSERTION);
        when(a.isOptional()).thenReturn(true);

        assertTrue(engine.getAssertions(a, false).isEmpty());

        when(a.getType()).thenReturn(Constants.TYPE_ASSERTION);
        // when(a.isOptional()).thenReturn(false);

        Collection<Assertion> ca = engine.getAssertions(a, true);
        assertEquals(1, ca.size());
        assertSame(a, ca.iterator().next());

        Policy p = mock(Policy.class);
        when(p.getType()).thenReturn(Constants.TYPE_POLICY);

        assertTrue(engine.getAssertions(p, false).isEmpty());
        verify(engine).addAssertions(eq(p), eq(false),
                CastUtils.cast(isA(Collection.class), Assertion.class));
    }

    @Test
    public void testAddAssertions() {
        engine = new PolicyEngineImpl();
        Collection<Assertion> assertions = new ArrayList<>();

        Assertion a = mock(Assertion.class);
        when(a.getType()).thenReturn(Constants.TYPE_ASSERTION);
        when(a.isOptional()).thenReturn(true);

        engine.addAssertions(a, false, assertions);
        assertTrue(assertions.isEmpty());

        when(a.getType()).thenReturn(Constants.TYPE_ASSERTION);
        engine.addAssertions(a, true, assertions);
        assertEquals(1, assertions.size());
        assertSame(a, assertions.iterator().next());

        assertions.clear();
        Policy p = new Policy();
        a = new PrimitiveAssertion(new QName("http://x.y.z", "a"));
        p.addAssertion(a);

        // id has no #
        engine.getRegistry().register("ab", p);

        // local reference is an id + #
        PolicyReference pr = new PolicyReference();
        pr.setURI("#ab");

        engine.addAssertions(pr, false, assertions);
        assertEquals(1, assertions.size());
        assertSame(a, assertions.iterator().next());
    }

    private Set<String> getInterceptorIds(List<Interceptor<? extends Message>> interceptors) {
        Set<String> ids = new HashSet<>();
        for (Interceptor<? extends Message> i : interceptors) {
            ids.add(((PhaseInterceptor<? extends Message>)i).getId());
        }
        return ids;
    }

    interface AssertingConduit extends Assertor, Conduit {
    }

    interface AssertingDestination extends Assertor, Destination {
    }


}
