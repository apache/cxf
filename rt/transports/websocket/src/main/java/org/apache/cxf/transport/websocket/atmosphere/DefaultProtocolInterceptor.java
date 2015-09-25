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
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.InvalidPathException;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AsyncIOInterceptor;
import org.atmosphere.cpr.AsyncIOInterceptorAdapter;
import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereInterceptorWriter;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.FrameworkConfig;

/**
 * DefaultProtocolInterceptor provides the default CXF's WebSocket protocol that uses.
 * 
 */
@AtmosphereInterceptorService
public class DefaultProtocolInterceptor extends AtmosphereInterceptorAdapter {
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultProtocolInterceptor.class);

    private static final String REQUEST_DISPATCHED = "request.dispatched";
    private static final String RESPONSE_PARENT = "response.parent";

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
        AtmosphereRequest request = r.getRequest();

        if (request.getAttribute(REQUEST_DISPATCHED) == null) {
            AtmosphereResponse response = new WrappedAtmosphereResponse(r.getResponse(), request);

            AtmosphereFramework framework = r.getAtmosphereConfig().framework();
            try {
                byte[] data = WebSocketUtils.readBody(request.getInputStream());
                if (data.length == 0) {
                    return Action.CANCELLED;
                }
                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "inspecting data {0}", new String(data));
                }
                try {
                    AtmosphereRequest ar = createAtmosphereRequest(request, data);
                    ar.attributes().put(REQUEST_DISPATCHED, "true");
                    String refid = ar.getHeader(WebSocketConstants.DEFAULT_REQUEST_ID_KEY);
                    if (refid != null) {
                        ar.attributes().put(WebSocketConstants.DEFAULT_REQUEST_ID_KEY, refid);
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
                    if (e instanceof InvalidPathException) {
                        response.setStatus(400);
                    } else {
                        response.setStatus(500);
                    }
                    response.getOutputStream().write(createResponse(response, null, true));
                }
                return Action.CANCELLED;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error during protocol processing", e);
                return Action.CONTINUE;
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
            //REVIST need a better way to add a custom filter at the first entry and not at the last as
            // e.g. interceptor(AsyncIOInterceptor interceptor, int position)
            Deque<AsyncIOInterceptor> filters = AtmosphereInterceptorWriter.class.cast(writer).filters();
            if (!filters.contains(interceptor)) {
                filters.addFirst(interceptor);
            }
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
        AtmosphereRequest.Builder b = new AtmosphereRequest.Builder();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        Map<String, String> hdrs = WebSocketUtils.readHeaders(in);
        String path = hdrs.get(WebSocketUtils.URI_KEY);
        String origin = r.getRequestURI();
        if (!path.startsWith(origin)) {
            LOG.log(Level.WARNING, "invalid path: {0} not within {1}", new Object[]{path, origin});
            throw new InvalidPathException();
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

        Map<String, String> headers = new HashMap<String, String>();
        if (refid != null) {
            response.addHeader(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, refid);
            headers.put(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, refid);
        }
        if (parent) {
            // include the status code and content-type and those matched headers
            headers.put(WebSocketUtils.SC_KEY, Integer.toString(response.getStatus()));
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
        public byte[] transformPayload(AtmosphereResponse response, byte[] responseDraft, byte[] data) 
            throws IOException {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "transformPayload with draft={0}", new String(responseDraft));
            }
            AtmosphereRequest request = response.request();
            if (request.attributes().get(RESPONSE_PARENT) == null) {
                request.attributes().put(RESPONSE_PARENT, "true");
                return createResponse(response, responseDraft, true);
            } else {
                return createResponse(response, responseDraft, false);
            }
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
    private class WrappedAtmosphereResponse extends AtmosphereResponse {
        public WrappedAtmosphereResponse(AtmosphereResponse resp, AtmosphereRequest req) {
            super((HttpServletResponse)resp.getResponse(), resp.getAsyncIOWriter(), req, resp.isDestroyable());
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            final ServletOutputStream delegate = super.getOutputStream();
            return new ServletOutputStream() {
                private boolean written;

                @Override
                public void write(int i) throws IOException {
                    written = true;
                    delegate.write(i);
                }

                @Override
                public void close() throws IOException {
                    if (!written) {
                        delegate.write(createResponse(WrappedAtmosphereResponse.this, null, true));
                    }
                    delegate.close();
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    written = true;
                    delegate.write(b, off, len);
                }

                @Override
                public void write(byte[] b) throws IOException {
                    written = true;
                    delegate.write(b);
                }
            };
        }
    }
}
