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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.extension.RegistryImpl;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.neethi.Assertion;

/**
 *
 */
@NoJSR250Annotations(unlessNull = "bus")
public class PolicyInterceptorProviderRegistryImpl
    extends RegistryImpl<QName, Set<PolicyInterceptorProvider>>
    implements PolicyInterceptorProviderRegistry, BusExtension {

    private Bus bus;
    private boolean dynamicLoaded;

    public PolicyInterceptorProviderRegistryImpl() {
        super(null);
    }
    public PolicyInterceptorProviderRegistryImpl(Bus b) {
        super(null);
        setBus(b);
    }

    public PolicyInterceptorProviderRegistryImpl(Map<QName, Set<PolicyInterceptorProvider>> interceptors) {
        super(interceptors);
    }

    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (b != null) {
            b.setExtension(this, PolicyInterceptorProviderRegistry.class);
        }
    }

    public void register(PolicyInterceptorProvider provider) {
        for (QName qn : provider.getAssertionTypes()) {
            Set<PolicyInterceptorProvider> providers = super.get(qn);
            if (providers == null) {
                providers = new CopyOnWriteArraySet<>();
            }
            providers.add(provider);
            super.register(qn, providers);
        }
    }

    public Class<?> getRegistrationType() {
        return PolicyInterceptorProviderRegistry.class;
    }

    protected synchronized void loadDynamic() {
        if (!dynamicLoaded && bus != null) {
            dynamicLoaded = true;
            ConfiguredBeanLocator c = bus.getExtension(ConfiguredBeanLocator.class);
            if (c != null) {
                c.getBeansOfType(PolicyInterceptorProviderLoader.class);
                for (PolicyInterceptorProvider b : c.getBeansOfType(PolicyInterceptorProvider.class)) {
                    register(b);
                }
            }
        }
    }

    @Override
    public Set<PolicyInterceptorProvider> get(QName qn) {
        Set<PolicyInterceptorProvider> pps = super.get(qn);
        if (pps == null) {
            pps = Collections.emptySet();
        }
        return pps;
    }

    public List<Interceptor<? extends Message>>
    getInterceptorsForAlternative(Collection<? extends Assertion> alternative,
                                  boolean out, boolean fault) {

        List<Interceptor<? extends Message>> interceptors = new ArrayList<>();
        for (Assertion a : alternative) {
            if (a.isOptional()) {
                continue;
            }
            QName qn = a.getName();
            interceptors.addAll(getInterceptorsForAssertion(qn, out, fault));
        }
        return interceptors;
    }

    public List<Interceptor<? extends Message>> getInInterceptorsForAssertion(QName qn) {
        return getInterceptorsForAssertion(qn, false, false);
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptorsForAssertion(QName qn) {
        return getInterceptorsForAssertion(qn, false, true);
    }

    public List<Interceptor<? extends Message>> getOutInterceptorsForAssertion(QName qn) {
        return getInterceptorsForAssertion(qn, true, false);
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptorsForAssertion(QName qn) {
        return getInterceptorsForAssertion(qn, true, true);
    }

    protected List<Interceptor<? extends Message>> getInterceptorsForAssertion(QName qn, boolean out,
                                                                               boolean fault) {
        loadDynamic();
        List<Interceptor<? extends Message>> interceptors = new ArrayList<>();
        Set<PolicyInterceptorProvider> pps = get(qn);
        for (PolicyInterceptorProvider pp : pps) {
            interceptors.addAll(out
                                ? (fault ? pp.getOutFaultInterceptors() : pp.getOutInterceptors())
                                    : (fault ? pp.getInFaultInterceptors() : pp.getInInterceptors()));
        }
        return interceptors;
    }

}
