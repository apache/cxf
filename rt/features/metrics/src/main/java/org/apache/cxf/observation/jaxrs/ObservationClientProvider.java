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
package org.apache.cxf.observation.jaxrs;

import static org.apache.cxf.observation.CxfObservationDocumentation.OUT_OBSERVATION;
import static org.apache.cxf.observation.DefaultMessageOutObservationConvention.INSTANCE;

import java.io.IOException;

import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.cxf.observation.MessageOutContext;
import org.apache.cxf.observation.MessageOutObservationConvention;
import org.apache.cxf.observation.AbstractObservationClientProvider;
import org.apache.cxf.observation.ObservationScope;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ObservationClientProvider extends AbstractObservationClientProvider
        implements ClientRequestFilter, ClientResponseFilter {

    private final MessageOutObservationConvention convention;

    public ObservationClientProvider(final ObservationRegistry observationRegistry) {
        this(observationRegistry, null);
    }

    public ObservationClientProvider(final ObservationRegistry observationRegistry,
                                     @Nullable MessageOutObservationConvention convention) {
        super(observationRegistry);
        this.convention = convention;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        final MessageOutContext messageOutContext = new MessageOutContext(null);  //TODO: Fix me

        Observation observation = OUT_OBSERVATION.start(convention,
                                                        INSTANCE,
                                                        () -> messageOutContext,
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
        super.stopTraceSpan(holder, null); //TODO: Fix me
    }
}
