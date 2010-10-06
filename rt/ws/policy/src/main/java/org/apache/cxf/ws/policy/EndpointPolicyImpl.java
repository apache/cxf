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
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
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
    public EndpointPolicyImpl(Policy p) {
        policy = p;
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
    
    public Assertor getAssertor() {
        return assertor;
    }
    
    public EndpointPolicy updatePolicy(Policy p) {
        EndpointPolicyImpl epi = createEndpointPolicy();
        
        if (!PolicyUtils.isEmptyPolicy(p)) {
            Policy normalizedPolicy 
                = (Policy)p.normalize(engine == null ? null : engine.getRegistry(), true);
            epi.setPolicy(getPolicy().merge(normalizedPolicy));
        } else {
            Policy clonedPolicy = new Policy();
            clonedPolicy.addPolicyComponents(getPolicy().getPolicyComponents());
            epi.setPolicy(clonedPolicy);
        }
        
        epi.checkExactlyOnes();
        epi.finalizeConfig();
        return epi;
    }
    
    public Collection<PolicyAssertion> getChosenAlternative() {
        return chosenAlternative;
    }
    
    public synchronized Collection<PolicyAssertion> getVocabulary() {
        if (vocabulary == null) {
            initializeVocabulary();
        }
        return vocabulary;
    }
    
    public synchronized Collection<PolicyAssertion> getFaultVocabulary() {
        if (vocabulary == null) {
            initializeVocabulary();
        }
        return faultVocabulary;
    }    
    
    public synchronized List<Interceptor> getInterceptors() {
        if (interceptors == null) {
            initializeInterceptors();
        }
        return interceptors;
    }
    
    public synchronized List<Interceptor> getFaultInterceptors() {
        if (interceptors == null) {
            initializeInterceptors();
        }
        return faultInterceptors;
    }
    
    void initialize() {
        initializePolicy();
        checkExactlyOnes();
        finalizeConfig();
    }
    
    void finalizeConfig() {
        chooseAlternative();
    }
   
    void initializePolicy() {
        policy = engine.getAggregatedServicePolicy(ei.getService());
        policy = policy.merge(engine.getAggregatedEndpointPolicy(ei));
        if (!policy.isEmpty()) {
            policy = (Policy)policy.normalize(engine == null ? null : engine.getRegistry(),
                                              true);
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
        if (getChosenAlternative() != null) { 
            for (PolicyAssertion a : getChosenAlternative()) {
                if (a.isOptional()) {
                    continue;
                }
                vocabulary.add(a);            
                if (null != faultVocabulary) {
                    faultVocabulary.add(a);
                }
            }
        }
   
        // add assertions for specific inbound (in case of a server endpoint) or outbound 
        // (in case of a client endpoint) messages
        for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
            EffectivePolicy p = null;
            if (this.requestor) {
                p = engine.getEffectiveClientRequestPolicy(ei, boi, 
                                                           (Conduit)assertor);
            } else {
                p = engine.getEffectiveServerRequestPolicy(ei, boi);
            }
            Collection<PolicyAssertion> c = engine.getAssertions(p, false);
            if (c != null) {
                vocabulary.addAll(c);
                if (null != faultVocabulary) {
                    faultVocabulary.addAll(c);
                }
            }
            if (this.requestor) {
                p = engine.getEffectiveClientResponsePolicy(ei, boi);
            } else {
                p = engine.getEffectiveServerResponsePolicy(ei, boi, 
                                                            (Destination)assertor);
            }
            c = engine.getAssertions(p, false);
            if (c != null) {
                vocabulary.addAll(c);
                if (null != faultVocabulary) {
                    faultVocabulary.addAll(c);
                }
            }
            if (boi.getFaults() != null) {
                for (BindingFaultInfo bfi : boi.getFaults()) {
                    if (this.requestor) {
                        p = engine.getEffectiveClientFaultPolicy(ei, bfi);
                    } else {
                        p = engine.getEffectiveServerFaultPolicy(ei, bfi, 
                                                                 (Destination)assertor);
                    }
                    c = engine.getAssertions(p, false);
                    if (c != null) {
                        vocabulary.addAll(c);
                        if (null != faultVocabulary) {
                            faultVocabulary.addAll(c);
                        }
                    }
                }
            }
        }
    }

    Collection<PolicyAssertion> getSupportedAlternatives(Policy p) {
        Collection<PolicyAssertion> alternatives = new ArrayList<PolicyAssertion>();
        for (Iterator it = p.getAlternatives(); it.hasNext();) {
            List<PolicyAssertion> alternative = CastUtils.cast((List)it.next(), PolicyAssertion.class);
            if (engine.supportsAlternative(alternative, null)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }

    void initializeInterceptors(PolicyInterceptorProviderRegistry reg,
                                Set<Interceptor> out,
                                PolicyAssertion a, 
                                boolean fault) {
        QName qn = a.getName();
        PolicyInterceptorProvider pp = reg.get(qn);
        if (null != pp) {
            out.addAll(fault ? pp.getInFaultInterceptors() : pp.getInInterceptors());
        }
        Policy p = a.getPolicy();
        if (p != null) {
            for (PolicyAssertion a2 : getSupportedAlternatives(p)) {
                initializeInterceptors(reg, out, a2, fault);
            }
        }
    }

    void initializeInterceptors() {
        if (engine == null || engine.getBus() == null
            || engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class) == null) {
            return;
        }
        PolicyInterceptorProviderRegistry reg 
            = engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class);
        
        Set<Interceptor> out = new LinkedHashSet<Interceptor>();
        if (getChosenAlternative() != null) {
            for (PolicyAssertion a : getChosenAlternative()) {
                initializeInterceptors(reg, out, a, false);
            }
        }

        if (requestor) {
            interceptors = new ArrayList<Interceptor>(out);
            out.clear();
            for (PolicyAssertion a : getChosenAlternative()) {
                initializeInterceptors(reg, out, a, true);
            }
            faultInterceptors = new ArrayList<Interceptor>(out);
        } else if (ei != null && ei.getBinding() != null) {
            for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
                EffectivePolicy p = engine.getEffectiveServerRequestPolicy(ei, boi);
                if (p == null || p.getPolicy() == null || p.getPolicy().isEmpty()) {
                    continue;
                }
                Collection<PolicyAssertion> c = engine.getAssertions(p, true);
                if (c != null) {
                    for (PolicyAssertion a : c) {
                        initializeInterceptors(reg, out, a, false);
                        initializeInterceptors(reg, out, a, true);
                    }
                }
            }
            interceptors = new ArrayList<Interceptor>(out);
        } else {
            interceptors = new ArrayList<Interceptor>(out);            
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
