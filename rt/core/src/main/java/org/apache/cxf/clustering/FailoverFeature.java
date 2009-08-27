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

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.AbstractFeature;

/**
 * This feature may be applied to a Client so as to enable
 * failover from the initial target endpoint to any other
 * compatible endpoint for the target service.
 */
@NoJSR250Annotations
public class FailoverFeature extends AbstractFeature {

    private FailoverStrategy failoverStrategy;
    
    @Override
    public void initialize(Client client, Bus bus) {
        FailoverTargetSelector selector =
            new FailoverTargetSelector();
        selector.setEndpoint(client.getEndpoint());
        selector.setStrategy(getStrategy());
        client.setConduitSelector(selector);
    }

    public void setStrategy(FailoverStrategy strategy) {
        failoverStrategy = strategy;
    }
    
    public FailoverStrategy getStrategy()  {
        return failoverStrategy;
    }
}
