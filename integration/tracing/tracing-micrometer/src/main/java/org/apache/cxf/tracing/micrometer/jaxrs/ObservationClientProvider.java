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
package org.apache.cxf.tracing.micrometer.jaxrs;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.tracing.micrometer.AbstractObservationClientProvider;
import org.apache.cxf.tracing.micrometer.ObservationScope;
import org.jspecify.annotations.Nullable;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import static org.apache.cxf.tracing.micrometer.jaxrs.DefaultContainerRequestSenderObservationConvention.INSTANCE;
import static org.apache.cxf.tracing.micrometer.jaxrs.JaxrsObservationDocumentation.OUT_OBSERVATION;

@Provider
public class ObservationClientProvider extends AbstractObservationClientProvider
        implements ClientRequestFilter, ClientResponseFilter {

    private final ContainerRequestSenderObservationConvention convention;

    public ObservationClientProvider(final ObservationRegistry observationRegistry) {
        this(observationRegistry, null);
    }

    public ObservationClientProvider(final ObservationRegistry observationRegistry,
                                     @Nullable ContainerRequestSenderObservationConvention convention) {
        super(observationRegistry);
        this.convention = convention;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final ContainerRequestSenderObservationContext senderContext =
                new ContainerRequestSenderObservationContext(requestContext);

        Observation observation = OUT_OBSERVATION.start(convention,
                                                        INSTANCE,
                                                        () -> senderContext,
                                                        getObservationRegistry());

        final TraceScopeHolder<ObservationScope> holder = super.startScopedObservation(observation);

        if (holder != null) {
            requestContext.setProperty(OBSERVATION_SCOPE, holder);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(final ClientRequestContext requestContext,
                       final ClientResponseContext responseContext) throws IOException {
        final TraceScopeHolder<ObservationScope> holder =
                (TraceScopeHolder<ObservationScope>) requestContext.getProperty(OBSERVATION_SCOPE);
        super.stopTraceSpan(holder, null, observation -> {
            ContainerRequestSenderObservationContext context =
                    (ContainerRequestSenderObservationContext) observation.getContext();
            context.setResponse(responseContext);
        });
    }
}
