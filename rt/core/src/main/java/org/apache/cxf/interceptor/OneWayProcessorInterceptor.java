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

package org.apache.cxf.interceptor;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.WorkQueueManager;


/**
 * 
 */
public class OneWayProcessorInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String USE_ORIGINAL_THREAD 
        = OneWayProcessorInterceptor.class.getName() + ".USE_ORIGINAL_THREAD"; 
    
    public OneWayProcessorInterceptor() {
        super(Phase.PRE_LOGICAL);
    }
    public OneWayProcessorInterceptor(String phase) {
        super(phase);
    }
    
    public void handleMessage(Message message) throws Fault {
        
        if (message.getExchange().isOneWay() 
            && !MessageUtils.isRequestor(message)
            && message.get(OneWayProcessorInterceptor.class) == null
            && message.getExchange().get(Executor.class) == null) { 
            //one way on server side, fork the rest of this chain onto the
            //workqueue, call the Outgoing chain directly.
            
            message.put(OneWayProcessorInterceptor.class, this);
            final InterceptorChain chain = message.getInterceptorChain();

            Object o = message.getContextualProperty(USE_ORIGINAL_THREAD);
            if (o == null) {
                o = Boolean.FALSE;
            } else if (o instanceof String) {
                o = Boolean.valueOf((String)o);
            }

            
            if (Boolean.FALSE.equals(o)) {
                //need to suck in all the data from the input stream as
                //the transport might discard any data on the stream when this 
                //thread unwinds or when the empty response is sent back
                DelegatingInputStream in = message.get(DelegatingInputStream.class);
                if (in != null) {
                    in.cacheInput();
                }
            }

            
            try {
                Message partial = createMessage(message.getExchange());
                partial.setExchange(message.getExchange());
                Conduit conduit = message.getExchange().getDestination()
                    .getBackChannel(message, null, null);
                conduit.prepare(partial);
                conduit.close(partial);
            } catch (IOException e) {
                //IGNORE
            }
            
            if (Boolean.FALSE.equals(o)) {
                chain.pause();
                try {
                    message.getExchange().get(Bus.class).getExtension(WorkQueueManager.class)
                    .getAutomaticWorkQueue().execute(new Runnable() {
                        public void run() {
                            chain.resume();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    //the executor queue is full, so run the task in the caller thread
                    chain.resume();
                }
            }
        }
    }
    
    private static Message createMessage(Exchange exchange) {
        Endpoint ep = exchange.get(Endpoint.class);
        Message msg = null;
        if (ep != null) {
            msg = new MessageImpl();
            msg.setExchange(exchange);
            msg = ep.getBinding().createMessage(msg);
        }
        return msg;
    }


}
