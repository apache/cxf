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

import java.util.concurrent.Callable;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.ObservationRegistry;

public class ObservationTracerContext implements TracerContext {
    private final ObservationRegistry observationRegistry;
    private final Observation continuationObservation;

    public ObservationTracerContext(final ObservationRegistry observationRegistry) {
        this(observationRegistry, null);
    }

    public ObservationTracerContext(final ObservationRegistry observationRegistry, final
    Observation continuationObservation) {
        this.observationRegistry = observationRegistry;
        this.continuationObservation = continuationObservation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObservationScope startSpan(final String description) {
        return new ObservationScope(Observation.start(description, this.observationRegistry));
    }

    @Override
    public <T> T continueSpan(final Traceable<T> traceable) throws Exception {
        Observation.Scope scope = null;

        if (observationRegistry.getCurrentObservation() == null && continuationObservation != null) {
            scope = continuationObservation.openScope();
        }

        try {
            return traceable.call(new ObservationTracerContext(observationRegistry));
        } finally {
            if (continuationObservation != null && scope != null) {
                scope.close();
            }
        }
    }

    @Override
    public <T> Callable<T> wrap(final String description, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new ObservationTracerContext(observationRegistry));
            }
        };

        // Carry over parent from the current thread
        final Observation parent = this.observationRegistry.getCurrentObservation();
        return () -> {
            try (ObservationScope span = newOrChildSpan(description, parent)) {
                return callable.call();
            }
        };
    }

    @Override
    public void annotate(String key, String value) {
        final Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.highCardinalityKeyValue(key, value);
        }
    }

    @Override
    public void timeline(String message) {
        final Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.event(Event.of(message));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(final Class<T> clazz) {
        if (ObservationRegistry.class.equals(clazz)) {
            return (T) observationRegistry;
        } else {
            throw new IllegalArgumentException("The class is '" + clazz
                                               + "'not supported and cannot be unwrapped");
        }
    }

    private ObservationScope newOrChildSpan(final String description, final Observation parent) {
        if (parent == null) {
            return new ObservationScope(Observation.start(description, observationRegistry));
        }
        return new ObservationScope(Observation.createNotStarted(description, observationRegistry)
                                               .parentObservation(parent).start());
    }
}
