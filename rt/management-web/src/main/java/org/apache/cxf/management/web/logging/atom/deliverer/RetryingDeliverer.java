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
package org.apache.cxf.management.web.logging.atom.deliverer;

import java.util.Calendar;
import java.util.Date;

import org.apache.abdera.model.Element;
import org.apache.commons.lang.Validate;

/**
 * Wrapper on other deliverer retrying delivery in case of failure. Delivery attempts repeat in loop with some
 * pause time between retries until successful delivery or exceeding time limit. Time delay between delivery
 * is configurable strategy. Two predefined strategies are given: each time pause same amount of time (linear)
 * and each next time pause time doubles (exponential).
 */
public final class RetryingDeliverer implements Deliverer {

    private Deliverer deliverer;
    private PauseCalculator pauser;
    private int timeout;

    /**
     * Creates retrying deliverer with predefined retry strategy.
     * 
     * @param worker real deliverer used to push data out.
     * @param timeout maximum time range (in seconds) that retrial is continued; time spent on delivery call
     *            is included. No timeout (infinite loop) if set to zero.
     * @param pause time of pause (in seconds) greater than zero.
     * @param linear if true linear strategy (each time pause same amount of time), exponential otherwise
     *            (each next time pause time doubles).
     */
    public RetryingDeliverer(Deliverer worker, int timeout, int pause, boolean linear) {
        Validate.notNull(worker, "worker is null");
        Validate.isTrue(timeout >= 0, "timeout is negative");
        Validate.isTrue(pause > 0, "pause is not greater than zero");
        deliverer = worker;
        this.timeout = timeout;
        this.pauser = linear ? new ConstantPause(pause) : new ExponentialPause(pause);
    }

    /**
     * Creates retrying deliverer with custom retry strategy.
     * 
     * @param worker real deliverer used to push data out.
     * @param timeout maximum time range (in seconds) that retrial is continued; time spent on delivery call
     *            is included. No timeout (infinite loop) if set to zero.
     * @param strategy custom retry pausing strategy.
     */
    public RetryingDeliverer(Deliverer worker, int timeout, PauseCalculator strategy) {
        Validate.notNull(worker, "worker is null");
        Validate.notNull(strategy, "strategy is null");
        Validate.isTrue(timeout >= 0, "timeout is negative");
        deliverer = worker;
        pauser = strategy;
        this.timeout = timeout;
    }

    public boolean deliver(Element element) throws InterruptedException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, timeout);
        Date timeoutDate = cal.getTime();
        while (!deliverer.deliver(element)) {
            int sleep = pauser.nextPause();
            cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, sleep);
            if (timeout == 0 || timeoutDate.after(cal.getTime())) {
                Thread.sleep((long)sleep * 1000L);
            } else {
                pauser.reset();
                return false;
            }
        }
        pauser.reset();
        return true;
    }

    /** Calculates time of subsequent pauses between delivery attempts. */
    public interface PauseCalculator {

        /** Time of next pause (in seconds). */
        int nextPause();

        /** Restarts calculation. */
        void reset();
    }

    private static class ConstantPause implements PauseCalculator {
        private int pause;

        ConstantPause(int pause) {
            this.pause = pause;
        }

        public int nextPause() {
            return pause;
        }

        public void reset() {
        }
    }

    private static class ExponentialPause implements PauseCalculator {
        private int pause;
        private int current;

        ExponentialPause(int pause) {
            this.pause = pause;
            current = pause;
        }

        public int nextPause() {
            int c = current;
            current *= 2;
            return c;
        }

        public void reset() {
            current = pause;
        }
    }

    public String getEndpointAddress() {
        return deliverer.getEndpointAddress();
    }

}
