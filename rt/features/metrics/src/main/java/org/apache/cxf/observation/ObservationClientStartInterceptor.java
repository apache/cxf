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
package org.apache.cxf.observation;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.ObservationRegistry;

public class ObservationClientStartInterceptor extends AbstractObservationClientInterceptor {

    final MessageOutObservationConvention messageOutObservationConvention;

    public ObservationClientStartInterceptor(final ObservationRegistry observationRegistry) {
        this(Phase.PRE_STREAM, observationRegistry, null);
    }

    public ObservationClientStartInterceptor(final ObservationRegistry observationRegistry,
                                             @Nullable
                                             MessageOutObservationConvention messageOutObservationConvention) {
        this(Phase.PRE_STREAM, observationRegistry, messageOutObservationConvention);
    }

    public ObservationClientStartInterceptor(final String phase, final ObservationRegistry observationRegistry,
                                             @Nullable
                                             MessageOutObservationConvention messageOutObservationConvention) {
        super(phase, observationRegistry);
        this.messageOutObservationConvention = messageOutObservationConvention;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        final MessageOutContext context = new MessageOutContext(message);

        final TraceScopeHolder<ObservationScope> holder = super.startScopedObservation(context,
                                                                                       this.messageOutObservationConvention);

        if (holder != null) {
            message.getExchange().put(OBSERVATION_SCOPE, holder);
        }
    }

    @Override
    public void handleFault(Message message) {
        @SuppressWarnings("unchecked")
        final TraceScopeHolder<ObservationScope> holder =
                (TraceScopeHolder<ObservationScope>) message.getExchange().get(OBSERVATION_SCOPE);

        super.stopTraceSpan(holder, message);
    }
}
