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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyInterceptorsTest {
    private static final QName ASSERTION_QNAME = new QName("http://apache.cxf", "test");

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
        bus = mock(Bus.class);
    }

    private List<Interceptor<? extends Message>> createMockInterceptorList() {
        @SuppressWarnings("unchecked")
        Interceptor<? extends Message> i = mock(Interceptor.class);
        Interceptor<? extends Message> m = i;
        List<Interceptor<? extends Message>> a = new ArrayList<>();
        a.add(m);
        return a;
    }

    @Test
    public void testClientPolicyOutInterceptor() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, true, true);

        setupMessage(true, true, true, true, true, true);
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);
        when(pe.getEffectiveClientRequestPolicy(ei, boi, conduit, message))
            .thenReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        when(effectivePolicy.getInterceptors())
            .thenReturn(li);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));

        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        when(effectivePolicy.getChosenAlternative()).thenReturn(assertions);
        interceptor.handleMessage(message);
    }

    @Test
    public void testClientPolicyInInterceptor() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, true, false);

        setupMessage(true, true, true, true, true, true);
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);
        when(pe.getEffectiveClientResponsePolicy(ei, boi, message)).thenReturn(effectivePolicy);
        when(effectivePolicy.getPolicy()).thenReturn(new Policy());

        @SuppressWarnings("unchecked")
        Interceptor<? extends Message> i = mock(Interceptor.class);
        List<Interceptor<? extends Message>> lst = new ArrayList<>();
        lst.add(i);
        when(effectivePolicy.getInterceptors()).thenReturn(lst);

        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(i);

        ic.add(PolicyVerificationInInterceptor.INSTANCE);
        interceptor.handleMessage(message);

        verify(effectivePolicy, times(2)).getPolicy();
        verify(message, times(1)).put(eq(AssertionInfoMap.class), isA(AssertionInfoMap.class));
    }

    @Test
    public void testClientPolicyInFaultInterceptor() {
        ClientPolicyInFaultInterceptor interceptor = new ClientPolicyInFaultInterceptor();

        doTestBasics(interceptor, true, false);

        setupMessage(true, true, false, false, true, true);
        EndpointPolicy endpointPolicy = mock(EndpointPolicy.class);
        when(pe.getClientEndpointPolicy(ei, conduit, message)).thenReturn(endpointPolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        when(endpointPolicy.getFaultInterceptors(message))
            .thenReturn(li);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));

        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        when(endpointPolicy.getFaultVocabulary(message)).thenReturn(assertions);
        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyInInterceptor() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, false, false);

        setupMessage(false, false, false, false, true, true);
        EndpointPolicy endpointPolicy = mock(EndpointPolicyImpl.class);
        when(pe.getServerEndpointPolicy(ei, destination, message)).thenReturn(endpointPolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        when(endpointPolicy.getInterceptors(message))
            .thenReturn(li);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));

        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        when(endpointPolicy.getVocabulary(message)).thenReturn(assertions);

        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyOutInterceptor() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, false, true);

        setupMessage(false, false, true, true, true, true);
        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);
        when(pe.getEffectiveServerResponsePolicy(ei, boi, destination, null, message))
            .thenReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        when(effectivePolicy.getInterceptors())
            .thenReturn(li);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));

        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        when(effectivePolicy.getChosenAlternative()).thenReturn(assertions);

        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyOutFaultInterceptor() throws NoSuchMethodException {
        ServerPolicyOutFaultInterceptor interceptor = spy(new ServerPolicyOutFaultInterceptor());

        doTestBasics(interceptor, false, true);

        setupMessage(false, false, true, true, true, true);
        Exception ex = mock(Exception.class);
        when(exchange.get(Exception.class)).thenReturn(ex);
        when(interceptor.getBindingFaultInfo(message, ex, boi)).thenReturn(null);
        interceptor.handleMessage(message);

        setupMessage(false, false, true, true, true, true);
        // Exception ex = mock(Exception.class);
        when(exchange.get(Exception.class)).thenReturn(ex);
        BindingFaultInfo bfi = mock(BindingFaultInfo.class);
        when(interceptor.getBindingFaultInfo(message, ex, boi)).thenReturn(bfi);
        EffectivePolicy effectivePolicy = mock(EffectivePolicyImpl.class);
        when(pe.getEffectiveServerFaultPolicy(ei, boi, bfi, destination, message))
            .thenReturn(effectivePolicy);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        when(effectivePolicy.getInterceptors())
            .thenReturn(li);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));

        Collection<Assertion> assertions =
            CastUtils.cast(Collections.EMPTY_LIST, Assertion.class);
        when(effectivePolicy.getChosenAlternative()).thenReturn(assertions);
        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyOutFaultInterceptorGetBindingFaultInfo() {
        ServerPolicyOutFaultInterceptor interceptor = new ServerPolicyOutFaultInterceptor();
        message = mock(Message.class);
        Exception ex = new UnsupportedOperationException(new RuntimeException());
        boi = mock(BindingOperationInfo.class);
        when(message.get(BindingFaultInfo.class)).thenReturn(null);
        BindingFaultInfo bfi = mock(BindingFaultInfo.class);
        Collection<BindingFaultInfo> bfis = CastUtils.cast(Collections.EMPTY_LIST);
        when(boi.getFaults()).thenReturn(bfis);
        BindingOperationInfo wrappedBoi = mock(BindingOperationInfo.class);
        when(boi.getWrappedOperation()).thenReturn(wrappedBoi);
        Collection<BindingFaultInfo> wrappedBfis = CastUtils.cast(Collections.singletonList(bfi));
        when(wrappedBoi.getFaults()).thenReturn(wrappedBfis);
        FaultInfo fi = mock(FaultInfo.class);
        when(bfi.getFaultInfo()).thenReturn(fi);
        when(fi.getProperty(Class.class.getName(), Class.class))
            .thenReturn(RuntimeException.class);
        message.put(BindingFaultInfo.class, bfi);

        assertSame(bfi, interceptor.getBindingFaultInfo(message, ex, boi));
        verify(boi, times(2)).getWrappedOperation();
    }

    @Test
    public void testClientPolicyInInterceptorPolicyOverride() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, true, false);

        setupMessage(true, true, true, true, true, true);
        coachPolicyOverride(true, false);

        interceptor.handleMessage(message);
    }

    @Test
    public void testClientPolicyOutInterceptorPolicyOverride() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, true, true);

        setupMessage(true, true, true, true, true, true);
        coachPolicyOverride(false, false);

        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyInInterceptorPolicyOverride() {
        PolicyInInterceptor interceptor = new PolicyInInterceptor();

        doTestBasics(interceptor, false, false);

        setupMessage(false, false, false, false, true, true);
        coachPolicyOverride(true, false);

        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyOutInterceptorPolicyOverride() {
        PolicyOutInterceptor interceptor = new PolicyOutInterceptor();

        doTestBasics(interceptor, false, true);

        setupMessage(false, false, true, true, true, true);
        coachPolicyOverride(false, false);

        interceptor.handleMessage(message);
    }


    @Test
    public void testClientPolicyInFaultInterceptorPolicyOverride() {
        ClientPolicyInFaultInterceptor interceptor = new ClientPolicyInFaultInterceptor();

        doTestBasics(interceptor, true, false);

        setupMessage(true, true, false, false, true, true);
        coachPolicyOverride(true, true);

        interceptor.handleMessage(message);
    }

    @Test
    public void testServerPolicyOutFaultInterceptorPolicyOverride() {
        ServerPolicyOutFaultInterceptor interceptor = new ServerPolicyOutFaultInterceptor();
        doTestBasics(interceptor, false, true);

        setupMessage(false, false, true, true, true, true);
        coachPolicyOverride(false, true);

        interceptor.handleMessage(message);
    }

    private void doTestBasics(Interceptor<Message> interceptor, boolean isClient, boolean usesOperationInfo) {
        setupMessage(!isClient, isClient, usesOperationInfo, !usesOperationInfo, false, false);
        interceptor.handleMessage(message);

        setupMessage(isClient, isClient, usesOperationInfo, !usesOperationInfo, false, false);
        interceptor.handleMessage(message);

        setupMessage(isClient, isClient, usesOperationInfo, usesOperationInfo, false, false);
        interceptor.handleMessage(message);

        setupMessage(isClient, isClient, usesOperationInfo, usesOperationInfo, true, false);
        interceptor.handleMessage(message);
    }

    void setupMessage(boolean setupRequestor,
                      boolean isClient,
                      boolean usesOperationInfo,
                      boolean setupOperation,
                      Boolean setupEndpoint,
                      Boolean setupEngine) {

        message = mock(Message.class);

        exchange = mock(Exchange.class);
        when(message.get(Message.REQUESTOR_ROLE))
            .thenReturn(isClient ? Boolean.TRUE : Boolean.FALSE);

        when(message.getExchange()).thenReturn(exchange);

        when(exchange.getBus()).thenReturn(bus);
        if (usesOperationInfo) {
            if (null == boi && setupOperation) {
                boi = mock(BindingOperationInfo.class);
            }
            when(exchange.getBindingOperationInfo()).thenReturn(setupOperation ? boi : null);
            if (!setupOperation) {
                return;
            }
        }

        if (null == endpoint && setupEndpoint) {
            endpoint = mock(Endpoint.class);
        }
        when(exchange.getEndpoint()).thenReturn(setupEndpoint ? endpoint : null);
        if (!setupEndpoint) {
            return;
        }
        if (null == ei) {
            ei = mock(EndpointInfo.class);
        }
        when(endpoint.getEndpointInfo()).thenReturn(ei);

        if (null == pe && setupEngine) {
            pe = mock(PolicyEngineImpl.class);
        }
        when(bus.getExtension(PolicyEngine.class)).thenReturn(setupEngine ? pe : null);
        if (!setupEngine) {
            return;
        }


        if (isClient) {
            conduit = mock(Conduit.class);
            when(exchange.getConduit(message)).thenReturn(conduit);
        } else {
            destination = mock(Destination.class);
            when(exchange.getDestination()).thenReturn(destination);
        }
    }

    private void coachPolicyOverride(boolean in, boolean fault) {
        Assertion assertion = mock(Assertion.class);
        when(assertion.getName()).thenReturn(ASSERTION_QNAME);
        Collection<Assertion> assertions =
            new ArrayList<>();
        assertions.add(assertion);

        Policy policyOverride = mock(Policy.class);
        when(message.getContextualProperty(PolicyConstants.POLICY_OVERRIDE))
            .thenReturn(policyOverride);
        AlternativeSelector selector = mock(AlternativeSelector.class);
        when(selector.selectAlternative(policyOverride, pe, null, null, message)).thenReturn(assertions);
        when(pe.getAlternativeSelector()).thenReturn(selector);
        when(pe.getBus()).thenReturn(bus);
        PolicyInterceptorProviderRegistry reg = mock(PolicyInterceptorProviderRegistry.class);
        when(bus.getExtension(PolicyInterceptorProviderRegistry.class)).thenReturn(reg);

        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        if (in && fault) {
            when(reg.getInFaultInterceptorsForAssertion(ASSERTION_QNAME)).thenReturn(li);
        } else if (!in && fault) {
            when(reg.getOutFaultInterceptorsForAssertion(ASSERTION_QNAME)).thenReturn(li);
        } else if (in && !fault) {
            when(reg.getInInterceptorsForAssertion(ASSERTION_QNAME)).thenReturn(li);
        } else if (!in && !fault) {
            when(reg.getOutInterceptorsForAssertion(ASSERTION_QNAME)).thenReturn(li);
        }
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        ic.add(li.get(0));
    }

}