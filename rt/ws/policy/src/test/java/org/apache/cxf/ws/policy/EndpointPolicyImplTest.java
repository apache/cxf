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

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class EndpointPolicyImplTest {

    private IMocksControl control;

    final class TestEndpointPolicy extends EndpointPolicyImpl {
        @Override
        protected EndpointPolicyImpl createEndpointPolicy() {
            return new TestEndpointPolicy();
        }

        @Override
        void finalizeConfig(Message m) {
        }
    };

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
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
        EndpointPolicyImpl epi = new EndpointPolicyImpl();
        Message m = new MessageImpl();
        assertNull(epi.getPolicy());
        assertNull(epi.getChosenAlternative());
        assertNull(epi.getInterceptors(m));
        assertNull(epi.getFaultInterceptors(m));

        Policy p = control.createMock(Policy.class);
        Assertion a = control.createMock(Assertion.class);
        List<Assertion> la = Collections.singletonList(a);
        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        control.replay();
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
        control.verify();
    }

    @Test
    public void testInitialize() throws NoSuchMethodException {
        Message m = new MessageImpl();
        Method m1 = EndpointPolicyImpl.class.getDeclaredMethod("initializePolicy", new Class[] {Message.class});
        Method m2 = EndpointPolicyImpl.class.getDeclaredMethod("checkExactlyOnes", new Class[] {});
        Method m3 = EndpointPolicyImpl.class.getDeclaredMethod("chooseAlternative", new Class[] {Message.class});
        Method m4 = EndpointPolicyImpl.class.getDeclaredMethod("initializeVocabulary", new Class[] {Message.class});
        Method m5 = EndpointPolicyImpl.class.getDeclaredMethod("initializeInterceptors", new Class[] {Message.class});
        EndpointPolicyImpl epi = EasyMock.createMockBuilder(EndpointPolicyImpl.class)
            .addMockedMethods(m1, m2, m3, m4, m5).createMock(control);

        epi.initializePolicy(m);
        EasyMock.expectLastCall();
        epi.checkExactlyOnes();
        EasyMock.expectLastCall();
        epi.chooseAlternative(m);
        EasyMock.expectLastCall();

        control.replay();
        epi.initialize(m);
        control.verify();
    }

    @Test
    public void testInitializePolicy() {
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si);
        Policy sp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedServicePolicy(si, null)).andReturn(sp);
        Policy ep = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedEndpointPolicy(ei, null)).andReturn(ep);
        Policy merged = control.createMock(Policy.class);
        EasyMock.expect(sp.merge(ep)).andReturn(merged);
        EasyMock.expect(merged.normalize(null, true)).andReturn(merged);

        control.replay();
        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, true, null);
        epi.initializePolicy(null);
        assertSame(merged, epi.getPolicy());
        control.verify();
    }

    @Test
    public void testChooseAlternative() {
        Policy policy = new Policy();

        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        Assertor assertor = control.createMock(Assertor.class);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);

        Message m = new MessageImpl();
        EndpointPolicyImpl epi = new EndpointPolicyImpl(null, engine, true, assertor);
        epi.setPolicy(policy);

        EasyMock.expect(engine.isEnabled()).andReturn(true).anyTimes();
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor, null, m)).andReturn(null);

        control.replay();
        try {
            epi.chooseAlternative(m);
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();

        control.reset();
        EasyMock.expect(engine.isEnabled()).andReturn(true).anyTimes();
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        Collection<Assertion> alternative = new ArrayList<>();
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor, null, m)).andReturn(alternative);
        control.replay();
        epi.chooseAlternative(m);
        Collection<Assertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);
        control.verify();

        control.reset();
        EasyMock.expect(engine.isEnabled()).andReturn(false).anyTimes();
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(null).anyTimes();
        control.replay();
        try {
            epi.chooseAlternative(m);
        } catch (Exception ex) {
            // no NPE expected
            fail("No Exception expected: " + ex);
        }
        choice = epi.getChosenAlternative();
        assertTrue("not an empty list", choice != null && choice.isEmpty());
        control.verify();
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
        p1.addAssertion(mockAssertion(aqn1, 5, true));

        Policy p2 = new Policy();
        QName aqn2 = new QName("http://x.y.z", "b");
        p2.addAssertion(mockAssertion(aqn2, 5, true));
        control.replay();

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
        p1.addAssertion(mockAssertion(aqn1, 5, true));

        EndpointPolicyImpl epi = new TestEndpointPolicy();
        control.replay();

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
    }

    private PolicyAssertion mockAssertion(QName name, int howMany, boolean normalize) {
        PolicyAssertion a = control.createMock(PolicyAssertion.class);
        EasyMock.expect(a.getName()).andReturn(name).times(howMany);
        if (normalize) {
            EasyMock.expect(a.getType()).andReturn(Constants.TYPE_ASSERTION).times(howMany);
            EasyMock.expect(a.normalize()).andReturn(a).times(howMany);
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

        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);

        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, requestor, null);
        Collection<Assertion> v = new ArrayList<>();
        Collection<Assertion> fv = new ArrayList<>();
        QName aqn = new QName("http://x.y.z", "a");
        v.add(mockAssertion(aqn, requestor ? 2 : 1, false));
        v.add(mockAssertion(aqn, requestor ? 2 : 1, false));
        fv.addAll(v);
        epi.setVocabulary(v);
        epi.setChosenAlternative(v);
        epi.setFaultVocabulary(fv);

        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);

        List<Interceptor<? extends Message>> li = createMockInterceptorList();
        Interceptor<? extends Message> api = li.get(0);
        EasyMock.expect(reg.getInInterceptorsForAssertion(aqn)).andReturn(li).anyTimes();
        if (requestor) {
            EasyMock.expect(reg.getInFaultInterceptorsForAssertion(aqn)).andReturn(li).anyTimes();
        }

        control.replay();
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
        control.verify();
    }

    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine,
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(engine.getBus()).andReturn(bus).anyTimes();
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg).anyTimes();
    }

}