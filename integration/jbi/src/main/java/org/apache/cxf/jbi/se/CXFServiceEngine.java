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


package org.apache.cxf.jbi.se;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jbi.se.state.AbstractServiceEngineStateMachine;
import org.apache.cxf.jbi.se.state.ServiceEngineStateFactory;
import org.apache.cxf.jbi.se.state.ServiceEngineStateMachine;


/** A JBI component.  Initializes the CXF JBI transport
 */
public class CXFServiceEngine implements ComponentLifeCycle, Component {
    
    public static final String JBI_TRANSPORT_ID = "http://cxf.apache.org/transports/jbi";
    
    
    
    
    private static final Logger LOG = LogUtils.getL7dLogger(CXFServiceEngine.class);
    
    
   
    private ServiceEngineStateFactory stateFactory = ServiceEngineStateFactory.getInstance();
    
    public CXFServiceEngine() {
        stateFactory.setCurrentState(stateFactory.getShutdownState());
    }
    
    // Implementation of javax.jbi.component.ComponentLifeCycle
    
    public final ObjectName getExtensionMBeanName() {
        return null;
    }
    
    public final void shutDown() throws JBIException {
        try {
            LOG.info(new Message("SE.SHUTDOWN", LOG).toString());
            stateFactory.getCurrentState().changeState(ServiceEngineStateMachine.SEOperation.shutdown, null);
        } catch (Throwable ex) { 
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new JBIException(ex);
        } 
    }
    
    public final void init(ComponentContext componentContext) throws JBIException {
        try {
            stateFactory.getCurrentState().changeState(
                ServiceEngineStateMachine.SEOperation.init, componentContext);
        } catch (Throwable ex) { 
            LOG.log(Level.SEVERE, new Message("SE.FAILED.INIT.BUS", LOG).toString(), ex);
            throw new JBIException(ex);
        } 
    }
    
    
    
    
    
    public final void start() throws JBIException {
        try { 
            LOG.info(new Message("SE.STARTUP", LOG).toString());
            stateFactory.getCurrentState().changeState(ServiceEngineStateMachine.SEOperation.start, null);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new JBIException(ex);
        }
    }
    
    public final void stop() throws JBIException {
        try {
            LOG.info(new Message("SE.STOP", LOG).toString());
            stateFactory.getCurrentState().changeState(ServiceEngineStateMachine.SEOperation.stop, null);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new JBIException(ex);
        }
    }
    
    // Implementation of javax.jbi.component.Component
    
    public final ComponentLifeCycle getLifeCycle() {
        LOG.fine("CXFServiceEngine returning life cycle");
        return this;
    }
    
    public final ServiceUnitManager getServiceUnitManager() {
        LOG.fine("CXFServiceEngine return service unit manager");
        return AbstractServiceEngineStateMachine.getSUManager();
    }
    
    public final Document getServiceDescription(final ServiceEndpoint serviceEndpoint) {
        Document doc = 
            AbstractServiceEngineStateMachine.getSUManager().getServiceDescription(serviceEndpoint);
        LOG.fine("CXFServiceEngine returning service description: " + doc);
        return doc;
    }
    
    public final boolean isExchangeWithConsumerOkay(final ServiceEndpoint ep, 
                                                    final MessageExchange exchg) {
        
        LOG.fine("isExchangeWithConsumerOkay: endpoint: " + ep 
                 + " exchange: " + exchg);
        return true;
    }
    
    public final boolean isExchangeWithProviderOkay(final ServiceEndpoint ep, 
                                                    final MessageExchange exchng) {
        LOG.fine("isExchangeWithConsumerOkay: endpoint: " + ep 
                 + " exchange: " + exchng);
        return true;
    }
    
    public final ServiceEndpoint resolveEndpointReference(final DocumentFragment documentFragment) {
        return null;
    }
    
    
    
    
    
}
