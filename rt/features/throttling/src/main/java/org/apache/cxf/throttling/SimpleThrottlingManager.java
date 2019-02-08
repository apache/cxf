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
package org.apache.cxf.throttling;

import java.util.Collections;
import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * Suspends or aborts the requests if the threshold has been reached
 */
public class SimpleThrottlingManager extends ThrottleResponse implements ThrottlingManager {
    private static final String THROTTLED_KEY = "THROTTLED";

    private int threshold;
    private ThrottlingCounter counter = new ThrottlingCounter();

    @Override
    public List<String> getDecisionPhases() {
        return Collections.singletonList(Phase.PRE_STREAM);
    }

    @Override
    public ThrottleResponse getThrottleResponse(String phase, Message m) {
        if (m.containsKey(THROTTLED_KEY)) {
            return null;
        }
        m.getExchange().put(ThrottlingCounter.class, counter);
        if (counter.incrementAndGet() >= threshold) {
            m.put(THROTTLED_KEY, true);
            return this;
        }
        return null;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

}
