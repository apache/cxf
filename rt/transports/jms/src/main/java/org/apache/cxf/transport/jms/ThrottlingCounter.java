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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.transport.jms.continuations.Counter;
import org.apache.cxf.transport.jms.util.JMSListenerContainer;

/**
 * Counter that throttles a jms listener on a high and low water mark.
 *
 * When the counter reaches the high watermark the listener will be stopped.
 * When the counter reaches the low watermark the listener will be started.
 */
public class ThrottlingCounter implements Counter {

    private AtomicInteger counter;
    private final int lowWatermark;
    private final int highWatermark;
    private JMSListenerContainer listenerContainer;

    public ThrottlingCounter(int lowWatermark, int highWatermark) {
        this.counter = new AtomicInteger();
        this.lowWatermark = lowWatermark;
        this.highWatermark = highWatermark;
    }

    public void setListenerContainer(JMSListenerContainer listenerContainer) {
        this.listenerContainer = listenerContainer;
    }

    public final int incrementAndGet() {
        int curCounter = counter.incrementAndGet();
        if (listenerContainer != null && highWatermark >= 0
            && curCounter >= highWatermark && listenerContainer.isRunning()) {
            listenerContainer.stop();
        }
        return curCounter;
    }

    public final int decrementAndGet() {
        int curCounter = counter.decrementAndGet();
        if (listenerContainer != null && curCounter <= lowWatermark && !listenerContainer.isRunning()) {
            listenerContainer.start();
        }
        return curCounter;
    }

}