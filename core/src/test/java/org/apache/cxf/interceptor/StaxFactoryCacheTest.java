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
package org.apache.cxf.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StaxFactoryCacheTest {

    public static class CountingInputFactory extends WstxInputFactory {
        static final AtomicInteger CREATED = new AtomicInteger();

        public CountingInputFactory() {
            CREATED.incrementAndGet();
        }
    }

    public static class CountingOutputFactory extends WstxOutputFactory {
        static final AtomicInteger CREATED = new AtomicInteger();

        public CountingOutputFactory() {
            CREATED.incrementAndGet();
        }
    }

    private static Message message(String property, Object value) {
        Message m = new MessageImpl();
        m.put(property, value);
        return m;
    }

    @Test
    public void testInputFactoryFromClassNameIsCached() throws Exception {
        String name = WstxInputFactory.class.getName();
        XMLInputFactory first =
            StaxInInterceptor.getXMLInputFactory(message(XMLInputFactory.class.getName(), name));
        XMLInputFactory second =
            StaxInInterceptor.getXMLInputFactory(message(XMLInputFactory.class.getName(), name));
        assertTrue(first instanceof WstxInputFactory);
        assertSame(first, second);
    }

    @Test
    public void testOutputFactoryFromClassIsCached() throws Exception {
        Message m1 = message(XMLOutputFactory.class.getName(), WstxOutputFactory.class);
        XMLOutputFactory first = StaxOutInterceptor.getXMLOutputFactory(m1);
        XMLOutputFactory second =
            StaxOutInterceptor.getXMLOutputFactory(message(XMLOutputFactory.class.getName(),
                                                           WstxOutputFactory.class));
        assertTrue(first instanceof WstxOutputFactory);
        assertSame(first, second);
        assertEquals(Boolean.TRUE, m1.get(StaxOutInterceptor.FORCE_START_DOCUMENT));
    }

    @Test
    public void testNonInstantiableFactoryFailsEveryTime() throws Exception {
        // XMLInputFactory cannot be instantiated, so construction fails and nothing must be cached
        for (int i = 0; i < 2; i++) {
            try {
                StaxInInterceptor.getXMLInputFactory(message(XMLInputFactory.class.getName(),
                                                             XMLInputFactory.class));
                fail("Expected a Fault");
            } catch (Fault expected) {
                assertTrue(expected.getCause() instanceof ReflectiveOperationException);
            }
        }
    }

    @Test
    public void testConcurrentFirstAccessCreatesOneFactory() throws Exception {
        assertEquals(1, concurrentlyCreate(() -> StaxInInterceptor.getXMLInputFactory(
            message(XMLInputFactory.class.getName(), CountingInputFactory.class))));
        assertEquals(1, CountingInputFactory.CREATED.get());

        assertEquals(1, concurrentlyCreate(() -> StaxOutInterceptor.getXMLOutputFactory(
            message(XMLOutputFactory.class.getName(), CountingOutputFactory.class))));
        assertEquals(1, CountingOutputFactory.CREATED.get());
    }

    /**
     * Calls the given task from several threads at once and returns the number of distinct
     * instances returned.
     */
    private static int concurrentlyCreate(Callable<Object> task) throws Exception {
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Object> distinct = new ArrayList<>();
            for (Future<Object> f : results) {
                Object o = f.get();
                if (distinct.stream().noneMatch(d -> d == o)) {
                    distinct.add(o);
                }
            }
            return distinct.size();
        } finally {
            executor.shutdownNow();
        }
    }
}
