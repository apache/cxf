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

package org.apache.cxf.clustering;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.AbstractConduitSelector;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Retryable;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;


/**
 * Implements a target selection strategy based on failover to an 
 * alternate target endpoint when a transport level failure is 
 * encountered.
 */
public class FailoverTargetSelector extends AbstractConduitSelector {

    private static final Logger LOG =
        LogUtils.getL7dLogger(FailoverTargetSelector.class);
    private Map<InvocationKey, InvocationContext> inProgress;
    private FailoverStrategy failoverStrategy;
    
    /**
     * Normal constructor.
     */
    public FailoverTargetSelector() {
        this(null);
    }
    
    /**
     * Constructor, allowing a specific conduit to override normal selection.
     * 
     * @param c specific conduit
     */
    public FailoverTargetSelector(Conduit c) {
        super(c);
        inProgress = new HashMap<InvocationKey, InvocationContext>();
    }
    
    /**
     * Called prior to the interceptor chain being traversed.
     * 
     * @param message the current Message
     */
    public synchronized void prepare(Message message) {
        Exchange exchange = message.getExchange();
        InvocationKey key = new InvocationKey(exchange);
        if (!inProgress.containsKey(key)) {
            Endpoint endpoint = exchange.get(Endpoint.class);
            BindingOperationInfo bindingOperationInfo =
                exchange.get(BindingOperationInfo.class);
            Object[] params = message.getContent(List.class).toArray();
            Map<String, Object> context =
                CastUtils.cast((Map)message.get(Message.INVOCATION_CONTEXT));
            InvocationContext invocation = 
                new InvocationContext(endpoint, 
                                      bindingOperationInfo,
                                      params,
                                      context);
            inProgress.put(key, invocation);
        }
    }

    /**
     * Called when a Conduit is actually required.
     * 
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    public Conduit selectConduit(Message message) {
        return getSelectedConduit(message);
    }

    /**
     * Called on completion of the MEP for which the Conduit was required.
     * 
     * @param exchange represents the completed MEP
     */
    public void complete(Exchange exchange) {
        InvocationKey key = new InvocationKey(exchange);
        InvocationContext invocation = null;
        synchronized (this) {
            invocation = inProgress.get(key);
        }
        boolean failover = false;
        if (requiresFailover(exchange)) {
            Endpoint failoverTarget = getFailoverTarget(exchange, invocation);
            if (failoverTarget != null) {
                endpoint = failoverTarget;
                selectedConduit.close();
                selectedConduit = null;
                Exception prevExchangeFault =
                    (Exception)exchange.remove(Exception.class.getName());
                Message outMessage = exchange.getOutMessage();
                Exception prevMessageFault =
                    outMessage.getContent(Exception.class);
                outMessage.setContent(Exception.class, null);
                overrideAddressProperty(invocation.getContext());
                Retryable retry = exchange.get(Retryable.class);
                exchange.clear();
                if (retry != null) {
                    try {
                        failover = true;
                        retry.invoke(invocation.getBindingOperationInfo(),
                                     invocation.getParams(),
                                     invocation.getContext(),
                                     exchange);
                    } catch (Exception e) {
                        if (exchange.get(Exception.class) != null) {
                            exchange.put(Exception.class, prevExchangeFault);
                        }
                        if (outMessage.getContent(Exception.class) != null) {
                            outMessage.setContent(Exception.class,
                                                  prevMessageFault);
                        }
                    }
                }
            } else {
                if (endpoint != invocation.getOriginalEndpoint()) {
                    endpoint = invocation.getOriginalEndpoint();
                    getLogger().log(Level.INFO,
                                    "REVERT_TO_ORIGINAL_TARGET",
                                    endpoint.getEndpointInfo().getName());
                }
            }
        }
        if (!failover) {
            getLogger().info("FAILOVER_NOT_REQUIRED");
            synchronized (this) {
                inProgress.remove(key);
            }
            super.complete(exchange);
        }
    }
    
    /**
     * @param strategy the FailoverStrategy to use
     */
    public synchronized void setStrategy(FailoverStrategy strategy) {
        getLogger().log(Level.INFO, "USING_STRATEGY", new Object[] {strategy});
        failoverStrategy = strategy;
    }
    
    /**
     * @return strategy the FailoverStrategy to use
     */    
    public synchronized FailoverStrategy getStrategy()  {
        if (failoverStrategy == null) {
            failoverStrategy = new SequentialStrategy();
            getLogger().log(Level.INFO,
                            "USING_STRATEGY",
                            new Object[] {failoverStrategy});
        }
        return failoverStrategy;
    }

    /**
     * @return the logger to use
     */
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Check if the exchange is suitable for a failover.
     * 
     * @param exchange the current Exchange
     * @return boolean true if a failover should be attempted
     */
    private boolean requiresFailover(Exchange exchange) {
        Message outMessage = exchange.getOutMessage();
        Exception ex = outMessage.get(Exception.class) != null
                       ? outMessage.get(Exception.class)
                       : exchange.get(Exception.class);
        getLogger().log(Level.INFO,
                        "CHECK_LAST_INVOKE_FAILED",
                        new Object[] {ex != null});
        Throwable curr = ex;
        boolean failover = false;
        while (curr != null) {
            getLogger().log(Level.WARNING,
                            "CHECK_FAILURE_IN_TRANSPORT",
                            new Object[] {ex, curr instanceof IOException});
            failover = curr instanceof java.io.IOException;
            curr = curr.getCause();
        }
        return failover;
    }
    
    /**
     * Get the failover target endpoint, if a suitable one is available.
     * 
     * @param exchange the current Exchange
     * @param invocation the current InvocationContext
     * @return a failover endpoint if one is available
     */
    private Endpoint getFailoverTarget(Exchange exchange,
                                       InvocationContext invocation) {        
        if (invocation.getAlternateTargets() == null) {
            // no previous failover attempt on this invocation
            //
            invocation.setAlternateTargets(
                getStrategy().getAlternateEndpoints(exchange));
        } 

        return getStrategy().selectAlternateEndpoint(
                   invocation.getAlternateTargets());
    }
    
    /**
     * Override the ENDPOINT_ADDRESS property in the request context
     * 
     * @param context the request context
     */
    private void overrideAddressProperty(Map<String, Object> context) {
        Map<String, Object> requestContext =
            CastUtils.cast((Map)context.get(Client.REQUEST_CONTEXT));
        if (requestContext != null) {
            requestContext.put(Message.ENDPOINT_ADDRESS,
                               getEndpoint().getEndpointInfo().getAddress());
            requestContext.put("javax.xml.ws.service.endpoint.address",
                               getEndpoint().getEndpointInfo().getAddress());
        }
    }
            
    /**
     * Used to wrap an Exchange for usage as a Map key. The raw Exchange
     * is not a suitable key type, as the hashCode is computed from its
     * current contents, which may obvioulsy change over the lifetime of
     * an invocation.
     */
    private static class InvocationKey {
        private Exchange exchange;
        
        InvocationKey(Exchange ex) {
            exchange = ex;     
        }
        
        @Override
        public int hashCode() {
            return System.identityHashCode(exchange);
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof InvocationKey
                   && exchange == ((InvocationKey)o).exchange;
        }
    }


    /**
     * Records the context of an invocation.
     */
    private static class InvocationContext {
        private Endpoint originalEndpoint;
        private BindingOperationInfo bindingOperationInfo;
        private Object[] params; 
        private Map<String, Object> context;
        private List<Endpoint> alternateTargets;
        
        InvocationContext(Endpoint endpoint,
                          BindingOperationInfo boi,
                          Object[] prms, 
                          Map<String, Object> ctx) {
            originalEndpoint = endpoint;
            bindingOperationInfo = boi;
            params = prms;
            context = ctx;
        }

        Endpoint getOriginalEndpoint() {
            return originalEndpoint;
        }
        
        BindingOperationInfo getBindingOperationInfo() {
            return bindingOperationInfo;
        }
        
        Object[] getParams() {
            return params;
        }
        
        Map<String, Object> getContext() {
            return context;
        }
        
        List<Endpoint> getAlternateTargets() {
            return alternateTargets;
        }

        void setAlternateTargets(List<Endpoint> alternates) {
            alternateTargets = alternates;
        }
    }    
}
