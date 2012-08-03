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

package org.apache.cxf.transport.udp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.workqueue.WorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;

/**
 * 
 */
public class UDPConduit extends AbstractConduit {
    private static final String CXF_MESSAGE_ATTR = "CXFMessage";
    private static final String HOST_PORT = UDPConduit.class + ".host:port";
    private static final Logger LOG = LogUtils.getL7dLogger(UDPDestination.class); 

    Bus bus;
    NioDatagramConnector connector = new NioDatagramConnector();
    ConcurrentHashMap<String, Queue<ConnectFuture>> connections 
        = new ConcurrentHashMap<String, Queue<ConnectFuture>>();
    
    public UDPConduit(EndpointReferenceType t, 
                      final Bus bus) {
        super(t);
        this.bus = bus;
        connector.setHandler(new IoHandlerAdapter() {
            public void messageReceived(IoSession session, Object buf) {
                Message message = (Message)session.getAttribute(CXF_MESSAGE_ATTR);
                if (message.getExchange().getInMessage() == null) {
                    final Message inMessage = new MessageImpl();
                    inMessage.setExchange(message.getExchange());
                    message.getExchange().setInMessage(inMessage);
                    
                    IoSessionInputStream ins = new IoSessionInputStream();
                    ins.write((IoBuffer)buf);
                    inMessage.setContent(InputStream.class, ins);
                    inMessage.put(IoSessionInputStream.class, ins);
                    
                    WorkQueueManager queuem = bus.getExtension(WorkQueueManager.class);
                    WorkQueue queue = queuem.getNamedWorkQueue("udp-conduit");
                    if (queue == null) {
                        queue = queuem.getAutomaticWorkQueue();
                    }
                    queue.execute(new Runnable() {
                        public void run() {
                            incomingObserver.onMessage(inMessage);
                        }
                    });
                    
                } else {
                    IoSessionInputStream ins = message.getExchange().getInMessage().get(IoSessionInputStream.class);
                    ins.write((IoBuffer)buf);
                }
            }
        });
    }

    
    public void close(Message msg) throws IOException {
        super.close(msg);
        if (msg.getExchange().isOneWay() 
            || msg.getExchange().getInMessage() == msg
            || msg.getExchange().getInFaultMessage() == msg) {
            String s = (String)msg.getExchange().get(HOST_PORT);
            ConnectFuture c = msg.getExchange().get(ConnectFuture.class);
            if (s != null && c != null) {
                c.getSession().removeAttribute(CXF_MESSAGE_ATTR);
    
                Queue<ConnectFuture> q = connections.get(s);
                if (q == null) {
                    connections.putIfAbsent(s, new ArrayBlockingQueue<ConnectFuture>(10));
                    q = connections.get(s);
                }
                if (!q.offer(c)) {
                    c.getSession().close(false);
                }
            }
        }
    }
    public void close() {
        super.close();
        for (Queue<ConnectFuture> f : connections.values()) {
            for (ConnectFuture cf : f) {
                cf.getSession().close(false);
            }
        }
        connections.clear();
        connector.dispose();
        connector = null;
    }


    public void prepare(final Message message) throws IOException {
        try {
            String address = (String)message.get(Message.ENDPOINT_ADDRESS);
            if (StringUtils.isEmpty(address)) {
                address = this.getTarget().getAddress().getValue();
            }
            URI uri = new URI(address);
            InetSocketAddress isa = null;
            String hp = ""; 
            if (StringUtils.isEmpty(uri.getHost())) {
                isa = new InetSocketAddress(uri.getPort());
                hp = ":" + uri.getPort();
            } else {
                isa = new InetSocketAddress(uri.getHost(), uri.getPort());
                hp = uri.getHost() + ":" + uri.getPort();
            }
    
            Queue<ConnectFuture> q = connections.get(hp);
            ConnectFuture connFuture = null;
            if (q != null) {
                connFuture = q.poll();
            }
            if (connFuture == null) {
                connFuture = connector.connect(isa);
                connFuture.await();
            }
            connFuture.getSession().setAttribute(CXF_MESSAGE_ATTR, message);
            message.setContent(OutputStream.class, new UDPConduitOutputStream(connector, connFuture, message));
            message.getExchange().put(ConnectFuture.class, connFuture);
            message.getExchange().put(HOST_PORT, uri.getHost() + ":" + uri.getPort());
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public class UDPConduitOutputStream extends OutputStream {
        final ConnectFuture future;
        final NioDatagramConnector connector;
        final IoBuffer buffer = IoBuffer.allocate(64 * 1024); //max size
        final Message message;
        boolean closed;
        
        public UDPConduitOutputStream(NioDatagramConnector connector,
                                      ConnectFuture connFuture,
                                      Message m) {
            this.connector = connector;
            this.future = connFuture;
            this.message = m;
        }

        public void write(int b) throws IOException {
            buffer.put((byte)b);
        }
        public void write(byte b[], int off, int len) throws IOException {
            buffer.put(b, off, len);
        }
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                future.await();
            } catch (InterruptedException e) {
                if (future.getException() != null) {
                    throw new IOException(future.getException());
                }
                throw new IOException(e);
            }
            if (future.getException() != null) {
                throw new IOException(future.getException());
            }
            buffer.flip();
            future.getSession().write(buffer);
            if (message.getExchange().isOneWay()) {
                future.getSession().close(true);
            }
        }
    }
    
    protected Logger getLogger() {
        return LOG;
    }

}
