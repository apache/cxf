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

import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.tracing.AbstractTracingProvider;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

public abstract class AbstractObservationProvider extends AbstractTracingProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractObservationProvider.class);
    protected static final String OBSERVATION_SCOPE = "org.apache.cxf.tracing.micrometer.observation";

    protected final ObservationRegistry observationRegistry;

    protected AbstractObservationProvider() {
        this(ObservationRegistry.NOOP);
    }

    protected AbstractObservationProvider(final ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    protected TraceScopeHolder<ObservationScope> startScopedObservation(final Observation observation) {

        // If the service resource is using asynchronous processing mode, the trace
        // scope will be closed in another thread and as such should be detached.
        Observation.Scope scope = null;
        if (isAsyncResponse()) {
            // Do not modify the current context span
            propagateContinuationSpan(observation);
        } else {
            scope = observation.openScope();
        }

        return new TraceScopeHolder<>(new ObservationScope(observation, scope), scope == null /* detached */);
    }

    protected void stopTraceSpan(final TraceScopeHolder<ObservationScope> holder, Consumer<Observation> addResponse) {
        if (holder == null) {
            return;
        }

        final ObservationScope observationScope = holder.getScope();
        Observation.Scope scope = null;
        if (observationScope != null) {
            try {
                // If the service resource is using asynchronous processing mode, the trace
                // scope has been created in another thread and should be re-attached to the current
                // one.
                Observation observation = observationScope.getObservation();
                if (holder.isDetached()) {
                    scope = observation.openScope();
                }

                if (!observation.isNoop()) {
                    addResponse.accept(observation);
                }
            } finally {
                if (scope != null) {
                    scope.close(); // if this is null then obsScope will not have a null scope and vice versa
                }
                observationScope.close(); // will close the observation
            }
        }
    }

    protected boolean isAsyncResponse() {
        return !PhaseInterceptorChain.getCurrentMessage().getExchange().isSynchronous();
    }

    private void propagateContinuationSpan(final Observation continuationScope) {
        PhaseInterceptorChain.getCurrentMessage().put(Observation.class, continuationScope);
    }
}
