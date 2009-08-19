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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.policy.Assertor;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public class BackendTcpConduit extends AbstractConduit implements Configurable, Assertor {
    private static final Logger LOG = LogUtils.getL7dLogger(TCPConduit.class);
    private IoSession session;
    
    public BackendTcpConduit(IoSession session) {
        super(null);
        this.session = session;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public String getBeanName() {
        // TODO Auto-generated method stub
        return null;
    }

    public void assertMessage(Message message) {
        // TODO Auto-generated method stub

    }

    public boolean canAssert(QName type) {
        // TODO Auto-generated method stub
        return false;
    }

    public void prepare(Message message) throws IOException {
        message.setContent(OutputStream.class, new ByteArrayOutputStream(512));
    }

    @Override
    public void close(Message msg) throws IOException {
        ByteArrayOutputStream baos = (ByteArrayOutputStream)msg.getContent(OutputStream.class);
        Exchange exchange = msg.getExchange();
        SoapTcpChannel channel = exchange.getInMessage().getContent(SoapTcpChannel.class);
        String message = new String(baos.toByteArray());
        SoapTcpMessage soapTcpMessage = SoapTcpMessage.createSoapTcpMessage(message, channel.getChannelId());
        IoBuffer buffer = IoBuffer.allocate(512);
        buffer.setAutoExpand(true);
        SoapTcpUtils.writeSoapTcpMessage(buffer.asOutputStream(), soapTcpMessage);
        buffer.flip();
        session.write(buffer);
    }
}
