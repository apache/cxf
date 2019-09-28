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
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 *
 */
public class PolicyInterceptorsTest {
    private static final QName ASSERTION_QNAME = new QName("http://apache.cxf", "test");

    private IMocksControl control;
    private Message message;
    private Exchange exchange;
    private BindingOperationInfo boi;
    private Endpoint endpoint;
    private EndpointInfo ei;
    private Bus bus;
    private PolicyEngineImpl pe;
    private Conduit conduit;
    private Destination destination;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
    }

    private List<Interceptor<? extends Message>> createMockInterceptorList() {
        Interceptor<? extends Message> i = control.createMock(Interceptor.class);
        Interceptor<? extends Message> m = i;
        List<Interceptor<? extends Message>> a = new ArrayList<>();
        a.add(m);
        return a;
    }

    @Test
    public void testClientPolicyOutInterceptor() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, true, true);

        control.reset();
        setupMessage(true, true, true, true, true, true);
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        EasyMock.expect(pe.getEffectiveClientRequestPolicy(ei, boi, conduit, message))
            .andReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        EasyMock.expect(effectivePolicy.getInterceptors())
            .andReturn(li);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic);
        ic.add(li.get(0));
        EasyMock.expectLastCall();
        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        EasyMock.expect(effectivePolicy.getChosenAlternative()).andReturn(assertions);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testClientPolicyInInterceptor() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, true, false);

        control.reset();
        setupMessage(true, true, true, true, true, true);
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        EasyMock.expect(pe.getEffectiveClientResponsePolicy(ei, boi, message)).andReturn(effectivePolicy);
        EasyMock.expect(effectivePolicy.getPolicy()).andReturn(new Policy()).times(2);
        Interceptor<? extends Message> i = control.createMock(Interceptor.class);
        List<Interceptor<? extends Message>> lst = new ArrayList<>();
        lst.add(i);
        EasyMock.expect(effectivePolicy.getInterceptors()).andReturn(lst);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic).anyTimes();
        ic.add(i);
        EasyMock.expectLastCall();
        message.put(EasyMock.eq(AssertionInfoMap.class), EasyMock.isA(AssertionInfoMap.class));
        EasyMock.expectLastCall();
        ic.add(PolicyVerificationInInterceptor.INSTANCE);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testClientPolicyInFaultInterceptor() {
        ClientPolicyInFaultInterceptor interceptor = new ClientPolicyInFaultInterceptor();

        doTestBasics(interceptor, true, false);

        control.reset();
        setupMessage(true, true, false, false, true, true);
        EndpointPolicy endpointPolicy = control.createMock(EndpointPolicy.class);
        EasyMock.expect(pe.getClientEndpointPolicy(ei, conduit, message)).andReturn(endpointPolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        EasyMock.expect(endpointPolicy.getFaultInterceptors(message))
            .andReturn(li);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic);
        ic.add(li.get(0));
        EasyMock.expectLastCall();
        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        EasyMock.expect(endpointPolicy.getFaultVocabulary(message)).andReturn(assertions);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyInInterceptor() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, false, false);

        control.reset();
        setupMessage(false, false, false, false, true, true);
        EndpointPolicy endpointPolicy = control.createMock(EndpointPolicyImpl.class);
        EasyMock.expect(pe.getServerEndpointPolicy(ei, destination, message)).andReturn(endpointPolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        EasyMock.expect(endpointPolicy.getInterceptors(message))
            .andReturn(li);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic);
        ic.add(li.get(0));
        EasyMock.expectLastCall();
        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        EasyMock.expect(endpointPolicy.getVocabulary(message)).andReturn(assertions);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyOutInterceptor() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, false, true);

        control.reset();
        setupMessage(false, false, true, true, true, true);
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class);
        EasyMock.expect(pe.getEffectiveServerResponsePolicy(ei, boi, destination, null, message))
            .andReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        EasyMock.expect(effectivePolicy.getInterceptors())
            .andReturn(li);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic);
        ic.add(li.get(0));
        EasyMock.expectLastCall();
        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        EasyMock.expect(effectivePolicy.getChosenAlternative()).andReturn(assertions);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyOutFaultInterceptor() throws NoSuchMethodException {
        Method m = AbstractPolicyInterceptor.class.getDeclaredMethod("getBindingFaultInfo",
            new Class[] {Message.class, Exception.class, BindingOperationInfo.class});

        ServerPolicyOutFaultInterceptor interceptor =
            EasyMock.createMockBuilder(ServerPolicyOutFaultInterceptor.class)
                .addMockedMethod(m).createMock(control);

        doTestBasics(interceptor, false, true);

        control.reset();
        setupMessage(false, false, true, true, true, true);
        Exception ex = control.createMock(Exception.class);
        EasyMock.expect(exchange.get(Exception.class)).andReturn(ex);
        EasyMock.expect(interceptor.getBindingFaultInfo(message, ex, boi)).andReturn(null);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(false, false, true, true, true, true);
        // Exception ex = control.createMock(Exception.class);
        EasyMock.expect(exchange.get(Exception.class)).andReturn(ex);
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        EasyMock.expect(interceptor.getBindingFaultInfo(message, ex, boi)).andReturn(bfi);
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(pe.getEffectiveServerFaultPolicy(ei, boi, bfi, destination, message))
            .andReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        EasyMock.expect(effectivePolicy.getInterceptors())
            .andReturn(li);
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic);
        ic.add(li.get(0));
        EasyMock.expectLastCall();
        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        EasyMock.expect(effectivePolicy.getChosenAlternative()).andReturn(assertions);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyOutFaultInterceptorGetBindingFaultInfo() {
        ServerPolicyOutFaultInterceptor interceptor = new ServerPolicyOutFaultInterceptor();
        message = control.createMock(Message.class);
        Exception ex = new UnsupportedOperationException(new RuntimeException());
        boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(message.get(BindingFaultInfo.class)).andReturn(null);
        BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
        Collection<BindingFaultInfo> bfis = CastUtils.cast(Collections.EMPTY_LIST);
        EasyMock.expect(boi.getFaults()).andReturn(bfis);
        BindingOperationInfo wrappedBoi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(boi.getWrappedOperation()).andReturn(wrappedBoi).times(2);
        Collection<BindingFaultInfo> wrappedBfis = CastUtils.cast(Collections.singletonList(bfi));
        EasyMock.expect(wrappedBoi.getFaults()).andReturn(wrappedBfis);
        FaultInfo fi = control.createMock(FaultInfo.class);
        EasyMock.expect(bfi.getFaultInfo()).andReturn(fi);
        EasyMock.expect(fi.getProperty(Class.class.getName(), Class.class))
            .andReturn(RuntimeException.class);
        message.put(BindingFaultInfo.class, bfi);
        EasyMock.expectLastCall();

        control.replay();
        assertSame(bfi, interceptor.getBindingFaultInfo(message, ex, boi));
        control.verify();
    }

    @Test
    public void testClientPolicyInInterceptorPolicyOverride() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, true, false);

        control.reset();
        setupMessage(true, true, true, true, true, true);
        coachPolicyOverride(true, false);

        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testClientPolicyOutInterceptorPolicyOverride() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, true, true);

        control.reset();
        setupMessage(true, true, true, true, true, true);
        coachPolicyOverride(false, false);

        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyInInterceptorPolicyOverride() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, false, false);

        control.reset();
        setupMessage(false, false, false, false, true, true);
        coachPolicyOverride(true, false);

        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyOutInterceptorPolicyOverride() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, false, true);

        control.reset();
        setupMessage(false, false, true, true, true, true);
        coachPolicyOverride(false, false);

        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }


    @Test
    public void testClientPolicyInFaultInterceptorPolicyOverride() {
        ClientPolicyInFaultInterceptor interceptor = new ClientPolicyInFaultInterceptor();

        doTestBasics(interceptor, true, false);

        control.reset();
        setupMessage(true, true, false, false, true, true);
        coachPolicyOverride(true, true);

        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    @Test
    public void testServerPolicyOutFaultInterceptorPolicyOverride() {
        ServerPolicyOutFaultInterceptor interceptor = new ServerPolicyOutFaultInterceptor();
        doTestBasics(interceptor, false, true);

        control.reset();
        setupMessage(false, false, true, true, true, true);
        coachPolicyOverride(false, true);
        control.replay();

        interceptor.handleMessage(message);
        control.verify();
    }

    private void doTestBasics(Interceptor<Message> interceptor, boolean isClient, boolean usesOperationInfo) {
        setupMessage(!isClient, isClient, usesOperationInfo, !usesOperationInfo, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(isClient, isClient, usesOperationInfo, !usesOperationInfo, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(isClient, isClient, usesOperationInfo, usesOperationInfo, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(isClient, isClient, usesOperationInfo, usesOperationInfo, true, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    void setupMessage(boolean setupRequestor,
                      boolean isClient,
                      boolean usesOperationInfo,
                      boolean setupOperation,
                      Boolean setupEndpoint,
                      Boolean setupEngine) {

        message = control.createMock(Message.class);

        exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE))
            .andReturn(isClient ? Boolean.TRUE : Boolean.FALSE).anyTimes();

        EasyMock.expect(message.getExchange()).andReturn(exchange);

        EasyMock.expect(exchange.getBus()).andReturn(bus).anyTimes();
        if (usesOperationInfo) {
            if (null == boi && setupOperation) {
                boi = control.createMock(BindingOperationInfo.class);
            }
            EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(setupOperation ? boi : null)
                .anyTimes();
            if (!setupOperation) {
                return;
            }
        }

        if (null == endpoint && setupEndpoint) {
            endpoint = control.createMock(Endpoint.class);
        }
        EasyMock.expect(exchange.getEndpoint()).andReturn(setupEndpoint ? endpoint : null);
        if (!setupEndpoint) {
            return;
        }
        if (null == ei) {
            ei = control.createMock(EndpointInfo.class);
        }
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);

        if (null == pe && setupEngine) {
            pe = control.createMock(PolicyEngineImpl.class);
        }
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(setupEngine ? pe : null);
        if (!setupEngine) {
            return;
        }


        if (isClient) {
            conduit = control.createMock(Conduit.class);
            EasyMock.expect(exchange.getConduit(message)).andReturn(conduit).anyTimes();
        } else {
            destination = control.createMock(Destination.class);
            EasyMock.expect(exchange.getDestination()).andReturn(destination).anyTimes();
        }
    }

    private void coachPolicyOverride(boolean in, boolean fault) {
        Assertion assertion = control.createMock(Assertion.class);
        EasyMock.expect(assertion.getName()).andReturn(ASSERTION_QNAME);
        Collection<Assertion> assertions =
            new ArrayList<>();
        assertions.add(assertion);

        Policy policyOverride = control.createMock(Policy.class);
        EasyMock.expect(message.getContextualProperty(PolicyConstants.POLICY_OVERRIDE))
            .andReturn(policyOverride);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);
        EasyMock.expect(selector.selectAlternative(policyOverride, pe, null, null, message)).andReturn(assertions);
        EasyMock.expect(pe.getAlternativeSelector()).andReturn(selector);
        EasyMock.expect(pe.getBus()).andReturn(bus).anyTimes();
        PolicyInterceptorProviderRegistry reg = control
            .createMock(PolicyInterceptorProviderRegistry.class);
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg);

        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        if (in && fault) {
            EasyMock.expect(reg.getInFaultInterceptorsForAssertion(ASSERTION_QNAME)).andReturn(li);
        } else if (!in && fault) {
            EasyMock.expect(reg.getOutFaultInterceptorsForAssertion(ASSERTION_QNAME)).andReturn(li);
        } else if (in && !fault) {
            EasyMock.expect(reg.getInInterceptorsForAssertion(ASSERTION_QNAME)).andReturn(li);
        } else if (!in && !fault) {
            EasyMock.expect(reg.getOutInterceptorsForAssertion(ASSERTION_QNAME)).andReturn(li);
        }
        InterceptorChain ic = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(ic).anyTimes();
        ic.add(li.get(0));
        EasyMock.expectLastCall();
    }

}