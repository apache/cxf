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

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.clustering.LoadDistributorTargetSelector;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.frontend.ClientProxy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LoadDistributorAddressOverrideTest  extends FailoverAddressOverrideTest {
    private static final String FAILOVER_CONFIG =
        "org/apache/cxf/systest/clustering/load_distributor_address_override.xml";

    @Override
    protected String getConfig() {
        return FAILOVER_CONFIG;
    }

    private String responseFrom(String response) {
        if (response.endsWith(REPLICA_A)) {
            return REPLICA_A;
        } else if (response.endsWith(REPLICA_B)) {
            return REPLICA_B;
        } else if (response.endsWith(REPLICA_C)) {
            return REPLICA_C;
        } else {
            return response;
        }
    }

    private void incrementResponseCount(Map< String, Integer > responseCounts,
            String response) {
        String responder = responseFrom(response);
        Integer currentCount = responseCounts.get(responder);
        responseCounts.put(responder, 1 + (currentCount == null ? 0 : currentCount));
    }

    @Test
    public void testDistributedSequentialStrategy() throws Exception {
        startTarget(REPLICA_A);
        startTarget(REPLICA_B);
        startTarget(REPLICA_C);
        setupGreeterA();
        verifyStrategy(greeter, SequentialStrategy.class, 3);
        Map< String, Integer > responseCounts = new HashMap<>();
        for (int i = 0; i < 12; ++i) {
            String response = greeter.greetMe("fred");
            assertNotNull("expected non-null response", response);
            incrementResponseCount(responseCounts, response);
        }
        assertEquals(4, (long)responseCounts.get(REPLICA_A));
        assertEquals(4, (long)responseCounts.get(REPLICA_B));
        assertEquals(4, (long)responseCounts.get(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);
        stopTarget(REPLICA_A);
        stopTarget(REPLICA_B);
        stopTarget(REPLICA_C);
    }

    @Test
    public void testDistributedSequentialStrategyWithFailover() throws Exception {
        startTarget(REPLICA_A);
        startTarget(REPLICA_C);
        setupGreeterA();
        verifyStrategy(greeter, SequentialStrategy.class, 3);
        Map< String, Integer > responseCounts = new HashMap<>();
        for (int i = 0; i < 12; ++i) {
            String response = greeter.greetMe("fred");
            assertNotNull("expected non-null response", response);
            incrementResponseCount(responseCounts, response);
        }
        // Note that when failover occurs the address list is requeried
        // so SequentialStrategy it starts again from the beginning
        // (the failed address is removed from the returned list, if it's there).
        assertEquals(8, (long)responseCounts.get(REPLICA_A));
        assertEquals(null, responseCounts.get(REPLICA_B));
        assertEquals(4, (long)responseCounts.get(REPLICA_C));
        verifyCurrentEndpoint(REPLICA_C);
        stopTarget(REPLICA_A);
        stopTarget(REPLICA_C);
    }

    @Test
    public void testDistributedSequentialStrategyWithoutFailover() throws Exception {
        startTarget(REPLICA_A);
        startTarget(REPLICA_C);
        setupGreeterA();
        verifyStrategy(greeter, SequentialStrategy.class, 3);
        ConduitSelector conduitSelector =
            ClientProxy.getClient(greeter).getConduitSelector();
        if (conduitSelector instanceof LoadDistributorTargetSelector) {
            ((LoadDistributorTargetSelector)conduitSelector).setFailover(false);
        } else {
            fail("unexpected conduit selector: " + conduitSelector);
        }

        Map< String, Integer > responseCounts = new HashMap<>();
        for (int i = 0; i < 12; ++i) {
            try {
                String response = greeter.greetMe("fred");
                assertNotNull("expected non-null response", response);
                incrementResponseCount(responseCounts, response);
            } catch (WebServiceException ex) {
                incrementResponseCount(responseCounts, "");
            }
        }
        assertEquals(4, (long)responseCounts.get(REPLICA_A));
        assertEquals(null, responseCounts.get(REPLICA_B));
        assertEquals(4, (long)responseCounts.get(REPLICA_C));
        assertEquals(4, (long)responseCounts.get(""));
        verifyCurrentEndpoint(REPLICA_C);
        stopTarget(REPLICA_A);
        stopTarget(REPLICA_C);
    }
}
