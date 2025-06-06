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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.FactoryBeanListenerManager;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.policy.selector.MinimalAlternativeSelector;
import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;

/**
 *
 */
@NoJSR250Annotations(unlessNull = "bus")
public class PolicyEngineImpl implements PolicyEngine, BusExtension {
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyEngineImpl.class);


    private static final String POLICY_INFO_REQUEST_SERVER = "policy-engine-info-serve-request";
    private static final String POLICY_INFO_FAULT_SERVER = "policy-engine-info-serve-fault";
    private static final String POLICY_INFO_RESPONSE_SERVER = "policy-engine-info-serve-response";
    private static final String POLICY_INFO_ENDPOINT_SERVER = "policy-engine-info-serve-rendpoint";

    private static final String POLICY_INFO_REQUEST_CLIENT = "policy-engine-info-client-request";
    private static final String POLICY_INFO_FAULT_CLIENT = "policy-engine-info-client-fault";
    private static final String POLICY_INFO_RESPONSE_CLIENT = "policy-engine-info-client-response";
    private static final String POLICY_INFO_ENDPOINT_CLIENT = "policy-engine-info-client-endpoint";

    private Bus bus;
    private PolicyRegistry registry;
    private Collection<PolicyProvider> policyProviders;
    private Collection<PolicyProvider> preSetPolicyProviders = new LinkedList<>();
    private Policy busPolicy;
    private boolean enabled = true;
    private Boolean ignoreUnknownAssertions;
    private boolean addedBusInterceptors;
    private AlternativeSelector alternativeSelector;


    public PolicyEngineImpl() {
        init();
    }
    public PolicyEngineImpl(boolean en) {
        enabled = en;
        init();
    }
    public PolicyEngineImpl(Bus b) {
        init();
        setBus(b);
    }

    // configuration

    public boolean isEnabled() {
        return enabled;
    }

    @Resource
    public final void setBus(Bus b) {
        if (this.bus == b) {
            //avoid bus init twice through injection
            return;
        }
        bus = b;
        addBusInterceptors();
        FactoryBeanListenerManager fblm = bus.getExtension(FactoryBeanListenerManager.class);
        if (fblm != null) {
            for (FactoryBeanListener l : fblm.getListeners()) {
                if (l instanceof PolicyAnnotationListener) {
                    return;
                }
            }
            fblm.addListener(new PolicyAnnotationListener(bus));
        }
    }

    public Bus getBus() {
        return bus;
    }
    @Override
    public void addPolicy(Policy p) {
        if (busPolicy == null) {
            busPolicy = p;
        } else {
            busPolicy = busPolicy.merge(p);
        }
    }

    
    public void setPolicyProviders(Collection<PolicyProvider> p) {
        policyProviders = new CopyOnWriteArrayList<>(p);
    }

    public synchronized void addPolicyProvider(PolicyProvider p) {
        if (policyProviders != null) {
            policyProviders.add(p);
        } else {
            preSetPolicyProviders.add(p);
        }
    }
    public synchronized Collection<PolicyProvider> getPolicyProviders() {
        if (policyProviders == null) {
            policyProviders = new CopyOnWriteArrayList<>();
            if (bus != null) {
                ConfiguredBeanLocator loc = bus.getExtension(ConfiguredBeanLocator.class);
                if (loc != null) {
                    loc.getBeansOfType(PolicyProvider.class);
                }
            }
            policyProviders.addAll(preSetPolicyProviders);
            preSetPolicyProviders = null;
        }
        return policyProviders;
    }

    public void setRegistry(PolicyRegistry r) {
        registry = r;
    }

    public PolicyRegistry getRegistry() {
        return registry;
    }

    public synchronized void setEnabled(boolean e) {
        enabled = e;
        if (enabled && !addedBusInterceptors) {
            addBusInterceptors();
        } else if (!enabled && addedBusInterceptors) {
            removeBusInterceptors();
        }
    }

    public synchronized AlternativeSelector getAlternativeSelector() {
        if (alternativeSelector == null && enabled) {
            alternativeSelector = new MinimalAlternativeSelector();
        }
        return alternativeSelector;
    }

    public void setAlternativeSelector(AlternativeSelector as) {
        alternativeSelector = as;
    }

    public boolean isIgnoreUnknownAssertions() {
        return ignoreUnknownAssertions == null || ignoreUnknownAssertions;
    }

    public void setIgnoreUnknownAssertions(boolean ignore) {
        ignoreUnknownAssertions = ignore;
    }

    // BusExtension interface


    public Class<?> getRegistrationType() {
        return PolicyEngine.class;
    }

    // PolicyEngine interface

    public EffectivePolicy getEffectiveClientRequestPolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                           Conduit c, Message m) {
        EffectivePolicy effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_REQUEST_CLIENT);
        if (effectivePolicy == null) {
            synchronized (ei) {
                effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_REQUEST_CLIENT);
                if (null == effectivePolicy) {
                    EffectivePolicyImpl epi = createOutPolicyInfo();
                    Assertor assertor = PolicyUtils.createAsserter(c);
                    epi.initialise(ei, boi, this, assertor, true, true, m);
                    if (m != null) {
                        boi.setProperty(POLICY_INFO_REQUEST_CLIENT, epi);
                    }
                    effectivePolicy = epi;
                }
            }
        }
        return effectivePolicy;
    }

    public void setEffectiveClientRequestPolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                EffectivePolicy ep) {
        boi.setProperty(POLICY_INFO_REQUEST_CLIENT, ep);
    }

    public EffectivePolicy getEffectiveServerResponsePolicy(EndpointInfo ei,
                                                            BindingOperationInfo boi,
                                                            Destination d,
                                                            List<List<Assertion>> incoming,
                                                            Message m) {
        if (incoming == null) {
            EffectivePolicy effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_RESPONSE_SERVER);
            if (effectivePolicy == null) {
                synchronized (ei) {
                    effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_RESPONSE_SERVER);
                    if (null == effectivePolicy) {
                        EffectivePolicyImpl epi = createOutPolicyInfo();
                        Assertor assertor = PolicyUtils.createAsserter(d);
                        epi.initialise(ei, boi, this, assertor, false, false, null);
                        if (m != null) {
                            boi.setProperty(POLICY_INFO_RESPONSE_SERVER, epi);
                        }
                        effectivePolicy = epi;
                    }
                }
            }
            return effectivePolicy;
        }
        EffectivePolicyImpl epi = createOutPolicyInfo();
        Assertor assertor = PolicyUtils.createAsserter(d);
        epi.initialise(ei, boi, this, assertor, incoming, m);
        return epi;
    }

    public void setEffectiveServerResponsePolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                 EffectivePolicy ep) {
        boi.setProperty(POLICY_INFO_RESPONSE_SERVER, ep);
    }

    public EffectivePolicy getEffectiveServerFaultPolicy(EndpointInfo ei,
                                                         BindingOperationInfo boi,
                                                         BindingFaultInfo bfi,
                                                         Destination d,
                                                         Message m) {

        if (bfi == null) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            Assertor assertor = PolicyUtils.createAsserter(d);
            epi.initialise(ei, boi, null, this, assertor, m);
            return epi;
        }
        bfi = mapToWrappedBindingFaultInfo(bfi);
        EffectivePolicy effectivePolicy = (EffectivePolicy)bfi.getProperty(POLICY_INFO_FAULT_SERVER);
        if (effectivePolicy == null) {
            synchronized (ei) {
                effectivePolicy = (EffectivePolicy)bfi.getProperty(POLICY_INFO_FAULT_SERVER);
                if (null == effectivePolicy) {
                    EffectivePolicyImpl epi = createOutPolicyInfo();
                    Assertor assertor = PolicyUtils.createAsserter(d);
                    epi.initialise(ei, boi, bfi, this, assertor, m);
                    if (m != null) {
                        bfi.setProperty(POLICY_INFO_FAULT_SERVER, epi);
                    }
                    effectivePolicy = epi;
                }
            }
        }
        return effectivePolicy;
    }

    private BindingFaultInfo mapToWrappedBindingFaultInfo(BindingFaultInfo bfi) {
        BindingOperationInfo boi = bfi.getBindingOperation();
        if (boi != null && boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
            for (BindingFaultInfo bf2 : boi.getFaults()) {
                if (bf2.getFaultInfo().getName().equals(bfi.getFaultInfo().getName())) {
                    return bf2;
                }
            }
        }
        return bfi;
    }
    public void setEffectiveServerFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep) {
        bfi.setProperty(POLICY_INFO_FAULT_SERVER, ep);
    }

    public EndpointPolicy getClientEndpointPolicy(EndpointInfo ei, Conduit conduit, Message m) {
        Assertor assertor = PolicyUtils.createAsserter(conduit);
        return getEndpointPolicy(ei, true, assertor, m);
    }

    public EndpointPolicy getServerEndpointPolicy(EndpointInfo ei, Destination destination, Message m) {
        Assertor assertor = PolicyUtils.createAsserter(destination);
        return getEndpointPolicy(ei, false, assertor, m);
    }

    private EndpointPolicy getEndpointPolicy(
        EndpointInfo ei,
        boolean isRequestor,
        Assertor assertor,
        Message m) {
        return createEndpointPolicyInfo(ei, isRequestor, assertor, m);
    }

    public void setClientEndpointPolicy(EndpointInfo ei, EndpointPolicy ep) {
        ei.setProperty(POLICY_INFO_ENDPOINT_CLIENT, ep);
    }

    public void setServerEndpointPolicy(EndpointInfo ei, EndpointPolicy ep) {
        ei.setProperty(POLICY_INFO_ENDPOINT_SERVER, ep);
    }

    public EffectivePolicy getEffectiveServerRequestPolicy(EndpointInfo ei,
                                                           BindingOperationInfo boi,
                                                           Message m) {
        EffectivePolicy effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_REQUEST_SERVER);
        if (effectivePolicy == null) {
            synchronized (ei) {
                effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_REQUEST_SERVER);
                if (null == effectivePolicy) {
                    EffectivePolicyImpl epi = createOutPolicyInfo();
                    epi.initialise(ei, boi, this, false, true, m);
                    if (m != null) {
                        boi.setProperty(POLICY_INFO_REQUEST_SERVER, epi);
                    }
                    effectivePolicy = epi;
                }
            }
        }
        return effectivePolicy;
    }

    public void setEffectiveServerRequestPolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                EffectivePolicy ep) {
        boi.setProperty(POLICY_INFO_REQUEST_SERVER, ep);
    }

    public EffectivePolicy getEffectiveClientResponsePolicy(EndpointInfo ei,
                                                            BindingOperationInfo boi,
                                                            Message m) {
        EffectivePolicy effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_RESPONSE_CLIENT);
        if (effectivePolicy == null) {
            synchronized (ei) {
                effectivePolicy = (EffectivePolicy)boi.getProperty(POLICY_INFO_RESPONSE_CLIENT);
                if (null == effectivePolicy) {
                    EffectivePolicyImpl epi = createOutPolicyInfo();
                    epi.initialise(ei, boi, this, true, false, m);
                    if (m != null) {
                        boi.setProperty(POLICY_INFO_RESPONSE_CLIENT, epi);
                    }
                    effectivePolicy = epi;
                }
            }
        }
        return effectivePolicy;
    }

    public void setEffectiveClientResponsePolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                 EffectivePolicy ep) {
        boi.setProperty(POLICY_INFO_RESPONSE_CLIENT, ep);
    }

    public EffectivePolicy getEffectiveClientFaultPolicy(EndpointInfo ei,
                                                         BindingOperationInfo boi,
                                                         BindingFaultInfo bfi,
                                                         Message m) {
        EffectivePolicy effectivePolicy = null;
        if (bfi != null) {
            effectivePolicy = (EffectivePolicy)bfi.getProperty(POLICY_INFO_FAULT_CLIENT);
        }
        if (effectivePolicy == null) {
            synchronized (ei) {
                if (bfi != null) {
                    effectivePolicy = (EffectivePolicy)bfi.getProperty(POLICY_INFO_FAULT_CLIENT);
                }
                if (null == effectivePolicy) {
                    EffectivePolicyImpl epi = createOutPolicyInfo();
                    epi.initialisePolicy(ei, boi, bfi, this, m);
                    if (bfi != null) {
                        bfi.setProperty(POLICY_INFO_FAULT_CLIENT, epi);
                    }
                    effectivePolicy = epi;
                }
            }
        }
        return effectivePolicy;
    }

    public void setEffectiveClientFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep) {
        bfi.setProperty(POLICY_INFO_FAULT_CLIENT, ep);
    }

    // implementation

    protected final void init() {
        registry = new PolicyRegistryImpl();
    }



    public synchronized void removeBusInterceptors() {
        bus.getInInterceptors().remove(PolicyInInterceptor.INSTANCE);
        bus.getOutInterceptors().remove(PolicyOutInterceptor.INSTANCE);
        bus.getInFaultInterceptors().remove(ClientPolicyInFaultInterceptor.INSTANCE);
        bus.getOutFaultInterceptors().remove(ServerPolicyOutFaultInterceptor.INSTANCE);
        bus.getInFaultInterceptors().remove(PolicyVerificationInFaultInterceptor.INSTANCE);
        addedBusInterceptors = false;
    }

    public final synchronized void addBusInterceptors() {
        if (null == bus || !enabled) {
            return;
        }

        if (ignoreUnknownAssertions != null) {
            AssertionBuilderRegistry abr = bus.getExtension(AssertionBuilderRegistry.class);
            if (null != abr) {
                abr.setIgnoreUnknownAssertions(ignoreUnknownAssertions);
            }
        }

        bus.getInInterceptors().add(PolicyInInterceptor.INSTANCE);
        bus.getOutInterceptors().add(PolicyOutInterceptor.INSTANCE);
        bus.getInFaultInterceptors().add(ClientPolicyInFaultInterceptor.INSTANCE);
        bus.getOutFaultInterceptors().add(ServerPolicyOutFaultInterceptor.INSTANCE);
        bus.getInFaultInterceptors().add(PolicyVerificationInFaultInterceptor.INSTANCE);

        addedBusInterceptors = true;
    }

    Policy getAggregatedServicePolicy(ServiceInfo si, Message m) {
        if (si == null) {
            return new Policy();
        }
        Policy aggregated = busPolicy;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(si, m);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedEndpointPolicy(EndpointInfo ei, Message m) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(ei, m);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedOperationPolicy(BindingOperationInfo boi, Message m) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(boi, m);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedMessagePolicy(BindingMessageInfo bmi, Message m) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bmi, m);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedFaultPolicy(BindingFaultInfo bfi, Message m) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bfi, m);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    /**
     * Return a collection of all assertions used in the given policy component,
     * optionally including optional assertions.
     * The policy need not be normalised, so any policy references will have to be resolved.
     * @param pc the policy component
     * @param includeOptional flag indicating if optional assertions should be included
     * @return the assertions
     */
    Collection<Assertion> getAssertions(PolicyComponent pc, boolean includeOptional) {

        Collection<Assertion> assertions = new ArrayList<>();

        if (Constants.TYPE_ASSERTION == pc.getType()) {
            Assertion a = (Assertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add(a);
            }
        } else {
            addAssertions(pc, includeOptional, assertions);
        }
        return assertions;
    }
    Collection<Assertion> getAssertions(EffectivePolicy pc, boolean includeOptional) {
        if (pc == null || pc.getChosenAlternative() == null) {
            return null;
        }
        Collection<Assertion> assertions = new ArrayList<>();
        for (Assertion assertion : pc.getChosenAlternative()) {
            if (Constants.TYPE_ASSERTION == assertion.getType()) {
                if (includeOptional || !assertion.isOptional()) {
                    assertions.add(assertion);
                }
            } else {
                addAssertions(assertion, includeOptional, assertions);
            }
        }
        return assertions;
    }

    void addAssertions(PolicyComponent pc, boolean includeOptional,
                       Collection<Assertion> assertions) {

        if (Constants.TYPE_ASSERTION == pc.getType()) {
            Assertion a = (Assertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add((Assertion)pc);
            }
            return;
        }

        if (Constants.TYPE_POLICY_REF == pc.getType()) {
            PolicyReference pr = (PolicyReference)pc;
            pc = pr.normalize(registry, false);
        }

        PolicyOperator po = (PolicyOperator)pc;

        List<PolicyComponent> pcs = CastUtils.cast(po.getPolicyComponents(), PolicyComponent.class);
        for (PolicyComponent child : pcs) {
            addAssertions(child, includeOptional, assertions);
        }
    }

    /**
     * Return the vocabulary of a policy component, i.e. the set of QNames of
     * the assertions used in the componente, duplicates removed.
     * @param pc the policy component
     * @param includeOptional flag indicating if optional assertions should be included
     * @return the vocabulary
     */
    Set<QName> getVocabulary(PolicyComponent pc, boolean includeOptional) {
        Collection<Assertion> assertions = getAssertions(pc, includeOptional);
        Set<QName> vocabulary = new HashSet<>();
        for (Assertion a : assertions) {
            vocabulary.add(a.getName());
        }
        return vocabulary;
    }

    EndpointPolicy createEndpointPolicyInfo(EndpointInfo ei,
                                                boolean isRequestor,
                                                Assertor assertor,
                                                Message m) {
        EndpointPolicy ep = (EndpointPolicy)ei.getProperty(isRequestor
                                                           ? POLICY_INFO_ENDPOINT_CLIENT : POLICY_INFO_ENDPOINT_SERVER);
        if (ep == null) {
            synchronized (ei) {
                ep = (EndpointPolicy)ei.getProperty(isRequestor
                    ? POLICY_INFO_ENDPOINT_CLIENT : POLICY_INFO_ENDPOINT_SERVER);
                if (ep == null) {
                    EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, this, isRequestor, assertor);
                    epi.initialize(m);
                    if (m != null) {
                        ei.setProperty(isRequestor ? POLICY_INFO_ENDPOINT_CLIENT : POLICY_INFO_ENDPOINT_SERVER, epi);
                    }
                    ep = epi;
                }
            }
        }
        return ep;
    }


    /**
     * Check if a given list of assertions can potentially be supported by
     * interceptors or by an already installed assertor (a conduit or transport
     * that implements the Assertor interface).
     *
     * @param alternative the policy alternative
     * @param assertor the assertor
     * @return true iff the alternative can be supported
     */
    public boolean supportsAlternative(Collection<? extends PolicyComponent> alternative,
                                       Assertor assertor,
                                       Message m) {
        PolicyInterceptorProviderRegistry pipr =
            bus.getExtension(PolicyInterceptorProviderRegistry.class);
        final boolean doLog = LOG.isLoggable(Level.FINE);
        for (PolicyComponent pc : alternative) {
            if (pc instanceof Assertion) {
                Assertion a = (Assertion)pc;
                if (!a.isOptional()) {
                    if (null != assertor && assertor.canAssert(a.getName())) {
                        continue;
                    }
                    Set<PolicyInterceptorProvider> s = pipr.get(a.getName());
                    if (s.isEmpty()) {
                        if (doLog) {
                            LOG.fine("Alternative " + a.getName() + " is not supported");
                        }
                        return false;
                    }
                    for (PolicyInterceptorProvider p : s) {
                        if (!p.configurationPresent(m, a)) {
                            if (doLog) {
                                LOG.fine("Alternative " + a.getName() + " is not supported");
                            }
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }


    // for test
    EffectivePolicyImpl createOutPolicyInfo() {
        return new EffectivePolicyImpl();
    }


}
