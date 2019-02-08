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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 *
 */
public class EffectivePolicyImplTest {

    private IMocksControl control;
    private Message msg = new MessageImpl();

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        Integer.valueOf(4);
    }

    private List<Interceptor<? extends Message>> createMockInterceptorList() {
        Interceptor<? extends Message> i = control.createMock(Interceptor.class);
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

        Policy p = control.createMock(Policy.class);
        Assertion a = control.createMock(Assertion.class);
        List<Assertion> la = Collections.singletonList(a);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        control.replay();
        effectivePolicy.setPolicy(p);
        assertSame(p, effectivePolicy.getPolicy());
        effectivePolicy.setChosenAlternative(la);
        assertSame(la, effectivePolicy.getChosenAlternative());
        effectivePolicy.setInterceptors(li);
        assertSame(li, effectivePolicy.getInterceptors());
        control.verify();
    }

    @Test
    public void testInitialiseFromEndpointPolicy() throws NoSuchMethodException {
        Method m = EffectivePolicyImpl.class.getDeclaredMethod("initialiseInterceptors",
                                                          new Class[] {PolicyEngine.class, Message.class});
        EffectivePolicyImpl effectivePolicy = EasyMock.createMockBuilder(EffectivePolicyImpl.class)
            .addMockedMethod(m).createMock(control);
        EndpointPolicyImpl endpointPolicy = control.createMock(EndpointPolicyImpl.class);
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(endpointPolicy.getPolicy()).andReturn(p);
        Collection<Assertion> chosenAlternative = new ArrayList<>();
        EasyMock.expect(endpointPolicy.getChosenAlternative()).andReturn(chosenAlternative);
        PolicyEngineImpl pe = new PolicyEngineImpl();
        effectivePolicy.initialiseInterceptors(pe, false, msg);
        EasyMock.expectLastCall();
        control.replay();
        effectivePolicy.initialise(endpointPolicy, pe, false, null);
        control.verify();
    }

    @Test
    public void testInitialise() throws NoSuchMethodException {
        Method m1 = EffectivePolicyImpl.class.getDeclaredMethod("initialisePolicy",
            new Class[] {EndpointInfo.class,
                         BindingOperationInfo.class,
                         PolicyEngine.class,
                         boolean.class,
                         boolean.class,
                         Assertor.class,
                         Message.class});
        Method m2 = EffectivePolicyImpl.class.getDeclaredMethod("chooseAlternative",
            new Class[] {PolicyEngine.class, Assertor.class, Message.class});
        Method m3 = EffectivePolicyImpl.class.getDeclaredMethod("initialiseInterceptors",
                                                          new Class[] {PolicyEngine.class, Message.class});
        EffectivePolicyImpl effectivePolicy = EasyMock.createMockBuilder(EffectivePolicyImpl.class)
            .addMockedMethods(m1, m2, m3).createMock(control);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        PolicyEngineImpl pe = new PolicyEngineImpl();
        Assertor a = control.createMock(Assertor.class);
        boolean requestor = true;

        effectivePolicy.initialisePolicy(ei, boi, pe, requestor, requestor, a, null);
        EasyMock.expectLastCall().andReturn(a);
        effectivePolicy.chooseAlternative(pe, a, null);
        EasyMock.expectLastCall();
        effectivePolicy.initialiseInterceptors(pe, false, msg);
        EasyMock.expectLastCall();

        control.replay();
        effectivePolicy.initialise(ei, boi, pe, a, requestor, requestor, null);
        control.verify();
    }

    @Test
    public void testInitialiseFault() throws NoSuchMethodException {
        Method m1 = EffectivePolicyImpl.class.getDeclaredMethod("initialisePolicy",
            new Class[] {EndpointInfo.class, BindingOperationInfo.class,
                         BindingFaultInfo.class, PolicyEngine.class, Message.class});
        Method m2 = EffectivePolicyImpl.class.getDeclaredMethod("chooseAlternative",
            new Class[] {PolicyEngine.class, Assertor.class, Message.class});
        Method m3 = EffectivePolicyImpl.class.getDeclaredMethod("initialiseInterceptors",
                                                          new Class[] {PolicyEngine.class, Message.class});
        EffectivePolicyImpl effectivePolicy = EasyMock.createMockBuilder(EffectivePolicyImpl.class)
            .addMockedMethods(m1, m2, m3).createMock(control);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        PolicyEngineImpl pe = new PolicyEngineImpl();
        Assertor a = control.createMock(Assertor.class);

        effectivePolicy.initialisePolicy(ei, null, bfi, pe, null);
        EasyMock.expectLastCall();
        effectivePolicy.chooseAlternative(pe, a, null);
        EasyMock.expectLastCall();
        effectivePolicy.initialiseInterceptors(pe, false, msg);
        EasyMock.expectLastCall();

        control.replay();
        effectivePolicy.initialise(ei, null, bfi, pe, a, null);
        control.verify();
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
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        if (requestor) {
            EasyMock.expect(boi.getInput()).andReturn(bmi);
        } else {
            EasyMock.expect(boi.getOutput()).andReturn(bmi);
        }

        EndpointPolicy effectivePolicy = control.createMock(EndpointPolicy.class);
        if (requestor) {
            EasyMock.expect(engine.getClientEndpointPolicy(ei, (Conduit)null, null)).andReturn(effectivePolicy);
        } else {
            EasyMock.expect(engine.getServerEndpointPolicy(ei, (Destination)null, null)).andReturn(effectivePolicy);
        }
        Policy ep = control.createMock(Policy.class);
        EasyMock.expect(effectivePolicy.getPolicy()).andReturn(ep);
        Policy op = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedOperationPolicy(boi, null)).andReturn(op);
        Policy merged = control.createMock(Policy.class);
        EasyMock.expect(ep.merge(op)).andReturn(merged);
        Policy mp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedMessagePolicy(bmi, null)).andReturn(mp);
        EasyMock.expect(merged.merge(mp)).andReturn(merged);
        EasyMock.expect(merged.normalize(null, true)).andReturn(merged);

        control.replay();
        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        epi.initialisePolicy(ei, boi, engine, requestor, requestor, null, null);
        assertSame(merged, epi.getPolicy());
        control.verify();
    }

    @Test
    public void testInitialiseServerFaultPolicy() {
        Message m = new MessageImpl();
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);

        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bfi.getBindingOperation()).andReturn(boi).anyTimes();
        EndpointPolicy endpointPolicy = control.createMock(EndpointPolicy.class);
        EasyMock.expect(engine.getServerEndpointPolicy(ei, (Destination)null, m)).andReturn(endpointPolicy);
        Policy ep = control.createMock(Policy.class);
        EasyMock.expect(endpointPolicy.getPolicy()).andReturn(ep);
        Policy op = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedOperationPolicy(boi, m)).andReturn(op);
        Policy merged = control.createMock(Policy.class);
        EasyMock.expect(ep.merge(op)).andReturn(merged);
        Policy fp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedFaultPolicy(bfi, m)).andReturn(fp);
        EasyMock.expect(merged.merge(fp)).andReturn(merged);
        EasyMock.expect(merged.normalize(null, true)).andReturn(merged);

        control.replay();
        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        epi.initialisePolicy(ei, boi, bfi, engine, m);
        assertSame(merged, epi.getPolicy());
        control.verify();
    }

    @Test
    public void testChooseAlternative() {
        Message m = new MessageImpl();
        EffectivePolicyImpl epi = new EffectivePolicyImpl();
        Policy policy = new Policy();
        epi.setPolicy(policy);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        Assertor assertor = control.createMock(Assertor.class);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor, null, m)).andReturn(null);

        control.replay();
        try {
            epi.chooseAlternative(engine, assertor, m);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();

        control.reset();
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        Collection<Assertion> alternative = new ArrayList<>();
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor, null, m)).andReturn(alternative);
        control.replay();
        epi.chooseAlternative(engine, assertor, m);
        Collection<Assertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);
        control.verify();
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

        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);

        control.replay();
        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());
        control.verify();

        control.reset();
        setupPolicyInterceptorProviderRegistry(engine, reg);
        List<Interceptor<? extends Message>> il = new ArrayList<>();
        setupRegistryInterceptors(useIn, fault, reg, null, il);
        PolicyAssertion a = control.createMock(PolicyAssertion.class);
        alternative.add(a);
        control.replay();
        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());
        control.verify();

        control.reset();
        setupPolicyInterceptorProviderRegistry(engine, reg);
        QName qn = new QName("http://x.y.z", "a");
        EasyMock.expect(a.getName()).andReturn(qn);
        il = new ArrayList<>();
        setupRegistryInterceptors(useIn, fault, reg, qn, il);
        control.replay();
        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(0, epi.getInterceptors().size());
        control.verify();

        control.reset();
        setupPolicyInterceptorProviderRegistry(engine, reg);
        EasyMock.expect(a.getName()).andReturn(qn);
        Interceptor<Message> pi = control.createMock(Interceptor.class);
        il = new ArrayList<>();
        il.add(pi);
        setupRegistryInterceptors(useIn, fault, reg, qn, il);
        control.replay();
        epi.initialiseInterceptors(engine, useIn, fault, msg);
        assertEquals(1, epi.getInterceptors().size());
        assertSame(pi, epi.getInterceptors().get(0));
        control.verify();
    }

    private void setupRegistryInterceptors(boolean useIn, boolean fault,
                                           PolicyInterceptorProviderRegistry reg, QName qn,
                                           List<Interceptor<? extends Message>> m) {
        if (useIn && !fault) {
            EasyMock.expect(reg.getInInterceptorsForAssertion(qn))
                .andReturn(m);
        } else if (!useIn && !fault) {
            EasyMock.expect(reg.getOutInterceptorsForAssertion(qn))
                .andReturn(m);
        } else if (useIn && fault) {
            EasyMock.expect(reg.getInFaultInterceptorsForAssertion(qn))
                .andReturn(m);
        } else if (!useIn && fault) {
            EasyMock.expect(reg.getOutFaultInterceptorsForAssertion(qn))
                .andReturn(m);
        }
    }

    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine,
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(engine.getBus()).andReturn(bus).anyTimes();
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg);
    }

}