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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyContainingAssertion;

/**
 *
 */
public class EndpointPolicyImpl implements EndpointPolicy {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(EndpointPolicyImpl.class);

    private Policy policy;
    private Collection<Assertion> chosenAlternative;

    private volatile Collection<Assertion> vocabulary;
    private Collection<Assertion> faultVocabulary;
    private volatile List<Interceptor<? extends Message>> interceptors;
    private List<Interceptor<? extends Message>> faultInterceptors;

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

    public EndpointPolicy updatePolicy(Policy p, Message msg) {
        EndpointPolicyImpl epi = createEndpointPolicy();

        if (!PolicyUtils.isEmptyPolicy(p)) {
            Policy normalizedPolicy
                = p.normalize(engine == null ? null : engine.getRegistry(), true);
            epi.setPolicy(getPolicy().merge(normalizedPolicy));
        } else {
            Policy clonedPolicy = new Policy();
            clonedPolicy.addPolicyComponents(getPolicy().getPolicyComponents());
            epi.setPolicy(clonedPolicy);
        }

        epi.checkExactlyOnes();
        epi.finalizeConfig(msg);
        return epi;
    }

    public Collection<Assertion> getChosenAlternative() {
        return chosenAlternative;
    }

    public Collection<Assertion> getVocabulary(Message m) {
        if (vocabulary == null) {
            initializeVocabulary(m);
        }
        return vocabulary;
    }

    public Collection<Assertion> getFaultVocabulary(Message m) {
        if (vocabulary == null) {
            initializeVocabulary(m);
        }
        return faultVocabulary;
    }

    public List<Interceptor<? extends Message>> getInterceptors(Message m) {
        if (interceptors == null) {
            initializeInterceptors(m);
        }
        return interceptors;
    }

    public List<Interceptor<? extends Message>> getFaultInterceptors(Message m) {
        if (interceptors == null) {
            initializeInterceptors(m);
        }
        return faultInterceptors;
    }

    public void initialize(Message m) {
        initializePolicy(m);
        checkExactlyOnes();
        finalizeConfig(m);
    }

    void finalizeConfig(Message m) {
        chooseAlternative(m);
    }

    void initializePolicy(Message m) {
        if (engine != null) {
            policy = engine.getAggregatedServicePolicy(ei.getService(), m);
            policy = policy.merge(engine.getAggregatedEndpointPolicy(ei, m));
            if (!policy.isEmpty()) {
                policy = policy.normalize(engine.getRegistry(), true);
            }
        }
    }

    void chooseAlternative(Message m) {
        final Collection<Assertion> alternative;
        if (requestor) {
            if (engine.isEnabled()) {
                alternative = engine.getAlternativeSelector().selectAlternative(policy, engine, assertor, null, m);
            } else {
                // use an empty list to avoid getting NPE
                alternative = Collections.emptyList();
            }
        } else {
            alternative = getSupportedAlternatives(m);
        }
        if (null == alternative) {
            throw new PolicyException(new org.apache.cxf.common.i18n.Message("NO_ALTERNATIVE_EXC", BUNDLE));
        }
        setChosenAlternative(alternative);
    }

    protected Collection<Assertion> getSupportedAlternatives(Message msg) {
        Collection<Assertion> alternatives = new ArrayList<>();

        for (Iterator<List<Assertion>> it = policy.getAlternatives(); it.hasNext();) {
            List<Assertion> alternative = it.next();
            if (engine.supportsAlternative(alternative, assertor, msg)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }

    private void addAll(Collection<Assertion> target, Collection<Assertion> l1) {
        for (Assertion l : l1) {
            if (!target.contains(l)) {
                target.add(l);
            }
        }
    }

    synchronized void initializeVocabulary(Message m) {
        if (vocabulary != null) {
            return;
        }

        List<Assertion> v = new ArrayList<>();
        List<Assertion> fv = null;
        if (requestor) {
            fv = new ArrayList<>();
        }

        // vocabulary of alternative chosen for endpoint
        if (getChosenAlternative() != null) {
            for (Assertion a : getChosenAlternative()) {
                if (a.isOptional()) {
                    continue;
                }
                v.add(a);
                if (null != fv) {
                    fv.add(a);
                }
            }
        }

        // add assertions for specific inbound (in case of a server endpoint) or outbound
        // (in case of a client endpoint) messages
        for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
            EffectivePolicy p;
            if (!this.requestor) {
                p = engine.getEffectiveServerRequestPolicy(ei, boi, m);
                Collection<Assertion> c = engine.getAssertions(p, false);
                if (c != null) {
                    addAll(v, c);
                }
            } else {
                p = engine.getEffectiveClientResponsePolicy(ei, boi, m);
                Collection<Assertion> c = engine.getAssertions(p, false);
                if (c != null) {
                    addAll(v, c);
                    if (null != fv) {
                        addAll(fv, c);
                    }
                }
                if (boi.getFaults() != null && null != fv) {
                    for (BindingFaultInfo bfi : boi.getFaults()) {
                        p = engine.getEffectiveClientFaultPolicy(ei, boi, bfi, m);
                        c = engine.getAssertions(p, false);
                        if (c != null) {
                            addAll(fv, c);
                        }
                    }
                }
            }
        }
        if (requestor) {
            faultVocabulary = fv;
        }
        vocabulary = v;
    }

    Collection<Assertion> getSupportedAlternatives(Policy p, Message msg) {
        Collection<Assertion> alternatives = new ArrayList<>();
        for (Iterator<List<Assertion>> it = p.getAlternatives(); it.hasNext();) {
            List<Assertion> alternative = it.next();
            if (engine.supportsAlternative(alternative, null, msg)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }

    void initializeInterceptors(PolicyInterceptorProviderRegistry reg,
                                Set<Interceptor<? extends Message>> out, Assertion a,
                                boolean fault, Message msg) {
        QName qn = a.getName();
        List<Interceptor<? extends org.apache.cxf.message.Message>> i
            = fault ? reg.getInFaultInterceptorsForAssertion(qn)
            : reg.getInInterceptorsForAssertion(qn);
        out.addAll(i);
        if (a instanceof PolicyContainingAssertion) {
            Policy p = ((PolicyContainingAssertion)a).getPolicy();
            if (p != null) {
                for (Assertion a2 : getSupportedAlternatives(p, msg)) {
                    initializeInterceptors(reg, out, a2, fault, msg);
                }
            }
        }
    }

    synchronized void initializeInterceptors(Message m) {
        if (interceptors != null) {
            return;
        }

        if (engine == null || engine.getBus() == null
            || engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class) == null) {
            return;
        }
        PolicyInterceptorProviderRegistry reg
            = engine.getBus().getExtension(PolicyInterceptorProviderRegistry.class);

        Set<Interceptor<? extends Message>> out = new LinkedHashSet<>();
        if (getChosenAlternative() != null) {
            for (Assertion a : getChosenAlternative()) {
                initializeInterceptors(reg, out, a, false, m);
            }
        }

        final List<Interceptor<? extends Message>> tmp;
        if (requestor) {
            tmp = new ArrayList<>(out);
            out.clear();
            for (Assertion a : getChosenAlternative()) {
                initializeInterceptors(reg, out, a, true, m);
            }
            faultInterceptors = new ArrayList<>(out);
        } else if (ei != null && ei.getBinding() != null) {
            for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
                EffectivePolicy p = engine.getEffectiveServerRequestPolicy(ei, boi, m);
                if (p == null || p.getPolicy() == null || p.getPolicy().isEmpty()) {
                    continue;
                }
                Collection<Assertion> c = engine.getAssertions(p, true);
                if (c != null) {
                    for (Assertion a : c) {
                        initializeInterceptors(reg, out, a, false, m);
                        initializeInterceptors(reg, out, a, true, m);
                    }
                }
            }
            tmp = new ArrayList<>(out);
        } else {
            tmp = new ArrayList<>(out);
        }
        interceptors = tmp;
    }

    // for test

    void setPolicy(Policy ep) {
        policy = ep;
    }

    void setChosenAlternative(Collection<Assertion> c) {
        chosenAlternative = c;
    }

    void setVocabulary(Collection<Assertion> v) {
        vocabulary = v;
    }

    void setFaultVocabulary(Collection<Assertion> v) {
        faultVocabulary = v;
    }

    void setInterceptors(List<Interceptor<? extends Message>> in) {
        interceptors = in;
    }

    void setFaultInterceptors(List<Interceptor<? extends Message>> inFault) {
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
