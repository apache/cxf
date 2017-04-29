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
package org.apache.cxf.jaxrs.sse.atmosphere;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.common.logging.LogUtils;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;

public class SseAtmosphereEventSinkImpl implements SseEventSink {
    private static final Logger LOG = LogUtils.getL7dLogger(SseAtmosphereEventSinkImpl.class);

    private final AtmosphereResource resource;
    private final MessageBodyWriter<OutboundSseEvent> writer;

    private volatile boolean closed;

    public SseAtmosphereEventSinkImpl(final MessageBodyWriter<OutboundSseEvent> writer,
            final AtmosphereResource resource) {
        this.writer = writer;
        this.resource = resource;

        if (!resource.isSuspended()) {
            LOG.fine("Atmosphere resource is not suspended, suspending");
            resource.suspend();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            LOG.fine("Closing Atmosphere SSE event output");
            if (resource.isSuspended()) {
                LOG.fine("Atmosphere resource is suspended, resuming");
                resource.resume();
            }

            final Broadcaster broadcaster = resource.getBroadcaster();
            resource.removeFromAllBroadcasters();

            try {
                final AtmosphereResponse response = resource.getResponse();
                if (!response.isCommitted()) {
                    LOG.fine("Response is not committed, flushing buffer");
                    try {
                        response.flushBuffer();
                    } catch (IOException ex) {
                        //REVISIT: and throw a runtime exception ?
                        LOG.warning("Failed to flush AtmosphereResponse buffer");
                    }
                }

                response.closeStreamOrWriter();
            } finally {
                try {
                    resource.close();
                } catch (IOException ex) {
                    // ignore
                }
                broadcaster.destroy();
                LOG.fine("Atmosphere SSE event output is closed");
            }
        }
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        final CompletableFuture<?> future = new CompletableFuture<>();
        
        if (!closed && writer != null) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                writer.writeTo(event, event.getClass(), null, new Annotation [] {}, event.getMediaType(), null, os);

                // Atmosphere broadcasts asynchronously which is acceptable in most cases.
                // Unfortunately, calling close() may lead to response stream being closed
                // while there are still some SSE delivery scheduled.
                return CompletableFuture.completedFuture(
                    resource
                        .getBroadcaster()
                        .broadcast(os.toString(StandardCharsets.UTF_8.name()))
                        .get(1, TimeUnit.SECONDS)
                    );
            } catch (final IOException ex) {
                LOG.warning("While writing the SSE event, an exception was raised: " + ex);
                future.completeExceptionally(ex);
            } catch (final ExecutionException | InterruptedException ex) {
                LOG.warning("SSE Atmosphere response was not delivered");
                future.completeExceptionally(ex);
            } catch (final TimeoutException ex) {
                LOG.warning("SSE Atmosphere response was not delivered within default timeout");
                future.completeExceptionally(ex);
            }
        } else {
            future.complete(null);
        }
        
        return future;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
