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
package org.apache.cxf.tracing.opentelemetry;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.cxf.interceptor.OneWayInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public abstract class AbstractOpenTelemetryClientInterceptor extends AbstractOpenTelemetryClientProvider
    implements PhaseInterceptor<Message>, OneWayInterceptor<Message> {

    private String phase;

    protected AbstractOpenTelemetryClientInterceptor(final String phase, final OpenTelemetry openTelemetry,
            final String instrumentationName) {
        super(openTelemetry, instrumentationName);
        this.phase = phase;
    }

    protected AbstractOpenTelemetryClientInterceptor(final String phase, final OpenTelemetry openTelemetry,
            final Tracer tracer) {
        super(openTelemetry, tracer);
        this.phase = phase;
    }

    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public Set<String> getAfter() {
        return Collections.emptySet();
    }

    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    public String getId() {
        return getClass().getName();
    }

    public String getPhase() {
        return phase;
    }

    public void handleFault(Message message) {
    }

}
