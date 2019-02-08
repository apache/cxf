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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyContainingAssertion;

/**
 *
 */
public class EffectivePolicyImpl implements EffectivePolicy {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(EffectivePolicyImpl.class);
    private static final Logger LOG = LogUtils.getL7dLogger(EffectivePolicyImpl.class);

    protected Policy policy;
    protected Collection<Assertion> chosenAlternative;
    protected List<Interceptor<? extends org.apache.cxf.message.Message>> interceptors;

    public EffectivePolicyImpl() {
    }

    public Policy getPolicy() {
        return policy;
    }

    public List<Interceptor<? extends org.apache.cxf.message.Message>> getInterceptors() {
        return interceptors;
    }

    public Collection<Assertion> getChosenAlternative() {
        return chosenAlternative;
    }

    public void initialise(EndpointPolicy epi, PolicyEngine engine, boolean inbound, Message m) {
        initialise(epi, engine, inbound, false, m);
    }

    public void initialise(EndpointPolicy epi, PolicyEngine engine, boolean inbound, boolean fault, Message m) {
        policy = epi.getPolicy();
        chosenAlternative = epi.getChosenAlternative();
        if (chosenAlternative == null) {
            chooseAlternative(engine, null, m);
        }
        initialiseInterceptors(engine, inbound, fault, m);
    }

    public void initialise(EndpointInfo ei,
                    BindingOperationInfo boi,
                    PolicyEngine engine,
                    Assertor assertor,
                    boolean requestor,
                    boolean request,
                    Message m) {
        initialisePolicy(ei, boi, engine, requestor, request, assertor, m);
        chooseAlternative(engine, assertor, m);
        initialiseInterceptors(engine, false, m);
    }

    public void initialise(EndpointInfo ei,
                    BindingOperationInfo boi,
                    PolicyEngine engine,
                    Assertor assertor,
                    List<List<Assertion>> incoming,
                    Message m) {
        initialisePolicy(ei, boi, engine, false, false, assertor, m);
        chooseAlternative(engine, assertor, incoming, m);
        initialiseInterceptors(engine, false, m);
    }

    public void initialise(EndpointInfo ei,
                    BindingOperationInfo boi,
                    PolicyEngine engine,
                    boolean requestor, boolean request,
                    Message m) {
        Assertor assertor = initialisePolicy(ei, boi, engine, requestor, request, null, m);
        if (requestor || !request) {
            chooseAlternative(engine, assertor, m);
            initialiseInterceptors(engine, requestor, m);
        } else {
            //incoming server should not choose an alternative, need to include all the policies
            Collection<Assertion> alternative = ((PolicyEngineImpl)engine).getAssertions(this.policy, true);
            this.setChosenAlternative(alternative);
        }
    }

    public void initialise(EndpointInfo ei,
                    BindingOperationInfo boi,
                    BindingFaultInfo bfi,
                    PolicyEngine engine,
                    Assertor assertor,
                    Message m) {
        initialisePolicy(ei, boi, bfi, engine, m);
        chooseAlternative(engine, assertor, m);
        initialiseInterceptors(engine, false, m);
    }

    private <T> T getAssertorAs(Assertor as, Class<T> t) {
        if (t.isInstance(as)) {
            return t.cast(as);
        } else if (as instanceof PolicyUtils.WrappedAssertor) {
            Object o = ((PolicyUtils.WrappedAssertor)as).getWrappedAssertor();
            if (t.isInstance(o)) {
                return t.cast(o);
            }
        }
        return null;
    }
    Assertor initialisePolicy(EndpointInfo ei,
                          BindingOperationInfo boi,
                          PolicyEngine engine,
                          boolean requestor,
                          boolean request,
                          Assertor assertor,
                          Message m) {

        if (boi.isUnwrapped()) {
            boi = boi.getUnwrappedOperation();
        }

        BindingMessageInfo bmi = request ? boi.getInput() : boi.getOutput();
        EndpointPolicy ep;
        if (requestor) {
            ep = engine.getClientEndpointPolicy(ei, getAssertorAs(assertor, Conduit.class), m);
        } else {
            ep = engine.getServerEndpointPolicy(ei, getAssertorAs(assertor, Destination.class), m);
        }
        policy = ep.getPolicy();
        if (ep instanceof EndpointPolicyImpl) {
            assertor = ((EndpointPolicyImpl)ep).getAssertor();
        }

        policy = policy.merge(((PolicyEngineImpl)engine).getAggregatedOperationPolicy(boi, m));
        if (null != bmi) {
            policy = policy.merge(((PolicyEngineImpl)engine).getAggregatedMessagePolicy(bmi, m));
        }
        policy = policy.normalize(engine.getRegistry(), true);
        return assertor;
    }

    void initialisePolicy(EndpointInfo ei, BindingOperationInfo boi,
                          BindingFaultInfo bfi, PolicyEngine engine, Message m) {
        policy = engine.getServerEndpointPolicy(ei, (Destination)null, m).getPolicy();
        policy = policy.merge(((PolicyEngineImpl)engine).getAggregatedOperationPolicy(boi, m));
        if (bfi != null) {
            policy = policy.merge(((PolicyEngineImpl)engine).getAggregatedFaultPolicy(bfi, m));
        }
        policy = policy.normalize(engine.getRegistry(), true);
    }

    void chooseAlternative(PolicyEngine engine, Assertor assertor, Message m) {
        chooseAlternative(engine, assertor, null, m);
    }
    void chooseAlternative(PolicyEngine engine, Assertor assertor, List<List<Assertion>> incoming, Message m) {
        Collection<Assertion> alternative = engine.getAlternativeSelector()
            .selectAlternative(policy, engine, assertor, incoming, m);
        if (null == alternative) {
            PolicyUtils.logPolicy(LOG, Level.FINE, "No alternative supported.", getPolicy());
            throw new PolicyException(new org.apache.cxf.common.i18n.Message("NO_ALTERNATIVE_EXC", BUNDLE));
        }
        setChosenAlternative(alternative);
    }

    void initialiseInterceptors(PolicyEngine engine, Message m) {
        initialiseInterceptors(engine, false, m);
    }

    void initialiseInterceptors(PolicyEngine engine, boolean useIn, Message m) {
        initialiseInterceptors(engine, useIn, false, m);
    }

    void initialiseInterceptors(PolicyEngine engine, boolean useIn, boolean fault, Message m) {
        if (((PolicyEngineImpl)engine).getBus() != null) {
            PolicyInterceptorProviderRegistry reg
                = ((PolicyEngineImpl)engine).getBus().getExtension(PolicyInterceptorProviderRegistry.class);
            Set<Interceptor<? extends org.apache.cxf.message.Message>> out
                = new LinkedHashSet<>();
            for (Assertion a : getChosenAlternative()) {
                initialiseInterceptors(reg, engine, out, a, useIn, fault, m);
            }
            setInterceptors(new ArrayList<Interceptor<? extends  org.apache.cxf.message.Message>>(out));
        }
    }


    protected Collection<Assertion> getSupportedAlternatives(PolicyEngine engine,
                                                             Policy p,
                                                             Message m) {
        Collection<Assertion> alternatives = new ArrayList<>();

        for (Iterator<List<Assertion>> it = p.getAlternatives(); it.hasNext();) {
            List<Assertion> alternative = it.next();
            if (engine.supportsAlternative(alternative, null, m)) {
                alternatives.addAll(alternative);
            }
        }
        return alternatives;
    }

    void initialiseInterceptors(PolicyInterceptorProviderRegistry reg, PolicyEngine engine,
                                Set<Interceptor<? extends org.apache.cxf.message.Message>> out, Assertion a,
                                boolean useIn, boolean fault,
                                Message m) {
        QName qn = a.getName();

        List<Interceptor<? extends org.apache.cxf.message.Message>> i = null;
        if (useIn && !fault) {
            i = reg.getInInterceptorsForAssertion(qn);
        } else if (!useIn && !fault) {
            i = reg.getOutInterceptorsForAssertion(qn);
        } else if (useIn && fault) {
            i = reg.getInFaultInterceptorsForAssertion(qn);
        } else if (!useIn && fault) {
            i = reg.getOutFaultInterceptorsForAssertion(qn);
        }
        out.addAll(i);

        if (a instanceof PolicyContainingAssertion) {
            Policy p = ((PolicyContainingAssertion)a).getPolicy();
            if (p != null) {
                for (Assertion a2 : getSupportedAlternatives(engine, p, m)) {
                    initialiseInterceptors(reg, engine, out, a2, useIn, fault, m);
                }
            }
        }
    }

    // for tests

    void setPolicy(Policy ep) {
        policy = ep;
    }

    void setChosenAlternative(Collection<Assertion> c) {
        chosenAlternative = c;
    }

    void setInterceptors(List<Interceptor<? extends org.apache.cxf.message.Message>> out) {
        interceptors = out;
    }

}
