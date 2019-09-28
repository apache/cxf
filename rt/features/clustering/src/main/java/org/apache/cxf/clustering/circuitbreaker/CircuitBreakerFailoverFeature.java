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

package org.apache.cxf.clustering.circuitbreaker;


import org.apache.cxf.clustering.CircuitBreakerTargetSelector;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.FailoverTargetSelector;

public class CircuitBreakerFailoverFeature extends FailoverFeature {
    public CircuitBreakerFailoverFeature() {
        this(CircuitBreakerTargetSelector.DEFAULT_THESHOLD,
                CircuitBreakerTargetSelector.DEFAULT_TIMEOUT);
    }

    public CircuitBreakerFailoverFeature(String clientBootstrapAddress) {
        this(CircuitBreakerTargetSelector.DEFAULT_THESHOLD,
                CircuitBreakerTargetSelector.DEFAULT_TIMEOUT,
                clientBootstrapAddress);
    }

    public CircuitBreakerFailoverFeature(int threshold, long timeout) {
        super(new Portable(threshold, timeout));
    }

    public CircuitBreakerFailoverFeature(int threshold, long timeout, String clientBootstrapAddress) {
        super(new Portable(threshold, timeout, clientBootstrapAddress));
    }

    @Override
    public FailoverTargetSelector getTargetSelector() {
        return delegate.getTargetSelector();
    }

    @Override
    public void setTargetSelector(FailoverTargetSelector targetSelector) {
        delegate.setTargetSelector(targetSelector);
    }

    public int getThreshold() {
        return Portable.class.cast(delegate).getThreshold();
    }

    public long getTimeout() {
        return Portable.class.cast(delegate).getTimeout();
    }

    public void setThreshold(int threshold) {
        Portable.class.cast(delegate).setThreshold(threshold);
    }

    public void setTimeout(long timeout) {
        Portable.class.cast(delegate).setTimeout(timeout);
    }

    public static class Portable extends FailoverFeature.Portable {
        private int threshold;
        private long timeout;
        private FailoverTargetSelector targetSelector;

        public Portable() {
            this(CircuitBreakerTargetSelector.DEFAULT_THESHOLD,
                    CircuitBreakerTargetSelector.DEFAULT_TIMEOUT);
        }

        public Portable(String clientBootstrapAddress) {
            this(CircuitBreakerTargetSelector.DEFAULT_THESHOLD,
                    CircuitBreakerTargetSelector.DEFAULT_TIMEOUT,
                    clientBootstrapAddress);
        }

        public Portable(int threshold, long timeout) {
            this.threshold = threshold;
            this.timeout = timeout;
        }

        public Portable(int threshold, long timeout, String clientBootstrapAddress) {
            super(clientBootstrapAddress);
            this.threshold = threshold;
            this.timeout = timeout;
        }

        @Override
        public FailoverTargetSelector getTargetSelector() {
            if (this.targetSelector == null) {
                this.targetSelector = new CircuitBreakerTargetSelector(threshold, timeout,
                        super.getClientBootstrapAddress());
            }
            return this.targetSelector;
        }

        @Override
        public void setTargetSelector(FailoverTargetSelector targetSelector) {
            this.targetSelector = targetSelector;
        }

        public int getThreshold() {
            return threshold;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }
}
