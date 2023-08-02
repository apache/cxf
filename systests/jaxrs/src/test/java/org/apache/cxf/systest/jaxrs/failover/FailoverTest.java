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
import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.Test;


/**
 * Tests failover within a static cluster.
 */
public class FailoverTest extends AbstractFailoverTest {
    @Test
    public void testSequentialStrategyWithCustomTargetSelector() throws Exception {
        FailoverFeature feature = getCustomFeature(true, false, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest("resolver://info", feature, Server.ADDRESS3, null, false, false, false);
    }

    @Test
    public void testSequentialStrategyWithCustomTargetSelector2() throws Exception {
        FailoverFeature feature = getCustomFeature(true, false, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest("resolver://info", feature, Server.ADDRESS3, null, false, false, true);
    }

    @Override
    protected FailoverFeature getFeature(boolean random, String... address) {
        return getCustomFeature(false, random, address);
    }

    private FailoverFeature getCustomFeature(boolean custom, boolean random, String ...address) {
        FailoverFeature feature = new FailoverFeature();
        List<String> alternateAddresses = new ArrayList<>();
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
        if (custom) {
            FailoverTargetSelector selector = new ReplaceInitialAddressSelector();
            feature.setTargetSelector(selector);
        }

        return feature;
    }

    private static final class ReplaceInitialAddressSelector extends FailoverTargetSelector {
        @Override
        public synchronized void prepare(Message message) {
            EndpointInfo ei = getEndpoint().getEndpointInfo();
            ei.setAddress(Server.ADDRESS3);
            message.put(Message.ENDPOINT_ADDRESS, Server.ADDRESS3);
            super.prepare(message);
        }

        @Override
        protected boolean requiresFailover(Exchange exchange, Exception ex) {
            return false;
        }
    }
}
