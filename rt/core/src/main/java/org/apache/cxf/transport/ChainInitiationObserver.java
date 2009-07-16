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


import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;

public class ChainInitiationObserver implements MessageObserver {
    protected Endpoint endpoint;
    protected Bus bus;
    
    private PhaseChainCache chainCache = new PhaseChainCache();

    public ChainInitiationObserver(Endpoint endpoint, Bus bus) {
        super();
        this.endpoint = endpoint;
        this.bus = bus;
    }

    public void onMessage(Message m) {
        Bus origBus = BusFactory.getThreadDefaultBus(false);
        BusFactory.setThreadDefaultBus(bus);
        try {
            PhaseInterceptorChain phaseChain = null;
            
            if (m.getInterceptorChain() instanceof PhaseInterceptorChain) {
                phaseChain = (PhaseInterceptorChain)m.getInterceptorChain();
                if (phaseChain.getState() == InterceptorChain.State.PAUSED) {
                    phaseChain.resume();
                    return;
                }
            }
            
            Message message = getBinding().createMessage(m);
            Exchange exchange = message.getExchange();
            if (exchange == null) {
                exchange = new ExchangeImpl();
            }
            exchange.setInMessage(message);
            setExchangeProperties(exchange, message);
    
            // setup chain
            phaseChain = chainCache.get(bus.getExtension(PhaseManager.class).getInPhases(),
                                                         bus.getInInterceptors(),
                                                         endpoint.getService().getInInterceptors(),
                                                         endpoint.getInInterceptors(),
                                                         getBinding().getInInterceptors());
            
            
            message.setInterceptorChain(phaseChain);
            
            phaseChain.setFaultObserver(endpoint.getOutFaultObserver());
           
            phaseChain.doIntercept(message);
        } finally {
            BusFactory.setThreadDefaultBus(origBus);
        }
    }


    protected Binding getBinding() {
        return endpoint.getBinding();
    }
    
    protected void setExchangeProperties(Exchange exchange, Message m) {
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Service.class, endpoint.getService());
        exchange.put(Binding.class, getBinding());
        exchange.put(Bus.class, bus);
        if (exchange.getDestination() == null) {
            exchange.setDestination(m.getDestination());
        }
        if (endpoint != null && (endpoint.getService() instanceof ServiceImpl)) {

            EndpointInfo endpointInfo = endpoint.getEndpointInfo();

            QName serviceQName = endpointInfo.getService().getName();
            exchange.put(Message.WSDL_SERVICE, serviceQName);

            QName interfaceQName = endpointInfo.getService().getInterface().getName();
            exchange.put(Message.WSDL_INTERFACE, interfaceQName);

            QName portQName = endpointInfo.getName();
            exchange.put(Message.WSDL_PORT, portQName);
            URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
            if (wsdlDescription == null) {
                String address = endpointInfo.getAddress();
                try {
                    wsdlDescription = new URI(address + "?wsdl");
                } catch (URISyntaxException e) {
                    // do nothing
                }
                endpointInfo.setProperty("URI", wsdlDescription);
            }
            exchange.put(Message.WSDL_DESCRIPTION, wsdlDescription);
        }  
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
    
}
