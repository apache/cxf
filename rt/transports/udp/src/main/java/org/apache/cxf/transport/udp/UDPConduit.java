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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
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
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;

/**
 *
 */
public class UDPConduit extends AbstractConduit {
    /**
     * For broadcast/multicast, the specific network interface to use.   This can either be
     * a specific  java.net.NetworkInterface or a string for NetworkInterface.getByName(String name)
     */
    public static final String NETWORK_INTERFACE = UDPConduit.class.getName() + ".NETWORK_INTERFACE";

    private static final String CXF_MESSAGE_ATTR = "CXFMessage";
    private static final String MULTI_RESPONSE_TIMEOUT = "udp.multi.response.timeout";
    private static final String HOST_PORT = UDPConduit.class + ".host:port";
    private static final Logger LOG = LogUtils.getL7dLogger(UDPDestination.class);

    Bus bus;
    NioDatagramConnector connector = new NioDatagramConnector();
    ConcurrentHashMap<String, Queue<ConnectFuture>> connections
        = new ConcurrentHashMap<>();

    public UDPConduit(EndpointReferenceType t,
                      final Bus bus) {
        super(t);
        this.bus = bus;
        connector.getSessionConfig().setReadBufferSize(64 * 1024);
        connector.getSessionConfig().setSendBufferSize(64 * 1024);
        connector.setHandler(new IoHandlerAdapter() {
            public void messageReceived(IoSession session, Object buf) {
                Message message = (Message)session.getAttribute(CXF_MESSAGE_ATTR);
                dataReceived(message, (IoBuffer)buf, true, false);
            }
        });
    }

    private void dataReceived(Message message, IoBuffer buf, boolean async, boolean multi) {
        synchronized (message.getExchange()) {
            if (message.getExchange().getInMessage() == null) {
                final Message inMessage = new MessageImpl();
                IoSessionInputStream ins = new IoSessionInputStream(buf);
                inMessage.setContent(InputStream.class, ins);
                inMessage.put(IoSessionInputStream.class, ins);

                message.getExchange().setInMessage(inMessage);
                inMessage.setExchange(message.getExchange());

                Map<String, Object> mp = null;
                if (multi) {
                    mp = new HashMap<>(message.getExchange());
                }

                if (async) {
                    WorkQueueManager queuem = bus.getExtension(WorkQueueManager.class);
                    WorkQueue queue = queuem.getNamedWorkQueue("udp-conduit");
                    if (queue == null) {
                        queue = queuem.getAutomaticWorkQueue();
                    }
                    queue.execute(() -> incomingObserver.onMessage(inMessage));
                } else {
                    incomingObserver.onMessage(inMessage);
                    if (!message.getExchange().isSynchronous() || multi) {
                        message.getExchange().setInMessage(null);
                        message.getExchange().setInFaultMessage(null);
                    }
                }
                if (mp != null) {
                    Collection<String> s = new ArrayList<>(message.getExchange().keySet());
                    for (String s2 : s) {
                        message.getExchange().remove(s2);
                    }
                    message.getExchange().putAll(mp);
                }
            } else {
                IoSessionInputStream ins = message.getExchange().getInMessage().get(IoSessionInputStream.class);
                ins.setBuffer(buf);
            }
        }
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
                    c.getSession().closeOnFlush();
                }
            }
        }
    }
    public void close() {
        super.close();
        for (Queue<ConnectFuture> f : connections.values()) {
            for (ConnectFuture cf : f) {
                cf.getSession().closeOnFlush();
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
            if (StringUtils.isEmpty(uri.getHost())) {
                //NIO doesn't support broadcast, we need to drop down to raw
                //java.io for these
                String s = uri.getSchemeSpecificPart();
                if (s.startsWith("//:")) {
                    s = s.substring(3);
                }
                if (s.indexOf('/') != -1) {
                    s = s.substring(0, s.indexOf('/'));
                }
                int port = Integer.parseInt(s);
                sendViaBroadcast(message, null, port);
            } else {
                final InetSocketAddress isa = new InetSocketAddress(uri.getHost(), uri.getPort());
                if (isa.getAddress().isMulticastAddress()) {
                    sendViaBroadcast(message, isa, isa.getPort());
                    return;
                }

                final String hp = uri.getHost() + ':' + uri.getPort();
                Queue<ConnectFuture> q = connections.get(hp);
                ConnectFuture connFuture = null;
                if (q != null) {
                    connFuture = q.poll();
                }
                if (connFuture == null) {
                    connFuture = connector.connect(isa);
                    connFuture.await();
                    ((DatagramSessionConfig)connFuture.getSession().getConfig()).setSendBufferSize(64 * 1024);
                    ((DatagramSessionConfig)connFuture.getSession().getConfig()).setReceiveBufferSize(64 * 1024);
                }
                connFuture.getSession().setAttribute(CXF_MESSAGE_ATTR, message);
                message.setContent(OutputStream.class, new UDPConduitOutputStream(connFuture));
                message.getExchange().put(ConnectFuture.class, connFuture);
                message.getExchange().put(HOST_PORT, hp);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private void sendViaBroadcast(Message message, InetSocketAddress isa, int port) {
        message.setContent(OutputStream.class,
                           new UDPBroadcastOutputStream(port, isa, message));

    }

    private final class UDPBroadcastOutputStream extends LoadingByteArrayOutputStream {
        private final int port;
        private final Message message;
        private final InetSocketAddress multicast;

        private UDPBroadcastOutputStream(int port, InetSocketAddress isa, Message message) {
            this.port = port;
            this.message = message;
            this.multicast = isa;
        }

        public void close() throws IOException {
            super.close();

            try (DatagramSocket socket = multicast != null ? new MulticastSocket(null) : new DatagramSocket()) {
                socket.setSendBufferSize(this.size());
                socket.setReceiveBufferSize(64 * 1024);
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                Object netIntFromMsg = message.getContextualProperty(NETWORK_INTERFACE);
                NetworkInterface netInf = null;
                if (netIntFromMsg instanceof String) {
                    netInf = NetworkInterface.getByName((String)netIntFromMsg);
                } else if (netIntFromMsg instanceof NetworkInterface) {
                    netInf = (NetworkInterface)netIntFromMsg;
                }
                if (multicast != null) {
                    ((MulticastSocket)socket).setLoopbackMode(false);
                    if (netInf != null) {
                        ((MulticastSocket)socket).setNetworkInterface(netInf);
                    }
                }

                if (multicast == null) {
                    List<NetworkInterface> interfaces = netInf == null 
                        ? Collections.list(NetworkInterface.getNetworkInterfaces()) : Collections.singletonList(netInf);
                    for (NetworkInterface networkInterface : interfaces) {
                        if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                            continue;
                        }
                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast == null) {
                                continue;
                            }
                            DatagramPacket sendPacket = new DatagramPacket(this.getRawBytes(),
                                                                           0,
                                                                           this.size(),
                                                                           broadcast,
                                                                           port);

                            try {
                                socket.send(sendPacket);
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                    }
                } else {
                    DatagramPacket sendPacket = new DatagramPacket(this.getRawBytes(),
                                                                   0,
                                                                   this.size(),
                                                                   multicast);

                    try {
                        socket.send(sendPacket);
                    } catch (Exception e) {
                        //ignore
                    }
                }

                if (!message.getExchange().isOneWay()) {
                    byte[] bytes = new byte[64 * 1024];
                    DatagramPacket p = new DatagramPacket(bytes, bytes.length);
                    Object to = message.getContextualProperty(MULTI_RESPONSE_TIMEOUT);
                    Integer i = null;
                    if (to instanceof String) {
                        i = Integer.parseInt((String)to);
                    } else if (to instanceof Integer) {
                        i = (Integer)to;
                    }
                    if (i == null || i <= 0 || message.getExchange().isSynchronous()) {
                        socket.setSoTimeout(30000);
                        socket.receive(p);
                        dataReceived(message, IoBuffer.wrap(bytes, 0, p.getLength()), false, false);
                    } else {
                        socket.setSoTimeout(i);
                        boolean found = false;
                        try {
                            while (true) {
                                socket.receive(p);
                                dataReceived(message, IoBuffer.wrap(bytes, 0, p.getLength()), false, true);
                                found = true;
                            }
                        } catch (java.net.SocketTimeoutException ex) {
                            if (!found) {
                                throw ex;
                            }
                        }
                    }
                }
            }
        }

        public void flush() throws IOException {
        }
    }

    static class UDPConduitOutputStream extends OutputStream {
        final ConnectFuture future;
        IoBuffer buffer = IoBuffer.allocate(64 * 1024 - 42); //max size
        boolean closed;

        UDPConduitOutputStream(ConnectFuture connFuture) {
            this.future = connFuture;
        }

        public void write(int b) throws IOException {
            buffer.put(new byte[] {(byte)b}, 0, 1);
        }
        public void write(byte[] b, int off, int len) throws IOException {
            while (len > buffer.remaining()) {
                int nlen = buffer.remaining();
                buffer.put(b, off, nlen);
                len -= nlen;
                off += nlen;
                send();
                buffer = IoBuffer.allocate((64 * 1024) - 42);
            }
            buffer.put(b, off, len);
        }
        private void send() throws IOException {
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
        }
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            send();
        }
    }

    protected Logger getLogger() {
        return LOG;
    }

}
