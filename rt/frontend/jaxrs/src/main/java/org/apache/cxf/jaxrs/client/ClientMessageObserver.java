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

package org.apache.cxf.jaxrs.client;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.MessageObserver;

class ClientMessageObserver implements MessageObserver {

    private ClientConfiguration cfg; 
    
    public ClientMessageObserver(ClientConfiguration cfg) {
        this.cfg = cfg;
    }
    
    public void onMessage(Message m) {
        
        Message message = cfg.getConduitSelector().getEndpoint().getBinding().createMessage(m);
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        message.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
        PhaseInterceptorChain chain = AbstractClient.setupInInterceptorChain(cfg);
        message.setInterceptorChain(chain);
        message.getExchange().setInMessage(message);
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(cfg.getBus());

        // execute chain
        try {
            chain.doIntercept(message);
        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }
    
}
