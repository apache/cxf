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

package org.apache.cxf.message;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.Session;

public class ExchangeImpl extends StringMapImpl implements Exchange {
    
    private Destination destination;
    private boolean oneWay;
    private boolean synchronous = true;
    
    private Message inMessage;
    private Message outMessage;
    private Message inFaultMessage;
    private Message outFaultMessage;
    
    private Session session;
    
    private Bus bus;
    private Endpoint endpoint;
    private Service service;
    private Binding binding;
    private BindingOperationInfo bindingOp;
    

    public <T> void put(Class<T> key, T value) {
        super.put(key, value);
        if (key == Bus.class) {
            bus = (Bus)value;
        } else if (key == Endpoint.class) {
            endpoint = (Endpoint)value;
        } else if (key == Service.class) {
            service = (Service)value;
        } else if (key == BindingOperationInfo.class) {
            bindingOp = (BindingOperationInfo)value;
        } else if (key == Binding.class) {
            binding = (Binding)value;
        }
    }
    
    public Destination getDestination() {
        return destination;
    }

    public Message getInMessage() {
        return inMessage;
    }

    public Conduit getConduit(Message message) {
        return get(ConduitSelector.class) != null
               ? get(ConduitSelector.class).selectConduit(message)
               : null;
    }

    public Message getOutMessage() {
        return outMessage;
    }

    public Message getInFaultMessage() {
        return inFaultMessage;
    }

    public void setInFaultMessage(Message m) {
        inFaultMessage = m;
        m.setExchange(this);
    }

    public Message getOutFaultMessage() {
        return outFaultMessage;
    }

    public void setOutFaultMessage(Message m) {
        outFaultMessage = m;
        m.setExchange(this);
    }

    public void setDestination(Destination d) {
        destination = d;
    }

    public void setInMessage(Message m) {
        inMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public void setConduit(Conduit c) {
        put(ConduitSelector.class,
            new PreexistingConduitSelector(c, get(Endpoint.class)));
    }

    public void setOutMessage(Message m) {
        outMessage = m;
        if (null != m) {
            m.setExchange(this);
        }
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean b) {
        oneWay = b;
    }
    
    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean b) {
        synchronous = b;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
    
    public void clear() {
        super.clear();
        destination = null;
        oneWay = false;
        inMessage = null;
        outMessage = null;
        inFaultMessage = null;
        outFaultMessage = null;
        session = null;
        bus = null;
    }

    public Bus getBus() {
        return bus;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public Service getService() {
        return service;
    }

    public Binding getBinding() {
        return binding;
    }

    public BindingOperationInfo getBindingOperationInfo() {
        return bindingOp;
    }
}
