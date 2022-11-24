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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.stream.StreamIoHandler;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

/**
 *
 */
public class UDPDestination extends AbstractDestination {
    public static final String NETWORK_INTERFACE = UDPDestination.class.getName() + ".NETWORK_INTERFACE";

    private static final Logger LOG = LogUtils.getL7dLogger(UDPDestination.class);
    private static final AttributeKey KEY_IN = new AttributeKey(StreamIoHandler.class, "in");
    private static final AttributeKey KEY_OUT = new AttributeKey(StreamIoHandler.class, "out");

    NioDatagramAcceptor acceptor;
    AutomaticWorkQueue queue;
    volatile MulticastSocket mcast;

    public UDPDestination(Bus b, EndpointReferenceType ref, EndpointInfo ei) {
        super(b, ref, ei);
    }

    class MCastListener implements Runnable {
        public void run() {
            while (true) {
                if (mcast == null) {
                    return;
                }
                try {
                    byte[] bytes = new byte[64 * 1024];
                    final DatagramPacket p = new DatagramPacket(bytes, bytes.length);
                    mcast.receive(p);

                    LoadingByteArrayOutputStream out = new LoadingByteArrayOutputStream() {
                        public void close() throws IOException {
                            super.close();
                            final DatagramPacket p2 = new DatagramPacket(getRawBytes(),
                                                                         0,
                                                                         this.size(),
                                                                         p.getSocketAddress());
                            mcast.send(p2);
                        }
                    };

                    final MessageImpl m = new MessageImpl();
                    final Exchange exchange = new ExchangeImpl();
                    exchange.setDestination(UDPDestination.this);
                    m.setDestination(UDPDestination.this);
                    exchange.setInMessage(m);
                    m.setContent(InputStream.class, new ByteArrayInputStream(bytes, 0, p.getLength()));
                    m.put(OutputStream.class, out);
                    queue.execute(() -> getMessageObserver().onMessage(m));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    /** {@inheritDoc}*/
    @Override
    protected Conduit getInbuiltBackChannel(final Message inMessage) {
        if (inMessage.getExchange().isOneWay()) {
            return null;
        }
        return new AbstractBackChannelConduit() {
            public void prepare(Message message) throws IOException {
                message.setContent(OutputStream.class, inMessage.get(OutputStream.class));
            }
        };
    }

    /** {@inheritDoc}*/
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    protected void activate() {
        WorkQueueManager queuem = bus.getExtension(WorkQueueManager.class);
        queue = queuem.getNamedWorkQueue("udp-transport");
        if (queue == null) {
            queue = queuem.getAutomaticWorkQueue();
        }

        try {
            URI uri = new URI(this.getAddress().getAddress().getValue());
            final InetSocketAddress isa;
            if (StringUtils.isEmpty(uri.getHost())) {
                String s = uri.getSchemeSpecificPart();
                if (s.startsWith("//:")) {
                    s = s.substring(3);
                }
                if (s.indexOf('/') != -1) {
                    s = s.substring(0, s.indexOf('/'));
                }
                int port = Integer.parseInt(s);
                isa = new InetSocketAddress(port);
            } else {
                isa = new InetSocketAddress(uri.getHost(), uri.getPort());
            }
            if (isa.getAddress().isMulticastAddress()) {
                //ouch...
                MulticastSocket socket = new MulticastSocket(null);
                socket.setReuseAddress(true);
                socket.setReceiveBufferSize(64 * 1024);
                socket.setSendBufferSize(64 * 1024);
                socket.setTimeToLive(1);
                socket.setLoopbackMode(false);
                socket.bind(new InetSocketAddress(isa.getPort()));
                socket.setNetworkInterface(findNetworkInterface());
                socket.joinGroup(isa.getAddress());
                mcast = socket;
                queue.execute(new MCastListener());
            } else {

                acceptor = new NioDatagramAcceptor();
                acceptor.setHandler(new UDPIOHandler());

                acceptor.setDefaultLocalAddress(isa);
                DatagramSessionConfig dcfg = acceptor.getSessionConfig();
                dcfg.setReadBufferSize(64 * 1024);
                dcfg.setSendBufferSize(64 * 1024);
                dcfg.setReuseAddress(true);
                acceptor.bind();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private NetworkInterface findNetworkInterface() throws SocketException {
        String name = (String)this.getEndpointInfo().getProperty(UDPDestination.NETWORK_INTERFACE);
        NetworkInterface ret = null;
        if (!StringUtils.isEmpty(name)) {
            ret = NetworkInterface.getByName(name);
        }
        if (ret == null) {
            Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
            if (ifcs != null) {
                List<NetworkInterface> possibles = new ArrayList<>();
                while (ifcs.hasMoreElements()) {
                    NetworkInterface ni = ifcs.nextElement();
                    if (ni.supportsMulticast() && ni.isUp()) {
                        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                            // Ignore any virtual interfaces created by/for a VPN connection.
                            if (ia != null && ia.getAddress() instanceof java.net.Inet4Address
                                    && !ia.getAddress().isLoopbackAddress()
                                    && !ni.getDisplayName().startsWith("vnic")) {
                                possibles.add(ni);
                            }
                        }
                    }
                }
                ret = possibles.isEmpty() ? null : possibles.get(possibles.size() - 1);
            }
        }
        return ret;
    }

    protected void deactivate() {
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose();
        }
        acceptor = null;
        if (mcast != null) {
            mcast.close();
            mcast = null;
        }
    }

    class UDPIOHandler extends StreamIoHandler {

        @Override
        public void sessionOpened(IoSession session) {
            // Set timeouts
            session.getConfig().setWriteTimeout(getWriteTimeout());
            session.getConfig().setIdleTime(IdleStatus.READER_IDLE, getReadTimeout());

            // Create streams
            InputStream in = new IoSessionInputStream();
            OutputStream out = new IoSessionOutputStream(session) {
                @Override
                public void close() throws IOException {
                    try {
                        flush();
                    } finally {
                        CloseFuture future = session.closeNow();
                        future.awaitUninterruptibly();
                    }
                }  
            };
            session.setAttribute(KEY_IN, in);
            session.setAttribute(KEY_OUT, out);
            processStreamIo(session, in, out);
        }

        protected void processStreamIo(IoSession session, InputStream in, OutputStream out) {
            final MessageImpl m = new MessageImpl();
            final Exchange exchange = new ExchangeImpl();
            exchange.setDestination(UDPDestination.this);
            m.setDestination(UDPDestination.this);
            exchange.setInMessage(m);
            m.setContent(InputStream.class, in);
            out = new UDPDestinationOutputStream(out);
            m.put(OutputStream.class, out);
            queue.execute(() -> getMessageObserver().onMessage(m));
        }

        public void sessionClosed(IoSession session) throws Exception {
            final InputStream in = (InputStream) session.getAttribute(KEY_IN);
            final OutputStream out = (OutputStream) session.getAttribute(KEY_OUT);
            try {
                in.close();
            } finally {
                out.close();
            }
        }

        public void messageReceived(IoSession session, Object buf) {
            final IoSessionInputStream in = (IoSessionInputStream) session
                    .getAttribute(KEY_IN);
            in.setBuffer((IoBuffer) buf);
        }

        public void exceptionCaught(IoSession session, Throwable cause) {
            final IoSessionInputStream in = (IoSessionInputStream) session
                    .getAttribute(KEY_IN);

            IOException e = null;
            if (cause instanceof StreamIoException) {
                e = (IOException) cause.getCause();
            } else if (cause instanceof IOException) {
                e = (IOException) cause;
            }

            if (e != null && in != null) {
                in.throwException(e);
            } else {
                session.closeOnFlush().awaitUninterruptibly();
            }
        }
        public void sessionIdle(IoSession session, IdleStatus status) {
            if (status == IdleStatus.READER_IDLE) {
                throw new StreamIoException(new SocketTimeoutException(
                        "Read timeout"));
            }
        }
    }
    private static class StreamIoException extends RuntimeException {
        private static final long serialVersionUID = 3976736960742503222L;

        StreamIoException(IOException cause) {
            super(cause);
        }
    }

    static class UDPDestinationOutputStream extends OutputStream {
        final OutputStream out;
        IoBuffer buffer = IoBuffer.allocate(64 * 1024 - 42); //max size
        boolean closed;

        UDPDestinationOutputStream(OutputStream out) {
            this.out = out;
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
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
        }
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            send();
            out.close();
        }
    }

}
