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
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transport.websocket.WebSocketUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * 
 */
public class AhcWebSocketConduit extends URLConnectionHTTPConduit {
    private static final Logger LOG = LogUtils.getL7dLogger(AhcWebSocketConduit.class);

    private AsyncHttpClient ahcclient;
    private WebSocket websocket;

    //FIXME use a ref-id based request and response association instead of using this sequential queue
    private BlockingQueue<Response> responseQueue = new ArrayBlockingQueue<Response>(4096);

    public AhcWebSocketConduit(Bus b, EndpointInfo ei, EndpointReferenceType t) throws IOException {
        super(b, ei, t);
        ahcclient = new AsyncHttpClient();
    }

    @Override
    protected void setupConnection(Message message, URI currentURL, HTTPClientPolicy csPolicy)
        throws IOException {

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

        final AhcWebSocketRequest request = new AhcWebSocketRequest(currentURL, httpRequestMethod);
        message.put(AhcWebSocketRequest.class, request);
    }

    @Override
    protected OutputStream createOutputStream(Message message, boolean needToCacheRequest,
                                              boolean isChunking, int chunkThreshold) throws IOException {
        AhcWebSocketRequest entity = message.get(AhcWebSocketRequest.class);
        AhcWebSocketWrappedOutputStream out =
            new AhcWebSocketWrappedOutputStream(message, needToCacheRequest, isChunking, chunkThreshold,
                                                getConduitName(), entity.getUri());
        return out;
    }

    public class AhcWebSocketWrappedOutputStream extends WrappedOutputStream {
        private Request request;
        private Response response;

        protected AhcWebSocketWrappedOutputStream(Message message, boolean possibleRetransmit,
                                                  boolean isChunking, int chunkThreshold, String conduitName, URI url) {
            super(message, possibleRetransmit, isChunking, chunkThreshold, conduitName, url);

            //REVISIT how we prepare the request
            this.request = new Request();
            request.setMethod((String)outMessage.get(Message.HTTP_REQUEST_METHOD));
            request.setPath(url.getPath() + (String)outMessage.getContextualProperty("org.apache.cxf.request.uri"));
        }

        @Override
        protected void setupWrappedStream() throws IOException {
            connect();

            wrappedStream = new OutputStream() {

                @Override
                public void write(byte b[], int off, int len) throws IOException {
                    //REVISIT support multiple writes and flush() to write the entire block data?
                    websocket.sendMessage(WebSocketUtils.buildRequest(
                        request.getMethod(), request.getPath(),
                        Collections.<String, String>singletonMap("Content-Type", request.getContentType()),
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
            websocket.sendMessage(WebSocketUtils.buildRequest(
                request.getMethod(), request.getPath(),
                Collections.<String, String>emptyMap(),
                null, 0, 0));
        }

        @Override
        protected HttpsURLConnectionInfo getHttpsURLConnectionInfo() throws IOException {
            return null;
        }

        @Override
        protected void setProtocolHeaders() throws IOException {
            Headers h = new Headers(outMessage);
            request.setContentType(h.determineContentType());

            //REVISIT may provide an option to add other headers
//          boolean addHeaders = MessageUtils.isTrue(outMessage.getContextualProperty(Headers.ADD_HEADERS_PROPERTY));
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
            // TODO Auto-generated method stub
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
            LOG.log(Level.INFO, "connecting");
            if (websocket == null) {
                try {
                    websocket = ahcclient.prepareGet(url.toASCIIString()).execute(
                            new WebSocketUpgradeHandler.Builder()
                            .addWebSocketListener(new AhcWebSocketListener()).build()).get();
                    LOG.log(Level.INFO, "connected");
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "unable to connect", e);
                }
            } else {
                LOG.log(Level.INFO, "already connected");
            }
        }

        synchronized Response getResponse() throws IOException {
            if (response == null) {
                //TODO add a configurable timeout
                try {
                    response = responseQueue.take();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            if (response == null) {
                throw new IOException("timeout");
            }
            return response;
        }
    }

    protected class AhcWebSocketListener implements WebSocketTextListener, WebSocketByteListener {

        public void onOpen(WebSocket ws) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "onOpen({0})", ws);
            }
        }

        public void onClose(WebSocket ws) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "onCose({0})", ws);
            }
        }

        public void onError(Throwable t) {
            LOG.log(Level.SEVERE, "[ws] onError", t);
        }

        public void onMessage(byte[] message) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "onMessage({0})", message);
            }

            responseQueue.add(new Response(message));
        }

        public void onFragment(byte[] fragment, boolean last) {
            //TODO
            LOG.log(Level.WARNING, "NOT IMPLEMENTED onFragment({0}, {1})", new Object[]{fragment, last});
        }

        public void onMessage(String message) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "onMessage({0})", message);
            }

            responseQueue.add(new Response(message));
        }

        public void onFragment(String fragment, boolean last) {
            //TODO
            LOG.log(Level.WARNING, "NOT IMPLEMENTED onFragment({0}, {1})", new Object[]{fragment, last});
        }
    }

    // Request and Response are used to represent request and response messages trasfered over the websocket
    //REVIST move these classes to be used in other places
    static class Request {
        private String method;
        private String url;
        private String contentType;

        public String getMethod() {
            return method;
        }
        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return url;
        }
        public void setPath(String path) {
            this.url = path;
        }

        public String getContentType() {
            return contentType;
        }
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    static class Response {
        private Object data;
        private int pos;
        private int statusCode;
        private String contentType;
        private Object entity;

        public Response(Object data) {
            this.data = data;
            String line = readLine();
            if (line != null) {
                statusCode = Integer.parseInt(line);
                while ((line = readLine()) != null) {
                    if (line.length() > 0) {
                        int del = line.indexOf(':');
                        String h = line.substring(0, del).trim();
                        String v = line.substring(del + 1).trim();
                        if ("Content-Type".equalsIgnoreCase(h)) {
                            contentType = v;
                        }
                    }
                }
            }
            if (data instanceof String) {
                entity = ((String)data).substring(pos);
            } else if (data instanceof byte[]) {
                entity = new byte[((byte[])data).length - pos];
                System.arraycopy((byte[])data, pos, (byte[])entity, 0, ((byte[])entity).length);
            }
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
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
            return o instanceof char[] ? ((String)o).length() : (o instanceof byte[] ? ((byte[])o).length : 0);
        }

        private int getchar(Object o, int p) {
            return 0xff & (o instanceof String ? ((String)o).charAt(p) : (o instanceof byte[] ? ((byte[])o)[p] : -1));
        }

        private String gettext(Object o) {
            return o instanceof String ? (String)o : (o instanceof byte[] ? new String((byte[])o) : null);
        }
    }

}
