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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;

import io.micrometer.observation.ObservationRegistry;

public abstract class AbstractObservationInterceptor extends AbstractObservationProvider
        implements PhaseInterceptor<Message> {
    private final String phase;

    protected AbstractObservationInterceptor(String phase, ObservationRegistry observationRegistry) {
        super(observationRegistry);
        this.phase = phase;
    }

    @Override
    public Set<String> getAfter() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return getClass().getName();
    }

    @Override
    public String getPhase() {
        return phase;
    }

    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    @Override
    public void handleFault(Message message) {
    }
}
