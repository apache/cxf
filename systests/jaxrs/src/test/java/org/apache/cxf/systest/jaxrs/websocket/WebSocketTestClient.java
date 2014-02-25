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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

import org.apache.cxf.common.logging.LogUtils;



/**
 * Test client to do websocket calls.
 * @see JAXRSClientServerWebSocketTest
 * 
 * we may put this in test-tools so that other systests can use this code.
 * for now keep it here to experiment jaxrs websocket scenarios.
 */
class WebSocketTestClient {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketTestClient.class);

    private List<String> received;
    private List<byte[]> receivedBytes;
    private CountDownLatch latch;
    private AsyncHttpClient client;
    private WebSocket websocket;
    private String url;
    
    public WebSocketTestClient(String url, int count) {
        this.received = new ArrayList<String>();
        this.receivedBytes = new ArrayList<byte[]>();
        this.latch = new CountDownLatch(count);
        this.client = new AsyncHttpClient();
        this.url = url;
    }
    
    public void connect() throws InterruptedException, ExecutionException, IOException {
        websocket = client.prepareGet(url).execute(
            new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WsSocketListener()).build()).get();
    }

    public void sendTextMessage(String message) {
        websocket.sendTextMessage(message);
    }

    public void sendMessage(byte[] message) {
        websocket.sendMessage(message);
    }
    
    public boolean await(int secs) throws InterruptedException {
        return latch.await(secs, TimeUnit.SECONDS);
    }
    
    public void reset(int count) {
        latch = new CountDownLatch(count);
        received.clear();
        receivedBytes.clear();
    }

    public List<String> getReceived() {
        return received;
    }
    
    public List<byte[]> getReceivedBytes() {
        return receivedBytes;
    }

    public void close() {
        websocket.close();
        client.close();
    }

    class WsSocketListener implements WebSocketTextListener, WebSocketByteListener {

        public void onOpen(WebSocket ws) {
            LOG.info("[ws] opened");            
        }

        public void onClose(WebSocket ws) {
            LOG.info("[ws] closed");            
        }

        public void onError(Throwable t) {
            LOG.info("[ws] error: " + t);                        
        }

        public void onMessage(byte[] message) {
            receivedBytes.add(message);
            LOG.info("[ws] received bytes --> " + makeString(message));
            latch.countDown();
        }

        public void onFragment(byte[] fragment, boolean last) {
            // TODO Auto-generated method stub
            LOG.info("TODO [ws] received fragment bytes --> " + makeString(fragment) + "; last? " + last);
        }

        public void onMessage(String message) {
            received.add(message);
            LOG.info("[ws] received --> " + message);
            latch.countDown();
        }

        public void onFragment(String fragment, boolean last) {
            // TODO Auto-generated method stub
            LOG.info("TODO [ws] received fragment --> " + fragment + "; last? " + last);
        }
        
    }
    
    private static String makeString(byte[] data) {
        return data == null ? null : makeString(data, 0, data.length).toString();
    }

    private static StringBuilder makeString(byte[] data, int offset, int length) {
        if (data .length > 256) {
            return makeString(data, offset, 256).append("...");
        }
        StringBuilder xbuf = new StringBuilder().append("\nHEX: ");
        StringBuilder cbuf = new StringBuilder().append("\nASC: ");
        for (byte b : data) {
            xbuf.append(Integer.toHexString(0xff & b)).append(' ');
            cbuf.append((0x80 & b) != 0 ? '.' : (char)b).append("  ");
        }
        return xbuf.append(cbuf);
    }
}
