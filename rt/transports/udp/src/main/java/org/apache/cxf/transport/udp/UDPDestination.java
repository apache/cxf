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
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
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
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.stream.StreamIoHandler;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

/**
 * 
 */
public class UDPDestination extends AbstractDestination {
    private static final Logger LOG = LogUtils.getL7dLogger(UDPDestination.class); 
    
    NioDatagramAcceptor acceptor;
    AutomaticWorkQueue queue;
    
    public UDPDestination(Bus b, EndpointReferenceType ref, EndpointInfo ei) {
        super(b, ref, ei);
    }

    /** {@inheritDoc}*/
    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        final UDPConnectionInfo info = inMessage.get(UDPConnectionInfo.class);
        return new AbstractBackChannelConduit() {
            public void prepare(Message message) throws IOException {
                message.setContent(OutputStream.class, info.out);
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
        
        
        acceptor = new NioDatagramAcceptor();
        acceptor.setHandler(new UDPIOHandler());
        try {
            URI uri = new URI(this.getAddress().getAddress().getValue());
            InetSocketAddress isa = null;
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
            acceptor.setDefaultLocalAddress(isa);

            DatagramSessionConfig dcfg = acceptor.getSessionConfig();
            dcfg.setReadBufferSize(64 * 1024);
            dcfg.setSendBufferSize(64 * 1024);
            dcfg.setReuseAddress(true);
            acceptor.bind();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
    protected void deactivate() {
        acceptor.unbind();
        acceptor.dispose();
        acceptor = null;
    }
    
    static class UDPConnectionInfo {
        final IoSession session;
        final OutputStream out;
        final InputStream in;
        
        public UDPConnectionInfo(IoSession io, OutputStream o, InputStream i) {
            session = io;
            out = o;
            in = i;
        }
    }
    
    
    class UDPIOHandler extends StreamIoHandler implements IoHandler {

        protected void processStreamIo(IoSession session, InputStream in, OutputStream out) {
            final MessageImpl m = new MessageImpl();
            final Exchange exchange = new ExchangeImpl();
            exchange.setDestination(UDPDestination.this);
            exchange.setInMessage(m);
            m.setContent(InputStream.class, in);
            out = new UDPDestinationOutputStream(out);
            m.put(UDPConnectionInfo.class, new UDPConnectionInfo(session, out, in));
            queue.execute(new Runnable() {
                public void run() {
                    getMessageObserver().onMessage(m);
                }
            });
        }
        
    }
    
    public class UDPDestinationOutputStream extends OutputStream {
        final OutputStream out;
        IoBuffer buffer = IoBuffer.allocate(64 * 1024 - 42); //max size
        boolean closed;
        
        public UDPDestinationOutputStream(OutputStream out) {
            this.out = out;
        }

        public void write(int b) throws IOException {
            buffer.put(new byte[] {(byte)b}, 0, 1);
        }
        public void write(byte b[], int off, int len) throws IOException {
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
