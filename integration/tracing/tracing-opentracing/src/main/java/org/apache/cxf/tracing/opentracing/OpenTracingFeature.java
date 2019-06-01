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
package org.apache.cxf.tracing.opentracing;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@NoJSR250Annotations
@Provider(value = Type.Feature, scope = Scope.Server)
public class OpenTracingFeature extends AbstractFeature {
    private Portable delegate;

    public OpenTracingFeature() {
        delegate = new Portable();
    }

    public OpenTracingFeature(final Tracer tracer) {
        delegate = new Portable(tracer);
    }

    @Override
    public void initializeProvider(InterceptorProvider provider, Bus bus) {
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

    @Provider(value = Type.Feature, scope = Scope.Server)
    public static class Portable implements AbstractPortableFeature {
        private OpenTracingStartInterceptor in;
        private OpenTracingStopInterceptor out;

        public Portable() {
            this(GlobalTracer.get());
        }

        public Portable(final Tracer tracer) {
            in = new OpenTracingStartInterceptor(tracer);
            out = new OpenTracingStopInterceptor(tracer);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);

            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }
}
