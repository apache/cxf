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
package org.apache.cxf.tracing.brave;

import java.util.UUID;

import brave.Tracing;
import brave.http.HttpTracing;
import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@NoJSR250Annotations
@Provider(value = Type.Feature, scope = Scope.Server)
public class BraveFeature extends DelegatingFeature<BraveFeature.Portable> {
    public BraveFeature() {
        this("cxf-svc-" + UUID.randomUUID().toString());
    }

    public BraveFeature(final Tracing tracing) {
        this(Portable.newTracer(tracing));
    }

    public BraveFeature(final String name) {
        this(Portable.newTracer(Portable.newTracing(name)));
    }

    public BraveFeature(HttpTracing brave) {
        super(new Portable(brave));
    }

    public static class Portable implements AbstractPortableFeature {
        private BraveStartInterceptor in;
        private BraveStopInterceptor out;

        public Portable() {
            this("cxf-svc-" + UUID.randomUUID().toString());
        }

        public Portable(final String name) {
            this(newTracer(newTracing(name)));
        }

        public Portable(final Tracing tracing) {
            this(newTracer(tracing));
        }

        public Portable(HttpTracing brave) {
            in = new BraveStartInterceptor(brave);
            out = new BraveStopInterceptor(brave);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);

            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }

        private static HttpTracing newTracer(Tracing tracing) {
            return HttpTracing
                    .newBuilder(tracing)
                    .build();
        }

        private static Tracing newTracing(String name) {
            return Tracing.newBuilder().localServiceName(name).build();
        }
    }
}
