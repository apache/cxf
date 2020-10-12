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

package org.apache.cxf.transport.websocket.ahc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

/**
 *
 */
public class AhcWebSocketConduit extends URLConnectionHTTPConduit {
    private static final Logger LOG = LogUtils.getL7dLogger(AhcWebSocketConduit.class);

    private AsyncHttpClient ahcclient;
    private WebSocket websocket;

    //REVISIT make these keys configurable
    private String requestIdKey = WebSocketConstants.DEFAULT_REQUEST_ID_KEY;
    private String responseIdKey = WebSocketConstants.DEFAULT_RESPONSE_ID_KEY;

    private Map<String, RequestResponse> uncorrelatedRequests = new ConcurrentHashMap<>();

    public AhcWebSocketConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
    }

    @Override
    protected void setupConnection(Message message, Address address, HTTPClientPolicy csPolicy)
        throws IOException {

        URI currentURL = address.getURI();
        String s = currentURL.getScheme();
        if (!"ws".equals(s) && !"wss".equals(s)) {
            throw new MalformedURLException("unknown protocol: " + s);
        }

        message.put("http.scheme", currentURL.getScheme());
        String httpRequestMethod =
                (String)message.get(Message.HTTP_REQUEST_METHOD);
        if (httpRequestMethod == null) {
            httpRequestMethod = "POST";
            message.put(Message.HTTP_REQUEST_METHOD, httpRequestMethod);
        }

        final AhcWebSocketConduitRequest request = new AhcWebSocketConduitRequest(currentURL, httpRequestMethod);
        final int rtimeout = determineReceiveTimeout(message, csPolicy);
        request.setReceiveTimeout(rtimeout);
        message.put(AhcWebSocketConduitRequest.class, request);


    }

    private synchronized AsyncHttpClient getAsyncHttpClient(Message message) {
        if (ahcclient == null) {
            DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
            AuthorizationPolicy ap = getEffectiveAuthPolicy(message);
            if (ap != null
                && (!StringUtils.isEmpty(ap.getAuthorizationType())
                    || !StringUtils.isEmpty(ap.getUserName()))) {
                Realm.Builder rb = new Realm.Builder(ap.getUserName(), ap.getPassword());
                if (ap.getAuthorizationType() == null) {
                    rb.setScheme(AuthScheme.BASIC);
                } else {
                    rb.setScheme(AuthScheme.valueOf(ap.getAuthorizationType().toUpperCase()));
                }
                rb.setUsePreemptiveAuth(true);
                builder.setRealm(rb.build());
            }

            AsyncHttpClientConfig config = builder.build();
            ahcclient = new DefaultAsyncHttpClient(config);
        }
        return ahcclient;
    }

    @Override
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest,
                                              boolean isChunking, int chunkThreshold) throws IOException {


        AhcWebSocketConduitRequest entity = message.get(AhcWebSocketConduitRequest.class);
        return new AhcWebSocketWrappedOutputStream(message, needToCacheRequest, isChunking, chunkThreshold,
                                                   getConduitName(), entity.getUri());
    }

    public class AhcWebSocketWrappedOutputStream extends WrappedOutputStream {
        private AhcWebSocketConduitRequest entity;
        private volatile Response response;

        protected AhcWebSocketWrappedOutputStream(Message message, boolean possibleRetransmit,
                                                  boolean isChunking, int chunkThreshold, String conduitName, URI url) {
            super(message, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);

            entity = message.get(AhcWebSocketConduitRequest.class);
            //REVISIT how we prepare the request
            String requri = (String)message.getContextualProperty("org.apache.cxf.request.uri");
            if (requri != null) {
                // jaxrs speicfies a sub-path using prop org.apache.cxf.request.uri
                if (requri.startsWith("ws")) {
                    entity.setPath(requri.substring(requri.indexOf('/', 3 + requri.indexOf(':'))));
                } else {
                    entity.setPath(url.getPath() + requri);
                }
            } else {
                // jaxws
                entity.setPath(url.getPath());
            }
            entity.setId(UUID.randomUUID().toString());
            uncorrelatedRequests.put(entity.getId(), new RequestResponse(entity));
        }

        @Override
        protected void setupWrappedStream() throws IOException {
            connect();

            wrappedStream = new OutputStream() {

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    //REVISIT support multiple writes and flush() to write the entire block data?
                    // or provides the fragment mode?
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", entity.getContentType());
                    headers.put(requestIdKey, entity.getId());
                    websocket.sendBinaryFrame(WebSocketUtils.buildRequest(
                        entity.getMethod(), entity.getPath(),
                        headers,
                        b, off, len));
                }

                @Override
                public void write(int b) throws IOException {
                    //REVISIT support this single byte write and use flush() to write the block data?
                }

                @Override
                public void close() throws IOException {
                }
            };
        }

        @Override
        protected void handleNoOutput() throws IOException {
            connect();
            Map<String, String> headers = new HashMap<>();
            headers.put(requestIdKey, entity.getId());
            websocket.sendBinaryFrame(WebSocketUtils.buildRequest(
                entity.getMethod(), entity.getPath(),
                headers,
                null, 0, 0));
        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            return null;
        }

        @Override
        protected void setProtocolHeaders() throws IOException {
            Headers h = new Headers(outMessage);
            entity.setContentType(h.determineContentType());
            //REVISIT may provide an option to add other headers
//          boolean addHeaders = PropertyUtils.isTrue(outMessage.getContextualProperty(Headers.ADD_HEADERS_PROPERTY));
        }

        @Override
        protected void setFixedLengthStreamingMode(int i) {
            // ignore
        }

        @Override
        protected int getResponseCode() throws IOException {
            Response r = getResponse();
            return r.getStatusCode();
        }

        @Override
        protected String getResponseMessage() throws IOException {
            //TODO return a generic message based on the status code
            return null;
        }

        @Override
        protected void updateResponseHeaders(Message inMessage) throws IOException {
            Headers h = new Headers(inMessage);
            String ct = getResponse().getContentType();
            inMessage.put(Message.CONTENT_TYPE, ct);

            //REVISIT if we are allowing more headers, we need to add them into the cxf's headers
            h.headerMap().put(Message.CONTENT_TYPE, Collections.singletonList(ct));
        }

        @Override
        protected void handleResponseAsync() throws IOException {
            handleResponseOnWorkqueue(true, false);
        }

        @Override
        protected void closeInputStream() throws IOException {
        }

        @Override
        protected boolean usingProxy() {
            // TODO add proxy support ...
            return false;
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            Response r = getResponse();
            //REVISIT
            return new java.io.ByteArrayInputStream(r.getTextEntity().getBytes());
        }

        @Override
        protected InputStream getPartialResponse() throws IOException {
            Response r = getResponse();
            //REVISIT
            return new java.io.ByteArrayInputStream(r.getTextEntity().getBytes());
        }

        @Override
        protected void setupNewConnection(String newURL) throws IOException {
            // TODO
            throw new IOException("not supported");
        }

        @Override
        protected void retransmitStream() throws IOException {
            // TODO
            throw new IOException("not supported");
        }

        @Override
        protected void updateCookiesBeforeRetransmit() throws IOException {
            // ignore for now and may consider a specific websocket binding variant to use cookies
        }

        @Override
        public void thresholdReached() throws IOException {
            // ignore
        }

        //
        // other methods follow
        //

        protected void connect() {
            LOG.log(Level.FINE, "connecting");
            if (websocket == null) {
                try {
                    websocket = getAsyncHttpClient(outMessage)
                        .prepareGet(url.toASCIIString())
                        .execute(
                            new WebSocketUpgradeHandler.Builder()
                            .addWebSocketListener(new AhcWebSocketListener()).build()).get();
                    LOG.log(Level.FINE, "connected");
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "unable to connect", e);
                }
            } else {
                LOG.log(Level.FINE, "already connected");
            }
        }

        Response getResponse() throws IOException {
            if (response == null) {
                String rid = entity.getId();
                RequestResponse rr = uncorrelatedRequests.get(rid);
                synchronized (rr) {
                    try {
                        long timetowait = entity.getReceiveTimeout();
                        response = rr.getResponse();
                        if (response == null) {
                            rr.wait(timetowait);
                            response = rr.getResponse();
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                if (response == null) {
                    throw new SocketTimeoutException("Read timed out while invoking " + entity.getUri());
                }
            }
            return response;
        }
    }

    protected class AhcWebSocketListener implements WebSocketListener {

        public void onOpen(WebSocket ws) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onOpen({0})", ws);
            }
        }

        public void onClose(WebSocket ws, int code, String reason) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onCose({0})", ws);
            }
        }

        public void onError(Throwable t) {
            LOG.log(Level.SEVERE, "[ws] onError", t);
        }

        @Override
        public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onMessage({0})", payload);
            }
            Response resp = new Response(responseIdKey, payload);
            RequestResponse rr = uncorrelatedRequests.get(resp.getId());
            if (rr != null) {
                synchronized (rr) {
                    rr.setResponse(resp);
                    rr.notifyAll();
                }
            }
        }

        @Override
        public void onTextFrame(String payload, boolean finalFragment, int rsv) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "onMessage({0})", payload);
            }
            Response resp = new Response(responseIdKey, payload);
            RequestResponse rr = uncorrelatedRequests.get(resp.getId());
            if (rr != null) {
                synchronized (rr) {
                    rr.setResponse(resp);
                    rr.notifyAll();
                }
            }
        }

    }

    // Request and Response are used to represent request and response messages transfered over the websocket
    //REVIST move these classes to be used in other places after finalizing their contained information.
    static class Response {
        private Object data;
        private int pos;
        private int statusCode;
        private String contentType;
        private String id;
        private Object entity;

        Response(String idKey, Object data) {
            this.data = data;
            String line;
            boolean first = true;
            while ((line = readLine()) != null) {
                if (first && isStatusCode(line)) {
                    statusCode = Integer.parseInt(line);
                    continue;
                }
                first = false;
                int del = line.indexOf(':');
                String h = line.substring(0, del).trim();
                String v = line.substring(del + 1).trim();
                if ("Content-Type".equalsIgnoreCase(h)) {
                    contentType = v;
                } else if (WebSocketConstants.DEFAULT_RESPONSE_ID_KEY.equals(h)) {
                    id = v;
                }
            }
            if (data instanceof String) {
                entity = ((String)data).substring(pos);
            } else if (data instanceof byte[]) {
                entity = new byte[((byte[])data).length - pos];
                System.arraycopy(data, pos, entity, 0, ((byte[])entity).length);
            }
        }

        private static boolean isStatusCode(String line) {
            char c = line.charAt(0);
            return '0' <= c && c <= '9';
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
        }

        public String getId() {
            return id;
        }

        public Object getEntity() {
            return entity;
        }

        public String getTextEntity() {
            return gettext(entity);
        }

        private String readLine() {
            StringBuilder sb = new StringBuilder();
            while (pos < length(data)) {
                int c = getchar(data, pos++);
                if (c == '\n') {
                    break;
                } else if (c == '\r') {
                    continue;
                } else {
                    sb.append((char)c);
                }
            }
            if (sb.length() == 0) {
                return null;
            }
            return sb.toString();
        }

        private int length(Object o) {
            if (o instanceof String) {
                return ((String)o).length();
            } else if (o instanceof char[]) {
                return ((char[])o).length;
            } else if (o instanceof byte[]) {
                return ((byte[])o).length;
            } else {
                return 0;
            }
        }

        private int getchar(Object o, int p) {
            return 0xff & (o instanceof String ? ((String)o).charAt(p) : (o instanceof byte[] ? ((byte[])o)[p] : -1));
        }

        private String gettext(Object o) {
            return o instanceof String ? (String)o : (o instanceof byte[] ? new String((byte[])o) : null);
        }
    }

    static class RequestResponse {
        private AhcWebSocketConduitRequest request;
        private Response response;
        RequestResponse(AhcWebSocketConduitRequest request) {
            this.request = request;
        }
        public AhcWebSocketConduitRequest getRequest() {
            return request;
        }
        public Response getResponse() {
            return response;
        }
        public void setResponse(Response response) {
            this.response = response;
        }
    }
}
