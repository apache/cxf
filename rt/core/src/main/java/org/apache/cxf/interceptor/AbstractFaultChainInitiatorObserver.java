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

import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.transport.MessageObserver;

public abstract class AbstractFaultChainInitiatorObserver implements MessageObserver {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractFaultChainInitiatorObserver.class);
    
    private Bus bus;

    public AbstractFaultChainInitiatorObserver(Bus bus) {
        this.bus = bus;
    }

    public void onMessage(Message message) {
      
        assert null != message;
        
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            
            Exchange exchange = message.getExchange();
    
            Message faultMessage = null;
    
            // now that we have switched over to the fault chain,
            // prevent any further operations on the in/out message 
    
            if (isOutboundObserver()) {
                Exception ex = message.getContent(Exception.class);
                if (!(ex instanceof Fault)) {
                    ex = new Fault(ex);
                }
                FaultMode mode = (FaultMode)message.get(FaultMode.class);
                
                faultMessage = exchange.getOutMessage();
                if (null == faultMessage) {
                    faultMessage = new MessageImpl();
                    faultMessage.setExchange(exchange);
                    faultMessage = exchange.get(Endpoint.class).getBinding().createMessage(faultMessage);
                }
                faultMessage.setContent(Exception.class, ex);
                if (null != mode) {
                    faultMessage.put(FaultMode.class, mode);
                }
                exchange.setOutMessage(null);
                exchange.setOutFaultMessage(faultMessage);
                if (message.get(BindingFaultInfo.class) != null) {
                    faultMessage.put(BindingFaultInfo.class, message.get(BindingFaultInfo.class));
                }
            } else {
                faultMessage = message;
                exchange.setInMessage(null);
                exchange.setInFaultMessage(faultMessage);
            }          
             
           
            // setup chain
            PhaseInterceptorChain chain = new PhaseInterceptorChain(getPhases());
            initializeInterceptors(faultMessage.getExchange(), chain);
            
            faultMessage.setInterceptorChain(chain);
            try {
                chain.doIntercept(faultMessage);
            } catch (Exception exc) {
                LOG.log(Level.SEVERE, "Error occurred during error handling, give up!", exc);
                throw new RuntimeException(exc);
            }
        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    protected abstract boolean isOutboundObserver();

    protected abstract SortedSet<Phase> getPhases();

    protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {
        
    }

    public Bus getBus() {
        return bus;
    }

}
