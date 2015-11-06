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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.clustering.circuitbreaker.CircuitBreaker;
import org.apache.cxf.clustering.circuitbreaker.ZestCircuitBreaker;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

public class CircuitBreakerTargetSelector extends FailoverTargetSelector {
    public static final int DEFAULT_TIMEOUT = 1000 * 60 /* 1 minute timeout as default */;
    public static final int DEFAULT_THESHOLD = 1;

    private static final Logger LOG = LogUtils.getL7dLogger(CircuitBreakerTargetSelector.class);
    
    private final int threshold;
    private final long timeout; 
    private final ConcurrentMap<String, CircuitBreaker> circuits = new ConcurrentHashMap<>();
    
    public CircuitBreakerTargetSelector(final int threshold, final long timeout) {
        super();
        this.threshold = threshold;
        this.timeout = timeout;
    }
    
    public CircuitBreakerTargetSelector() {
        this(DEFAULT_THESHOLD, DEFAULT_TIMEOUT);
    }
    
    @Override
    public synchronized void setStrategy(FailoverStrategy strategy) {
        super.setStrategy(strategy);
        
        if (strategy != null) {
            for (String alternative: strategy.getAlternateAddresses(null /* no Exchange at this point */)) {
                if (!StringUtils.isEmpty(alternative)) {
                    circuits.putIfAbsent(
                        alternative, 
                        new ZestCircuitBreaker(threshold, timeout)
                    );
                }
            }
        }
    }
    
    @Override
    protected Endpoint getFailoverTarget(final Exchange exchange, final InvocationContext invocation) {
        if (circuits.isEmpty()) {
            LOG.log(Level.SEVERE, "No alternative addresses configured");
            return null;
        }
        
        final List<String> alternateAddresses = new ArrayList<>();
        for (final Map.Entry<String, CircuitBreaker> entry: circuits.entrySet()) {
            if (entry.getValue().allowRequest()) {
                alternateAddresses.add(entry.getKey());
            }
        }

        Endpoint failoverTarget = null;
        if (!alternateAddresses.isEmpty()) {
            final String alternateAddress = getStrategy().selectAlternateAddress(alternateAddresses);
            
            // Reuse current endpoint
            if (alternateAddress != null) {
                failoverTarget = getEndpoint();
                failoverTarget.getEndpointInfo().setAddress(alternateAddress);
            }
        } 
        
        return failoverTarget;
    }
    
    @Override
    public void prepare(Message message) {
        super.prepare(message);
    }
    
    @Override
    protected void onFailure(InvocationContext context, Exception ex) {
        super.onFailure(context, ex);
        
        final Map<String, Object> requestContext =
            CastUtils.cast((Map<?, ?>)context.getContext().get(Client.REQUEST_CONTEXT));
        
        if (requestContext != null) {
            final String address = (String)requestContext.get(Message.ENDPOINT_ADDRESS);
            final CircuitBreaker circuitBreaker = circuits.get(address);
            if (circuitBreaker != null) {
                circuitBreaker.markFailure(ex);
            }
        }
    }
    
    @Override
    protected void onSuccess(InvocationContext context) {
        super.onSuccess(context);
        
        final Map<String, Object> requestContext =
            CastUtils.cast((Map<?, ?>)context.getContext().get(Client.REQUEST_CONTEXT));
        
        if (requestContext != null) {
            final String address = (String)requestContext.get(Message.ENDPOINT_ADDRESS);
            final CircuitBreaker circuitBreaker = circuits.get(address);
            if (circuitBreaker != null) {
                circuitBreaker.markSuccess();
            }
        }
    }
}
