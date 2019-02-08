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

import org.apache.cxf.Bus;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;

public class JMSContinuationProvider implements ContinuationProvider {

    private Bus bus;
    private Message inMessage;
    private MessageObserver incomingObserver;
    private Counter suspendendContinuations;

    public JMSContinuationProvider(Bus b,
                                   Message m,
                                   MessageObserver observer,
                                   Counter suspendendContinuations) {
        bus = b;
        inMessage = m;
        incomingObserver = observer;
        this.suspendendContinuations = suspendendContinuations;
    }

    public void complete() {
        JMSContinuation cw = inMessage.get(JMSContinuation.class);
        if (cw != null) {
            cw.reset();
        }
    }

    public Continuation getContinuation() {
        Message m = inMessage;
        // Get the real message which is used in the interceptor chain
        if (m != null && m.getExchange() != null && m.getExchange().getInMessage() != null) {
            m = m.getExchange().getInMessage();
        }
        if (m == null || m.getExchange() == null || m.getExchange().isOneWay()) {
            return null;
        }
        JMSContinuation cw = m.get(JMSContinuation.class);
        if (cw == null) {
            cw = new JMSContinuation(bus, m,  incomingObserver, suspendendContinuations);
            m.put(JMSContinuation.class, cw);
        }
        return cw;


    }

}
