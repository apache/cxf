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

package org.apache.cxf.systest.jaxws;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Regression test for CXF-9234.
 *
 * Concurrent DynamicClientFactory.createClient() calls for the same WSDL share a single
 * cached schema DOM (WSDLManagerImpl#schemaCacheMap). DynamicClientFactory#removeImportElement
 * deep-clones that shared DOM via the native Node.cloneNode(true), which fires
 * StaxUtils$LocationUserDataHandler callbacks that mutate the source document's internal
 * userData table. That table (WeakHashMap in Xerces-J, HashMap in the JDK DOM impl) is not
 * thread-safe, so concurrent clones used to corrupt it and spin forever. This test drives many
 * threads through createClient() for the same, already-cached WSDL/schema at once.
 */
public class DynamicClientConcurrencyTest {

    private static final int THREAD_COUNT = 16;
    private static final int ITERATIONS_PER_THREAD = 8;

    private Bus bus;

    @Before
    public void setUp() {
        bus = new SpringBusFactory().createBus();
    }

    @After
    public void tearDown() {
        bus.shutdown(true);
    }

    @Test(timeout = 90000)
    public void testConcurrentCreateClientDoesNotHang() throws Exception {
        final URL wsdlUrl = getClass().getClassLoader().getResource("wsdl_systest_jaxws/cxf8979.wsdl");
        assertNotNull("test wsdl not found on classpath", wsdlUrl);

        // Warm the WSDLManager cache once, single-threaded, so every worker below races on
        // the exact same cached Definition/SchemaInfo/DOM instance.
        JaxWsDynamicClientFactory.newInstance(bus).createClient(wsdlUrl).destroy();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger failures = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                // separate factory instance per thread, but same shared Bus/WSDLManager cache
                JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance(bus);
                startLatch.await();
                for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                    try {
                        Client client = dcf.createClient(wsdlUrl);
                        client.destroy();
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }
        startLatch.countDown();

        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        assertEquals("no concurrent createClient() invocation should have failed", 0, failures.get());
    }
}
