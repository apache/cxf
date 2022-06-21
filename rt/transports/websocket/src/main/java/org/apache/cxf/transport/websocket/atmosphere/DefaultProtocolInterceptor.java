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

package org.apache.cxf.transport.websocket.atmosphere;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AsyncIOInterceptor;
import org.atmosphere.cpr.AsyncIOInterceptorAdapter;
import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereInterceptorWriter;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.atmosphere.cpr.FrameworkConfig;

/**
 * DefaultProtocolInterceptor provides the default CXF's WebSocket protocol that uses.
 *
 * This interceptor is automatically engaged when no atmosphere interceptor is configured.
 */
public class DefaultProtocolInterceptor extends AtmosphereInterceptorAdapter {
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultProtocolInterceptor.class);

    private static final String REQUEST_DISPATCHED = "request.dispatched";
    private static final String RESPONSE_PARENT = "response.parent";

    private Map<String, AtmosphereResponse> suspendedResponses = new HashMap<>();

    private final AsyncIOInterceptor interceptor = new Interceptor();

    private Pattern includedheaders;
    private Pattern excludedheaders;

    @Override
    public void configure(AtmosphereConfig config) {
        super.configure(config);
        String p = config.getInitParameter("org.apache.cxf.transport.websocket.atmosphere.transport.includedheaders");
        if (p != null) {
            includedheaders = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        }
        p = config.getInitParameter("org.apache.cxf.transport.websocket.atmosphere.transport.excludedheaders");
        if (p != null) {
            excludedheaders = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        }
    }

    public DefaultProtocolInterceptor includedheaders(String p) {
        if (p != null) {
            this.includedheaders = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        }
        return this;
    }

    public void setIncludedheaders(Pattern includedheaders) {
        this.includedheaders = includedheaders;
    }

    public DefaultProtocolInterceptor excludedheaders(String p) {
        if (p != null) {
            this.excludedheaders = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
        }
        return this;
    }

    public void setExcludedheaders(Pattern excludedheaders) {
        this.excludedheaders = excludedheaders;
    }

    @Override
    public Action inspect(final AtmosphereResource r) {
        LOG.log(Level.FINE, "inspect");
        if (AtmosphereResource.TRANSPORT.WEBSOCKET != r.transport()
            && AtmosphereResource.TRANSPORT.SSE != r.transport()
            && AtmosphereResource.TRANSPORT.POLLING != r.transport()) {
            LOG.fine("Skipping ignorable request");
            return Action.CONTINUE;
        }
        if (AtmosphereResource.TRANSPORT.POLLING == r.transport()) {
            final String saruuid = (String)r.getRequest()
                .getAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID);
            final AtmosphereResponse suspendedResponse = suspendedResponses.get(saruuid);
            LOG.fine("Attaching a proxy writer to suspended response");
            r.getResponse().asyncIOWriter(new AtmosphereInterceptorWriter() {
                @Override
                public AsyncIOWriter write(AtmosphereResponse r, String data) throws IOException {
                    suspendedResponse.write(data);
                    suspendedResponse.flushBuffer();
                    return this;
                }

                @Override
                public AsyncIOWriter write(AtmosphereResponse r, byte[] data) throws IOException {
                    suspendedResponse.write(data);
                    suspendedResponse.flushBuffer();
                    return this;
                }

                @Override
                public AsyncIOWriter write(AtmosphereResponse r, byte[] data, int offset, int length)
                    throws IOException {
                    suspendedResponse.write(data, offset, length);
                    suspendedResponse.flushBuffer();
                    return this;
                }

                @Override
                public void close(AtmosphereResponse response) throws IOException {
                }
            });
            // REVISIT we need to keep this response's asyncwriter alive so that data can be written to the
            //   suspended response, but investigate if there is a better alternative.
            r.getResponse().destroyable(false);
            return Action.CONTINUE;
        }

        r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
            @Override
            public void onSuspend(AtmosphereResourceEvent event) {
                final String srid = (String)event.getResource().getRequest()
                    .getAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID);
                LOG.log(Level.FINE, "Registrering suspended resource: {}", srid);
                suspendedResponses.put(srid, event.getResource().getResponse());

                AsyncIOWriter writer = event.getResource().getResponse().getAsyncIOWriter();
                if (writer instanceof AtmosphereInterceptorWriter) {
                    ((AtmosphereInterceptorWriter)writer).interceptor(interceptor);
                }
            }

            @Override
            public void onDisconnect(AtmosphereResourceEvent event) {
                super.onDisconnect(event);
                final String srid = (String)event.getResource().getRequest()
                    .getAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID);
                LOG.log(Level.FINE, "Unregistrering suspended resource: {}", srid);
                suspendedResponses.remove(srid);
            }

        });
        AtmosphereRequest request = r.getRequest();

        if (request.getAttribute(REQUEST_DISPATCHED) == null) {
            AtmosphereResponse response = null;

            AtmosphereFramework framework = r.getAtmosphereConfig().framework();
            try {
                byte[] data = WebSocketUtils.readBody(request.getInputStream());
                if (data.length == 0) {
                    if (AtmosphereResource.TRANSPORT.WEBSOCKET == r.transport()
                        || AtmosphereResource.TRANSPORT.SSE == r.transport()) {
                        r.suspend();
                        return Action.SUSPEND;
                    }
                    return Action.CANCELLED;
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "inspecting data {0}", new String(data));
                }
                try {
                    AtmosphereRequest ar = createAtmosphereRequest(request, data);
                    response = new WrappedAtmosphereResponse(r.getResponse(), ar);
                    ar.localAttributes().put(REQUEST_DISPATCHED, "true");
                    String refid = ar.getHeader(WebSocketConstants.DEFAULT_REQUEST_ID_KEY);
                    if (refid != null) {
                        ar.localAttributes().put(WebSocketConstants.DEFAULT_REQUEST_ID_KEY, refid);
                    }
                    // This is a new request, we must clean the Websocket AtmosphereResource.
                    request.removeAttribute(FrameworkConfig.INJECTED_ATMOSPHERE_RESOURCE);
                    response.request(ar);
                    attachWriter(r);

                    Action action = framework.doCometSupport(ar, response);
                    if (action.type() == Action.TYPE.SUSPEND) {
                        ar.destroyable(false);
                        response.destroyable(false);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error during request dispatching", e);
                    if (response == null) {
                        response = new WrappedAtmosphereResponse(r.getResponse(), request);
                    }
                    if (e instanceof InvalidPathException) {
                        response.setIntHeader(WebSocketUtils.SC_KEY, 400);
                    } else {
                        response.setIntHeader(WebSocketUtils.SC_KEY, 500);
                    }
                    OutputStream out = response.getOutputStream();
                    out.write(createResponse(response, null, true));
                    out.close();
                }
                return Action.CANCELLED;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error during protocol processing", e);
            }
        } else {
            request.destroyable(false);
        }
        return Action.CONTINUE;
    }

    private void attachWriter(final AtmosphereResource r) {
        AtmosphereResponse res = r.getResponse();
        AsyncIOWriter writer = res.getAsyncIOWriter();

        if (writer instanceof AtmosphereInterceptorWriter) {
            AtmosphereInterceptorWriter.class.cast(writer).interceptor(interceptor, 0);
        }
    }

    /**
     * Creates a virtual request using the specified parent request and the actual data.
     *
     * @param r
     * @param data
     * @return
     * @throws IOException
     */
    protected AtmosphereRequest createAtmosphereRequest(AtmosphereRequest r, byte[] data) throws IOException {
        AtmosphereRequest.Builder b = new AtmosphereRequestImpl.Builder();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        Map<String, String> hdrs = WebSocketUtils.readHeaders(in);
        String path = hdrs.get(WebSocketUtils.URI_KEY);
        String origin = r.getRequestURI();
        if (!path.startsWith(origin)) {
            LOG.log(Level.WARNING, "invalid path: {0} not within {1}", new Object[]{path, origin});
            throw new InvalidPathException();
        }

        String queryString = "";
        int index = path.indexOf('?');
        if (index != -1) {
            queryString = path.substring(index + 1);
            path = path.substring(0, index);
        }

        String requestURI = path;
        String requestURL = r.getRequestURL() + requestURI.substring(r.getRequestURI().length());
        String contentType = hdrs.get("Content-Type");

        String method = hdrs.get(WebSocketUtils.METHOD_KEY);
        b.pathInfo(path)
                .contentType(contentType)
                .headers(hdrs)
                .method(method)
                .requestURI(requestURI)
                .requestURL(requestURL)
                .queryString(queryString)
                .request(r);
        // add the body only if it is present
        byte[] body = WebSocketUtils.readBody(in);
        if (body.length > 0) {
            b.body(body);
        }
        return b.build();
    }

    /**
     * Creates a response data based on the specified payload.
     *
     * @param response
     * @param payload
     * @param parent
     * @return
     */
    protected byte[] createResponse(AtmosphereResponse response, byte[] payload, boolean parent) {
        AtmosphereRequest request = response.request();
        String refid = (String)request.getAttribute(WebSocketConstants.DEFAULT_REQUEST_ID_KEY);

        if (AtmosphereResource.TRANSPORT.WEBSOCKET != response.resource().transport()) {
            return payload;
        }
        Map<String, String> headers = new HashMap<>();
        if (refid != null) {
            response.addHeader(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, refid);
            headers.put(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, refid);
        }
        if (parent) {
            // include the status code and content-type and those matched headers
            String sc = response.getHeader(WebSocketUtils.SC_KEY);
            if (sc == null) {
                sc = Integer.toString(response.getStatus());
            }
            headers.put(WebSocketUtils.SC_KEY, sc);
            if (payload != null && payload.length > 0) {
                headers.put("Content-Type",  response.getContentType());
            }
            for (Map.Entry<String, String> hv : response.headers().entrySet()) {
                if (!"Content-Type".equalsIgnoreCase(hv.getKey())
                    && includedheaders != null && includedheaders.matcher(hv.getKey()).matches()
                    && !(excludedheaders != null && excludedheaders.matcher(hv.getKey()).matches())) {
                    headers.put(hv.getKey(), hv.getValue());
                }
            }
        }
        return WebSocketUtils.buildResponse(headers, payload, 0, payload == null ? 0 : payload.length);
    }

    private final class Interceptor extends AsyncIOInterceptorAdapter {

        @Override
        @SuppressWarnings("deprecation")
        public byte[] transformPayload(AtmosphereResponse response, byte[] responseDraft, byte[] data)
            throws IOException {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "transformPayload with draft={0}", new String(responseDraft));
            }
            AtmosphereRequest request = response.request();
            if (request.attributes().get(RESPONSE_PARENT) == null) {
                request.attributes().put(RESPONSE_PARENT, "true");
                return createResponse(response, responseDraft, true);
            }
            return createResponse(response, responseDraft, false);
        }

        @Override
        public byte[] error(AtmosphereResponse response, int statusCode, String reasonPhrase) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "status={0}", statusCode);
            }
            response.setStatus(statusCode, reasonPhrase);
            return createResponse(response, null, true);
        }
    }

    // a workaround to flush the header data upon close when no write operation occurs
    private static class WrappedAtmosphereResponse extends AtmosphereResponseImpl {
        final AtmosphereResponse response;
        ServletOutputStream sout;
        WrappedAtmosphereResponse(AtmosphereResponse resp, AtmosphereRequest req) throws IOException {
            super((HttpServletResponse)resp.getResponse(), null, req, resp.isDestroyable());
            response = resp;
            response.request(req);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (sout == null) {
                sout = new BufferedServletOutputStream(super.getOutputStream());
            }
            return sout;
        }

        private final class BufferedServletOutputStream extends ServletOutputStream {
            final ServletOutputStream delegate;
            CachedOutputStream out = new CachedOutputStream();

            BufferedServletOutputStream(ServletOutputStream d) {
                delegate = d;
            }

            OutputStream getOut() {
                if (out == null) {
                    out = new CachedOutputStream();
                }
                return out;
            }

            void send(boolean complete) throws IOException {
                if (out == null) {
                    return;
                }
                if (response.getStatus() >= 400) {
                    int i = response.getStatus();
                    response.setStatus(200);
                    response.addIntHeader(WebSocketUtils.SC_KEY, i);
                }
                out.flush();
                out.lockOutputStream();
                out.writeCacheTo(delegate);
                delegate.flush();
                out.close();
                out = null;
            }

            public void write(int i) throws IOException {
                getOut().write(i);
            }

            public void close() throws IOException {
                send(true);
                delegate.close();
            }

            public void flush() throws IOException {
                send(false);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                getOut().write(b, off, len);
            }

            public void write(byte[] b) throws IOException {
                getOut().write(b);
            }

            @Override
            public boolean isReady() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setWriteListener(WriteListener arg0) {
                throw new UnsupportedOperationException();
            }
        }


    }
}
