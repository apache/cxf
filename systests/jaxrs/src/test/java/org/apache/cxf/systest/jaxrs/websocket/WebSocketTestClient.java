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

package org.apache.cxf.systest.jaxrs.websocket;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

/**
 * Test client to do websocket calls.
 * @see JAXRSClientServerWebSocketTest
 *
 * we may put this in test-tools so that other systests can use this code.
 * for now keep it here to experiment jaxrs websocket scenarios.
 */
class WebSocketTestClient implements Closeable {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketTestClient.class);

    private final List<Object> received = new ArrayList<>();
    private CountDownLatch latch;
    private final AsyncHttpClient client = new DefaultAsyncHttpClient();
    private final WebSocket websocket;

    WebSocketTestClient(String url) throws InterruptedException, ExecutionException, TimeoutException {
        websocket = Objects.requireNonNull(client.prepareGet(url).execute(
            new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                new WsSocketListener()).build()).get(1000, TimeUnit.SECONDS));
        reset(1);
    }

    public void sendTextMessage(String message) {
        websocket.sendTextFrame(message);
    }

    public void sendMessage(byte[] message) {
        websocket.sendBinaryFrame(message);
    }

    public boolean await(int secs) throws InterruptedException {
        return latch.await(secs, TimeUnit.SECONDS);
    }

    public void reset(int count) {
        latch = new CountDownLatch(count);
        received.clear();
    }

    public List<Object> getReceived() {
        return received;
    }

    public List<Response> getReceivedResponses() {
        List<Response> responses = new ArrayList<>(received.size());
        for (Object o : received) {
            responses.add(new Response(o));
        }
        return responses;
    }

    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    class WsSocketListener implements WebSocketListener {

        private final List<Object> fragments = new ArrayList<>();

        public void onOpen(WebSocket ws) {
            LOG.info("[ws] opened");
        }

        public void onClose(WebSocket ws, int code, String reason) {
            LOG.info("[ws] closed");
        }

        public void onError(Throwable t) {
            LOG.info("[ws] error: " + t);
        }

        @Override
        public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
            LOG.info("[ws] received binary frame (last?" + finalFragment + ") --> " + new String(payload));
            if (!finalFragment) {
                fragments.add(payload);
            } else {
                if (fragments.isEmpty()) {
                    received.add(payload);
                } else {
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    for (Iterator<Object> it = fragments.iterator(); it.hasNext();) {
                        Object o = it.next();
                        if (o instanceof byte[]) {
                            bao.write((byte[])o, 0, ((byte[])o).length);
                            it.remove();
                        }
                    }
                    received.add(bao.toByteArray());
                }
                latch.countDown();
            }
        }

        @Override
        public void onTextFrame(String payload, boolean finalFragment, int rsv) {
            LOG.info("[ws] received text frame (last?" + finalFragment + ") --> " + payload);
            if (!finalFragment) {
                fragments.add(payload);
            } else {
                if (fragments.isEmpty()) {
                    received.add(payload);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Iterator<Object> it = fragments.iterator(); it.hasNext();) {
                        Object o = it.next();
                        if (o instanceof String) {
                            sb.append((String)o);
                            it.remove();
                        }
                    }
                    received.add(sb.toString());
                }
                latch.countDown();
            }
        }
    }

    //TODO this is a temporary way to verify the response; we should come up with something better.
    public static class Response {
        private Object data;
        private int pos;
        private int statusCode;
        private String contentType;
        private String id;
        private Object entity;

        Response(Object data) {
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

        public Object getEntity() {
            return entity;
        }

        public String getTextEntity() {
            return gettext(entity);
        }

        public String getId() {
            return id;
        }

        public String toString() {
            return new StringBuilder(128)
                .append("Status: ").append(statusCode).append("\r\n")
                .append("Type: ").append(contentType).append("\r\n")
                .append("Entity: ").append(gettext(entity)).append("\r\n")
                .toString();
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
}
