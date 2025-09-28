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
package org.apache.cxf.management.counters;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.ObjectName;

import org.apache.cxf.message.FaultMode;

public class ResponseTimeCounter implements ResponseTimeCounterMBean, Counter {

    private ObjectName objectName;
    private final AtomicInteger invocations = new AtomicInteger();
    private final AtomicInteger checkedApplicationFaults = new AtomicInteger();
    private final AtomicInteger unCheckedApplicationFaults = new AtomicInteger();
    private final AtomicInteger runtimeFaults = new AtomicInteger();
    private final AtomicInteger logicalRuntimeFaults = new AtomicInteger();
    private final AtomicLong totalHandlingTime = new AtomicLong();
    private final ReentrantLock write = new ReentrantLock();
    private final AtomicLong maxHandlingTime = new AtomicLong();
    private final AtomicLong minHandlingTime = new AtomicLong();
    private final AtomicLong averageProcessingTime = new AtomicLong();
    private boolean enabled = true;

    public ResponseTimeCounter(ObjectName on) {
        objectName = on;
    }

    @SuppressWarnings("PMD.UselessPureMethodCall")
    public void  increase(MessageHandlingTimeRecorder mhtr) {
        if (!enabled) {
            return;
        }
        long handlingTime = 0;
        if (mhtr.isOneWay()) {
            // We can count the response time
            if (mhtr.getEndTime() > 0) {
                handlingTime = mhtr.getHandlingTime();
            }
        } else {
            handlingTime = mhtr.getHandlingTime();
        }
        write.lock();
        try {
            FaultMode faultMode = mhtr.getFaultMode();

            invocations.getAndIncrement();
            if (null == faultMode) {
                // no exception occured
            } else {
                switch (faultMode) {
                case CHECKED_APPLICATION_FAULT:
                    checkedApplicationFaults.incrementAndGet();
                    break;
                case LOGICAL_RUNTIME_FAULT:
                    logicalRuntimeFaults.incrementAndGet();
                    break;
                case RUNTIME_FAULT:
                    runtimeFaults.incrementAndGet();
                    break;
                case UNCHECKED_APPLICATION_FAULT:
                    unCheckedApplicationFaults.incrementAndGet();
                    break;
                default:
                    runtimeFaults.incrementAndGet();
                    break;
                }
            }
            totalHandlingTime.addAndGet(handlingTime);
            averageProcessingTime.getAndSet(totalHandlingTime.get() / invocations.get());
        } finally {
            write.unlock();
        }
        updateMax(handlingTime);
        updateMin(handlingTime);
    }

    public void reset() {
        invocations.set(0);
        checkedApplicationFaults.set(0);
        unCheckedApplicationFaults.set(0);
        runtimeFaults.set(0);
        logicalRuntimeFaults.set(0);

        totalHandlingTime.set(0);
        maxHandlingTime.set(0);
        minHandlingTime.set(0);
        averageProcessingTime.set(0);
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Number getAvgResponseTime() {
        return averageProcessingTime.get();
    }

    public Number getMaxResponseTime() {
        return maxHandlingTime.get();
    }

    public Number getMinResponseTime() {
        return minHandlingTime.get();
    }

    public Number getNumInvocations() {
        return invocations.get();
    }

    public Number getNumCheckedApplicationFaults() {
        return checkedApplicationFaults.get();
    }

    public Number getNumLogicalRuntimeFaults() {
        return logicalRuntimeFaults.get();
    }

    public Number getNumRuntimeFaults() {
        return runtimeFaults.get();
    }

    public Number getNumUnCheckedApplicationFaults() {
        return unCheckedApplicationFaults.get();
    }

    public Number getTotalHandlingTime() {
        return totalHandlingTime;
    }

    @Override
    public void enable(boolean value) {
        enabled = value;

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void updateMax(long handleTime) {
        while (true) {
            long current = maxHandlingTime.get();
            if (current >= handleTime) {
                break;
            }
            if (maxHandlingTime.compareAndSet(current, handleTime)) {
                break;
            }
        }
    }

    private void updateMin(long handleTime) {
        while (true) {
            long current = minHandlingTime.get();
            if (current < handleTime && current != 0) {
                break;
            }
            if (minHandlingTime.compareAndSet(current, handleTime)) {
                break;
            }
        }
    }
}
