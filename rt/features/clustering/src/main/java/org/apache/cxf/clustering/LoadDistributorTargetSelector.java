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
import java.util.logging.Logger;

import org.apache.cxf.clustering.FailoverTargetSelector.InvocationContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;

/**
 * The LoadDistributorTargetSelector attempts to do the same job as the
 * FailoverTargetSelector, but to choose an alternate target on every request
 * rather than just when a fault occurs.
 * The LoadDistributorTargetSelector uses the same FailoverStrategy interface as
 * the FailoverTargetSelector, but has a few significant limitations:
 * 1. Because the LoadDistributorTargetSelector needs to maintain a list of targets
 *    between calls it has to obtain that list without reference to a Message.
 *    Most FailoverStrategy classes can support this for addresses, but it cannot
 *    be supported for endpoints.
 *    If the list of targets cannot be obtained without reference to a Message then
 *    the list will still be obtained but it will be specific to the Message and thus
 *    discarded after this message has been processed.  As a consequence, if the
 *    strategy chosen is a simple sequential one the first item in the list will
 *    be chosen every time.
 *    Conclusion: Be aware that if you are working with targets that are
 *    dependent on the Message the process will be less efficient and that the
 *    SequentialStrategy will not distribute the load at all.
 * 2. The AbstractStaticFailoverStrategy base class excludes the 'default' endpoint
 *    from the list of alternate endpoints.
 *    If alternate endpoints (as opposed to alternate addresses) are to be used
 *    you should probably ensure that your FailoverStrategy overrides getAlternateEndpoints
 *    and calls getEndpoints with acceptCandidatesWithSameAddress = true.
 */
public class LoadDistributorTargetSelector extends FailoverTargetSelector {
    private static final Logger LOG = LogUtils.getL7dLogger(
                        LoadDistributorTargetSelector.class);
    private static final String IS_DISTRIBUTED =
            "org.apache.cxf.clustering.LoadDistributorTargetSelector.IS_DISTRIBUTED";

    private List<String> addressList;

    private boolean failover = true;

    /**
     * Normal constructor.
     */
    public LoadDistributorTargetSelector() {
        super();
    }

    public LoadDistributorTargetSelector(String clientBootstrapAddress) {
        super(clientBootstrapAddress);
    }

    /**
     * Constructor, allowing a specific conduit to override normal selection.
     *
     * @param c specific conduit
     */
    public LoadDistributorTargetSelector(Conduit c) {
        super(c);
    }

    public boolean isFailover() {
        return failover;
    }

    public void setFailover(boolean failover) {
        this.failover = failover;
    }

    @Override
    protected java.util.logging.Logger getLogger() {
        return LOG;
    }

    /**
     * Called when a Conduit is actually required.
     *
     * @param message
     * @return the Conduit to use for mediation of the message
     */
    public synchronized Conduit selectConduit(Message message) {
        Conduit c = message.get(Conduit.class);
        if (c != null) {
            return c;
        }
        Exchange exchange = message.getExchange();
        String key = String.valueOf(System.identityHashCode(exchange));
        InvocationContext invocation = getInvocationContext(key);
        if ((invocation != null) && !invocation.getContext().containsKey(IS_DISTRIBUTED)) {
            Endpoint target = getDistributionTarget(exchange, invocation);
            if (target != null) {
                setEndpoint(target);
                message.put(Message.ENDPOINT_ADDRESS, target.getEndpointInfo().getAddress());
                message.put(CONDUIT_COMPARE_FULL_URL, Boolean.TRUE);
                overrideAddressProperty(invocation.getContext());
                invocation.getContext().put(IS_DISTRIBUTED, null);
            }
        }
        return getSelectedConduit(message);
    }

    /**
     * Get the failover target endpoint, if a suitable one is available.
     *
     * @param exchange the current Exchange
     * @param invocation the current InvocationContext
     * @return a failover endpoint if one is available
     *
     * Note: The only difference between this and the super implementation is
     * that the current (failed) address is removed from the list set of alternates,
     * it could be argued that that change should be in the super implementation
     * but I'm not sure of the impact.
     */
    protected Endpoint getFailoverTarget(Exchange exchange,
                                       InvocationContext invocation) {
        final List<String> alternateAddresses;
        if (!invocation.hasAlternates()) {
            // no previous failover attempt on this invocation
            //
            alternateAddresses =
                getStrategy().getAlternateAddresses(exchange);
            if (alternateAddresses != null) {
                alternateAddresses.remove(exchange.getEndpoint().getEndpointInfo().getAddress());
                invocation.setAlternateAddresses(alternateAddresses);
            } else {
                invocation.setAlternateEndpoints(
                    getStrategy().getAlternateEndpoints(exchange));
            }
        } else {
            alternateAddresses = invocation.getAlternateAddresses();
        }

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
            if (basePath.startsWith(endpointAddress)) {
                endpointAddress = basePath;
            }
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

    /**
     * Get the distribution target endpoint, if a suitable one is available.
     *
     * @param exchange the current Exchange
     * @param invocation the current InvocationContext
     * @return a distribution endpoint if one is available
     */
    private Endpoint getDistributionTarget(Exchange exchange,
                                           InvocationContext invocation) {
        if ((addressList == null) || (addressList.isEmpty())) {
            try {
                addressList = getStrategy().getAlternateAddresses(null);
            } catch (NullPointerException ex) {
                getLogger().fine("Strategy " + getStrategy().getClass()
                        + " cannot handle a null argument to getAlternateAddresses: " + ex.toString());
            }
        }
        List<String> alternateAddresses = addressList;

        if ((alternateAddresses == null) || (alternateAddresses.isEmpty())) {
            alternateAddresses = getStrategy().getAlternateAddresses(exchange);
            if (alternateAddresses != null) {
                invocation.setAlternateAddresses(alternateAddresses);
            } else {
                invocation.setAlternateEndpoints(
                    getStrategy().getAlternateEndpoints(exchange));
            }
        }

        Endpoint distributionTarget = null;
        if ((alternateAddresses != null) && !alternateAddresses.isEmpty()) {
            String alternateAddress =
                getStrategy().selectAlternateAddress(alternateAddresses);
            if (alternateAddress != null) {
                // re-use current endpoint
                distributionTarget = getEndpoint();
                distributionTarget.getEndpointInfo().setAddress(alternateAddress);
            }
        } else {
            distributionTarget = getStrategy().selectAlternateEndpoint(
                                 invocation.getAlternateEndpoints());
        }
        return distributionTarget;
    }

    @Override
    protected boolean requiresFailover(Exchange exchange, Exception ex) {
        return failover && super.requiresFailover(exchange, ex);
    }

}
