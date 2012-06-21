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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.ConduitSelectorHolder;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.HTTPConduit;

/**
 * Represents the configuration of the current proxy or WebClient.
 * Given an instance with the name 'client', one can access its configuration
 * using a WebClient.getConfig(client) call.
 */
public class ClientConfiguration implements InterceptorProvider, ConduitSelectorHolder {
    private static final Logger LOG = LogUtils.getL7dLogger(ClientConfiguration.class);
    
    private List<Interceptor<? extends Message>> inInterceptors 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outInterceptors 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outFault 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> inFault 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    private ConduitSelector conduitSelector;
    private Bus bus;
    private Map<String, Object> requestContext = new HashMap<String, Object>();
    private Map<String, Object> responseContext = new HashMap<String, Object>();
    private long synchronousTimeout = 60000;
    
    public long getSynchronousTimeout() {
        Conduit conduit = getConduit();
        if (conduit instanceof HTTPConduit) {
            return ((HTTPConduit)conduit).getClient().getReceiveTimeout();
        } else {
            return synchronousTimeout;
        }
    }

    /**
     * Sets the synchronous timeout
     * @param synchronousTimeout
     */
    public void setSynchronousTimeout(long synchronousTimeout) {
        this.synchronousTimeout = synchronousTimeout;
    }
    
    /**
     * Indicates if Response may still be expected for oneway requests.
     * For example, 202 in case of HTTP
     * @return true if the response can be expected
     */
    public boolean isResponseExpectedForOneway() {
        return getConduit() instanceof HTTPConduit ? true : false;
    }
    
    /**
     * Sets the conduit selector 
     * @param cs the selector
     */
    public void setConduitSelector(ConduitSelector cs) {
        this.conduitSelector = cs;
    }
    /**
     * Gets the conduit selector 
     * @return the conduit the selector
     */
    public ConduitSelector getConduitSelector() {
        return conduitSelector;
    }
    
    void prepareConduitSelector(Message message) {
        try {
            getConduitSelector().prepare(message);
        } catch (Fault ex) {
            LOG.fine("Failure to prepare a message from conduit selector");
        }
    }
    
    /**
     * Sets the bus
     * @param bus the bus
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    /**
     * Gets the bus
     * @return the bus
     */
    public Bus getBus() {
        return bus;
    }
    
    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        return inInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return outInterceptors;
    }

    /**
     * Sets the list of in interceptors which pre-process 
     * the responses from remote services.
     *  
     * @param interceptors in interceptors
     */
    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inInterceptors = interceptors;
    }

    /**
     * Sets the list of out interceptors which post-process 
     * the requests to the remote services.
     *  
     * @param interceptors out interceptors
     */
    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outInterceptors = interceptors;
    }
    
    /**
     * Sets the list of in fault interceptors which will deal with the HTTP
     * faults; the client code may choose to catch {@link ServerWebApplicationException}
     * exceptions instead.
     *  
     * @param interceptors in fault interceptors
     */
    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inFault = interceptors;
    }

    /**
     * Sets the list of out fault interceptors which will deal with the client-side
     * faults; the client code may choose to catch {@link ClientWebApplicationException}
     * exceptions instead.
     *  
     * @param interceptors out fault interceptors
     */
    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outFault = interceptors;
    }
    
    /**
     * Gets the conduit responsible for a transport-level
     * communication with the remote service. 
     * @return the conduit
     */
    public Conduit getConduit() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.put(MessageObserver.class, new ClientMessageObserver(this));
        exchange.put(Bus.class, bus);
        prepareConduitSelector(message);
        return getConduitSelector().selectConduit(message);
    }
    
    /**
     * Gets the HTTP conduit responsible for a transport-level
     * communication with the remote service. 
     * @return the HTTP conduit
     */
    public HTTPConduit getHttpConduit() {
        Conduit conduit = getConduit();
        return conduit instanceof HTTPConduit ? (HTTPConduit)conduit : null;
    }
 
    /**
     * Get the map of properties which affect the responses only. 
     * These additional properties may be optionally set after a 
     * proxy or WebClient has been created.
     * @return the response context properties
     */
    public Map<String, Object> getResponseContext() {
        return responseContext;
    }
    
    /**
     * Get the map of properties which affect the requests only. 
     * These additional properties may be optionally set after a 
     * proxy or WebClient has been created.
     * @return the request context properties
     */
    public Map<String, Object> getRequestContext() {
        return requestContext;
    }
}
