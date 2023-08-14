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
package org.apache.cxf.tracing.micrometer;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.ObservationRegistry;

@NoJSR250Annotations
@Provider(value = Type.Feature, scope = Scope.Client)
public class ObservationClientFeature extends DelegatingFeature<ObservationClientFeature.Portable> {
    public ObservationClientFeature(ObservationRegistry observationRegistry,
                                    @Nullable MessageOutObservationConvention messageOutObservationConvention) {
        super(new Portable(observationRegistry, messageOutObservationConvention));
    }

    public ObservationClientFeature(ObservationRegistry observationRegistry) {
        super(new Portable(observationRegistry, null));
    }

    public static class Portable implements AbstractPortableFeature {
        private final ObservationClientStartInterceptor out;
        private final ObservationClientStopInterceptor in;

        public Portable(ObservationRegistry observationRegistry,
                        @Nullable MessageOutObservationConvention messageOutObservationConvention) {
            out = new ObservationClientStartInterceptor(observationRegistry, messageOutObservationConvention);
            in = new ObservationClientStopInterceptor(observationRegistry);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(in);
            provider.getOutInterceptors().add(out);
        }
    }
}
