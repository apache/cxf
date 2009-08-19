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

package org.apache.cxf.binding.soap.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public final class SoapTcpDestination extends AbstractDestination implements IoHandler {
    private static final String MAGIC_IDENTIFIER = "vnd.sun.ws.tcp";
    private static final Logger LOG = LogUtils.getL7dLogger(SoapTcpDestination.class);
    
    public SoapTcpDestination(EndpointReferenceType ref, EndpointInfo ei) throws IOException {
        this(null, ref, ei);
    }
    
    public SoapTcpDestination(Bus b, EndpointReferenceType ref, EndpointInfo ei) throws IOException {
        super(b, ref, ei);
        
        String address = ref.getAddress().getValue();
        if (address.contains("soap.tcp://")) {
            //String endPointAddress = address;
            int beginIndex = address.indexOf("://");
            int endIndex = address.indexOf(":", beginIndex + 1);
            //String hostName = address.substring(beginIndex + 3, endIndex);
            beginIndex = endIndex;
            endIndex = address.indexOf("/", beginIndex);
            int port = Integer.parseInt(address.substring(beginIndex + 1, endIndex));
            //System.out.println("hostName: " + hostName);
            //System.out.println("port: " + port);
            
            IoAcceptor acceptor = new NioSocketAcceptor();
            acceptor.getFilterChain().addLast("logger", new LoggingFilter());
            //acceptor.getFilterChain().addLast("LowLevelProtocol", new SoapTcpIoFilter());
            acceptor.getFilterChain().addLast("HighLevelProtocol",
                                              new ProtocolCodecFilter(new SoapTcpCodecFactory()));
            acceptor.setDefaultLocalAddress(new InetSocketAddress(port));
            acceptor.setHandler(this);
            acceptor.bind();
            System.out.println("server is listenig at port " + port);
        }
    }
    
    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        return inMessage.getExchange().getConduit(inMessage);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        // TODO Auto-generated method stub
        
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        if (message instanceof SoapTcpMessage) {
            BackendTcpConduit conduit = (BackendTcpConduit)session.getAttribute("conduit");
            if (conduit == null) {
                conduit = new BackendTcpConduit(session);
                session.setAttribute("conduit", conduit);
            }
            
            if (((SoapTcpMessage)message).getChannelId() == 0) {
                ChannelService.service(session, (SoapTcpMessage)message);
            } else {
                Message msg = new MessageImpl();
                Exchange exchange = new ExchangeImpl();
                exchange.setConduit(conduit);
                exchange.setDestination(this);
                msg.setExchange(exchange);
                msg.setContent(InputStream.class, ((SoapTcpMessage)message).getContentAsStream());
                msg.setContent(SoapTcpChannel.class, getChannel(session, (SoapTcpMessage)message));
                msg.setContent(IoSession.class, session);
                incomingObserver.onMessage(msg);
            }
        } else if (message instanceof IoBuffer) {
            SoapTcpSessionState sessionState = (SoapTcpSessionState)session.getAttribute("sessionState");
            if (sessionState != null
                && sessionState.getStateId() == SoapTcpSessionState.SOAP_TCP_SESSION_STATE_NEW) {
                IoBuffer buffer = (IoBuffer) message;
                InputStream inStream = buffer.asInputStream();
                byte magicIdBuffer[] = new byte[MAGIC_IDENTIFIER.length()];
                inStream.read(magicIdBuffer);
                String magicId = new String(magicIdBuffer, "US-ASCII");
                if (magicId.equals(MAGIC_IDENTIFIER)) {
                    int version[] = new int[4];
                    DataCodingUtils.readInts4(inStream, version, 4);
                    if (version[0] == SoapTcpProtocolConsts.PROTOCOL_VERSION_MAJOR
                        && version[1] == SoapTcpProtocolConsts.PROTOCOL_VERSION_MINOR
                        && version[2] == SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MAJOR
                        && version[3] == SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MINOR) {
                        sessionState.setStateId(SoapTcpSessionState.SOAP_TCP_SESSION_STATE_AFTER_HANDSHAKE);
                        IoBuffer response = IoBuffer.allocate(2);
                        OutputStream out = response.asOutputStream();
                        DataCodingUtils.writeInts4(out, SoapTcpProtocolConsts.PROTOCOL_VERSION_MAJOR,
                                                   SoapTcpProtocolConsts.PROTOCOL_VERSION_MINOR,
                                                   SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MAJOR,
                                                   SoapTcpProtocolConsts.CONNECTION_MANAGEMENT_VERSION_MINOR);
                        out.close();
                        response.flip();
                        session.write(response);
                    }
                }
            }
        }
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        System.out.println("messageSent");
        
    }

    public void sessionClosed(IoSession session) throws Exception {
        System.out.println("sessionClosed");
        
    }

    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("sessionCreated");
        
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        System.out.println("sessionIdle");
        
    }

    public void sessionOpened(IoSession session) throws Exception {
        System.out.println("sessionOpened");
        session.setAttribute("sessionState", new SoapTcpSessionState());
        List<SoapTcpChannel> channels = new ArrayList<SoapTcpChannel>();
        SoapTcpChannel channel0 = new SoapTcpChannel(0, "");
        channels.add(channel0);
        session.setAttribute("channels", channels);
        
    }

    @SuppressWarnings("unchecked")
    private SoapTcpChannel getChannel(IoSession session, SoapTcpMessage message) {
        List<SoapTcpChannel> channels = (List<SoapTcpChannel>)session.getAttribute("channels");
        if (channels != null) {
            for (SoapTcpChannel channel : channels) {
                if (channel.getChannelId() == message.getChannelId()) {
                    return channel;
                }
            }
        }
        return null;
    }
}
