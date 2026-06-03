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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for CXF-9213: RetryStrategy per-invocation isolation via
 * {@link PerInvocationFailoverStrategy#newStrategy()}.
 */
public class RetryStrategyTest {

    private static RetryStrategy strategyWith(int maxRetries) {
        RetryStrategy s = new RetryStrategy();
        s.setMaxNumberOfRetries(maxRetries);
        return s;
    }

    // -----------------------------------------------------------------------
    // Basic counter behaviour on a single instance
    // -----------------------------------------------------------------------

    @Test
    public void testRetriesExactlyMaxTimes() {
        RetryStrategy s = strategyWith(3);
        assertTrue("attempt 1", s.stillTheSameAddress());
        assertTrue("attempt 2", s.stillTheSameAddress());
        assertTrue("attempt 3", s.stillTheSameAddress());
        assertFalse("attempt 4 must exhaust", s.stillTheSameAddress());
    }

    @Test
    public void testMaxRetriesZeroAlwaysReturnsSameAddress() {
        RetryStrategy s = strategyWith(0);
        for (int i = 0; i < 100; i++) {
            assertTrue(s.stillTheSameAddress());
        }
    }

    @Test
    public void testCounterResetsAfterExhaustion() {
        RetryStrategy s = strategyWith(2);
        s.stillTheSameAddress(); // 1 – true
        s.stillTheSameAddress(); // 2 – true
        assertFalse(s.stillTheSameAddress()); // exhausted, resets to 0
        assertTrue(s.stillTheSameAddress());  // new cycle starts
    }

    // -----------------------------------------------------------------------
    // PerInvocationFailoverStrategy contract
    // -----------------------------------------------------------------------

    @Test
    public void testImplementsPerInvocationFailoverStrategy() {
        assertTrue(new RetryStrategy() instanceof PerInvocationFailoverStrategy);
    }

    @Test
    public void testNewStrategyReturnsDistinctInstance() {
        RetryStrategy template = strategyWith(3);
        FailoverStrategy s1 = template.newStrategy();
        FailoverStrategy s2 = template.newStrategy();
        assertNotSame(template, s1);
        assertNotSame(s1, s2);
    }

    @Test
    public void testNewStrategyInheritsMaxRetries() {
        RetryStrategy template = strategyWith(5);
        RetryStrategy copy = (RetryStrategy) template.newStrategy();
        assertEquals(5, copy.getMaxNumberOfRetries());
    }

    @Test
    public void testNewStrategyCopiesAlternateAddresses() {
        RetryStrategy template = strategyWith(2);
        template.setAlternateAddresses(Arrays.asList("http://a", "http://b"));
        RetryStrategy copy = (RetryStrategy) template.newStrategy();
        assertEquals(Arrays.asList("http://a", "http://b"), copy.getAlternateAddresses(null));
    }

    @Test
    public void testNewStrategyHasFreshCounter() {
        RetryStrategy template = strategyWith(3);
        // advance the template's own counter
        template.stillTheSameAddress();
        template.stillTheSameAddress();

        // a new instance must start from zero regardless
        RetryStrategy copy = (RetryStrategy) template.newStrategy();
        assertTrue("copy attempt 1", copy.stillTheSameAddress());
        assertTrue("copy attempt 2", copy.stillTheSameAddress());
        assertTrue("copy attempt 3", copy.stillTheSameAddress());
        assertFalse("copy attempt 4 must exhaust", copy.stillTheSameAddress());
    }

    @Test
    public void testTwoInstancesAreIndependent() {
        RetryStrategy template = strategyWith(3);
        RetryStrategy i1 = (RetryStrategy) template.newStrategy();
        RetryStrategy i2 = (RetryStrategy) template.newStrategy();

        // advance i1 without exhausting it
        i1.stillTheSameAddress();
        i1.stillTheSameAddress();

        // i2 must still be at zero
        assertTrue("i2 attempt 1", i2.stillTheSameAddress());
        assertTrue("i2 attempt 2", i2.stillTheSameAddress());
        assertTrue("i2 attempt 3", i2.stillTheSameAddress());
        assertFalse("i2 attempt 4 must exhaust", i2.stillTheSameAddress());
    }

    // -----------------------------------------------------------------------
    // Concurrency: concurrent newStrategy() calls on a shared template
    // -----------------------------------------------------------------------

    @Test
    public void testConcurrentPerInvocationInstancesAreIndependent() throws InterruptedException {
        final int maxRetries = 4;
        RetryStrategy template = strategyWith(maxRetries);
        AtomicInteger successes1 = new AtomicInteger();
        AtomicInteger successes2 = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        for (AtomicInteger counter : new AtomicInteger[]{successes1, successes2}) {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            pool.submit(() -> {
                try {
                    start.await();
                    // Simulates FailoverTargetSelector calling newStrategy() per invocation.
                    RetryStrategy instance = (RetryStrategy) template.newStrategy();
                    for (int i = 0; i < maxRetries; i++) {
                        if (instance.stillTheSameAddress()) {
                            counter.incrementAndGet();
                        }
                    }
                    assertFalse("instance must be exhausted", instance.stillTheSameAddress());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            pool.shutdown();
        }

        start.countDown();
        done.await();
        assertEquals("invocation 1 retry count", maxRetries, successes1.get());
        assertEquals("invocation 2 retry count", maxRetries, successes2.get());
    }
}
