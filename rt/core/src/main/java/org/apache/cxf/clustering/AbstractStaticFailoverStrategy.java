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
    
    /**
     * Get the alternate endpoints for this invocation.
     * 
     * @param exchange the current Exchange
     * @return a List of alternate endpoints if available
     */
    public List<Endpoint> getAlternateEndpoints(Exchange exchange) {
        Endpoint endpoint = exchange.get(Endpoint.class);
        Collection<ServiceInfo> services = endpoint.getService().getServiceInfos();
        QName currentBinding = endpoint.getBinding().getBindingInfo().getName();
        List<Endpoint> alternates = new ArrayList<Endpoint>();
        for (ServiceInfo service : services) {
            Collection<EndpointInfo> candidates = service.getEndpoints();
            for (EndpointInfo candidate : candidates) {
                QName candidateBinding = candidate.getBinding().getName();
                if (candidateBinding.equals(currentBinding)) {
                    if (!candidate.getAddress().equals(
                             endpoint.getEndpointInfo().getAddress())) {
                        Endpoint alternate =
                            endpoint.getService().getEndpoints().get(candidate.getName());
                        if (alternate != null) {
                            LOG.log(Level.INFO,
                                    "FAILOVER_CANDIDATE_ACCEPTED",
                                    candidate.getName());
                            alternates.add(alternate);
                        }
                    }
                } else {
                    LOG.log(Level.INFO,
                            "FAILOVER_CANDIDATE_REJECTED",
                            new Object[] {candidate.getName(), candidateBinding});
                }
            }
        }
        return alternates;
    }

    /**
     * Select one of the alternate endpoints for a retried invocation.
     * 
     * @param a List of alternate endpoints if available
     * @return the selected endpoint
     */
    public Endpoint selectAlternateEndpoint(List<Endpoint> alternates) {
        Endpoint selected = null;
        if (alternates != null && alternates.size() > 0) {
            selected = getNextAlternate(alternates);
            LOG.log(Level.WARNING,
                    "FAILING_OVER_TO",
                     new Object[] {selected.getEndpointInfo().getName(),
                                   selected.getEndpointInfo().getAddress()});
        } else {
            LOG.warning("NO_ALTERNATE_TARGETS_REMAIN");
        }
        return selected;
    }
    
    /**
     * Get next alternate endpoint.
     * 
     * @param alternates non-empty List of alternate endpoints 
     * @return
     */
    protected abstract Endpoint getNextAlternate(List<Endpoint> alternates);
}
