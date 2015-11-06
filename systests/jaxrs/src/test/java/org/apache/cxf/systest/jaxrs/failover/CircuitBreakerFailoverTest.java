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

package org.apache.cxf.systest.jaxrs.failover;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.clustering.circuitbreaker.CircuitBreakerFailoverFeature;

/**
 * Tests failover within a static cluster.
 */
public class CircuitBreakerFailoverTest extends AbstractFailoverTest {
    public static final String NON_PORT = allocatePort(CircuitBreakerFailoverTest.class);

    @Override
    protected FailoverFeature getFeature(boolean random, String ...address) {
        CircuitBreakerFailoverFeature feature = new CircuitBreakerFailoverFeature();
        List<String> alternateAddresses = new ArrayList<String>();
        for (String s : address) {
            alternateAddresses.add(s);
        }
        if (!random) {
            SequentialStrategy strategy = new SequentialStrategy();
            strategy.setAlternateAddresses(alternateAddresses);
            feature.setStrategy(strategy);
        } else {
            RandomStrategy strategy = new RandomStrategy();
            strategy.setAlternateAddresses(alternateAddresses);
            feature.setStrategy(strategy);
        }
        
        return feature;
    }
}
