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

package org.apache.cxf.ws.rm;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class SequenceMonitor {

    private static final long DEFAULT_MONITOR_INTERVAL = 60000L;
    private static final Logger LOG = LogUtils.getL7dLogger(SequenceMonitor.class);
    private long monitorInterval = DEFAULT_MONITOR_INTERVAL;
    private long firstCheck;
    private final Deque<Long> receiveTimes = new ConcurrentLinkedDeque<>();

    public void acknowledgeMessage() {
        long now = System.currentTimeMillis();
        if (0L == firstCheck) {
            firstCheck = now + monitorInterval;
        }
        receiveTimes.add(now);
    }

    public int getMPM() {
        long now = System.currentTimeMillis();
        int mpm = 0;
        if (firstCheck > 0L && now >= firstCheck) {
            long threshold = now - monitorInterval;
            while (!receiveTimes.isEmpty()) {
                if (receiveTimes.getFirst() <= threshold) {
                    receiveTimes.removeFirst();
                } else {
                    break;
                }
            }
            mpm = receiveTimes.size();
        }

        return mpm;
    }

    public long getLastArrivalTime() {
        return !receiveTimes.isEmpty() ? receiveTimes.getLast() : 0L;
    }

    protected void setMonitorInterval(long i) {
        if (receiveTimes.isEmpty()) {
            firstCheck = 0L;
            monitorInterval = i;
        } else {
            LOG.warning("Cannot change monitor interval at this point.");
        }
    }
}
