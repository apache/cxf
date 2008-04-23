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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.policy.selector.MinimalAlternativeSelector;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyOperator;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;

/**
 * 
 */
public class PolicyEngineImpl implements PolicyEngine, BusExtension, ServerLifeCycleListener {

    private Bus bus;
    private PolicyRegistry registry;
    private Collection<PolicyProvider> policyProviders;
    private boolean enabled;
    private boolean ignoreUnknownAssertions;
    private boolean addedBusInterceptors;
    private AlternativeSelector alternativeSelector;

    private Map<BindingOperation, EffectivePolicy> clientRequestInfo;
    private Map<BindingOperation, EffectivePolicy> clientResponseInfo;
    private Map<BindingFault, EffectivePolicy> clientFaultInfo;
    private Map<BindingOperation, EffectivePolicy> serverRequestInfo;
    private Map<BindingOperation, EffectivePolicy> serverResponseInfo;
    private Map<BindingFault, EffectivePolicy> serverFaultInfo;
    private Map<EndpointInfo, EndpointPolicy> serverEndpointInfo;
    private Map<EndpointInfo, EndpointPolicy> clientEndpointInfo;

    public PolicyEngineImpl() { 
        init();
    }

    // configuration

    public boolean isEnabled() {
        return enabled;
    }

    public void setBus(Bus b) {
        bus = b;
    }

    public Bus getBus() {
        return bus;
    }

    public void setPolicyProviders(Collection<PolicyProvider> p) {
        policyProviders = p;
    }
   
    public Collection<PolicyProvider> getPolicyProviders() {
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
    
        if (!addedBusInterceptors) {
            addBusInterceptors();
        }
    }

    public AlternativeSelector getAlternativeSelector() {    
        return alternativeSelector;
    }

    public void setAlternativeSelector(AlternativeSelector as) {
        alternativeSelector = as;
    }

    public boolean isIgnoreUnknownAssertions() {
        return ignoreUnknownAssertions;
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
                                                           Conduit c) {
        BindingOperation bo = new BindingOperation(ei, boi);
        EffectivePolicy effectivePolicy = clientRequestInfo.get(bo);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            Assertor assertor = null;
            if (c instanceof Assertor) {
                assertor = (Assertor)c;
            }
            epi.initialise(ei, bo.getBindingOperation(), this, assertor, true);
            clientRequestInfo.put(bo, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveClientRequestPolicy(EndpointInfo ei, BindingOperationInfo boi, 
                                                EffectivePolicy ep) {
        BindingOperation bo = new BindingOperation(ei, boi);
        clientRequestInfo.put(bo, ep);
    }

    public EffectivePolicy getEffectiveServerResponsePolicy(EndpointInfo ei, BindingOperationInfo boi,
                                                            Destination d) {
        BindingOperation bo = new BindingOperation(ei, boi);
        EffectivePolicy effectivePolicy = serverResponseInfo.get(bo);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            Assertor assertor = null;
            if (d instanceof Assertor) {
                assertor = (Assertor)d;
            }
            epi.initialise(ei, bo.getBindingOperation(), this, assertor, false);
            serverResponseInfo.put(bo, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveServerResponsePolicy(EndpointInfo ei, BindingOperationInfo boi, 
                                                 EffectivePolicy ep) {
        BindingOperation bo = new BindingOperation(ei, boi);
        serverResponseInfo.put(bo, ep);
    }
  
    public EffectivePolicy getEffectiveServerFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, 
                                                         Destination d) {
        BindingFault bf = new BindingFault(ei, bfi);
        EffectivePolicy effectivePolicy = serverFaultInfo.get(bf);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            Assertor assertor = null;
            if (d instanceof Assertor) {
                assertor = (Assertor)d;
            }
            epi.initialise(ei, bfi, this, assertor);
            serverFaultInfo.put(bf, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveServerFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep) {
        BindingFault bf = new BindingFault(ei, bfi);
        serverFaultInfo.put(bf, ep);
    }

    public EndpointPolicy getClientEndpointPolicy(EndpointInfo ei, Conduit conduit) {
        Assertor assertor = conduit instanceof Assertor ? (Assertor)conduit : null;
        return getEndpointPolicy(ei, clientEndpointInfo.get(ei), true, assertor);
    }
   
    public EndpointPolicy getServerEndpointPolicy(EndpointInfo ei, Destination destination) {
    
        Assertor assertor = destination instanceof Assertor ? (Assertor)destination : null;
        return getEndpointPolicy(ei, serverEndpointInfo.get(ei), false, assertor);
    }

    private EndpointPolicy getEndpointPolicy(
        EndpointInfo ei, EndpointPolicy ep, boolean isRequestor, Assertor assertor) {
        if (null != ep) {
            return ep; 
        }
        return createEndpointPolicyInfo(ei, isRequestor, assertor);
    }

    public void setClientEndpointPolicy(EndpointInfo ei, EndpointPolicy ep) {
        clientEndpointInfo.put(ei, ep);
    }

    public void setServerEndpointPolicy(EndpointInfo ei, EndpointPolicy ep) {
        serverEndpointInfo.put(ei, ep);
    }

    public EffectivePolicy getEffectiveServerRequestPolicy(EndpointInfo ei, BindingOperationInfo boi) {
        BindingOperation bo = new BindingOperation(ei, boi);
        EffectivePolicy effectivePolicy = serverRequestInfo.get(bo);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            epi.initialisePolicy(ei, bo.getBindingOperation(), this, false);
            serverRequestInfo.put(bo, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveServerRequestPolicy(EndpointInfo ei, BindingOperationInfo boi, 
                                                EffectivePolicy ep) {
        BindingOperation bo = new BindingOperation(ei, boi);
        serverRequestInfo.put(bo, ep);
    }

    public EffectivePolicy getEffectiveClientResponsePolicy(EndpointInfo ei, BindingOperationInfo boi) {
        BindingOperation bo = new BindingOperation(ei, boi);
        EffectivePolicy effectivePolicy = clientResponseInfo.get(bo);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            epi.initialisePolicy(ei, bo.getBindingOperation(), this, true);        
            clientResponseInfo.put(bo, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveClientResponsePolicy(EndpointInfo ei, BindingOperationInfo boi, 
                                                 EffectivePolicy ep) {
        BindingOperation bo = new BindingOperation(ei, boi);
        clientResponseInfo.put(bo, ep);
    }

    public EffectivePolicy getEffectiveClientFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi) {
        BindingFault bf = new BindingFault(ei, bfi);
        EffectivePolicy effectivePolicy = clientFaultInfo.get(bf);
        if (null == effectivePolicy) {
            EffectivePolicyImpl epi = createOutPolicyInfo();
            epi.initialisePolicy(ei, bfi, this);
            clientFaultInfo.put(bf, epi);
            effectivePolicy = epi;
        }
        return effectivePolicy;
    }

    public void setEffectiveClientFaultPolicy(EndpointInfo ei, BindingFaultInfo bfi, EffectivePolicy ep) {
        BindingFault bf = new BindingFault(ei, bfi);
        clientFaultInfo.put(bf, ep);
    }

    // implementation

    protected final void init() {
    
        registry = new PolicyRegistryImpl();
    
        clientRequestInfo 
            = new ConcurrentHashMap<BindingOperation, EffectivePolicy>();

        clientResponseInfo 
            = new ConcurrentHashMap<BindingOperation, EffectivePolicy>();

        clientFaultInfo 
            = new ConcurrentHashMap<BindingFault, EffectivePolicy>();

        serverEndpointInfo 
            = new EndpointPolicyMap();
        clientEndpointInfo 
            = new EndpointPolicyMap();

        serverRequestInfo 
            = new ConcurrentHashMap<BindingOperation, EffectivePolicy>();

        serverResponseInfo 
            = new ConcurrentHashMap<BindingOperation, EffectivePolicy>();

        serverFaultInfo 
            = new ConcurrentHashMap<BindingFault, EffectivePolicy>();    
    }



    @PostConstruct
    public void addBusInterceptors() {
    
        if (null == alternativeSelector) {
            alternativeSelector = new MinimalAlternativeSelector();
        }
    
        if (null == bus || !enabled) {
            return;
        }
    
        AssertionBuilderRegistry abr = bus.getExtension(AssertionBuilderRegistry.class);
        if (null != abr) {
            abr.setIgnoreUnknownAssertions(ignoreUnknownAssertions);
        }

        ClientPolicyOutInterceptor clientOut = new ClientPolicyOutInterceptor();
        clientOut.setBus(bus);
        bus.getOutInterceptors().add(clientOut);
        ClientPolicyInInterceptor clientIn = new ClientPolicyInInterceptor();
        clientIn.setBus(bus);
        bus.getInInterceptors().add(clientIn);
        ClientPolicyInFaultInterceptor clientInFault = new ClientPolicyInFaultInterceptor();
        clientInFault.setBus(bus);
        bus.getInFaultInterceptors().add(clientInFault);
    
        ServerPolicyInInterceptor serverIn = new ServerPolicyInInterceptor();
        serverIn.setBus(bus);
        bus.getInInterceptors().add(serverIn);
        ServerPolicyOutInterceptor serverOut = new ServerPolicyOutInterceptor();
        serverOut.setBus(bus);
        bus.getOutInterceptors().add(serverOut);
        ServerPolicyOutFaultInterceptor serverOutFault = new ServerPolicyOutFaultInterceptor();
        serverOutFault.setBus(bus);
        bus.getOutFaultInterceptors().add(serverOutFault);
    
        PolicyVerificationOutInterceptor verifyOut = new PolicyVerificationOutInterceptor();
        verifyOut.setBus(bus);
        bus.getOutInterceptors().add(verifyOut);
        PolicyVerificationInInterceptor verifyIn = new PolicyVerificationInInterceptor();
        verifyIn.setBus(bus);
        bus.getInInterceptors().add(verifyIn);
        PolicyVerificationInFaultInterceptor verifyInFault = new PolicyVerificationInFaultInterceptor();
        verifyInFault.setBus(bus);
        bus.getInFaultInterceptors().add(verifyInFault);
    
        addedBusInterceptors = true;
    }  

    @PostConstruct
    public void registerListener() {
        ServerLifeCycleManager slm = bus.getExtension(ServerLifeCycleManager.class);
        if (slm != null) {
            slm.registerListener(this);
        }
    }

    Policy getAggregatedServicePolicy(ServiceInfo si) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(si);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedEndpointPolicy(EndpointInfo ei) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(ei);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedOperationPolicy(BindingOperationInfo boi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(boi);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedMessagePolicy(BindingMessageInfo bmi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bmi);
            if (null == aggregated) {
                aggregated = p;
            } else if (p != null) {
                aggregated = aggregated.merge(p);
            }
        }
        return aggregated == null ? new Policy() : aggregated;
    }

    Policy getAggregatedFaultPolicy(BindingFaultInfo bfi) {
        Policy aggregated = null;
        for (PolicyProvider pp : getPolicyProviders()) {
            Policy p = pp.getEffectivePolicy(bfi);
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
    Collection<PolicyAssertion> getAssertions(PolicyComponent pc, boolean includeOptional) {
    
        Collection<PolicyAssertion> assertions = new ArrayList<PolicyAssertion>();
    
        if (Constants.TYPE_ASSERTION == pc.getType()) {
            PolicyAssertion a = (PolicyAssertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add(a);
            }
        } else {   
            addAssertions(pc, includeOptional, assertions);
        }
        return assertions;
    }

    void addAssertions(PolicyComponent pc, boolean includeOptional, 
                               Collection<PolicyAssertion> assertions) {
   
        if (Constants.TYPE_ASSERTION == pc.getType()) {
            PolicyAssertion a = (PolicyAssertion)pc;
            if (includeOptional || !a.isOptional()) {
                assertions.add((PolicyAssertion)pc);            
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
        Collection<PolicyAssertion> assertions = getAssertions(pc, includeOptional);
        Set<QName> vocabulary = new HashSet<QName>();
        for (PolicyAssertion a : assertions) {
            vocabulary.add(a.getName());
        }
        return vocabulary;
    } 

    EndpointPolicyImpl createEndpointPolicyInfo(EndpointInfo ei, boolean isRequestor, Assertor assertor) {
        EndpointPolicyImpl epi = new EndpointPolicyImpl(ei, this, isRequestor, assertor);
        epi.initialize();
    
        Map<EndpointInfo, EndpointPolicy> map = isRequestor ? clientEndpointInfo : serverEndpointInfo;
        map.put(ei, epi);

        return epi;
    }


    /**
     * Check if a given list of assertions can potentially be supported by
     * interceptors or by an already installed assertor (a conduit or transport
     * that implements the Assertor interface).
     * 
     * @param alternative the policy alternative
     * @param Assertor the assertor
     * @return true iff the alternative can be supported
     */
    public boolean supportsAlternative(Collection<PolicyAssertion> alternative, 
                                       Assertor assertor) {
        PolicyInterceptorProviderRegistry pipr = 
            bus.getExtension(PolicyInterceptorProviderRegistry.class);
        for (PolicyAssertion a : alternative) {
            if (!(a.isOptional() 
                || (null != pipr.get(a.getName())) 
                || (null != assertor && assertor.canAssert(a.getName())))) {
                return false;
            }
        }
        return true;
    }

    public void startServer(Server server) {
        // empty
    }

    /**
     * Callback recieved while the server side endpoint is being undeployed.
     *
     * @param server
     */
    public void stopServer(Server server) {
        EndpointInfo ei = server.getEndpoint().getEndpointInfo();
        serverEndpointInfo.remove(ei);

        /**
         * While cleaning up the entries of requestInfo's, responseInfo's and faultInfo's map, we create
         * a temperory Set with all the keys. Later we iterate over the keys of this temp set,
         * and if it's same as that of the endpoint being undeployed, we remove the corresponding
         * entries from the client and server maps.
         */

        cleanupBindingOperations(ei, new HashSet<BindingOperation>(clientRequestInfo.keySet()),
                clientRequestInfo);
        cleanupBindingOperations(ei, new HashSet<BindingOperation>(clientResponseInfo.keySet()),
                clientResponseInfo);

        cleanupBindingOperations(ei, new HashSet<BindingOperation>(serverRequestInfo.keySet()),
                serverRequestInfo);
        cleanupBindingOperations(ei, new HashSet<BindingOperation>(serverResponseInfo.keySet()),
                serverResponseInfo);

        cleanupBindingFaults(ei, new HashSet<BindingFault>(clientFaultInfo.keySet()),
                clientFaultInfo);
        cleanupBindingFaults(ei, new HashSet<BindingFault>(serverFaultInfo.keySet()),
                serverFaultInfo);
    }

    private void cleanupBindingOperations(EndpointInfo ei,
                                          Set<BindingOperation> bindingOperations,
                                          Map<BindingOperation, EffectivePolicy> originalMap) {
        
        Iterator<BindingOperation> bindingOpsItr = bindingOperations.iterator();
        while (bindingOpsItr.hasNext()) {
            BindingOperation bindingOperation = bindingOpsItr.next();
            if (ei.isSameAs(bindingOperation.getEndpoint())) {
                originalMap.remove(bindingOperation);
            }
        }
    }

    private void cleanupBindingFaults(EndpointInfo ei, Set <BindingFault> bindingFaults,
                                      Map<BindingFault, EffectivePolicy> originalMap) {
        Iterator<BindingFault> bindingFaultsItr = bindingFaults.iterator();
        while (bindingFaultsItr.hasNext()) {
            BindingFault bindingFault = bindingFaultsItr.next();
            if (ei.isSameAs(bindingFault.getEndpoint())) {
                originalMap.remove(bindingFault);
            }
        }
    }
    /**
     * Class used as key in the client request policy and server response policy maps.
     */
    class BindingOperation {
        private EndpointInfo ei;
        private BindingOperationInfo boi;
    
        BindingOperation(EndpointInfo e, BindingOperationInfo b) {
            ei = e;
            boi = b.isUnwrapped() ? b.getWrappedOperation() : b;
        }
    
        EndpointInfo getEndpoint() {
            return ei;
        }
    
        BindingOperationInfo getBindingOperation() {
            return boi;
        }

        @Override
        public int hashCode() {
            return boi.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            BindingOperation other = (BindingOperation)obj;
            return boi.equals(other.boi) && ei.equals(other.ei);
        }
    
        @Override
        public String toString() {
            return ei.getName().toString() + "." + boi.getName().toString();
        }
    
    
    }

    /**
     * Class used as key in the server fault policy map.
     */
    class BindingFault {
        private EndpointInfo ei;
        private BindingFaultInfo bfi;
    
        BindingFault(EndpointInfo e, BindingFaultInfo b) {
            ei = e;
            bfi = b;
        }
    
        EndpointInfo getEndpoint() {
            return ei;
        }
    
        BindingFaultInfo getBindingFault() {
            return bfi;
        }
    
        @Override
        public int hashCode() {
            return bfi.hashCode();
        }
    
        @Override
        public boolean equals(Object obj) {
            BindingFault other = (BindingFault)obj;
            return bfi.equals(other.bfi) && ei.equals(other.ei);
        }
    
        @Override
        public String toString() {
            return ei.getName().toString() + "." + bfi.getFaultInfo().toString();
        }
    }

    // for test

    EffectivePolicyImpl createOutPolicyInfo() {
        return new EffectivePolicyImpl();
    }
    
    private class EndpointPolicyMap extends ConcurrentHashMap<EndpointInfo, EndpointPolicy> {
        public EndpointPolicy remove(Object key) {
            EndpointInfo toRemove = (EndpointInfo) key;
            EndpointPolicy pol = null;
            for (EndpointInfo info : keySet()) {
                if (info.isSameAs(toRemove)) {
                    pol = super.remove(info);
                }
            }
            return pol;
        }
    }


}
