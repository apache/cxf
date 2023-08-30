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


import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import static org.apache.cxf.tracing.micrometer.CxfObservationDocumentation.OUT_OBSERVATION;
import static org.apache.cxf.tracing.micrometer.DefaultMessageOutObservationConvention.INSTANCE;

public class ObservationClientStartInterceptor extends AbstractObservationClientInterceptor {

    final MessageOutObservationConvention convention;

    public ObservationClientStartInterceptor(final ObservationRegistry observationRegistry) {
        this(Phase.PRE_STREAM, observationRegistry, null);
    }

    public ObservationClientStartInterceptor(final ObservationRegistry observationRegistry,
                                             @Nullable
                                             MessageOutObservationConvention convention) {
        this(Phase.PRE_STREAM, observationRegistry, convention);
    }

    public ObservationClientStartInterceptor(final String phase, final ObservationRegistry observationRegistry,
                                             @Nullable
                                             MessageOutObservationConvention convention) {
        super(phase, observationRegistry);
        this.convention = convention;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final MessageOutContext messageOutContext = new MessageOutContext(message);

        Observation observation = OUT_OBSERVATION.start(convention,
                                          INSTANCE,
                                          () -> messageOutContext,
                                          getObservationRegistry());

        final TraceScopeHolder<ObservationScope> holder = super.startScopedObservation(observation);

        if (holder != null) {
            message.getExchange().put(OBSERVATION_SCOPE, holder);
        }
    }

    @Override
    public void handleFault(Message message) {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<ObservationScope> holder =
                (TraceScopeHolder<ObservationScope>) message.getExchange().get(OBSERVATION_SCOPE);

        final Exception ex = message.getContent(Exception.class);

        super.stopTraceSpan(holder, ex, observation -> {
            MessageOutContext context = (MessageOutContext) observation.getContext();
            context.setResponse(message);
        });
    }
}
