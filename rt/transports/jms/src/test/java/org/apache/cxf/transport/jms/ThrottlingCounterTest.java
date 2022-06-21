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
package org.apache.cxf.transport.jms;

import jakarta.jms.Connection;
import org.apache.cxf.transport.jms.util.JMSListenerContainer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThrottlingCounterTest {

    @Test
    public void testThrottleWithJmsStartAndStop() {
        JMSListenerContainer listenerContainer = new DummyJMSListener();

        ThrottlingCounter counter = new ThrottlingCounter(0, 1);
        counter.setListenerContainer(listenerContainer);
        assertTrue(listenerContainer.isRunning());

        counter.incrementAndGet();
        assertFalse(listenerContainer.isRunning());

        counter.decrementAndGet();
        assertTrue(listenerContainer.isRunning());

    }

    public class DummyJMSListener implements JMSListenerContainer {
        boolean running = true;

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void shutdown() {
        }

        public Connection getConnection() {
            return null;
        }
    }

}
