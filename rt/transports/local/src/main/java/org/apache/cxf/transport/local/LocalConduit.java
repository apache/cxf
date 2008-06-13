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
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.AbstractConduit;

public class LocalConduit extends AbstractConduit {

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
        if (!Boolean.TRUE.equals(message.get(DIRECT_DISPATCH))) {
            dispatchViaPipe(message);
        } else {
            // prepare the stream here
            CachedOutputStream stream = new CachedOutputStream();
            message.setContent(OutputStream.class, stream);
        }
    }

    @Override
    public void close(Message message) throws IOException {
        if (Boolean.TRUE.equals(message.get(DIRECT_DISPATCH))
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
        
        CachedOutputStream stream = (CachedOutputStream)message.getContent(OutputStream.class);
        copy.setContent(InputStream.class, stream.getInputStream());

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
            = new AbstractWrappedOutputStream() {
                protected void onFirstWrite() throws IOException {
                    final PipedInputStream stream = new PipedInputStream();
                    wrappedStream = new PipedOutputStream(stream);

                    final MessageImpl inMsg = new MessageImpl();
                    transportFactory.copy(message, inMsg); 

                    inMsg.setContent(InputStream.class, stream);
                    inMsg.setDestination(destination);
                    inMsg.put(IN_CONDUIT, conduit);

                    final Runnable receiver = new Runnable() {
                        public void run() {                            
                            ExchangeImpl ex = new ExchangeImpl();
                            ex.setInMessage(inMsg);
                            ex.put(IN_EXCHANGE, exchange);
                            destination.getMessageObserver().onMessage(inMsg);
                        }
                    };
                    
                    new Thread(receiver).start();
                }
            };
        message.setContent(OutputStream.class, cout);
    }
    
    protected Logger getLogger() {
        return LOG;
    }
}
