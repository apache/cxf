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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AsyncIOInterceptorAdapter;
import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereInterceptorWriter;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter.OnPreSuspend;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.interceptor.AllowInterceptor;
import org.atmosphere.interceptor.SSEAtmosphereInterceptor;
import org.atmosphere.util.Utils;

import static org.apache.cxf.jaxrs.sse.OutboundSseEventBodyWriter.SERVER_SENT_EVENTS;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_USE_STREAM;
import static org.atmosphere.cpr.FrameworkConfig.CALLBACK_JAVASCRIPT_PROTOCOL;
import static org.atmosphere.cpr.FrameworkConfig.CONTAINER_RESPONSE;

/**
 * Most of this class implementation is borrowed from SSEAtmosphereInterceptor. The original
 * implementation does two things which do not fit well into SSE support:
 *  - closes the response stream (overridden by SseAtmosphereInterceptorWriter)
 *  - wraps the whatever object is being written to SSE payload (overridden using 
 *    the complete SSE protocol) 
 */
public class SseAtmosphereInterceptor extends SSEAtmosphereInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(SseAtmosphereInterceptor.class);

    private static final byte[] PADDING;
    private static final String PADDING_TEXT;
    private static final byte[] END = "\r\n\r\n".getBytes();
    
    static {
        StringBuffer whitespace = new StringBuffer();
        for (int i = 0; i < 2000; i++) {
            whitespace.append(" ");
        }
        whitespace.append("\n");
        PADDING_TEXT = whitespace.toString();
        PADDING = PADDING_TEXT.getBytes();
    }
    
    private boolean writePadding(AtmosphereResponse response) {
        if (response.request() != null && response.request().getAttribute("paddingWritten") != null) {
            return false;
        }

        response.setContentType(SERVER_SENT_EVENTS);
        response.setCharacterEncoding("utf-8");
        boolean isUsingStream = (Boolean) response.request().getAttribute(PROPERTY_USE_STREAM);
        if (isUsingStream) {
            try {
                OutputStream stream = response.getResponse().getOutputStream();
                try {
                    stream.write(PADDING);
                    stream.flush();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "SSE may not work", ex);
                }
            } catch (IOException e) {
                LOG.log(Level.FINEST, "", e);
            }
        } else {
            try {
                PrintWriter w = response.getResponse().getWriter();
                w.println(PADDING_TEXT);
                w.flush();
            } catch (IOException e) {
                LOG.log(Level.FINEST, "", e);
            }
        }
        response.resource().getRequest().setAttribute("paddingWritten", "true");
        return true;
    }
    
    @Override
    public Action inspect(final AtmosphereResource r) {
        if (Utils.webSocketMessage(r)) {
            return Action.CONTINUE;
        }

        final AtmosphereRequest request = r.getRequest();
        final String accept = request.getHeader("Accept") == null ? "text/plain" : request.getHeader("Accept").trim();

        if (r.transport().equals(AtmosphereResource.TRANSPORT.SSE) || SERVER_SENT_EVENTS.equalsIgnoreCase(accept)) {
            final AtmosphereResponse response = r.getResponse();
            if (response.getAsyncIOWriter() == null) {
                response.asyncIOWriter(new SseAtmosphereInterceptorWriter());
            }
            
            r.addEventListener(new P(response));

            AsyncIOWriter writer = response.getAsyncIOWriter();
            if (AtmosphereInterceptorWriter.class.isAssignableFrom(writer.getClass())) {
                AtmosphereInterceptorWriter.class.cast(writer).interceptor(new AsyncIOInterceptorAdapter() {
                    private boolean padding() {
                        if (!r.isSuspended()) {
                            return writePadding(response);
                        }
                        return false;
                    }

                    @Override
                    public void prePayload(AtmosphereResponse response, byte[] data, int offset, int length) {
                        padding();
                    }

                    @Override
                    public void postPayload(AtmosphereResponse response, byte[] data, int offset, int length) {
                        // The CALLBACK_JAVASCRIPT_PROTOCOL may be called by a framework running on top of Atmosphere
                        // In that case, we must pad/protocol indenendently of the state of the AtmosphereResource
                        if (r.isSuspended() || r.getRequest().getAttribute(CALLBACK_JAVASCRIPT_PROTOCOL) != null
                                || r.getRequest().getAttribute(CONTAINER_RESPONSE) != null) {
                            response.write(END, true);
                        }

                        /**
                         * When used with https://github.com/remy/polyfills/blob/master/EventSource.js , we
                         * resume after every message.
                         */
                        String ua = r.getRequest().getHeader("User-Agent");
                        if (ua != null && ua.contains("MSIE")) {
                            try {
                                response.flushBuffer();
                            } catch (IOException e) {
                                LOG.log(Level.FINEST, "", e);
                            }
                            r.resume();
                        }
                    }
                });
            } else {
                LOG.warning(String.format("Unable to apply %s. Your AsyncIOWriter must implement %s", 
                    getClass().getName(), AtmosphereInterceptorWriter.class.getName()));
            }
        }
        
        return Action.CONTINUE;
    }
    
    private final class P extends OnPreSuspend implements AllowInterceptor {

        private final AtmosphereResponse response;

        private P(AtmosphereResponse response) {
            this.response = response;
        }

        @Override
        public void onPreSuspend(AtmosphereResourceEvent event) {
            writePadding(response);
        }
    }    
}
