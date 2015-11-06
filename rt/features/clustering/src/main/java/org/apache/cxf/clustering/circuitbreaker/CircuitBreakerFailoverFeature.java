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

import static org.apache.cxf.clustering.CircuitBreakerTargetSelector.DEFAULT_THESHOLD;
import static org.apache.cxf.clustering.CircuitBreakerTargetSelector.DEFAULT_TIMEOUT;

public class CircuitBreakerFailoverFeature extends FailoverFeature {
    private int threshold;
    private long timeout;
    private FailoverTargetSelector targetSelector;
    
    public CircuitBreakerFailoverFeature() {
        this(DEFAULT_THESHOLD, DEFAULT_TIMEOUT);
    }
    
    public CircuitBreakerFailoverFeature(int threshold, long timeout) {
        this.threshold = threshold;
        this.timeout = timeout;
    }
    
    @Override
    public FailoverTargetSelector getTargetSelector() {
        if (this.targetSelector == null) {
            this.targetSelector = new CircuitBreakerTargetSelector(threshold, timeout);
        }
        return this.targetSelector;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public long getTimeout() {
        return timeout;
    }
}
