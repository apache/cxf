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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
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
 * Note that this feature changes the conduit on the fly and thus makes
 * the Client not thread safe.
 */
public class FailoverTargetSelector extends AbstractConduitSelector {

    private static final Logger LOG = LogUtils.getL7dLogger(FailoverTargetSelector.class);
    private static final String COMPLETE_IF_SERVICE_NOT_AVAIL_PROPERTY =
        "org.apache.cxf.transport.complete_if_service_not_available";

    protected FailoverStrategy failoverStrategy;
    private ConcurrentHashMap<String, InvocationContext> inProgress = new ConcurrentHashMap<>();
    private boolean supportNotAvailableErrorsOnly = true;
    private String clientBootstrapAddress;

    /**
     * Normal constructor.
     */
    public FailoverTargetSelector() {
        super();
    }

    public FailoverTargetSelector(String clientBootstrapAddress) {
        super();
        this.clientBootstrapAddress = clientBootstrapAddress;
    }

    /**
     * Constructor, allowing a specific conduit to override normal selection.
     *
     * @param c specific conduit
     */
    public FailoverTargetSelector(Conduit c) {
        super(c);
    }

    /**
     * Called prior to the interceptor chain being traversed.
     *
     * @param message the current Message
     */
    public void prepare(Message message) {
        if (message.getContent(List.class) == null) {
            return;
        }
        Exchange exchange = message.getExchange();
        setupExchangeExceptionProperties(exchange);

        String key = String.valueOf(System.identityHashCode(exchange));
        if (getInvocationContext(key) == null) {

            if (getClientBootstrapAddress() != null
                && getClientBootstrapAddress().equals(message.get(Message.ENDPOINT_ADDRESS))) {
                List<String> addresses = failoverStrategy.getAlternateAddresses(exchange);
                if (addresses != null && !addresses.isEmpty()) {
                    getEndpoint().getEndpointInfo().setAddress(addresses.get(0));
                    message.put(Message.ENDPOINT_ADDRESS, addresses.get(0));
                }
            }

            Endpoint endpoint = exchange.getEndpoint();
            BindingOperationInfo bindingOperationInfo =
                exchange.getBindingOperationInfo();
            Object[] params = message.getContent(List.class).toArray();
            Map<String, Object> context =
                CastUtils.cast((Map<?, ?>)message.get(Message.INVOCATION_CONTEXT));
            InvocationContext invocation =
                new InvocationContext(endpoint,
                                      bindingOperationInfo,
                                      params,
                                      context);
            inProgress.putIfAbsent(key, invocation);
        }
    }

    protected void setupExchangeExceptionProperties(Exchange ex) {
        if (!isSupportNotAvailableErrorsOnly()) {
            ex.remove("org.apache.cxf.transport.no_io_exceptions");
        }
        ex.put(COMPLETE_IF_SERVICE_NOT_AVAIL_PROPERTY, true);
    }

    /**
     * Called when a Conduit is actually required.
     *
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    public Conduit selectConduit(Message message) {
        Conduit c = message.get(Conduit.class);
        if (c != null) {
            return c;
        }
        return getSelectedConduit(message);
    }

    protected InvocationContext getInvocationContext(String key) {
        if (key != null) {
            return inProgress.get(key);
        }
        return null;
    }

    /**
     * Called on completion of the MEP for which the Conduit was required.
     *
     * @param exchange represents the completed MEP
     */
    public void complete(Exchange exchange) {
        String key = String.valueOf(System.identityHashCode(exchange));
        InvocationContext invocation = getInvocationContext(key);
        if (invocation == null) {
            super.complete(exchange);
            return;
        }

        boolean failover = false;
        final Exception ex = getExceptionIfPresent(exchange);
        if (requiresFailover(exchange, ex)) {
            onFailure(invocation, ex);
            Conduit old = (Conduit)exchange.getOutMessage().remove(Conduit.class.getName());

            Endpoint failoverTarget = getFailoverTarget(exchange, invocation);
            if (failoverTarget != null) {
                setEndpoint(failoverTarget);
                removeConduit(old);
                failover = performFailover(exchange, invocation);
            } else {
                exchange.remove(COMPLETE_IF_SERVICE_NOT_AVAIL_PROPERTY);
                setOriginalEndpoint(invocation);
            }
        } else {
            getLogger().fine("FAILOVER_NOT_REQUIRED");
            onSuccess(invocation);
        }

        if (!failover) {
            inProgress.remove(key);
            doComplete(exchange);
        }
    }

    protected void doComplete(Exchange exchange) {
        super.complete(exchange);
    }

    protected void setOriginalEndpoint(InvocationContext invocation) {
        setEndpoint(invocation.retrieveOriginalEndpoint(endpoint));
    }

    protected boolean performFailover(Exchange exchange, InvocationContext invocation) {
        Exception prevExchangeFault = (Exception)exchange.remove(Exception.class.getName());
        Message outMessage = exchange.getOutMessage();
        Exception prevMessageFault = outMessage.getContent(Exception.class);
        outMessage.setContent(Exception.class, null);
        overrideAddressProperty(invocation.getContext());

        Retryable retry = exchange.get(Retryable.class);
        exchange.clear();
        boolean failover = false;
        if (retry != null) {
            try {
                failover = true;
                long delay = getDelayBetweenRetries();
                if (delay > 0) {
                    Thread.sleep(delay);
                }
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
        return failover;
    }

    protected void onSuccess(InvocationContext context) {
    }

    protected void onFailure(InvocationContext context, Exception ex) {
    }

    /**
     * @param strategy the FailoverStrategy to use
     */
    public synchronized void setStrategy(FailoverStrategy strategy) {
        if (strategy != null) {
            getLogger().log(Level.INFO, "USING_STRATEGY", new Object[] {strategy});
            failoverStrategy = strategy;
        }
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
     * Returns delay (in milliseconds) between retries
     * @return delay, 0 means no delay
     */
    protected long getDelayBetweenRetries() {
        FailoverStrategy strategy = getStrategy();
        if (strategy instanceof AbstractStaticFailoverStrategy) {
            return ((AbstractStaticFailoverStrategy)strategy).getDelayBetweenRetries();
        }
        //perhaps supporting FailoverTargetSelector specific property can make sense too
        return 0;
    }

    /**
     * Check if the exchange is suitable for a failover.
     *
     * @param exchange the current Exchange
     * @return boolean true if a failover should be attempted
     */
    protected boolean requiresFailover(Exchange exchange, Exception ex) {
        getLogger().log(Level.FINE,
                        "CHECK_LAST_INVOKE_FAILED",
                        new Object[] {ex != null});
        Throwable curr = ex;
        boolean failover = false;
        while (curr != null) {
            failover = curr instanceof java.io.IOException;
            curr = curr.getCause();
        }
        if (ex != null) {
            getLogger().log(Level.INFO,
                            "CHECK_FAILURE_IN_TRANSPORT",
                            new Object[] {ex, failover});
        }

        if (isSupportNotAvailableErrorsOnly() && exchange.get(Message.RESPONSE_CODE) != null) {
            failover = PropertyUtils.isTrue(exchange.get("org.apache.cxf.transport.service_not_available"));
        }

        return failover;
    }

    private Exception getExceptionIfPresent(Exchange exchange) {
        Message outMessage = exchange.getOutMessage();
        Exception ex = outMessage.get(Exception.class);
        if (ex == null) {
            ex = outMessage.getContent(Exception.class);
        }
        return ex != null ? ex : exchange.get(Exception.class);
    }

    /**
     * Get the failover target endpoint, if a suitable one is available.
     *
     * @param exchange the current Exchange
     * @param invocation the current InvocationContext
     * @return a failover endpoint if one is available
     */
    protected Endpoint getFailoverTarget(Exchange exchange,
                                       InvocationContext invocation) {
        List<String> alternateAddresses = updateContextAlternatives(exchange, invocation);
        Endpoint failoverTarget = null;
        if (alternateAddresses != null) {
            String alternateAddress =
                getStrategy().selectAlternateAddress(alternateAddresses);
            if (alternateAddress != null) {
                // re-use current endpoint
                //
                failoverTarget = getEndpoint();

                failoverTarget.getEndpointInfo().setAddress(alternateAddress);
            }
        } else {
            failoverTarget = getStrategy().selectAlternateEndpoint(
                                 invocation.getAlternateEndpoints());
        }
        return failoverTarget;
    }

    /**
     * Fetches and updates the alternative address or/and alternative endpoints
     * (depending on the strategy) for current invocation context.
     * @param exchange the current Exchange
     * @param invocation the current InvocationContext
     * @return alternative addresses
     */
    protected List<String> updateContextAlternatives(Exchange exchange, InvocationContext invocation) {
        final List<String> alternateAddresses;
        if (!invocation.hasAlternates()) {
            // no previous failover attempt on this invocation
            //
            alternateAddresses =
                getStrategy().getAlternateAddresses(exchange);
            if (alternateAddresses != null) {
                invocation.setAlternateAddresses(alternateAddresses);
            } else {
                invocation.setAlternateEndpoints(
                    getStrategy().getAlternateEndpoints(exchange));
            }
        } else {
            alternateAddresses = invocation.getAlternateAddresses();
        }
        return alternateAddresses;
    }

    /**
     * Override the ENDPOINT_ADDRESS property in the request context
     *
     * @param context the request context
     */
    protected void overrideAddressProperty(Map<String, Object> context) {
        overrideAddressProperty(context, getEndpoint().getEndpointInfo().getAddress());
    }

    protected void overrideAddressProperty(Map<String, Object> context,
                                           String address) {
        Map<String, Object> requestContext =
            CastUtils.cast((Map<?, ?>)context.get(Client.REQUEST_CONTEXT));
        if (requestContext != null) {
            requestContext.put(Message.ENDPOINT_ADDRESS, address);
            requestContext.put("jakarta.xml.ws.service.endpoint.address", address);
        }
    }

    // Some conduits may replace the endpoint address after it has already been prepared
    // but before the invocation has been done (ex, org.apache.cxf.clustering.LoadDistributorTargetSelector)
    // which may affect JAX-RS clients where actual endpoint address property may include additional path
    // segments.
    protected boolean replaceEndpointAddressPropertyIfNeeded(Message message,
                                                             String endpointAddress,
                                                             Conduit cond) {
        String requestURI = (String)message.get(Message.REQUEST_URI);
        if (requestURI != null && endpointAddress != null && !requestURI.equals(endpointAddress)) {
            String basePath = (String)message.get(Message.BASE_PATH);
            if (basePath != null && requestURI.startsWith(basePath)) {
                String pathInfo = requestURI.substring(basePath.length());
                message.put(Message.BASE_PATH, endpointAddress);
                final String slash = "/";
                boolean startsWithSlash = pathInfo.startsWith(slash);
                if (endpointAddress.endsWith(slash)) {
                    endpointAddress = endpointAddress + (startsWithSlash ? pathInfo.substring(1) : pathInfo);
                } else {
                    endpointAddress = endpointAddress + (startsWithSlash ? pathInfo : (slash + pathInfo));
                }
                message.put(Message.ENDPOINT_ADDRESS, endpointAddress);
                message.put(Message.REQUEST_URI, endpointAddress);

                Exchange exchange = message.getExchange();
                String key = String.valueOf(System.identityHashCode(exchange));
                InvocationContext invocation = getInvocationContext(key);
                if (invocation != null) {
                    overrideAddressProperty(invocation.getContext(),
                                            cond.getTarget().getAddress().getValue());
                }
                return true;
            }
        }
        return false;
    }

    public boolean isSupportNotAvailableErrorsOnly() {
        return supportNotAvailableErrorsOnly;
    }

    public void setSupportNotAvailableErrorsOnly(boolean support) {
        this.supportNotAvailableErrorsOnly = support;
    }

    public String getClientBootstrapAddress() {
        return clientBootstrapAddress;
    }

    public void setClientBootstrapAddress(String clientBootstrapAddress) {
        this.clientBootstrapAddress = clientBootstrapAddress;
    }

    protected String getInvocationKey(Exchange e) {
        return String.valueOf(System.identityHashCode(e));
    }

    /**
     * Records the context of an invocation.
     */
    protected class InvocationContext {
        private Endpoint originalEndpoint;
        private String originalAddress;
        private BindingOperationInfo bindingOperationInfo;
        private Object[] params;
        private Map<String, Object> context;
        private List<Endpoint> alternateEndpoints;
        private List<String> alternateAddresses;
        protected InvocationContext(Endpoint endpoint,
                          BindingOperationInfo boi,
                          Object[] prms,
                          Map<String, Object> ctx) {
            originalEndpoint = endpoint;
            originalAddress = endpoint.getEndpointInfo().getAddress();
            bindingOperationInfo = boi;
            params = prms;
            context = ctx;
        }

        public Endpoint retrieveOriginalEndpoint(Endpoint endpoint) {
            if (endpoint != null) {
                if (endpoint != originalEndpoint) {
                    getLogger().log(Level.INFO,
                                    "REVERT_TO_ORIGINAL_TARGET",
                                    endpoint.getEndpointInfo().getName());
                }
                if (!endpoint.getEndpointInfo().getAddress().equals(originalAddress)) {
                    endpoint.getEndpointInfo().setAddress(originalAddress);
                    getLogger().log(Level.INFO,
                                    "REVERT_TO_ORIGINAL_ADDRESS",
                                    endpoint.getEndpointInfo().getAddress());
                }
            }
            return originalEndpoint;
        }

        public BindingOperationInfo getBindingOperationInfo() {
            return bindingOperationInfo;
        }

        public Object[] getParams() {
            return params;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public List<Endpoint> getAlternateEndpoints() {
            return alternateEndpoints;
        }

        public List<String> getAlternateAddresses() {
            return alternateAddresses;
        }

        protected void setAlternateEndpoints(List<Endpoint> alternates) {
            alternateEndpoints = alternates;
        }

        protected void setAlternateAddresses(List<String> alternates) {
            alternateAddresses = alternates;
        }

        public boolean hasAlternates() {
            return !(alternateEndpoints == null && alternateAddresses == null);
        }
    }
}
