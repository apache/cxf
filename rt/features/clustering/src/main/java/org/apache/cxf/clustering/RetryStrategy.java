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

import java.util.List;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;

/**
 * Retry strategy that retries the same endpoint for a configured number of
 * attempts before advancing to the next alternate (CXF-2036).
 *
 * <p>This class implements {@link PerInvocationFailoverStrategy}: when used
 * via {@link FailoverTargetSelector}, a fresh instance is created per
 * top-level invocation through {@link #newStrategy()}, so the shared
 * singleton bean is never mutated and concurrent invocations are fully
 * isolated (CXF-9213).  The per-invocation instance carries its own retry
 * counter as a plain instance field
 *
 * <p>Subclasses that accumulate cross-invocation state (e.g. call counters)
 * should override {@link #newStrategy()} to return a delegate that writes
 * that state back to the shared instance while keeping its own retry counter.
 */
public class RetryStrategy extends SequentialStrategy implements PerInvocationFailoverStrategy {

    private int maxNumberOfRetries;
    private int count;

    @Override
    public List<Endpoint> getAlternateEndpoints(Exchange exchange) {
        return getEndpoints(exchange, stillTheSameAddress(exchange));
    }

    @Override
    protected <T> T getNextAlternate(List<T> alternates) {
        if (!stillTheSameAddress() && !alternates.isEmpty()) {
            alternates.remove(0);
        }
        return alternates.isEmpty() ? null : alternates.get(0);
    }

    /**
     * Exchange-aware variant; delegates to {@link #stillTheSameAddress()} so
     * subclasses may override either form.
     */
    protected boolean stillTheSameAddress(Exchange exchange) {
        return stillTheSameAddress();
    }

    protected boolean stillTheSameAddress() {
        if (maxNumberOfRetries == 0) {
            return true;
        }
        if (++count <= maxNumberOfRetries) {
            return true;
        }
        count = 0;
        return false;
    }

    /**
     * Returns a new {@link RetryStrategy} with the same configuration but a
     * zeroed counter.  Subclasses that need to accumulate state on the shared
     * instance should override this method and return a delegating wrapper.
     */
    @Override
    public FailoverStrategy newStrategy() {
        RetryStrategy copy = new RetryStrategy();
        copy.maxNumberOfRetries = this.maxNumberOfRetries;
        List<String> addresses = getAlternateAddresses(null);
        if (addresses != null) {
            copy.setAlternateAddresses(addresses);
        }
        copy.setDelayBetweenRetries(getDelayBetweenRetries());
        return copy;
    }

    public void setMaxNumberOfRetries(int maxNumberOfRetries) {
        if (maxNumberOfRetries < 0) {
            throw new IllegalArgumentException();
        }
        this.maxNumberOfRetries = maxNumberOfRetries;
    }

    public int getMaxNumberOfRetries() {
        return maxNumberOfRetries;
    }
}
