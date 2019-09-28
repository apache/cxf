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

package org.apache.cxf.transport.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.workqueue.SynchronousExecutor;

public class LocalConduit extends AbstractConduit {

    private final class LocalConduitOutputStream extends AbstractWrappedOutputStream {
        private final LocalConduit conduit;
        private final Exchange exchange;
        private final Message message;

        private LocalConduitOutputStream(LocalConduit conduit, Exchange exchange, Message message) {
            this.conduit = conduit;
            this.exchange = exchange;
            this.message = message;
        }

        public void close() throws IOException {
            if (!written) {
                dispatchToService(true);
            }
            super.close();
        }

        protected void onFirstWrite() throws IOException {
            dispatchToService(false);
        }
        protected void dispatchToService(boolean empty) throws IOException {
            final MessageImpl inMsg = new MessageImpl();
            transportFactory.copy(message, inMsg);

            if (!empty) {
                final PipedInputStream stream = new PipedInputStream();
                wrappedStream = new PipedOutputStream(stream);

                inMsg.setContent(InputStream.class, stream);
            }
            inMsg.setDestination(destination);
            inMsg.put(IN_CONDUIT, conduit);

            final Runnable receiver = new Runnable() {
                public void run() {
                    ExchangeImpl ex = new ExchangeImpl();
                    ex.put(Bus.class, destination.getBus());
                    ex.setInMessage(inMsg);
                    inMsg.setExchange(ex);
                    ex.put(IN_EXCHANGE, exchange);
                    try {
                        destination.getMessageObserver().onMessage(inMsg);
                    } catch (Throwable t) {
                        Message m = inMsg.getExchange().getOutFaultMessage();
                        if (m == null) {
                            m = inMsg.getExchange().getOutMessage();
                        }
                        if (m != null) {
                            try {
                                m.put(Message.RESPONSE_CODE, 500);
                                m.put(Message.PROTOCOL_HEADERS, new HashMap<String, List<String>>());
                                m.getExchange().put(Message.RESPONSE_CODE, 500);
                                m.getContent(OutputStream.class).close();
                            } catch (IOException e) {
                                //ignore
                            }
                        }
                    }
                }
            };
            Executor ex = message.getExchange() != null
                ? message.getExchange().get(Executor.class) : null;
            if (ex == null || SynchronousExecutor.isA(ex)) {
                ex = transportFactory.getExecutor(destination.getBus());
                if (ex != null) {
                    ex.execute(receiver);
                } else {
                    new Thread(receiver).start();
                }
            } else {
                ex.execute(receiver);
            }
        }
    }

    public static final String IN_CONDUIT = LocalConduit.class.getName() + ".inConduit";
    public static final String RESPONSE_CONDUIT = LocalConduit.class.getName() + ".inConduit";
    public static final String IN_EXCHANGE = LocalConduit.class.getName() + ".inExchange";
    public static final String DIRECT_DISPATCH = LocalConduit.class.getName() + ".directDispatch";
    public static final String MESSAGE_FILTER_PROPERTIES = LocalTransportFactory.MESSAGE_FILTER_PROPERTIES;

    private static final Logger LOG = LogUtils.getL7dLogger(LocalConduit.class);

    private LocalDestination destination;
    private LocalTransportFactory transportFactory;

    public LocalConduit(LocalTransportFactory transportFactory, LocalDestination destination) {
        super(destination.getAddress());
        this.destination = destination;
        this.transportFactory = transportFactory;
    }

    public void prepare(final Message message) throws IOException {
        if (!MessageUtils.getContextualBoolean(message, DIRECT_DISPATCH)) {
            dispatchViaPipe(message);
        } else {
            // prepare the stream here
            CachedOutputStream stream = new CachedOutputStream();
            message.setContent(OutputStream.class, stream);
            //save the original stream
            message.put(CachedOutputStream.class, stream);
            stream.holdTempFile();
        }
    }

    @Override
    public void close(Message message) throws IOException {
        if (MessageUtils.getContextualBoolean(message, DIRECT_DISPATCH)
            && !Boolean.TRUE.equals(message.get(Message.INBOUND_MESSAGE))) {
            dispatchDirect(message);
        }

        super.close(message);
    }

    private void dispatchDirect(Message message) throws IOException {
        if (destination.getMessageObserver() == null) {
            throw new IllegalStateException("Local destination does not have a MessageObserver on address "
                                            + destination.getAddress().getAddress().getValue());
        }

        MessageImpl copy = new MessageImpl();
        copy.put(IN_CONDUIT, this);
        copy.setDestination(destination);

        transportFactory.copy(message, copy);
        MessageImpl.copyContent(message, copy);

        OutputStream out = message.getContent(OutputStream.class);
        out.flush();
        out.close();

        CachedOutputStream stream = message.get(CachedOutputStream.class);
        copy.setContent(InputStream.class, stream.getInputStream());
        copy.removeContent(CachedOutputStream.class);
        stream.releaseTempFileHold();

        // Create a new incoming exchange and store the original exchange for the response
        ExchangeImpl ex = new ExchangeImpl();
        ex.setInMessage(copy);
        ex.put(IN_EXCHANGE, message.getExchange());
        ex.put(LocalConduit.DIRECT_DISPATCH, true);
        ex.setDestination(destination);

        destination.getMessageObserver().onMessage(copy);
    }


    private void dispatchViaPipe(final Message message) throws IOException {
        final LocalConduit conduit = this;
        final Exchange exchange = message.getExchange();

        if (destination.getMessageObserver() == null) {
            throw new IllegalStateException("Local destination does not have a MessageObserver on address "
                                            + destination.getAddress().getAddress().getValue());
        }

        AbstractWrappedOutputStream cout
            = new LocalConduitOutputStream(conduit, exchange, message);
        message.setContent(OutputStream.class, cout);
    }

    protected Logger getLogger() {
        return LOG;
    }
}
