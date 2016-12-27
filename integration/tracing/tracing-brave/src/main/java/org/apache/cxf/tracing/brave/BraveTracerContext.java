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

import java.util.concurrent.Callable;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerSpanThreadBinder;

import org.apache.cxf.tracing.Traceable;
import org.apache.cxf.tracing.TracerContext;

import zipkin.Constants;

public class BraveTracerContext implements TracerContext {
    private final Brave brave;
    private final ServerSpan continuationSpan;
    private final ServerSpanThreadBinder serverSpanThreadBinder;
    
    public BraveTracerContext(final Brave brave) {
        this(brave, null);
    }
    
    public BraveTracerContext(final Brave brave, final ServerSpan continuationSpan) {
        this.brave = brave;
        this.continuationSpan = continuationSpan;
        this.serverSpanThreadBinder = brave.serverSpanThreadBinder();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public TraceScope startSpan(final String description) {
        return new TraceScope(brave, 
            brave
                .localTracer()
                .startNewSpan(Constants.LOCAL_COMPONENT, description));
    }
    
    @Override
    public <T> T continueSpan(final Traceable<T> traceable) throws Exception {
        boolean attached = false;
        if (serverSpanThreadBinder.getCurrentServerSpan() != null && continuationSpan != null) {
            serverSpanThreadBinder.setCurrentSpan(continuationSpan);
            attached = true;
        }
        
        try {
            return traceable.call(new BraveTracerContext(brave));
        } finally {
            if (continuationSpan != null && attached) {
                serverSpanThreadBinder.setCurrentSpan(null);
            }
        }
    }
    
    @Override
    public <T> Callable<T> wrap(final String description, final Traceable<T> traceable) {
        final Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return traceable.call(new BraveTracerContext(brave));
            }
        };
        
        return () -> {
            try {
                startSpan(description);
                return callable.call();
            } finally {
                brave.localTracer().finishSpan();
            }
        };
    }
    
    @Override
    public void annotate(String key, String value) {
        if (brave.localSpanThreadBinder().getCurrentLocalSpan() != null) {
            brave.localTracer().submitBinaryAnnotation(key, value);
        } else if (brave.serverSpanThreadBinder().getCurrentServerSpan() != null) {
            brave.serverTracer().submitBinaryAnnotation(key, value);
        }
    }
    
    @Override
    public void timeline(String message) {
        if (brave.localSpanThreadBinder().getCurrentLocalSpan() != null) {
            brave.localTracer().submitAnnotation(message);
        } else if (brave.serverSpanThreadBinder().getCurrentServerSpan() != null) {
            brave.serverTracer().submitAnnotation(message);
        }
    }
}
