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
package org.apache.cxf.transport;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

/**
 * This MessageObserver creates an Interceptor chain which adds in the interceptors
 * set on this class and the global Bus interceptors. At somepoint, it is expected
 * that these interceptors will resolve the appropriate Endpoint/Binding combination
 * and continue setting up the chain.
 *
 */
public class MultipleEndpointObserver implements MessageObserver {
    
    public static final String ENDPOINTS = "multipleEndpointObserver.endpoints";
    
    protected Bus bus;
    protected List<Interceptor> bindingInterceptors = new CopyOnWriteArrayList<Interceptor>();
    protected List<Interceptor> routingInterceptors = new CopyOnWriteArrayList<Interceptor>();
    private Set<Endpoint> endpoints = new CopyOnWriteArraySet<Endpoint>();
    
    public MultipleEndpointObserver(Bus bus) {
        super();
        this.bus = bus;
    }

    public void onMessage(Message message) {
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            message = createMessage(message);
            Exchange exchange = message.getExchange();
            if (exchange == null) {
                exchange = new ExchangeImpl();
                exchange.setInMessage(message);
                message.setExchange(exchange);
            }
            setExchangeProperties(exchange, message);
            
            // setup chain
            PhaseInterceptorChain chain = createChain();
            
            message.setInterceptorChain(chain);
            
            chain.add(bus.getInInterceptors());
            if (bindingInterceptors != null) {
                chain.add(bindingInterceptors);
            }
            if (routingInterceptors != null) {
                chain.add(routingInterceptors);
            }
            
            if (endpoints != null) {
                exchange.put(ENDPOINTS, endpoints);
            }
            
            chain.doIntercept(message);
        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }

    /**
     * Give a chance for a Binding to customize their message
     */
    protected Message createMessage(Message message) {
        return message;
    }

    protected PhaseInterceptorChain createChain() {
        PhaseInterceptorChain chain = new PhaseInterceptorChain(bus.getExtension(PhaseManager.class)
            .getInPhases());
        return chain;
    }
    
    protected void setExchangeProperties(Exchange exchange, Message m) {
        exchange.put(Bus.class, bus);
        if (exchange.getDestination() == null) {
            exchange.setDestination(m.getDestination());
        }
    }

    public List<Interceptor> getBindingInterceptors() {
        return bindingInterceptors;
    }

    public List<Interceptor> getRoutingInterceptors() {
        return routingInterceptors;
    }

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

}
