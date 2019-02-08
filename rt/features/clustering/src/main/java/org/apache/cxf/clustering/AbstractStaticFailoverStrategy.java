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
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * Failover strategy based on a static cluster represented by
 * multiple endpoints associated with the same service instance.
 */
public abstract class AbstractStaticFailoverStrategy implements FailoverStrategy {
    private static final Logger LOG =
        LogUtils.getL7dLogger(AbstractStaticFailoverStrategy.class);

    private List<String> alternateAddresses;
    private long delayBetweenRetries;

    public void setDelayBetweenRetries(long delay) {
        this.delayBetweenRetries = delay;
    }

    public long getDelayBetweenRetries() {
        return this.delayBetweenRetries;
    }

    public void setAlternateAddresses(List<String> alternateAddresses) {
        this.alternateAddresses = alternateAddresses;
    }

    /**
     * Get the alternate addresses for this invocation.
     *
     * @param exchange the current Exchange
     * @return a List of alternate addresses if available
     */
    public List<String> getAlternateAddresses(Exchange exchange) {
        return alternateAddresses != null
               ? new ArrayList<>(alternateAddresses)
               : null;
    }

    /**
     * Select one of the alternate addresses for a retried invocation.
     *
     * @param a List of alternate addresses if available
     * @return the selected address
     */
    public String selectAlternateAddress(List<String> alternates) {
        String selected = null;
        if (alternates != null && !alternates.isEmpty()) {
            selected = getNextAlternate(alternates);
            Level level = getLogLevel();
            if (LOG.isLoggable(level)) {
                LOG.log(level,
                        "FAILING_OVER_TO_ADDRESS_OVERRIDE",
                        selected);
            }
        } else {
            LOG.warning("NO_ALTERNATE_TARGETS_REMAIN");
        }
        return selected;
    }

    /**
     * Get the alternate endpoints for this invocation.
     *
     * @param exchange the current Exchange
     * @return a List of alternate endpoints if available
     */
    public List<Endpoint> getAlternateEndpoints(Exchange exchange) {
        return getEndpoints(exchange, false);
    }

    /**
     * Select one of the alternate endpoints for a retried invocation.
     *
     * @param a List of alternate endpoints if available
     * @return the selected endpoint
     */
    public Endpoint selectAlternateEndpoint(List<Endpoint> alternates) {
        Endpoint selected = null;
        if (alternates != null && !alternates.isEmpty()) {
            selected = getNextAlternate(alternates);
            Level level = getLogLevel();
            if (LOG.isLoggable(level)) {
                LOG.log(level,
                        "FAILING_OVER_TO_ALTERNATE_ENDPOINT",
                         new Object[] {selected.getEndpointInfo().getName(),
                                       selected.getEndpointInfo().getAddress()});
            }
        } else {
            LOG.warning("NO_ALTERNATE_TARGETS_REMAIN");
        }
        return selected;
    }

    /**
     * Get the endpoints for this invocation.
     *
     * @param exchange the current Exchange
     * @param acceptCandidatesWithSameAddress true to accept candidates with the same address
     * @return a List of alternate endpoints if available
     */
    protected List<Endpoint> getEndpoints(Exchange exchange, boolean acceptCandidatesWithSameAddress) {
        Endpoint endpoint = exchange.getEndpoint();
        Collection<ServiceInfo> services = endpoint.getService().getServiceInfos();
        QName currentBinding = endpoint.getBinding().getBindingInfo().getName();
        List<Endpoint> alternates = new ArrayList<>();
        for (ServiceInfo service : services) {
            Collection<EndpointInfo> candidates = service.getEndpoints();
            for (EndpointInfo candidate : candidates) {
                QName candidateBinding = candidate.getBinding().getName();
                if (candidateBinding.equals(currentBinding)) {
                    if (acceptCandidatesWithSameAddress || !candidate.getAddress().equals(
                             endpoint.getEndpointInfo().getAddress())) {
                        Endpoint alternate =
                            endpoint.getService().getEndpoints().get(candidate.getName());
                        if (alternate != null) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.log(Level.FINE,
                                        "FAILOVER_CANDIDATE_ACCEPTED",
                                        candidate.getName());
                            }
                            alternates.add(alternate);
                        }
                    }
                } else if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE,
                            "FAILOVER_CANDIDATE_REJECTED",
                            new Object[] {candidate.getName(), candidateBinding});
                }
            }
        }
        return alternates;
    }

    /**
     * Get next alternate endpoint.
     *
     * @param alternates non-empty List of alternate endpoints
     * @return
     */
    protected abstract <T> T getNextAlternate(List<T> alternates);

    /**
     * Get the log level for reporting the selection of the new alternative address or endpoint
     * @return the log level
     */
    protected Level getLogLevel() {
        return Level.WARNING;
    }
}
