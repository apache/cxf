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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.ChainInitiationObserver;

public class ColocMessageObserver extends ChainInitiationObserver {
    private static final Logger LOG = LogUtils.getL7dLogger(ColocMessageObserver.class);
    private static final String COLOCATED = Message.class.getName() + ".COLOCATED";
    private ClassLoader loader;
    public ColocMessageObserver(Endpoint endpoint, Bus bus) {
        super(endpoint, bus);
        loader = bus.getExtension(ClassLoader.class);
    }

    public void onMessage(Message m) {
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        ClassLoaderHolder origLoader = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Processing Message at collocated endpoint.  Request message: " + m);
            }
            Exchange ex = new ExchangeImpl();
            setExchangeProperties(ex, m);
            
            Message inMsg = endpoint.getBinding().createMessage();
            MessageImpl.copyContent(m, inMsg);
            
            //Copy Request Context to Server inBound Message
            //TODO a Context Filter Strategy required. 
            inMsg.putAll(m);
    
            inMsg.put(COLOCATED, Boolean.TRUE);
            inMsg.put(Message.REQUESTOR_ROLE, Boolean.FALSE);
            inMsg.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
            OperationInfo oi = ex.get(OperationInfo.class);
            if (oi != null) {
                inMsg.put(MessageInfo.class, oi.getInput());
            }
            ex.setInMessage(inMsg);
            inMsg.setExchange(ex);
            
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Build inbound interceptor chain.");
            }
    
            //Add all interceptors between USER_LOGICAL and INVOKE.
            SortedSet<Phase> phases = new TreeSet<Phase>(bus.getExtension(PhaseManager.class).getInPhases());
            ColocUtil.setPhases(phases, Phase.USER_LOGICAL, Phase.INVOKE);
            InterceptorChain chain = ColocUtil.getInInterceptorChain(ex, phases);
            chain.add(addColocInterceptors());
            inMsg.setInterceptorChain(chain);
    
            //Convert the coloc object type if necessary
            OperationInfo soi = m.getExchange().get(OperationInfo.class);
            if (soi != null && oi != null) {
                if (ColocUtil.isAssignableOperationInfo(soi, Source.class) 
                    && !ColocUtil.isAssignableOperationInfo(oi, Source.class)) {
                    // converting source -> pojo
                    ColocUtil.convertSourceToObject(inMsg);
                } else if (ColocUtil.isAssignableOperationInfo(oi, Source.class) 
                    && !ColocUtil.isAssignableOperationInfo(soi, Source.class)) {
                    // converting pojo -> source
                    ColocUtil.convertObjectToSource(inMsg);
                }
            }
            chain.doIntercept(inMsg);
            if (soi != null && oi != null) {
                if (ColocUtil.isAssignableOperationInfo(soi, Source.class) 
                    && !ColocUtil.isAssignableOperationInfo(oi, Source.class)
                    && ex.getOutMessage() != null) {
                    // converting pojo -> source                
                    ColocUtil.convertObjectToSource(ex.getOutMessage());
                } else if (ColocUtil.isAssignableOperationInfo(oi, Source.class) 
                    && !ColocUtil.isAssignableOperationInfo(soi, Source.class)
                    && ex.getOutMessage() != null) {
                    // converting pojo -> source
                    ColocUtil.convertSourceToObject(ex.getOutMessage());
                }
            }
            //Set Server OutBound Message onto InBound Exchange.
            setOutBoundMessage(ex, m.getExchange());
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }
    
    protected void setOutBoundMessage(Exchange from, Exchange to) {
        if (from.getOutFaultMessage() != null) {
            to.setInFaultMessage(from.getOutFaultMessage());
        } else {
            to.setInMessage(from.getOutMessage());
        }
    }
    
    protected void setExchangeProperties(Exchange exchange, Message m) {
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);
        exchange.put(Service.class, endpoint.getService());
        exchange.put(Binding.class, endpoint.getBinding());

        //Setup the BindingOperationInfo
        QName opName = (QName) m.get(Message.WSDL_OPERATION);
        BindingInfo bi = endpoint.getEndpointInfo().getBinding();
        BindingOperationInfo boi = bi.getOperation(opName);
        if (boi != null && boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        
        exchange.put(BindingInfo.class, bi);
        exchange.put(BindingOperationInfo.class, boi);
        exchange.put(OperationInfo.class, boi.getOperationInfo());
    }
    
    protected List<Interceptor<? extends Message>> addColocInterceptors() {
        List<Interceptor<? extends Message>> list = new ArrayList<Interceptor<? extends Message>>();
        list.add(new ColocInInterceptor());
        return list;
    }
}
