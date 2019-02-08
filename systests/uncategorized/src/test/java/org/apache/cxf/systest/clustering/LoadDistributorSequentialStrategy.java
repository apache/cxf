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

package org.apache.cxf.systest.clustering;

import java.util.List;

import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;

public class LoadDistributorSequentialStrategy extends SequentialStrategy {

    @Override
    public List<Endpoint> getAlternateEndpoints(Exchange exchange) {

        // Get the list of endpoints, including the current one.
        // This part is required for most FailoverStrategys that provide alternate
        // target endpoints for the LoadDistributorTargetSelector.
        List<Endpoint> alternateEndpoints = getEndpoints(exchange, true);

        // Put the original endpoint at the head of the list
        // This is only required if the client wants to always try one endpoint first,
        // which is not typically desired for a load distributed configuration
        // (but is required by one of the unit tests)
        Endpoint endpoint = exchange.getEndpoint();
        String defaultAddress = endpoint.getEndpointInfo().getAddress();
        for (Endpoint alternate : alternateEndpoints) {
            if (defaultAddress.equals(alternate.getEndpointInfo().getAddress())) {
                alternateEndpoints.remove(alternate);
                alternateEndpoints.add(0, alternate);
                break;
            }
        }

        return alternateEndpoints;
    }
}
