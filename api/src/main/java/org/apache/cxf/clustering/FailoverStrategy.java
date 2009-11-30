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

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;

/**
 * Supports pluggable strategies for alternate endpoint selection on
 * failover.
 */
public interface FailoverStrategy {
    /**
     * Get the alternate endpoints for this invocation.
     * 
     * @param exchange the current Exchange     
     * @return a failover endpoint if one is available
     */
    List<Endpoint> getAlternateEndpoints(Exchange exchange);
    
    /**
     * Select one of the alternate endpoints for a retried invocation.
     * 
     * @param alternates List of alternate endpoints if available
     * @return the selected endpoint
     */
    Endpoint selectAlternateEndpoint(List<Endpoint> alternates);

    /**
     * Get the alternate addresses for this invocation.
     * These addresses over-ride any addresses specified in the WSDL.
     * 
     * @param exchange the current Exchange     
     * @return a failover endpoint if one is available
     */
    List<String> getAlternateAddresses(Exchange exchange);

    /**
     * Select one of the alternate addresses for a retried invocation.
     * 
     * @param alternates List of alternate addresses if available
     * @return the selected address
     */
    String selectAlternateAddress(List<String> addresses);
}
