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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.ProcessingException;
import org.apache.cxf.clustering.FailoverFailedException;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.clustering.circuitbreaker.CircuitBreakerFailoverFeature;
import org.apache.cxf.systest.jaxrs.BookStore;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Tests failover within a static cluster.
 */
public class CircuitBreakerFailoverTest extends AbstractFailoverTest {
    public static final String NON_PORT = allocatePort(CircuitBreakerFailoverTest.class);


    @Test(expected = FailoverFailedException.class)
    public void testSequentialStrategyUnavailableAlternatives() throws Exception {
        FailoverFeature feature = getFeature(false,
            "http://localhost:" + NON_PORT + "/non-existent",
            "http://localhost:" + NON_PORT + "/non-existent2");

        final BookStore bookStore = getBookStore(
            "http://localhost:" + NON_PORT + "/non-existent", feature);

        // First iteration is going to open all circuit breakers.
        // Second iteration should not call any URL as all targets are not available.
        for (int i = 0; i < 2; ++i) {
            try {
                bookStore.getBook(1);
                fail("Exception expected");
            } catch (ProcessingException ex) {
                if (ex.getCause() instanceof FailoverFailedException) {
                    throw (FailoverFailedException) ex.getCause();
                }
            }
        }
    }

    @Test
    public void testSequentialStrategyWithElapsingCircuitBreakerTimeout() throws Throwable {
        FailoverFeature feature = customizeFeature(
            new CircuitBreakerFailoverFeature(1, 3000), false,
            "http://localhost:" + NON_PORT + "/non-existent",
            "http://localhost:" + NON_PORT + "/non-existent2");

        final BookStore bookStore = getBookStore(
            "http://localhost:" + NON_PORT + "/non-existent", feature);

        // First iteration is going to open all circuit breakers. The timeout at the end
        // should reset all circuit breakers and the URLs could be tried again.
        for (int i = 0; i < 2; ++i) {
            try {
                bookStore.getBook(1);
                fail("Exception expected");
            } catch (ProcessingException ex) {
                if (!(ex.getCause() instanceof IOException)) {
                    throw ex.getCause();
                }
            }

            // Let's wait a bit more than circuit breaker timeout
            Thread.sleep(4000);
        }
    }

    @Test
    public void testSequentialStrategyWithRetry() throws Exception {
        FailoverFeature feature = getFeature(false,
            "http://localhost:" + NON_PORT + "/non-existent",
            Server.ADDRESS2);

        strategyTest("http://localhost:" + NON_PORT + "/non-existent", feature,
            Server.ADDRESS2, null, false, false, false);
    }

    @Override
    protected FailoverFeature getFeature(boolean random, String ...address) {
        return customizeFeature(new CircuitBreakerFailoverFeature(), random, address);
    }

    private FailoverFeature customizeFeature(CircuitBreakerFailoverFeature feature,
            boolean random, String ...address) {
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

        return feature;
    }
}
