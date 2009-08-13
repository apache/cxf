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

package org.apache.cxf.transport.jms.continuations;

import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

public class JMSContinuationProvider implements ContinuationProvider {

    private Bus bus;
    private Message inMessage;
    private MessageObserver incomingObserver;
    private Collection<JMSContinuation> continuations;
    private AbstractMessageListenerContainer jmsListener;
    private JMSConfiguration jmsConfig;
    
    public JMSContinuationProvider(Bus b,
                                   Message m, 
                                   MessageObserver observer,
                                   Collection<JMSContinuation> cList,
                                   AbstractMessageListenerContainer jmsListener,
                                   JMSConfiguration jmsConfig) {
        bus = b;
        inMessage = m;    
        incomingObserver = observer;
        continuations = cList;
        this.jmsListener = jmsListener;
        this.jmsConfig = jmsConfig;
    }
    
    public Continuation getContinuation() {
        if (inMessage.getExchange().isOneWay()) {
            return null;
        }
        JMSContinuation cw = inMessage.get(JMSContinuation.class);
        if (cw == null) {
            cw = new JMSContinuation(bus, inMessage,  incomingObserver, continuations, 
                                     jmsListener, jmsConfig);
            inMessage.put(JMSContinuation.class, cw);
        }
        return cw;
        
        
    }

}
