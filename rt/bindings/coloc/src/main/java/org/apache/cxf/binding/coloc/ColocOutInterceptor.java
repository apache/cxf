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
package org.apache.cxf.binding.coloc;

import java.util.List;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
//import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
//import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.MessageObserver;

public class ColocOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ColocOutInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(ClientImpl.class);
    private static final String COLOCATED = Message.class.getName() + ".COLOCATED";
    private MessageObserver colocObserver;
    private Bus bus; 
    
    public ColocOutInterceptor() {
        super(Phase.POST_LOGICAL);
    }
    public ColocOutInterceptor(Bus b) {
        super(Phase.POST_LOGICAL);
        bus = b;
    }

    public void setBus(Bus bus) {
        this.bus = bus; 
    }
    
    public void handleMessage(Message message) throws Fault {
        if (bus == null) {
            bus = message.getExchange().get(Bus.class);
            if (bus == null) {
                bus = BusFactory.getDefaultBus(false);
            }
            if (bus == null) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("BUS_NOT_FOUND", BUNDLE));
            }
        }
        
        ServerRegistry registry = bus.getExtension(ServerRegistry.class);
        
        if (registry == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("SERVER_REGISTRY_NOT_FOUND", BUNDLE));
        }
        
        Exchange exchange = message.getExchange();
        Endpoint senderEndpoint = exchange.get(Endpoint.class);

        if (senderEndpoint == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("ENDPOINT_NOT_FOUND", 
                                                                   BUNDLE));
        }

        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        
        if (boi == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("OPERATIONINFO_NOT_FOUND", 
                                                                   BUNDLE));
        }

        Server srv = isColocated(registry.getServers(), senderEndpoint, boi);
        
        if (srv != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Operation:" + boi.getName() + " dispatched as colocated call.");
            }

            InterceptorChain outChain = message.getInterceptorChain();
            outChain.abort();
            exchange.put(Bus.class, bus);
            message.put(COLOCATED, Boolean.TRUE);
            message.put(Message.WSDL_OPERATION, boi.getName());
            message.put(Message.WSDL_INTERFACE, boi.getBinding().getInterface().getName());
            invokeColocObserver(message, srv.getEndpoint());
            if (!exchange.isOneWay()) {
                invokeInboundChain(exchange, senderEndpoint);
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Operation:" + boi.getName() + " dispatched as remote call.");
            }
            
            message.put(COLOCATED, Boolean.FALSE);
        }
    }
    
    protected void invokeColocObserver(Message outMsg, Endpoint inboundEndpoint) {
        if (colocObserver == null) {
            colocObserver = new ColocMessageObserver(inboundEndpoint, bus);
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Invoke on Coloc Observer.");
        }

        colocObserver.onMessage(outMsg);
    }

    protected void invokeInboundChain(Exchange ex, Endpoint ep) {
        Message m = getInBoundMessage(ex);
        Message inMsg = ep.getBinding().createMessage();
        MessageImpl.copyContent(m, inMsg);
        
        //Copy Response Context to Client inBound Message
        //TODO a Context Filter Strategy required. 
        inMsg.putAll(m);
        
        inMsg.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        inMsg.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
        inMsg.setExchange(ex);
        
        Exception exc = inMsg.getContent(Exception.class);
        if (exc != null) {
            ex.setInFaultMessage(inMsg);
            ColocInFaultObserver observer = new ColocInFaultObserver(bus);
            observer.onMessage(inMsg);            
        } else {
            //Handle Response
            ex.setInMessage(inMsg);
            PhaseManager pm = bus.getExtension(PhaseManager.class);
            SortedSet<Phase> phases = new TreeSet<Phase>(pm.getInPhases());
            ColocUtil.setPhases(phases, Phase.USER_LOGICAL, Phase.PRE_INVOKE);
            
            InterceptorChain chain = ColocUtil.getInInterceptorChain(ex, phases);        
            inMsg.setInterceptorChain(chain);        
            chain.doIntercept(inMsg);
        }
        ex.put(ClientImpl.FINISHED, Boolean.TRUE);
    }
    
    protected Message getInBoundMessage(Exchange ex) {
        return  (ex.getInFaultMessage() != null)
                   ? ex.getInFaultMessage()
                   : ex.getInMessage();
    }
    
    protected void setMessageObserver(MessageObserver observer) {
        colocObserver = observer;
    }

    protected Server isColocated(List<Server> servers, Endpoint endpoint, BindingOperationInfo boi) {
        if (servers != null) {
            Service senderService = endpoint.getService();
            EndpointInfo senderEI = endpoint.getEndpointInfo();
            for (Server s : servers) {
                Endpoint receiverEndpoint = s.getEndpoint();
                Service receiverService = receiverEndpoint.getService();
                EndpointInfo receiverEI = receiverEndpoint.getEndpointInfo();
                if (receiverService.getName().equals(senderService.getName())
                    && receiverEI.getName().equals(senderEI.getName())) {
                    //Check For Operation Match.
                    BindingOperationInfo receiverOI = receiverEI.getBinding().getOperation(boi.getName());
                    if (receiverOI != null 
                        && isSameOperationInfo(boi, receiverOI)) {
                        return s;
                    }
                }
            }
        }
        
        return null;
    }
    
    protected boolean isSameOperationInfo(BindingOperationInfo sender,
                                          BindingOperationInfo receiver) {
        return ColocUtil.isSameOperationInfo(sender.getOperationInfo(), 
                                             receiver.getOperationInfo());
    }
    
    public void setExchangeProperties(Exchange exchange, Endpoint ep) {
        exchange.put(Endpoint.class, ep);
        exchange.put(Service.class, ep.getService());
        exchange.put(Binding.class, ep.getBinding());
        exchange.put(Bus.class, bus == null ? BusFactory.getDefaultBus(false) : bus);
    }
}
