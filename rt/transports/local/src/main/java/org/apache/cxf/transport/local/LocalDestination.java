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
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class LocalDestination extends AbstractDestination {

    private static final Logger LOG = LogUtils.getL7dLogger(LocalDestination.class);

    private LocalTransportFactory localDestinationFactory;

    public LocalDestination(LocalTransportFactory localDestinationFactory,
                            EndpointReferenceType epr,
                            EndpointInfo ei,
                            Bus bus) {
        super(bus, epr, ei);
        this.localDestinationFactory = localDestinationFactory;
    }

    public void shutdown() {
        localDestinationFactory.remove(this);
    }
    public Bus getBus() {
        return bus;
    }

    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        Conduit conduit = (Conduit)inMessage.get(LocalConduit.IN_CONDUIT);
        if (conduit instanceof LocalConduit) {
            return new SynchronousConduit((LocalConduit)conduit);
        }
        return null;
    }

    class SynchronousConduit extends AbstractConduit {
        private final class LocalDestinationOutputStream extends AbstractWrappedOutputStream {
            private final Exchange exchange;
            private final Message message;

            private LocalDestinationOutputStream(Exchange exchange, Message message) {
                this.exchange = exchange;
                this.message = message;
            }

            public void close() throws IOException {
                if (!written) {
                    dispatchToClient(true);
                }
                super.close();
            }

            protected void onFirstWrite() throws IOException {
                dispatchToClient(false);
            }

            protected void dispatchToClient(boolean empty) throws IOException {
                final MessageImpl m = new MessageImpl();
                localDestinationFactory.copy(message, m);
                if (!empty) {
                    final PipedInputStream stream = new PipedInputStream();
                    wrappedStream = new PipedOutputStream(stream);
                    m.setContent(InputStream.class, stream);
                }

                final Runnable receiver = new Runnable() {
                    public void run() {
                        if (exchange != null) {
                            exchange.setInMessage(m);
                        }
                        conduit.getMessageObserver().onMessage(m);
                    }
                };
                Executor ex = message.getExchange() != null
                    ? message.getExchange().get(Executor.class) : null;
                // Need to avoid to get the SynchronousExecutor
                if (ex == null || SynchronousExecutor.isA(ex)) {
                    if (exchange == null) {
                        ex = localDestinationFactory.getExecutor(bus);
                    } else {
                        ex = localDestinationFactory.getExecutor(exchange.getBus());
                    }
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

        private LocalConduit conduit;

        SynchronousConduit(LocalConduit conduit) {
            super(null);
            this.conduit = conduit;
        }

        public void prepare(final Message message) throws IOException {
            if (!Boolean.TRUE.equals(message.getExchange().get(LocalConduit.DIRECT_DISPATCH))) {
                final Exchange exchange = (Exchange)message.getExchange().get(LocalConduit.IN_EXCHANGE);

                AbstractWrappedOutputStream cout
                    = new LocalDestinationOutputStream(exchange, message);

                message.setContent(OutputStream.class, cout);

            } else {
                CachedOutputStream stream = new CachedOutputStream();
                message.setContent(OutputStream.class, stream);
                message.setContent(CachedOutputStream.class, stream);
                stream.holdTempFile();
            }
        }

        @Override
        public void close(Message message) throws IOException {
            // set the pseudo status code if not set (REVISIT add this method in MessageUtils to be reused elsewhere?)
            Integer i = (Integer)message.get(Message.RESPONSE_CODE);
            if (i == null) {
                int code = ((message.getExchange().isOneWay() && !MessageUtils.isPartialResponse(message))
                    || MessageUtils.isEmptyPartialResponse(message)) ? 202 : 200;
                message.put(Message.RESPONSE_CODE, code);
            }
            if (Boolean.TRUE.equals(message.getExchange().get(LocalConduit.DIRECT_DISPATCH))) {
                final Exchange exchange = (Exchange)message.getExchange().get(LocalConduit.IN_EXCHANGE);

                MessageImpl copy = new MessageImpl();
                copy.putAll(message);
                message.getContent(OutputStream.class).close();
                CachedOutputStream stream = message.getContent(CachedOutputStream.class);
                message.setContent(OutputStream.class, stream);
                MessageImpl.copyContent(message, copy);
                copy.setContent(InputStream.class, stream.getInputStream());
                stream.releaseTempFileHold();
                if (exchange != null && exchange.getInMessage() == null) {
                    exchange.setInMessage(copy);
                }
                conduit.getMessageObserver().onMessage(copy);
                return;
            }

            super.close(message);
        }

        protected Logger getLogger() {
            return LOG;
        }
    }

}
