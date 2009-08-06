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

package org.apache.cxf.service.factory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.service.Service;

public abstract class AbstractServiceFactoryBean {
    private Bus bus;
    private DataBinding dataBinding;
    private Service service;
    private List<FactoryBeanListener> listeners = new LinkedList<FactoryBeanListener>();
    private Map<String, Object> sessionState = new HashMap<String, Object>();
    
    public abstract Service create();
    
    /**
     * Returns a map that is useful for ServiceFactoryBeanListener to store state across 
     * events during processing.   
     */
    public Map<String, Object> getSessionState() {
        return sessionState;
    }
    
    public void sendEvent(FactoryBeanListener.Event ev, Object ... args) {
        for (FactoryBeanListener l : listeners) {
            l.handleEvent(ev, this, args);
        }
    }
    
    protected void initializeDefaultInterceptors() {
        service.getInInterceptors().add(new ServiceInvokerInterceptor());
        service.getInInterceptors().add(new OutgoingChainInterceptor());
        service.getInInterceptors().add(new OneWayProcessorInterceptor());
    }
    
    protected void initializeDataBindings() {
        getDataBinding().initialize(getService());
        
        service.setDataBinding(getDataBinding());
        sendEvent(FactoryBeanListener.Event.DATABINDING_INITIALIZED, dataBinding);
    }
    
    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
        FactoryBeanListenerManager m = bus.getExtension(FactoryBeanListenerManager.class);
        if (m != null) {
            listeners.addAll(m.getListeners());
        }
    }

    public DataBinding getDataBinding() {
        if (dataBinding == null) {
            dataBinding = createDefaultDataBinding();
        }
        return dataBinding;
    }
    protected DataBinding createDefaultDataBinding() {
        return null;
    }

    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    public Service getService() {
        return service;
    }

    protected void setService(Service service) {
        this.service = service;
    }
 
}
