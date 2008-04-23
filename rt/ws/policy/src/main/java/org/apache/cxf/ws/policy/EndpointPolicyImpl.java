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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;

/**
 * 
 */
public class EndpointPolicyImpl implements EndpointPolicy {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(EndpointPolicyImpl.class);
    
    private Policy policy;
    private Collection<PolicyAssertion> chosenAlternative;
    private Collection<PolicyAssertion> vocabulary;
    private Collection<PolicyAssertion> faultVocabulary;
    private List<Interceptor> interceptors;
    private List<Interceptor> faultInterceptors;
    
    private EndpointInfo ei;
    private PolicyEngineImpl engine;
    private boolean requestor;
    private Assertor assertor;
        
    public EndpointPolicyImpl() {
        
    }
    
    public EndpointPolicyImpl(EndpointInfo ei,
                              PolicyEngineImpl engine, 
                              boolean requestor,
                              Assertor assertor) {
        this.ei = ei;
        this.engine = engine;
        this.requestor = requestor;
        this.assertor = assertor;
    }
        
    public Policy getPolicy() {
        return policy;        
    }
    
    public EndpointPolicy updatePolicy(Policy p) {
        EndpointPolicyImpl epi = createEndpointPolicy();
        Policy np = (Policy)p.normalize(true);
        epi.setPolicy(getPolicy().merge(np));
        epi.checkExactlyOnes();
        epi.finalizeConfig();
        return epi;
    }
    
    public Collection<PolicyAssertion> getChosenAlternative() {
        return chosenAlternative;
    }
    
    public Collection<PolicyAssertion> getVocabulary() {
        return vocabulary;
    }
    
    public Collection<PolicyAssertion> getFaultVocabulary() {
        return faultVocabulary;
    }    
    
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }
    
    public List<Interceptor> getFaultInterceptors() {
        return faultInterceptors;
    }
    
    void initialize() {
        initializePolicy();
        checkExactlyOnes();
        finalizeConfig();
    }
    
    void finalizeConfig() {
        chooseAlternative();
        initializeVocabulary();
        initializeInterceptors(); 
    }
   
    void initializePolicy() {
        policy = engine.getAggregatedServicePolicy(ei.getService());
        policy = policy.merge(engine.getAggregatedEndpointPolicy(ei));
        if (!policy.isEmpty()) {
            policy = (Policy)policy.normalize(true);
        }
    }

    void chooseAlternative() {
        Collection<PolicyAssertion> alternative = null;
        if (requestor) {
            alternative = engine.getAlternativeSelector().selectAlternative(policy, engine, assertor);
        } else {
            alternative = getSupportedAlternatives();
        }
        if (null == alternative) {
            throw new PolicyException(new Message("NO_ALTERNATIVE_EXC", BUNDLE));
        } else {
            setChosenAlternative(alternative);
        }
    }
    
    protected Collection<PolicyAssertion> getSupportedAlternatives() {
        Collection<PolicyAssertion> alternatives = new ArrayList<PolicyAssertion>();
        for (Iterator it = policy.getAlternatives(); it.hasNext();) {
            List<PolicyAssertion> alternative = CastUtils.cast((List)it.next(), PolicyAssertion.class);
            if (engine.supportsAlternative(alternative, assertor)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }
    
    void initializeVocabulary() {
        vocabulary = new ArrayList<PolicyAssertion>();
        if (requestor) {
            faultVocabulary = new ArrayList<PolicyAssertion>();
        }
       
        // vocabulary of alternative chosen for endpoint
        
        for (PolicyAssertion a : getChosenAlternative()) {
            if (a.isOptional()) {
                continue;
            }
            vocabulary.add(a);            
            if (null != faultVocabulary) {
                faultVocabulary.add(a);
            }
        }
   
        // add assertions for specific inbound (in case of a server endpoint) or outbound 
        // (in case of a client endpoint) messages
        
        for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
            Policy p = engine.getAggregatedOperationPolicy(boi);
            Collection<PolicyAssertion> c = engine.getAssertions(p, false);
            vocabulary.addAll(c);
            if (null != faultVocabulary) {
                faultVocabulary.addAll(c);
            }
 
            if (!requestor) {
                p = engine.getAggregatedMessagePolicy(boi.getInput());
                vocabulary.addAll(engine.getAssertions(p, false));
            } else if (null != boi.getOutput()) {
                p = engine.getAggregatedMessagePolicy(boi.getOutput());
                vocabulary.addAll(engine.getAssertions(p, false));
                
                for (BindingFaultInfo bfi : boi.getFaults()) { 
                    p = engine.getAggregatedFaultPolicy(bfi);
                    faultVocabulary.addAll(engine.getAssertions(p, false));
                }
            }
        }
    }

    void initializeInterceptors() {
        PolicyInterceptorProviderRegistry reg 
            = engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class);
        interceptors = new ArrayList<Interceptor>();
        if (requestor) {
            faultInterceptors = new ArrayList<Interceptor>();
        }
        
        Set<QName> v = new HashSet<QName>();
        for (PolicyAssertion a : vocabulary) {
            v.add(a.getName());
        }
        
        for (QName qn : v) {
            PolicyInterceptorProvider pp = reg.get(qn);
            if (null != pp) {
                interceptors.addAll(pp.getInInterceptors());
            }
        }
        
        if (!requestor) {
            return;
        }
        
        Set<QName> faultV = new HashSet<QName>();
        for (PolicyAssertion a : faultVocabulary) {
            faultV.add(a.getName());
        }
        
        for (QName qn : faultV) {
            PolicyInterceptorProvider pp = reg.get(qn);
            if (null != pp) {
                faultInterceptors.addAll(pp.getInFaultInterceptors());
            }
        }        
    }
    
    // for test
    
    void setPolicy(Policy ep) {
        policy = ep;
    }
    
    void setChosenAlternative(Collection<PolicyAssertion> c) {
        chosenAlternative = c;
    }
    
    void setVocabulary(Collection<PolicyAssertion> v) {
        vocabulary = v;
    }
    
    void setFaultVocabulary(Collection<PolicyAssertion> v) {
        faultVocabulary = v;
    }
    
    void setInterceptors(List<Interceptor> in) {
        interceptors = in;
    }
    
    void setFaultInterceptors(List<Interceptor> inFault) {
        faultInterceptors = inFault;
    }
    
    protected EndpointPolicyImpl createEndpointPolicy() {
        return new EndpointPolicyImpl(this.ei,
                                      this.engine,
                                      this.requestor,
                                      this.assertor);
    }
    
    void checkExactlyOnes() {
        // Policy has been normalized and merged by now but unfortunately
        // ExactlyOnce have not been normalized properly by Neethi, for ex
        // <Policy>
        // <ExactlyOne><All><A></All></ExactlyOne>
        // <ExactlyOne><All><B></All></ExactlyOne>
        //  </Policy>
        // this is what we can see after the normalization happens but in fact this
        // is still unnormalized expression, should be
        // <Policy>
        // <ExactlyOne><All><A></All><All><B></All></ExactlyOne>
        // </Policy>
                
        List<?> assertions = policy.getPolicyComponents();
        if (assertions.size() <= 1) {
            return;
        }
        
        Policy p = new Policy();
        ExactlyOne alternatives = new ExactlyOne();
        p.addPolicyComponent(alternatives);
        for (Object a : assertions) {
            alternatives.addPolicyComponents(((ExactlyOne)a).getPolicyComponents());
        }
        setPolicy(p);        
    }
}
