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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.neethi.All;
import org.apache.neethi.Constants;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class EndpointPolicyImplTest extends Assert {

    private IMocksControl control;
    final class TestEndpointPolicy extends EndpointPolicyImpl {
        @Override
        protected EndpointPolicyImpl createEndpointPolicy() {
            return new TestEndpointPolicy();
        }
        @Override 
        void finalizeConfig() {
        }
    };
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    } 
    
    @Test
    public void testAccessors() {
        EndpointPolicyImpl epi = new EndpointPolicyImpl();
        assertNull(epi.getPolicy());
        assertNull(epi.getChosenAlternative());
        assertNull(epi.getInterceptors());
        assertNull(epi.getFaultInterceptors());
        assertNull(epi.getVocabulary());
        assertNull(epi.getFaultVocabulary());
        
        Policy p = control.createMock(Policy.class);
        PolicyAssertion a = control.createMock(PolicyAssertion.class);
        List<PolicyAssertion> la = Collections.singletonList(a);
        Interceptor i = control.createMock(Interceptor.class);
        List<Interceptor> li = Collections.singletonList(i);
        control.replay();
        epi.setPolicy(p);
        assertSame(p, epi.getPolicy());
        epi.setChosenAlternative(la);
        assertSame(la, epi.getChosenAlternative());
        epi.setInterceptors(li);
        assertSame(li, epi.getInterceptors());
        epi.setFaultInterceptors(li);
        assertSame(li, epi.getFaultInterceptors());
        epi.setVocabulary(la);
        assertSame(la, epi.getVocabulary());
        epi.setFaultVocabulary(la);
        assertSame(la, epi.getFaultVocabulary());
        control.verify();
    }
    
    @Test
    public void testInitialize() throws NoSuchMethodException {
        Method m1 = EndpointPolicyImpl.class.getDeclaredMethod("initializePolicy",
            new Class[] {});
        Method m2 = EndpointPolicyImpl.class.getDeclaredMethod("checkExactlyOnes",
            new Class[] {});
        Method m3 = EndpointPolicyImpl.class.getDeclaredMethod("chooseAlternative",
            new Class[] {});
        Method m4 = EndpointPolicyImpl.class.getDeclaredMethod("initializeVocabulary",
            new Class[] {});
        Method m5 = EndpointPolicyImpl.class.getDeclaredMethod("initializeInterceptors",
            new Class[] {});
        EndpointPolicyImpl epi = control.createMock(EndpointPolicyImpl.class, 
                                                    new Method[] {m1, m2, m3, m4, m5});
         
        epi.initializePolicy();
        EasyMock.expectLastCall();
        epi.checkExactlyOnes();
        EasyMock.expectLastCall();
        epi.chooseAlternative();
        EasyMock.expectLastCall();
        epi.initializeInterceptors(); 
        EasyMock.expectLastCall();
        
        control.replay();
        epi.initialize();
        control.verify();        
    }
    
    @Test
    public void testInitializePolicy() {        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si);
        Policy sp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedServicePolicy(si)).andReturn(sp);
        Policy ep = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedEndpointPolicy(ei)).andReturn(ep);
        Policy merged = control.createMock(Policy.class);
        EasyMock.expect(sp.merge(ep)).andReturn(merged);
        EasyMock.expect(merged.normalize(true)).andReturn(merged);
        
        control.replay();
        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, true, null);
        epi.initializePolicy();
        assertSame(merged, epi.getPolicy());
        control.verify();
    }
       
    @Test
    public void testChooseAlternative() {
        Policy policy = new Policy();
        
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        Assertor assertor = control.createMock(Assertor.class);
        AlternativeSelector selector = control.createMock(AlternativeSelector.class);
        
        EndpointPolicyImpl epi = new EndpointPolicyImpl(null, engine, true, assertor);
        epi.setPolicy(policy);        
        
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor)).andReturn(null);
        
        control.replay();
        try {
            epi.chooseAlternative();  
            fail("Expected PolicyException not thrown.");
        } catch (PolicyException ex) {
            // expected
        }
        control.verify();
        
        control.reset();        
        EasyMock.expect(engine.getAlternativeSelector()).andReturn(selector);
        Collection<PolicyAssertion> alternative = new ArrayList<PolicyAssertion>();
        EasyMock.expect(selector.selectAlternative(policy, engine, assertor)).andReturn(alternative);
        control.replay();        
        epi.chooseAlternative();
        Collection<PolicyAssertion> choice = epi.getChosenAlternative();
        assertSame(choice, alternative);   
        control.verify();
    }
    
    @Test
    public void testInitialiseVocabularyServer() {
        dotestInitializeVocabulary(false);
    }
    
    @Test
    public void testInitialiseVocabularyClient() {
        dotestInitializeVocabulary(true);
    }
    
    private void dotestInitializeVocabulary(boolean requestor) {
        
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        PolicyEngineImpl engine = control.createMock(PolicyEngineImpl.class);
        
        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, engine, requestor, null);   
        List<PolicyAssertion> alternative = new ArrayList<PolicyAssertion>();
        epi.setChosenAlternative(alternative);
        PolicyAssertion ea = control.createMock(PolicyAssertion.class);        
        alternative.add(ea);
        EasyMock.expect(ea.isOptional()).andReturn(false);
        PolicyAssertion eaOpt = control.createMock(PolicyAssertion.class);        
        alternative.add(eaOpt);
        EasyMock.expect(eaOpt.isOptional()).andReturn(true);
        
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bi.getOperations()).andReturn(Collections.singletonList(boi));
        
        Policy op = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedOperationPolicy(boi)).andReturn(op);
        PolicyAssertion oa = control.createMock(PolicyAssertion.class);
        EasyMock.expect(engine.getAssertions(op, false)).andReturn(Collections.singletonList(oa));
        BindingMessageInfo bmi = control.createMock(BindingMessageInfo.class);
        if (requestor) {
            EasyMock.expect(boi.getOutput()).andReturn(bmi).times(2);
        } else {
            EasyMock.expect(boi.getInput()).andReturn(bmi);
        }
        Policy mp = control.createMock(Policy.class);
        EasyMock.expect(engine.getAggregatedMessagePolicy(bmi)).andReturn(mp);
        PolicyAssertion ma = control.createMock(PolicyAssertion.class);
        EasyMock.expect(engine.getAssertions(mp, false)).andReturn(Collections.singletonList(ma));
        PolicyAssertion fa = null;
        if (requestor) {
            BindingFaultInfo bfi = control.createMock(BindingFaultInfo.class);
            EasyMock.expect(boi.getFaults()).andReturn(Collections.singletonList(bfi));
            Policy fp = control.createMock(Policy.class);
            EasyMock.expect(engine.getAggregatedFaultPolicy(bfi)).andReturn(fp);
            fa = control.createMock(PolicyAssertion.class);
            EasyMock.expect(engine.getAssertions(fp, false)).andReturn(Collections.singletonList(fa));
        }
        
        control.replay();
        epi.initializeVocabulary();
        Collection<PolicyAssertion> expected = new ArrayList<PolicyAssertion>();
        expected.add(ea);
        expected.add(oa);
        expected.add(ma);
        verifyVocabulary(expected, epi.getVocabulary());
        if (requestor) {
            expected.remove(ma);
            expected.add(fa);
            verifyVocabulary(expected, epi.getFaultVocabulary());
            // 
        } else {
            assertNull(epi.getFaultVocabulary());
        }
          
        control.verify();          
    }
    
    private void verifyVocabulary(Collection<PolicyAssertion> expected, 
                                  Collection<PolicyAssertion> actual) {
        
        assertEquals(expected.size(), actual.size());
        for (Iterator<PolicyAssertion> i = expected.iterator(); i.hasNext();) {
            PolicyAssertion e = i.next();
            Iterator<PolicyAssertion> j = actual.iterator();
            boolean eFound = false;
            while (j.hasNext()) {
                if (e == j.next()) {
                    eFound = true;
                    break;
                }              
            }
            assertTrue("Expected assertion not found.", eFound);
        }
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
        
        epi.setPolicy((Policy)p1.normalize(true));
                
        Policy ep = epi.updatePolicy(p2).getPolicy();
        
        List<ExactlyOne> pops = 
            CastUtils.cast(ep.getPolicyComponents(), ExactlyOne.class);
        assertEquals("New policy must have 1 top level policy operator", 1, pops.size());
        List<All> alts = 
            CastUtils.cast(pops.get(0).getPolicyComponents(), All.class);
        assertEquals("2 alternatives should be available", 2, alts.size());
        
        List<PolicyAssertion> assertions1 = 
            CastUtils.cast(alts.get(0).getAssertions(), PolicyAssertion.class);
        assertEquals("1 assertion should be available", 1, assertions1.size());
            
        List<PolicyAssertion> assertions2 = 
                CastUtils.cast(alts.get(1).getAssertions(), PolicyAssertion.class);
        assertEquals("1 assertion should be available", 1, assertions2.size());    
        
        QName n1 = assertions1.get(0).getName();
        QName n2 = assertions2.get(0).getName();
        assertTrue("Policy was not merged",
                   n1.equals(aqn1) && n2.equals(aqn2) || n1.equals(aqn2) && n2.equals(aqn1));
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
        Collection<PolicyAssertion> v = new ArrayList<PolicyAssertion>();
        Collection<PolicyAssertion> fv = new ArrayList<PolicyAssertion>();
        QName aqn = new QName("http://x.y.z", "a");
        v.add(mockAssertion(aqn, requestor ? 2 : 1, false));
        v.add(mockAssertion(aqn, requestor ? 2 : 1, false));
        fv.addAll(v);
        epi.setVocabulary(v);
        epi.setFaultVocabulary(fv);
        
        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        setupPolicyInterceptorProviderRegistry(engine, reg);
        
        PolicyInterceptorProvider app = control.createMock(PolicyInterceptorProvider.class);               
        EasyMock.expect(reg.get(aqn)).andReturn(app).times(requestor ? 2 : 1);
        Interceptor api = control.createMock(Interceptor.class);
        EasyMock.expect(app.getInInterceptors())
            .andReturn(Collections.singletonList(api));
        if (requestor) {
            EasyMock.expect(app.getInFaultInterceptors())
                .andReturn(Collections.singletonList(api));
        }
        
        control.replay();
        epi.initializeInterceptors();
        assertEquals(1, epi.getInterceptors().size());
        assertSame(api, epi.getInterceptors().get(0));
        if (requestor) {
            assertEquals(1, epi.getFaultInterceptors().size());
            assertSame(api, epi.getFaultInterceptors().get(0));
        } else {
            assertNull(epi.getFaultInterceptors());
        }
        control.verify();          
    }
    
    private void setupPolicyInterceptorProviderRegistry(PolicyEngineImpl engine, 
                                                        PolicyInterceptorProviderRegistry reg) {
        Bus bus = control.createMock(Bus.class);        
        EasyMock.expect(engine.getBus()).andReturn(bus);
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg);
    }
    
  
}
