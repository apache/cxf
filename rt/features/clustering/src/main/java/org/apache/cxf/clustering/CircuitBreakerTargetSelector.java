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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.clustering.circuitbreaker.CircuitBreaker;
import org.apache.cxf.clustering.circuitbreaker.ZestCircuitBreaker;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;

public class CircuitBreakerTargetSelector extends FailoverTargetSelector {
    public static final int DEFAULT_TIMEOUT = 1000 * 60 /* 1 minute timeout as default */;
    public static final int DEFAULT_THESHOLD = 1;

    private static final String IS_SELECTED = "org.apache.cxf.clustering.CircuitBreakerTargetSelector.IS_SELECTED";
    private static final Logger LOG = LogUtils.getL7dLogger(CircuitBreakerTargetSelector.class);

    /**
     * Static instance of empty (or noop) circuit breaker to handle use cases
     * when alternative addresses or alternative endpoint addresses are nullable
     * (or non-valid).
     */
    private static final  CircuitBreaker NOOP_CIRCUIT_BREAKER = new CircuitBreaker() {
        @Override
        public boolean allowRequest() {
            return true;
        }

        @Override
        public void markFailure(Throwable cause) {
        }

        @Override
        public void markSuccess() {
        }
    };

    private final int threshold;
    private final long timeout;
    private final Map<String, CircuitBreaker> circuits = new LinkedHashMap<>();

    public CircuitBreakerTargetSelector(final int threshold, final long timeout) {
        super();
        this.threshold = threshold;
        this.timeout = timeout;
    }

    public CircuitBreakerTargetSelector(final int threshold, final long timeout,
                                        final String clientBootstrapAddress) {
        super(clientBootstrapAddress);
        this.threshold = threshold;
        this.timeout = timeout;
    }

    public CircuitBreakerTargetSelector() {
        this(DEFAULT_THESHOLD, DEFAULT_TIMEOUT);
    }

    @Override
    public synchronized void setStrategy(FailoverStrategy strategy) {
        super.setStrategy(strategy);

        // Registering the original endpoint in the list of circuit breakers
        if (getEndpoint() != null) {
            final String address = getEndpoint().getEndpointInfo().getAddress();
            if (!StringUtils.isEmpty(address)) {
                circuits.putIfAbsent(address, new ZestCircuitBreaker(threshold, timeout));
            }
        }

        if (strategy != null) {
            final List<String> alternatives = strategy.getAlternateAddresses(null /* no Exchange at this point */);
            if (alternatives != null) {
                for (String alternative: alternatives) {
                    if (!StringUtils.isEmpty(alternative)) {
                        circuits.putIfAbsent(alternative, new ZestCircuitBreaker(threshold, timeout));
                    }
                }
            }
        }
    }
    @Override
    public synchronized Conduit selectConduit(Message message) {
        Conduit c = message.get(Conduit.class);
        if (c != null) {
            return c;
        }
        Exchange exchange = message.getExchange();
        String key = String.valueOf(System.identityHashCode(exchange));
        InvocationContext invocation = getInvocationContext(key);
        if (invocation != null && !invocation.getContext().containsKey(IS_SELECTED)) {
            final String address = (String) message.get(Message.ENDPOINT_ADDRESS);

            if (isFailoverRequired(address)) {
                Endpoint target = getFailoverTarget(exchange, invocation);

                if (target == null) {
                    throw new Fault(new FailoverFailedException(
                        "None of alternative addresses are available at the moment"));
                }

                if (isEndpointChanged(address, target)) {
                    setEndpoint(target);
                    message.put(Message.ENDPOINT_ADDRESS, target.getEndpointInfo().getAddress());
                    overrideAddressProperty(invocation.getContext());
                    invocation.getContext().put(IS_SELECTED, null);
                }
            }
        }

        return getSelectedConduit(message);
    }

    @Override
    protected Endpoint getFailoverTarget(final Exchange exchange, final InvocationContext invocation) {
        if (circuits.isEmpty()) {
            LOG.log(Level.SEVERE, "No alternative addresses configured");
            return null;
        }

        final List<String> alternateAddresses = updateContextAlternatives(exchange, invocation);
        if (alternateAddresses != null) {
            final Iterator<String> alternateAddressIterator = alternateAddresses.iterator();

            while (alternateAddressIterator.hasNext()) {
                final String alternateAddress = alternateAddressIterator.next();
                final CircuitBreaker circuitBreaker = getCircuitBreaker(alternateAddress);

                if (!circuitBreaker.allowRequest()) {
                    alternateAddressIterator.remove();
                }
            }
        }

        Endpoint failoverTarget = null;
        if (alternateAddresses != null && !alternateAddresses.isEmpty()) {
            final String alternateAddress = getStrategy().selectAlternateAddress(alternateAddresses);

            // Reuse current endpoint
            if (alternateAddress != null) {
                failoverTarget = getEndpoint();
                failoverTarget.getEndpointInfo().setAddress(alternateAddress);
            }
        } else {
            final List<Endpoint> alternateEndpoints = invocation.getAlternateEndpoints();

            if (alternateEndpoints != null) {
                final Iterator<Endpoint> alternateEndpointIterator = alternateEndpoints.iterator();

                while (alternateEndpointIterator.hasNext()) {
                    final Endpoint endpoint = alternateEndpointIterator.next();
                    final CircuitBreaker circuitBreaker = getCircuitBreaker(endpoint);
                    if (!circuitBreaker.allowRequest()) {
                        alternateEndpointIterator.remove();
                    }
                }
            }

            failoverTarget = getStrategy().selectAlternateEndpoint(alternateEndpoints);
        }

        return failoverTarget;
    }

    @Override
    protected void onFailure(InvocationContext context, Exception ex) {
        super.onFailure(context, ex);

        final Map<String, Object> requestContext =
            CastUtils.cast((Map<?, ?>)context.getContext().get(Client.REQUEST_CONTEXT));

        if (requestContext != null) {
            final String address = (String)requestContext.get(Message.ENDPOINT_ADDRESS);
            getCircuitBreaker(address).markFailure(ex);
        }
    }

    @Override
    protected void onSuccess(InvocationContext context) {
        super.onSuccess(context);

        final Map<String, Object> requestContext =
            CastUtils.cast((Map<?, ?>)context.getContext().get(Client.REQUEST_CONTEXT));

        if (requestContext != null) {
            final String address = (String)requestContext.get(Message.ENDPOINT_ADDRESS);
            getCircuitBreaker(address).markSuccess();
        }
    }

    private CircuitBreaker getCircuitBreaker(final Endpoint endpoint) {
        return getCircuitBreaker(endpoint.getEndpointInfo().getAddress());
    }

    private synchronized CircuitBreaker getCircuitBreaker(final String alternateAddress) {
        CircuitBreaker circuitBreaker = null;

        if (!StringUtils.isEmpty(alternateAddress)) {
            for (Map.Entry<String, CircuitBreaker> entry: circuits.entrySet()) {
                if (alternateAddress.startsWith(entry.getKey())) {
                    circuitBreaker = entry.getValue();
                    break;
                }
            }

            if (circuitBreaker == null) {
                circuitBreaker = new ZestCircuitBreaker(threshold, timeout);
                circuits.put(alternateAddress, circuitBreaker);
            }
        }

        if (circuitBreaker == null) {
            circuitBreaker = NOOP_CIRCUIT_BREAKER;
        }

        return circuitBreaker;
    }

    private boolean isEndpointChanged(final String address, final Endpoint target) {
        if (!StringUtils.isEmpty(address)) {
            return !address.startsWith(target.getEndpointInfo().getAddress());
        }

        if (getEndpoint().equals(target)) {
            return false;
        }

        return !getEndpoint().getEndpointInfo().getAddress().startsWith(
            target.getEndpointInfo().getAddress());
    }

    private boolean isFailoverRequired(final String address) {
        if (!StringUtils.isEmpty(address)) {
            for (final Map.Entry<String, CircuitBreaker> entry: circuits.entrySet()) {
                if (address.startsWith(entry.getKey())) {
                    return !entry.getValue().allowRequest();
                }
            }
        }

        LOG.log(Level.WARNING, "No circuit breaker present for address: " + address);
        return false;
    }
}
