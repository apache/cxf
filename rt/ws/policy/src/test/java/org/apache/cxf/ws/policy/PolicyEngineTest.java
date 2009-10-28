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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
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
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PolicyEngineTest extends Assert {

    private IMocksControl control;
    private PolicyEngineImpl engine;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl(); 
    } 
    
    private EndpointInfo createMockEndpointInfo() throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setName(new QName("mock", "mock"));
        return ei;
    }
    private BindingOperationInfo createMockBindingOperationInfo() throws Exception {
        OperationInfo info = new OperationInfo();
        BindingOperationInfo boi = new BindingOperationInfo(null, info) {
            public boolean isUnwrapped() {
                return false;
            }
        };
        return boi;
    }
    
    @Test
    public void testAccessors() throws Exception {
        engine = new PolicyEngineImpl(false);
        assertNotNull(engine.getRegistry());
        assertNull(engine.getBus());
        assertNotNull(engine.getPolicyProviders()); 
        assertNull(engine.getAlternativeSelector());
        assertTrue(!engine.isEnabled());
        Bus bus = new CXFBusImpl();
        
        engine.setBus(bus);
        List<PolicyProvider> providers = CastUtils.cast(Collections.EMPTY_LIST, PolicyProvider.class);
        engine.setPolicyProviders(providers);
        PolicyRegistry reg = control.createMock(PolicyRegistry.class);
        engine.setRegistry(reg);
        engine.setEnabled(true);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);
        engine.setAlternativeSelector(selector);
        assertSame(bus, engine.getBus());
        assertSame(reg, engine.getRegistry());
        assertTrue(engine.isEnabled()); 
        assertSame(selector, engine.getAlternativeSelector());
        assertNotNull(engine.createOutPolicyInfo());
        
    }
    
    @Test
    public void testGetEffectiveClientRequestPolicy() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo(); 
        AssertingConduit conduit = control.createMock(AssertingConduit.class);
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialise(ei, boi, engine, conduit, true, true);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveClientRequestPolicy(ei, boi, conduit));
        assertSame(epi, engine.getEffectiveClientRequestPolicy(ei, boi, conduit));
        control.verify();
    }
    
    @Test 
    public void testSetEffectiveClientRequestPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        control.replay();
        engine.setEffectiveClientRequestPolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy, 
                   engine.getEffectiveClientRequestPolicy(ei, boi, (Conduit)null)); 
        control.verify();
    }
    
    @Test
    public void testGetEffectiveServerResponsePolicy() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo(); 
        AssertingDestination destination = control.createMock(AssertingDestination.class);
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialise(ei, boi, engine, destination, false, false);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveServerResponsePolicy(ei, boi, destination));
        assertSame(epi, engine.getEffectiveServerResponsePolicy(ei, boi, destination));
        control.verify();
    }
    
    @Test
    public void testSetEffectiveServerResponsePolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        control.replay();
        engine.setEffectiveServerResponsePolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy, 
                   engine.getEffectiveServerResponsePolicy(ei, boi, (Destination)null));
        control.verify();
    }
   
    @Test
    public void testGetEffectiveServerFaultPolicy() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null); 
        AssertingDestination destination = control.createMock(AssertingDestination.class);
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialise(ei, bfi, engine, destination);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, bfi, destination));
        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, bfi, destination));
        control.verify();
    }
    
    @Test
    public void testSetEffectiveServerFaultPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null); 
        EffectivePolicy epi = control.createMock(EffectivePolicy.class);
        engine.setEffectiveServerFaultPolicy(ei, bfi, epi);
        assertSame(epi, engine.getEffectiveServerFaultPolicy(ei, bfi, (Destination)null));   
    }
       
    @Test
    public void testGetEffectiveServerRequestPolicyInfo() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo(); 
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialise(ei, boi, engine, false, true);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveServerRequestPolicy(ei, boi));
        assertSame(epi, engine.getEffectiveServerRequestPolicy(ei, boi));
        control.verify();
    }
    
    @Test 
    public void testSetEffectiveServerRequestPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        control.replay();
        engine.setEffectiveServerRequestPolicy(ei, boi, effectivePolicy);
        assertSame(effectivePolicy, engine.getEffectiveServerRequestPolicy(ei, boi));   
        control.verify();
    }
    
    @Test
    public void testGetEffectiveClientResponsePolicy() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo(); 
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialise(ei, boi, engine, true, true);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi));
        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi));
        control.verify();
    }
    
    @Test 
    public void testSetEffectiveClientResponsePolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        EffectivePolicy epi = control.createMock(EffectivePolicy.class);
        control.replay();
        engine.setEffectiveClientResponsePolicy(ei, boi, epi);
        assertSame(epi, engine.getEffectiveClientResponsePolicy(ei, boi));   
        control.verify();
    }
    
    @Test
    public void testGetEffectiveClientFaultPolicy() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createOutPolicyInfo", new Class[] {});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null); 
        EffectivePolicyImpl epi = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.createOutPolicyInfo()).andReturn(epi);
        epi.initialisePolicy(ei, bfi, engine);
        EasyMock.expectLastCall();
        control.replay();
        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, bfi));
        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, bfi));
        control.verify();
    }
    
    @Test 
    public void testSetEffectiveClientFaultPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        BindingFaultInfo bfi = new BindingFaultInfo(null, null); 
        EffectivePolicy epi = control.createMock(EffectivePolicy.class);
        engine.setEffectiveClientFaultPolicy(ei, bfi, epi);
        assertSame(epi, engine.getEffectiveClientFaultPolicy(ei, bfi));        
    }
    
    @Test
    public void testGetEndpointPolicyClientSide() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createEndpointPolicyInfo", 
            new Class[] {EndpointInfo.class, boolean.class, Assertor.class});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        AssertingConduit conduit = control.createMock(AssertingConduit.class);
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class);
        EasyMock.expect(engine.createEndpointPolicyInfo(ei, true, conduit)).andReturn(epi);
        control.replay();
        assertSame(epi, engine.getClientEndpointPolicy(ei, conduit));
        control.verify();        
    }
    
    @Test
    public void testGetEndpointPolicyServerSide() throws Exception {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("createEndpointPolicyInfo", 
            new Class[] {EndpointInfo.class, boolean.class, Assertor.class});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        AssertingDestination destination = control.createMock(AssertingDestination.class);
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class);
        EasyMock.expect(engine.createEndpointPolicyInfo(ei, false, destination)).andReturn(epi);
        control.replay();
        assertSame(epi, engine.getServerEndpointPolicy(ei, destination));
        control.verify();        
    }
    
    @Test
    public void testCreateEndpointPolicyInfo() throws Exception {
        Method m1 = PolicyEngineImpl.class.getDeclaredMethod("createEndpointPolicyInfo", 
            new Class[] {EndpointInfo.class, boolean.class, Assertor.class});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m1});
        engine.init();
        EndpointInfo ei = createMockEndpointInfo();
        Assertor assertor = control.createMock(Assertor.class);
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class);
        EasyMock.expect(engine.createEndpointPolicyInfo(ei, false, assertor)).andReturn(epi);
        control.replay();
        assertSame(epi, engine.createEndpointPolicyInfo(ei, false, assertor));
        control.verify();
    }
    
    @Test
    public void testEndpointPolicyWithEqualPolicies() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        ei.setService(si);
        si.getExtensor(Policy.class);
        EasyMock.expectLastCall().andReturn(null).times(2);
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class);
        control.replay();
        engine.setServerEndpointPolicy(ei, epi);
        engine.setClientEndpointPolicy(ei, epi);
        
        assertSame(epi, engine.getClientEndpointPolicy(ei, (Conduit)null));
        assertSame(epi, engine.getServerEndpointPolicy(ei, (Destination)null));
        
        control.reset();
        ei.setService(si);
        Policy p = new Policy();
        si.getExtensor(Policy.class);
        EasyMock.expectLastCall().andReturn(p).times(2);
        epi.getPolicy();
        EasyMock.expectLastCall().andReturn(p).times(2);
        control.replay();
        assertSame(epi, engine.getServerEndpointPolicy(ei, (Destination)null));
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
    
        Bus bus = control.createMock(Bus.class);
        engine.setBus(bus);
        List<Interceptor> out = new ArrayList<Interceptor>();
        List<Interceptor> in = new ArrayList<Interceptor>();
        List<Interceptor> inFault = new ArrayList<Interceptor>();
        List<Interceptor> outFault = new ArrayList<Interceptor>();
        if (enabled) {
            EasyMock.expect(bus.getOutInterceptors()).andReturn(out).times(1);
            EasyMock.expect(bus.getInInterceptors()).andReturn(in).times(1);
            EasyMock.expect(bus.getInFaultInterceptors()).andReturn(inFault).times(2);
            EasyMock.expect(bus.getOutFaultInterceptors()).andReturn(outFault);
            control.replay();
        }
        
        engine.addBusInterceptors();
        
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
            control.verify();
        }
        assertNotNull(engine.getAlternativeSelector());
    }
    
    @Test
    public void testGetAggregatedServicePolicy() {
        engine = new PolicyEngineImpl();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        
        control.replay();
        Policy p = engine.getAggregatedServicePolicy(si);
        assertTrue(p.isEmpty());
        control.verify();
        control.reset();
        
        PolicyProvider provider1 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(si)).andReturn(p1);
        
        control.replay();
        assertSame(p1, engine.getAggregatedServicePolicy(si));
        control.verify();
        control.reset();
        
        PolicyProvider provider2 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = control.createMock(Policy.class);
        Policy p3 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(si)).andReturn(p1);
        EasyMock.expect(provider2.getEffectivePolicy(si)).andReturn(p2);
        EasyMock.expect(p1.merge(p2)).andReturn(p3);
        
        control.replay();
        assertSame(p3, engine.getAggregatedServicePolicy(si));
        control.verify();      
    }
    
    @Test
    public void testGetAggregatedEndpointPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        EndpointInfo ei = createMockEndpointInfo();
        
        control.replay();
        Policy p = engine.getAggregatedEndpointPolicy(ei);
        assertTrue(p.isEmpty());
        control.verify();
        control.reset();
        
        PolicyProvider provider1 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(ei)).andReturn(p1);
        
        control.replay();
        assertSame(p1, engine.getAggregatedEndpointPolicy(ei));
        control.verify();
        control.reset();
        
        PolicyProvider provider2 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = control.createMock(Policy.class);
        Policy p3 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(ei)).andReturn(p1);
        EasyMock.expect(provider2.getEffectivePolicy(ei)).andReturn(p2);
        EasyMock.expect(p1.merge(p2)).andReturn(p3);
        
        control.replay();
        assertSame(p3, engine.getAggregatedEndpointPolicy(ei));
        control.verify();      
    }
    
    @Test
    public void testGetAggregatedOperationPolicy() throws Exception {
        engine = new PolicyEngineImpl();
        BindingOperationInfo boi = createMockBindingOperationInfo();
        
        control.replay();
        Policy p = engine.getAggregatedOperationPolicy(boi);
        assertTrue(p.isEmpty());
        control.verify();
        control.reset();
        
        PolicyProvider provider1 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(boi)).andReturn(p1);
        
        control.replay();
        assertSame(p1, engine.getAggregatedOperationPolicy(boi));
        control.verify();
        control.reset();
        
        PolicyProvider provider2 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = control.createMock(Policy.class);
        Policy p3 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(boi)).andReturn(p1);
        EasyMock.expect(provider2.getEffectivePolicy(boi)).andReturn(p2);
        EasyMock.expect(p1.merge(p2)).andReturn(p3);
        
        control.replay();
        assertSame(p3, engine.getAggregatedOperationPolicy(boi));
        control.verify();      
    }
    
    @Test
    public void testGetAggregatedMessagePolicy() {
        engine = new PolicyEngineImpl();
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        
        control.replay();
        Policy p = engine.getAggregatedMessagePolicy(bmi);
        assertTrue(p.isEmpty());
        control.verify();
        control.reset();
        
        PolicyProvider provider1 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(bmi)).andReturn(p1);
        
        control.replay();
        assertSame(p1, engine.getAggregatedMessagePolicy(bmi));
        control.verify();
        control.reset();
        
        PolicyProvider provider2 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = control.createMock(Policy.class);
        Policy p3 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(bmi)).andReturn(p1);
        EasyMock.expect(provider2.getEffectivePolicy(bmi)).andReturn(p2);
        EasyMock.expect(p1.merge(p2)).andReturn(p3);
        
        control.replay();
        assertSame(p3, engine.getAggregatedMessagePolicy(bmi));
        control.verify();      
    }
    
    @Test
    public void testGetAggregatedFaultPolicy() {
        engine = new PolicyEngineImpl();
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        
        control.replay();
        Policy p = engine.getAggregatedFaultPolicy(bfi);
        assertTrue(p.isEmpty());
        control.verify();
        control.reset();
        
        PolicyProvider provider1 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider1);
        Policy p1 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(bfi)).andReturn(p1);
        
        control.replay();
        assertSame(p1, engine.getAggregatedFaultPolicy(bfi));
        control.verify();
        control.reset();
        
        PolicyProvider provider2 = control.createMock(PolicyProvider.class);
        engine.getPolicyProviders().add(provider2);
        Policy p2 = control.createMock(Policy.class);
        Policy p3 = control.createMock(Policy.class);
        EasyMock.expect(provider1.getEffectivePolicy(bfi)).andReturn(p1);
        EasyMock.expect(provider2.getEffectivePolicy(bfi)).andReturn(p2);
        EasyMock.expect(p1.merge(p2)).andReturn(p3);
        
        control.replay();
        assertSame(p3, engine.getAggregatedFaultPolicy(bfi));
        control.verify();      
    }
    
    @Test
    public void testGetAssertions() throws NoSuchMethodException {
        Method m = PolicyEngineImpl.class.getDeclaredMethod("addAssertions",
            new Class[] {PolicyComponent.class, boolean.class, Collection.class});
        engine = control.createMock(PolicyEngineImpl.class, new Method[] {m});
        PolicyAssertion a = control.createMock(PolicyAssertion.class);
        EasyMock.expect(a.getType()).andReturn(Constants.TYPE_ASSERTION);
        EasyMock.expect(a.isOptional()).andReturn(true);
        
        control.replay();
        assertTrue(engine.getAssertions(a, false).isEmpty());
        control.verify();
        
        control.reset();
        EasyMock.expect(a.getType()).andReturn(Constants.TYPE_ASSERTION);
        // EasyMock.expect(a.isOptional()).andReturn(false);
        
        control.replay();
        Collection<PolicyAssertion> ca = engine.getAssertions(a, true);
        assertEquals(1, ca.size());
        assertSame(a, ca.iterator().next());
        control.verify();
        
        control.reset();
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(p.getType()).andReturn(Constants.TYPE_POLICY);
        engine.addAssertions(EasyMock.eq(p), EasyMock.eq(false), 
                             CastUtils.cast(EasyMock.isA(Collection.class), PolicyAssertion.class));
        EasyMock.expectLastCall();
        
        control.replay();
        assertTrue(engine.getAssertions(p, false).isEmpty());
        control.verify();
    }
    
    @Test
    public void testAddAssertions() {
        engine = new PolicyEngineImpl();
        Collection<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>();
        
        PolicyAssertion a = control.createMock(PolicyAssertion.class);
        EasyMock.expect(a.getType()).andReturn(Constants.TYPE_ASSERTION);
        EasyMock.expect(a.isOptional()).andReturn(true);
        
        control.replay();
        engine.addAssertions(a, false, assertions);
        assertTrue(assertions.isEmpty());
        control.verify();
        
        control.reset();
        EasyMock.expect(a.getType()).andReturn(Constants.TYPE_ASSERTION);
        control.replay();
        engine.addAssertions(a, true, assertions);
        assertEquals(1, assertions.size());
        assertSame(a, assertions.iterator().next());        
        control.verify();
        
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
        
    private Set<String> getInterceptorIds(List<Interceptor> interceptors) {
        Set<String> ids = new HashSet<String>();
        for (Interceptor i : interceptors) {
            ids.add(((PhaseInterceptor)i).getId());
        }
        return ids;
    }
    
    interface AssertingConduit extends Assertor, Conduit {
    }
    
    interface AssertingDestination extends Assertor, Destination {
    }
    
    
}
