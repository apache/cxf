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

package org.apache.cxf.throttling;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 *
 */
public class ThrottlingFeature extends AbstractFeature {
    private final Portable delegate;

    public ThrottlingFeature() {
        delegate = new Portable();
    }

    public ThrottlingFeature(ThrottlingManager manager) {
        delegate = new Portable(manager);
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        delegate.doInitializeProvider(provider, bus);
    }

    @Override
    public void initialize(Server server, Bus bus) {
        delegate.initialize(server, bus);
    }

    @Override
    public void initialize(Client client, Bus bus) {
        delegate.initialize(client, bus);
    }

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        delegate.initialize(interceptorProvider, bus);
    }

    @Override
    public void initialize(Bus bus) {
        delegate.initialize(bus);
    }

    public static class Portable implements AbstractPortableFeature {
        final ThrottlingManager manager;

        public Portable() {
            manager = null;
        }

        public Portable(ThrottlingManager manager) {
            this.manager = manager;
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            ThrottlingManager m = manager;
            if (m == null) {
                m = bus.getExtension(ThrottlingManager.class);
            }
            if (m == null) {
                throw new IllegalArgumentException("ThrottlingManager must not be null");
            }
            for (String p : m.getDecisionPhases()) {
                provider.getInInterceptors().add(new ThrottlingInterceptor(p, m));
            }
            provider.getOutInterceptors().add(new ThrottlingResponseInterceptor());
            provider.getOutFaultInterceptors().add(new ThrottlingResponseInterceptor());
        }
    }
}
