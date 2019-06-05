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
package org.apache.cxf.clustering;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.EvaluateAllEndpoints;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.ConduitSelectorHolder;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * This feature may be applied to a Client so as to enable
 * failover from the initial target endpoint to any other
 * compatible endpoint for the target service.
 */
@NoJSR250Annotations
@EvaluateAllEndpoints
@Provider(value = Type.Feature, scope = Scope.Client)
public class FailoverFeature extends DelegatingFeature<FailoverFeature.Portable> {
    protected FailoverFeature(Portable portable) {
        super(portable);
    }
    public FailoverFeature() {
        super(new Portable());
    }
    public FailoverFeature(String clientBootstrapAddress) {
        super(new Portable(clientBootstrapAddress));
    }

    public ConduitSelector initTargetSelector(Endpoint endpoint) {
        return delegate.initTargetSelector(endpoint);
    }

    public FailoverTargetSelector getTargetSelector() {
        return delegate.getTargetSelector();
    }

    public void setTargetSelector(FailoverTargetSelector selector) {
        delegate.setTargetSelector(selector);
    }

    public void setStrategy(FailoverStrategy strategy) {
        delegate.setStrategy(strategy);
    }

    public FailoverStrategy getStrategy() {
        return delegate.getStrategy();
    }

    public String getClientBootstrapAddress() {
        return delegate.getClientBootstrapAddress();
    }

    public void setClientBootstrapAddress(String clientBootstrapAddress) {
        delegate.setClientBootstrapAddress(clientBootstrapAddress);
    }

    public static class Portable implements AbstractPortableFeature {
        private FailoverStrategy failoverStrategy;
        private FailoverTargetSelector targetSelector;
        private String clientBootstrapAddress;

        public Portable() {

        }
        public Portable(String clientBootstrapAddress) {
            this.clientBootstrapAddress = clientBootstrapAddress;
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            if (provider instanceof ConduitSelectorHolder) {
                ConduitSelectorHolder csHolder = (ConduitSelectorHolder) provider;
                Endpoint endpoint = csHolder.getConduitSelector().getEndpoint();
                ConduitSelector conduitSelector = initTargetSelector(endpoint);
                csHolder.setConduitSelector(conduitSelector);
            }
        }

        @Override
        public void initialize(Client client, Bus bus) {
            ConduitSelector selector = initTargetSelector(client.getConduitSelector().getEndpoint());
            client.setConduitSelector(selector);
        }

        protected ConduitSelector initTargetSelector(Endpoint endpoint) {
            FailoverTargetSelector selector = getTargetSelector();
            selector.setEndpoint(endpoint);
            if (getStrategy() != null) {
                selector.setStrategy(getStrategy());
            }
            return selector;
        }

        public FailoverTargetSelector getTargetSelector() {
            if (this.targetSelector == null) {
                this.targetSelector = new FailoverTargetSelector(clientBootstrapAddress);
            }
            return this.targetSelector;
        }

        public void setTargetSelector(FailoverTargetSelector selector) {
            this.targetSelector = selector;
        }

        public void setStrategy(FailoverStrategy strategy) {
            failoverStrategy = strategy;
        }

        public FailoverStrategy getStrategy()  {
            return failoverStrategy;
        }

        public String getClientBootstrapAddress() {
            return clientBootstrapAddress;
        }

        public void setClientBootstrapAddress(String clientBootstrapAddress) {
            this.clientBootstrapAddress = clientBootstrapAddress;
        }
    }
}
